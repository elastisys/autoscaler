package com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.stubs;

/**
 * Dummy metric stream definition for a {@link MetricStreamerStubConfig}.
 *
 * @see MetricStreamerStubConfig
 */
public class MetricStreamerStubStreamDefinition {

    /** The stream's id. */
    private final String id;
    /** The metric being fetched by the stream. */
    private final String metric;
    /** Aggregator function to apply to data points. */
    private final String aggregator;

    /**
     * Creates a {@link MetricStreamerStubStreamDefinition}
     *
     * @param id
     *            The stream's id.
     * @param metric
     *            The metric being fetched by the stream.
     * @param aggregator
     *            Aggregator function to apply to data points.
     */
    public MetricStreamerStubStreamDefinition(String id, String metric, String aggregator) {
        this.id = id;
        this.metric = metric;
        this.aggregator = aggregator;
    }

    public String getId() {
        return this.id;
    }

    public String getMetric() {
        return this.metric;
    }

    public String getAggregator() {
        return this.aggregator;
    }
}
