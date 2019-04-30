package com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.stubs;

import java.util.List;

import org.joda.time.Interval;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.impl.SinglePageResultSet;

/**
 * {@link MetricStreaer} for testing purposes which, when invoked, only returns
 * a pre-specified set of {@link MetricValue}s.
 *
 */
public class MetricStreamStub implements MetricStream {

    private final String id;
    private final String metric;

    /** The pre-specified set of {@link MetricValue}s to return when invoked. */
    private final List<MetricValue> metricValues;

    /**
     * Creates a new {@link MetricFetcherStub}.
     *
     * @param metricValues
     *            The pre-specified set of {@link MetricValue}s to return when
     *            invoked.
     */
    public MetricStreamStub(String id, String metric, List<MetricValue> metricValues) {
        this.id = id;
        this.metric = metric;
        this.metricValues = metricValues;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getMetric() {
        return this.metric;
    }

    @Override
    public QueryResultSet query(Interval timeInterval, QueryOptions options) {
        return new SinglePageResultSet(this.metricValues);
    }

}
