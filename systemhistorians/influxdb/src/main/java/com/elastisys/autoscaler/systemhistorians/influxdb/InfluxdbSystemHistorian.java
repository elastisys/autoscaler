package com.elastisys.autoscaler.systemhistorians.influxdb;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;

import java.lang.ref.SoftReference;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.Builder;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.autoscaler.AutoScalerMetadata;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorianFlushException;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.autoscaler.systemhistorians.influxdb.config.InfluxdbSystemHistorianConfig;
import com.elastisys.autoscaler.systemhistorians.influxdb.inserter.impl.HttpInfluxdbInserter;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * A {@link SystemHistorian} that reports {@link AutoScaler} metrics to an
 * InfluxDB server.
 */
public class InfluxdbSystemHistorian implements SystemHistorian<InfluxdbSystemHistorianConfig> {

    /** UUID of {@link AutoScaler} for which metric values are reported. */
    private final UUID autoScalerUuid;
    /** Id/name of {@link AutoScaler} for which metric values are reported. */
    private final String autoScalerId;
    private final Logger logger;
    private final ScheduledExecutorService executor;
    private final EventBus eventBus;

    /** The currently executing influxdb reporting loop. */
    private ScheduledFuture<?> ongoingReportLoop;
    /** The {@link InfluxdbReporter} currently active in the report loop. */
    private InfluxdbReporter influxdbReporter;

    /** Currently set configuration. */
    private InfluxdbSystemHistorianConfig config;

    /**
     * Queue of data points which have not (yet) been reported to InfluxDB. Soft
     * references are used to prevent the queue from growing beyond the JVM's
     * memory allocation. They may be cleared at the discretion of the JVM's
     * garbage collector in response to low memory.
     */
    private Queue<SoftReference<MetricValue>> sendQueue;

    @Inject
    public InfluxdbSystemHistorian(@Named("Uuid") UUID autoScalerUuid, @Named("AutoScalerId") String autoScalerId,
            Logger logger, ScheduledExecutorService executor, EventBus eventBus) {
        this.autoScalerUuid = autoScalerUuid;
        this.autoScalerId = autoScalerId;
        this.logger = logger;
        this.executor = executor;
        this.eventBus = eventBus;

        this.sendQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void validate(InfluxdbSystemHistorianConfig configuration) throws IllegalArgumentException {
        checkArgument(configuration != null, "systemHistorian: configuration cannot be null");
        configuration.validate();
    }

    @Override
    public void configure(InfluxdbSystemHistorianConfig configuration) throws IllegalArgumentException {
        validate(configuration);

        boolean needsRestart = isStarted();
        if (needsRestart) {
            stop();
        }

        this.logger.info("applying new configuration ...");
        this.config = configuration;

        if (needsRestart) {
            start();
        }
    }

    @Override
    public void start() throws IllegalStateException {
        checkState(getConfiguration() != null, "attempt to start before configuring");
        if (isStarted()) {
            this.logger.info(getClass().getSimpleName() + " already started, ignoring request to start.");
            return;
        }

        // register with AutoScaler's event bus to listen for SystemMetricEvents
        this.eventBus.register(this);

        // start influxdb reporting loop
        TimeInterval reportInterval = this.config.getReportingInterval();
        long delay = reportInterval.getTime();
        HttpInfluxdbInserter inserter = new HttpInfluxdbInserter(this.logger, this.config);
        this.influxdbReporter = new InfluxdbReporter(this.logger, inserter, this.sendQueue,
                this.config.getMaxBatchSize());
        this.ongoingReportLoop = this.executor.scheduleWithFixedDelay(() -> flush(), delay, delay,
                reportInterval.getUnit());

        this.logger.info(getClass().getSimpleName() + " started.");
    }

    @Override
    public void stop() {
        if (!isStarted()) {
            this.logger.info(getClass().getSimpleName() + " already stopped, ignoring request to stop.");
            return;
        }

        // unregister from AutoScaler's event bus to stop listening for
        // SystemMetricEvents
        this.eventBus.unregister(this);

        // stop reporting loop
        this.ongoingReportLoop.cancel(false);
        this.ongoingReportLoop = null;
        this.influxdbReporter = null;

        this.logger.info(getClass().getSimpleName() + " stopped.");
    }

    @Override
    public ServiceStatus getStatus() {
        Builder builder = new ServiceStatus.Builder().started(isStarted());
        if (this.influxdbReporter != null) {
            builder.lastFault(this.influxdbReporter.getLastError());
        }
        return builder.build();
    }

    @Override
    public InfluxdbSystemHistorianConfig getConfiguration() {
        return this.config;
    }

    @Override
    public Class<InfluxdbSystemHistorianConfig> getConfigurationClass() {
        return InfluxdbSystemHistorianConfig.class;
    }

    @Override
    public void onEvent(SystemMetricEvent event) {
        // add autoscaler id and uuid to each data point to distinguish values
        // reported by different autoscaler instances
        MetricValue qualifiedEvent = event.getValue()
                .withTags(AutoScalerMetadata.metricTags(this.autoScalerUuid, this.autoScalerId));
        this.logger.debug("received event: {}", qualifiedEvent);

        this.sendQueue.add(new SoftReference<MetricValue>(qualifiedEvent));
    }

    @Override
    public void flush() throws SystemHistorianFlushException {
        ensureStarted();
        this.influxdbReporter.run();
    }

    private void ensureStarted() {
        checkState(isStarted(), "attempt to use system historian before being started");
    }

    private boolean isStarted() {
        return this.ongoingReportLoop != null;
    }

    /**
     * The {@link Queue} of buffered {@link MetricValue}s that will be written
     * to InfluxDB on next {@link #flush()}.
     *
     * @return
     */
    Queue<SoftReference<MetricValue>> sendQueue() {
        return this.sendQueue;
    }
}
