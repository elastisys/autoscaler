package com.elastisys.autoscaler.systemhistorians.file;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Named;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.autoscaler.AutoScalerMetadata;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorianFlushException;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.inject.Inject;

/**
 * A {@link SystemHistorian} that writes values to a {@link File} on the local
 * file system.
 */
public class FileStoreSystemHistorian implements SystemHistorian<FileStoreSystemHistorianConfig> {

    /**
     * The UUID of the {@link AutoScaler} instance for which metric values are
     * reported.
     */
    private final UUID autoScalerUuid;
    /**
     * The id/name of the {@link AutoScaler} instance for which metric values
     * are reported.
     */
    private final String autoScalerId;
    private final Logger logger;
    private final EventBus eventBus;

    private FileStoreSystemHistorianConfig config;
    private boolean started;

    private final Lock writeLock = new ReentrantLock(true);

    /**
     * Constructs a new {@link OpenTsdbSystemHistorian}.
     *
     * @param autoScalerId
     *            The id of the {@link AutoScaler} that this instance reports
     *            metric values for.
     * @param logger
     * @param eventBus
     */
    @Inject
    public FileStoreSystemHistorian(@Named("Uuid") UUID autoScalerUuid, @Named("AutoScalerId") String autoScalerId,
            Logger logger, EventBus eventBus) {
        this.autoScalerUuid = autoScalerUuid;
        this.autoScalerId = autoScalerId;
        this.logger = logger;
        this.eventBus = eventBus;

        this.config = null;
        this.started = false;
    }

    @Override
    public void validate(FileStoreSystemHistorianConfig configuration) throws IllegalArgumentException {
        checkArgument(configuration != null, "systemHistorian: configuration cannot be null");
        configuration.validate();
    }

    @Override
    public void configure(FileStoreSystemHistorianConfig configuration) throws IllegalArgumentException {
        validate(configuration);
        this.config = configuration;
    }

    @Override
    public void start() throws IllegalStateException {
        checkState(getConfiguration() != null, "attempt to start before configuring");

        if (isStarted()) {
            this.logger.info(getClass().getSimpleName() + " already started, ignoring request.");
            return;
        }

        // register with AutoScaler's event bus to listen for SystemMetricEvents
        this.eventBus.register(this);

        this.started = true;
        this.logger.info(getClass().getSimpleName() + " started.");
    }

    @Override
    public void stop() {
        if (!isStarted()) {
            this.logger.info(getClass().getSimpleName() + " already stopped, ignoring request.");
            return;
        }

        // unregister from AutoScaler's event bus to stop listening for
        // SystemMetricEvents
        this.eventBus.unregister(this);

        this.started = false;
        this.logger.info(getClass().getSimpleName() + " stopped.");
    }

    @Override
    public ServiceStatus getStatus() {
        return new ServiceStatus.Builder().started(isStarted()).build();
    }

    private boolean isStarted() {
        return this.started;
    }

    @Override
    public FileStoreSystemHistorianConfig getConfiguration() {
        return this.config;
    }

    @Override
    public Class<FileStoreSystemHistorianConfig> getConfigurationClass() {
        return FileStoreSystemHistorianConfig.class;
    }

    @Override
    public void onEvent(SystemMetricEvent event) {
        // add autoScalerId to each data point to distinguish values reported
        // for different AutoScaler instances.
        MetricValue qualifiedEvent = event.getValue()
                .withTags(AutoScalerMetadata.metricTags(this.autoScalerUuid, this.autoScalerId));
        this.logger.trace("received event: {}", qualifiedEvent);
        try {
            this.writeLock.lock();
            String eventAsJson = JsonUtils.toString(JsonUtils.toJson(qualifiedEvent));
            File logFile = getConfiguration().getLog();
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(eventAsJson + "\n");
            } catch (IOException e) {
                throw new RuntimeException("failed to write event: " + e.getMessage(), e);
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public void flush() throws SystemHistorianFlushException {
        // no-op (no buffering of events)
    }
}
