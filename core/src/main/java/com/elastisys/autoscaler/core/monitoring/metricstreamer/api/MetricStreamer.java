package com.elastisys.autoscaler.core.monitoring.metricstreamer.api;

import java.util.List;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.metronome.api.MetronomeEvent;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.MetricStreamDriver;
import com.elastisys.scale.commons.eventbus.EventBus;

/**
 * A {@link MetricStreamer} is a component of the {@link MonitoringSubsystem}
 * responsible for periodically obtaining metric values from a metric source
 * (such as a time-series database) and streaming those metric values to a set
 * of interested subscribers via {@link MetricStreamMessage}s sent over the
 * {@link AutoScaler} {@link EventBus}.
 * <p/>
 * A {@link MetricStreamer} can collect several different metrics from its
 * source. Each such collected metric forms a separate {@link MetricStream},
 * whose values are broadcast (with a unique identifier) onto the
 * {@link AutoScaler} {@link EventBus} where interested parties can consume
 * streamed metric values.
 * <p/>
 * A {@link MetricStream} can also be queried for historical data. Queries for
 * historical data can, for example, be a predictor that needs week-old data to
 * build its internal data structures.
 * <p/>
 * A {@link MetricStreamer} implementation must itself make sure to have the
 * {@link EventBus} injected and set itself up for periodical execution.
 * Implementation may choose to make use of the {@link MetricStreamDriver}
 * convenience class, which takes care of most heavy lifting.
 * <p/>
 * A {@link MetricStreamer} may also trigger a new resize iteration (a new round
 * of predictions and a cloudpool resize) by sending a
 * {@link MetronomeEvent#RESIZE_ITERATION} over the {@link AutoScaler}
 * {@link EventBus}.
 *
 * @see MonitoringSubsystem
 * @see MetricStream
 *
 * @see MetricStreamDriver
 *
 * @param <T>
 *            The configuration class to use for configuring instances of
 *            implementation classes.
 */
public interface MetricStreamer<T> extends Service<T> {

    /**
     * Returns the list of {@link MetricStream}s being published by this
     * {@link MetricStreamer}.
     *
     * @return
     */
    List<MetricStream> getMetricStreams();

    /**
     * Returns a {@link MetricStream} with a particular stream id. If the
     * {@link MetricStream} does not exist, an {@link IllegalArgumentException}
     * is thrown.
     *
     * @return
     */
    MetricStream getMetricStream(String metricStreamId) throws IllegalArgumentException;

    /**
     * Triggers an immediate metric fetch, which causes the
     * {@link MetricStreamer} to retrieve new metrics for all
     * {@link MetricStream}s it has been configured to collect. For any
     * {@link MetricStream} with new metric arrivals, a
     * {@link MetricStreamMessage} is to be posted on the {@link AutoScaler}
     * {@link EventBus}.
     *
     * @throws MetricStreamException
     *             If metric retrieval failed.
     * @throws IllegalStateException
     *             If the {@link MetricStreamer} is not yet started.
     */
    void fetch() throws MetricStreamException, IllegalStateException;
}
