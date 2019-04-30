package com.elastisys.autoscaler.systemhistorians.opentsdb;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.Builder;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.autoscaler.AutoScalerMetadata;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorianFlushException;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.autoscaler.systemhistorians.opentsdb.config.OpenTsdbSystemHistorianConfig;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;
import com.elastisys.scale.commons.util.precond.Preconditions;

/**
 * A {@link SystemHistorian} implementation that uses OpenTSDB as its backend.
 */
public class OpenTsdbSystemHistorian implements SystemHistorian<OpenTsdbSystemHistorianConfig> {

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
    private final ScheduledExecutorService executorService;
    private final EventBus eventBus;

    /** A queue of {@link MetricValue}s to be reported to OpenTSDB. */
    private final ConcurrentLinkedQueue<MetricValue> unreportedDatapoints;
    /** {@link Runnable} task that pushes queued data points to OpenTSDB. */
    private DataPointPusher pusher;
    /** The currently executing pusher task. */
    private ScheduledFuture<?> ongoingPusher;
    /** The configuration currently set. */
    private OpenTsdbSystemHistorianConfig configuration;

    /**
     * Constructs a new {@link OpenTsdbSystemHistorian}.
     *
     * @param autoScalerId
     *            The id of the {@link AutoScaler} that this instance reports
     *            metric values for.
     * @param logger
     * @param executorService
     * @param eventBus
     */
    @Inject
    public OpenTsdbSystemHistorian(@Named("Uuid") UUID autoScalerUuid, @Named("AutoScalerId") String autoScalerId,
            Logger logger, ScheduledExecutorService executorService, EventBus eventBus) {
        this.autoScalerUuid = autoScalerUuid;
        this.autoScalerId = autoScalerId;
        this.logger = logger;
        this.executorService = executorService;
        this.eventBus = eventBus;

        this.unreportedDatapoints = new ConcurrentLinkedQueue<>();
        this.pusher = null;
        this.configuration = null;

    }

    @Override
    public void validate(OpenTsdbSystemHistorianConfig configuration) throws IllegalArgumentException {
        Preconditions.checkArgument(configuration != null, "systemHistorian: configuration cannot be null");
        configuration.validate();
    }

    @Override
    public synchronized void configure(OpenTsdbSystemHistorianConfig configuration) throws IllegalArgumentException {
        validate(configuration);

        OpenTsdbSocketInserter newInserter = new OpenTsdbSocketInserter(this.logger, configuration.getOpenTsdbHost(),
                configuration.getOpenTsdbPort());
        DataPointPusher newPusher = new DataPointPusher(this.logger, newInserter, this.unreportedDatapoints);
        if (isStarted()) {
            stop();
            this.configuration = configuration;
            this.pusher = newPusher;
            start();
        } else {
            this.configuration = configuration;
            this.pusher = newPusher;
        }
    }

    @Override
    public synchronized void start() {
        checkState(getConfiguration() != null, "attempt to start before configuring");

        if (isStarted()) {
            this.logger.info(getClass().getSimpleName() + " already started, ignoring request.");
            return;
        }

        // register with AutoScaler's event bus to listen for SystemMetricEvents
        this.eventBus.register(this);

        long interval = getConfiguration().getPushInterval().getSeconds();
        this.ongoingPusher = this.executorService.scheduleWithFixedDelay(() -> flush(), interval, interval,
                TimeUnit.SECONDS);
        this.logger.info(getClass().getSimpleName() + " started.");
    }

    /**
     * Returns <code>true</code> if this {@link Service} is running.
     *
     * @return
     */
    private boolean isStarted() {
        return this.ongoingPusher != null;
    }

    @Override
    public synchronized void stop() {
        if (!isStarted()) {
            this.logger.info(getClass().getSimpleName() + " already stopped, ignoring request.");
            return;
        }

        // unregister from AutoScaler's event bus to stop listening for
        // SystemMetricEvents
        this.eventBus.unregister(this);

        // Request cancellation, but let ongoing tasks finish. If a task has
        // been scheduled, but does not currently run, it will simply never be
        // allowed to run.
        this.ongoingPusher.cancel(false);
        this.ongoingPusher = null;
        this.logger.info(getClass().getSimpleName() + " stopped.");
    }

    @Override
    public ServiceStatus getStatus() {
        Builder builder = new ServiceStatus.Builder().started(isStarted());
        if (this.pusher != null) {
            builder.lastFault(this.pusher.getLastError());
        }
        return builder.build();
    }

    @Override
    public OpenTsdbSystemHistorianConfig getConfiguration() {
        return this.configuration;
    }

