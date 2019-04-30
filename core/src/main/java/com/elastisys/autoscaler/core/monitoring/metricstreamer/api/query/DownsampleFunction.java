package com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;

/**
 * Functions that can be used to {@link Downsample} {@link MetricStream} query
 * results.
 *
 * @see Downsample
 */
public enum DownsampleFunction {
    MIN, MAX, SUM, MEAN;
}
