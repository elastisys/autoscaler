package com.elastisys.autoscaler.core.monitoring.metricstreamer.api;

import org.joda.time.Interval;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.scale.commons.eventbus.EventBus;

/**
 * Represents a particular metric being collected by a {@link MetricStreamer}
 * and published onto the {@link AutoScaler} {@link EventBus}.
 * <p/>
 * A {@link MetricStream} can also be queried for historical data. Queries for
 * historical data can, for example, be executed by a predictor that needs
 * week-old data to build its internal data structures on startup.
 *
 * @see MetricStreamer
 */
public interface MetricStream {
    /**
     * Returns the identifier of the {@link MetricStream}, used in
     * {@link MetricStreamMessage} sent on the {@link EventBus} to identify
     * {@link MetricValue}s belonging to this {@link MetricStream}.
     *
     * @return
     */
    public String getId();

    /**
     * Returns the name of the metric being collected by this
     * {@link MetricStream}.
     * <p/>
     * {@link MetricStreamMessage}s published with this {@link MetricStream}'s
     * id will only contain {@link MetricValue}s of this kind.
     *
     * @return The metric being collected by this {@link MetricStream}.
     */
    public String getMetric();

    /**
     * Queries a {@link MetricStream} for historical {@link MetricValue}s.
     * <p/>
     * Note that implementations may choose to fetch results lazily, which means
     * that a query may not be executed until {@link QueryResultSet#fetchNext()}
     * is called.
     *
     * @param timeInterval
     *            Limits the query in time. No {@link MetricValue} with a
     *            timestamp outside of this interval will be fetched.
     * @param options
     *            Query option hints which the {@link MetricStream}
     *            implementation may choose to ignore depending on its
     *            capabilities. May be <code>null</code>, which means the
     *            {@link MetricStream}s is free to execute the query in any
     *            manner.
     * @return A {@link QueryResultSet} which can be used to iterate through the
     *         retrieved {@link MetricValue}s.
     * @throws MetricStreamException
     *             If the query could not be executed.
     */
    public QueryResultSet query(Interval timeInterval, QueryOptions options) throws MetricStreamException;
}
