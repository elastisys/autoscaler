package com.elastisys.autoscaler.core.monitoring.impl.standard.config;

public enum MetricStreamerAlias {
    /** Alias for the {@code CloudWatchMetricStreamer} class. */
    CeilometerMetricStreamer("com.elastisys.autoscaler.metricstreamers.ceilometer.CeilometerMetricStreamer"),
    /** Alias for the {@code CloudWatchMetricStreamer} class. */
    CloudWatchMetricStreamer("com.elastisys.autoscaler.metricstreamers.cloudwatch.CloudWatchMetricStreamer"),
    /** Alias for the {@code OpenTsdbMetricStreamer} class. */
    OpenTsdbMetricStreamer("com.elastisys.autoscaler.metricstreamers.opentsdb.OpenTsdbMetricStreamer"),
    /** Alias for the {@code InfluxdbMetricStreamer} class. */
    InfluxdbMetricStreamer("com.elastisys.autoscaler.metricstreamers.influxdb.InfluxdbMetricStreamer"),
    /** Alias for the {@code MetricStreamJoiner} class. */
    MetricStreamJoiner("com.elastisys.autoscaler.metricstreamers.streamjoiner.MetricStreamJoiner");
    /**
     * The fully (package-)qualified class name of the subsystem implementation
     * class that this alias refers to.
     */
    private String qualifiedClassName;

    private MetricStreamerAlias(String qualifiedClassName) {
        this.qualifiedClassName = qualifiedClassName;
    }

    /**
     * Returns the fully (package-)qualified class name of the subsystem
     * implementation class that this alias refers to.
     *
     * @return
     */
    public String getQualifiedClassName() {
        return this.qualifiedClassName;
    }
}