    @Override
    public Class<OpenTsdbSystemHistorianConfig> getConfigurationClass() {
        return OpenTsdbSystemHistorianConfig.class;
    }

    @Override
    @Subscriber
    public void onEvent(SystemMetricEvent event) {
        // add autoScalerId to each data point to distinguish values reported
        // for different AutoScaler instances.
        MetricValue qualifiedEvent = event.getValue()
                .withTags(AutoScalerMetadata.metricTags(this.autoScalerUuid, this.autoScalerId));
        this.logger.debug("received event: {}", qualifiedEvent);
        this.unreportedDatapoints.add(qualifiedEvent);
    }

    @Override
    public void flush() throws SystemHistorianFlushException {
        ensureStarted();
        this.pusher.run();
    }

    private void ensureStarted() {
        checkState(isStarted(), "attempt to use system historian before being started");
    }

    int getDeliveredDataPoints() {
        DataPointPusher dataPointPusher = this.pusher;
        if (dataPointPusher == null) {
            return 0;
        }
        return dataPointPusher.getDeliveredDataPoints();
    }

    /**
     * A {@link Runnable} task that, when executed, pushes data points to a
     * OpenTSDB server.
     */
    private class DataPointPusher implements Runnable {
        /** {@link Logger} instance. */
        private final Logger logger;
        /** The {@link OpenTsdbInserter} used to push data points. */
        private final OpenTsdbInserter opentsdbInserter;
        /**
         * The queue of data points that are to be reported. Will be populated
         * by a separate thread (controlled by the creator of this task).
         */
        private final Queue<MetricValue> unreportedDatapoints;
        private Optional<? extends Throwable> lastError;
        /** Running count of delivered data points. */
        private int deliveredDataPoints = 0;
        /** Lock to prevent concurrent writes. */
        private final Lock lock = new ReentrantLock();

        /**
         * Creates a new {@link DataPointPusher}.
         *
         * @param logger
         *            The logger to use for logging from this instance.
         * @param opentsdbInserter
         *            The {@link OpenTsdbInserter} used to push data points.
         * @param unreportedDatapoints
         *            The queue of data points that are to be reported. The
         *            {@link DataPointPusher} will use this queue as-is to
         *            consume new values as they enter the queue. That is, the
         *            invoking client may push additional values onto the queue
         *            and have them reported the next time this task is run.
         */
        public DataPointPusher(Logger logger, OpenTsdbInserter opentsdbInserter,
                ConcurrentLinkedQueue<MetricValue> unreportedDatapoints) {
            requireNonNull(logger, "logger is null");
            requireNonNull(opentsdbInserter, "OpenTSDB inserter is null");
            requireNonNull(unreportedDatapoints, "data point queue is null");

            this.logger = logger;
            this.opentsdbInserter = opentsdbInserter;
            this.unreportedDatapoints = unreportedDatapoints;
            this.lastError = Optional.empty();
        }

        @Override
        public void run() {
            this.lastError = Optional.empty();

            try {
                this.lock.lock();
                // defensive copy of datapoints, since it can change at any
                // time.
                List<MetricValue> pointsToInsert = new ArrayList<>(this.unreportedDatapoints);
                if (pointsToInsert.isEmpty()) {
                    return; // nothing to do
                }

                List<MetricValue> successfullyInserted = new ArrayList<>();
                for (MetricValue dataPoint : pointsToInsert) {
                    try {
                        this.opentsdbInserter.insert(dataPoint);
                        successfullyInserted.add(dataPoint);
                    } catch (Exception e) {
                        this.logger.error(format("failed to push data" + " point to OpenTSDB: %s", e.getMessage()), e);
                    }
                }
                this.unreportedDatapoints.removeAll(successfullyInserted);

                // check that all data points were properly inserted
                if (!successfullyInserted.equals(pointsToInsert)) {
                    throw new IOException(String.format("only inserted %d of %d data points",
                            successfullyInserted.size(), pointsToInsert.size()));
                }
                this.logger.debug(String.format("successfully pushed %d data points", successfullyInserted.size()));
                this.deliveredDataPoints += successfullyInserted.size();
            } catch (Exception e) {
                this.logger.error("failed to post data points to OpenTSDB: " + e.getMessage(), e);
                this.lastError = Optional.of(e);
            } finally {
                this.lock.unlock();
            }
        }

        /**
         * @return The error of the previous execution, if any.
         */
        public Optional<? extends Throwable> getLastError() {
            return this.lastError;
        }

        int getDeliveredDataPoints() {
            return this.deliveredDataPoints;
        }

    }
}
