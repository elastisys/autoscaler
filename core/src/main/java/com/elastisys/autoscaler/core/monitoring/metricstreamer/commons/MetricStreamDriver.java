package com.elastisys.autoscaler.core.monitoring.metricstreamer.commons;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamException;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * A {@link MetricStreamDriver} is a convenience class that can be used to carry
 * out the heavy lifting for a {@link MetricStreamer} implementation. The
 * {@link MetricStreamDriver} periodically collects metrics for a set of
 * {@link MetricStream}s and publishes any new metric values onto the
 * {@link AutoScaler} {@link EventBus}.
 * <p/>
 * The {@link MetricValue}s are sent to the {@link EventBus} as
 * {@link MetricStreamMessage}s.
 * <p/>
 * Metric values for a given stream are delivered in monotonically increasing
 * time-stamp order to consumers. That is, any already observed values or "late
 * arrivals" that would be delivered out of order are suppressed from delivery.
 */
public class MetricStreamDriver {

    private final Logger logger;
    private final ScheduledExecutorService executor;

    /** The time interval between polling of {@link MetricStream}s. */
    private final TimeInterval pollInterval;

    /** The task that performs metric streaming. Runs in a separate thread. */
    private final MetricStreamingLoop metricStreamingLoop;
    /** Tracks the execution of the ongoing {@link MetricStreamingLoop}. */
    private ScheduledFuture<?> ongoingMetricStreamingLoop;

    /** <code>true</code> if started, <code>false</code> otherwise. */
    private boolean started;

    /**
     * Creates a new {@link MetricStreamDriver}. It is in a passive state until
     * {@link #start()} is called.
     *
     * @param logger
     * @param executor
     *            Task execution service for performing work in separate
     *            threads.
     * @param eventBus
     *            The {@link EventBus} onto which collected metric values are
     *            sent.
     * @param metricStreams
     *            The collection of {@link MetricStream}s for which metrics are
     *            to be collected and published.
     * @param pollInterval
     *            The time interval between polling of {@link MetricStream}s.
     * @param firstQueryLookback
     *            How far back in time to look on the first query to a
     *            {@link MetricStream}. May be <code>null</code>, in which case
     *            {@value #DEFAULT_FIRST_QUERY_LOOKBACK} is used.
     */
    public MetricStreamDriver(Logger logger, ScheduledExecutorService executor, EventBus eventBus,
            List<MetricStream> metricStreams, TimeInterval pollInterval, TimeInterval firstQueryLookback) {
        this.logger = logger;
        this.executor = executor;
        this.pollInterval = pollInterval;

        this.metricStreamingLoop = new MetricStreamingLoop(this.logger, executor, eventBus, metricStreams,
                firstQueryLookback);
        this.started = false;
    }

    /**
     * Starts the the main metric streaming loop, which means that the
     * {@link MetricStreamDriver} will start collecting and publishing metrics
     * for its {@link MetricStream}s.
     */
    public void start() {
        if (isStarted()) {
            this.logger.info(getClass().getSimpleName() + " already started, ignoring request to start");
            return;
        }

        this.ongoingMetricStreamingLoop = this.executor.scheduleWithFixedDelay(this.metricStreamingLoop,
                this.pollInterval.getTime(), this.pollInterval.getTime(), this.pollInterval.getUnit());
        this.started = true;

        this.logger.info(getClass().getSimpleName() + " started.");
    }

    /**
     * Stops the main metric streaming loop, which means that the
     * {@link MetricStreamDriver} will enter a passive state where it stops
     * collecting and publishing metrics for its {@link MetricStream}s.
     */
    public void stop() {
        if (!isStarted()) {
            this.logger.info(getClass().getSimpleName() + " already stopped, ignoring request to stop");
            return;
        }

        // stops all ongoing streaming activities
        if (this.ongoingMetricStreamingLoop != null) {
            this.ongoingMetricStreamingLoop.cancel(true);
            this.ongoingMetricStreamingLoop = null;
        }
        this.started = false;

        this.logger.info(getClass().getSimpleName() + " stopped.");
    }

    /**
     * Returns <code>true</code> if the {@link MetricStreamDriver} has been
     * started.
     *
     * @return
     */
    public boolean isStarted() {
        return this.started;
    }

    /**
     * Returns the {@link MetricStream}s that this {@link MetricStreamDriver}
     * has been set up to manage.
     *
     * @return
     */
    public List<MetricStream> getMetricStreams() {
        return this.metricStreamingLoop.getMetricStreams();
    }

    /**
     * Triggers an immediate metric fetch for the configured
     * {@link MetricStream}s. Any new {@link MetricStream} metric arrivals, are
     * posted in {@link MetricStreamMessage}s on the {@link AutoScaler}
     * {@link EventBus}.
     *
     * @throws MetricStreamException
     * @throws IllegalStateException
     */
    public void fetch() throws MetricStreamException, IllegalStateException {
        ensureStarted();
        this.metricStreamingLoop.run();
    }

    private void ensureStarted() throws IllegalStateException {
        checkState(this.started, "attempt to use metric streamer before being started");
    }
}
