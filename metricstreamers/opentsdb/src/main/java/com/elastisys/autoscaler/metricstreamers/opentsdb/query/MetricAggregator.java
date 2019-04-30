package com.elastisys.autoscaler.metricstreamers.opentsdb.query;

import com.elastisys.autoscaler.core.api.types.MetricValue;

/**
 * The available set of {@link MetricValue} aggregation functions supported by
 * OpenTSDB to aggregate time-series values for a given metric.
 * <p/>
 * See the <a href="http://opentsdb.net/query-execution.html">OpenTSDB
 * manual</a> to learn about metric aggregation functions and how OpenTSDB
 * aggregates values for different time-series.
 */
public enum MetricAggregator {
    MIN, MAX, AVG, SUM;
}
