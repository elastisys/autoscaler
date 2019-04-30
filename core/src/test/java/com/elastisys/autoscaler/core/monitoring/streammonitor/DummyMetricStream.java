package com.elastisys.autoscaler.core.monitoring.streammonitor;

import org.joda.time.Interval;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.impl.EmptyResultSet;

/**
 * Dummy {@link MetricStream} implementation used in test.
 */
class DummyMetricStream implements MetricStream {

    private String metric;

    public DummyMetricStream(String metricName) {
        this.metric = metricName;
    }

    @Override
    public String getId() {
        return this.metric + ".stream";
    }

    @Override
    public String getMetric() {
        return this.metric;
    }

    @Override
    public QueryResultSet query(Interval timeInterval, QueryOptions options) {
        return new EmptyResultSet();
    }
}