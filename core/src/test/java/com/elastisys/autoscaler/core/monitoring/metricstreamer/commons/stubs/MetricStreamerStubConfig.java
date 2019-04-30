package com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.stubs;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * A dummy configuration for a {@link MetricStreamerStub}.
 *
 * @see MetricStreamerStub
 */
public class MetricStreamerStubConfig {

    private final String host;
    private final int port;
    private final List<MetricStreamerStubStreamDefinition> metricStreams;

    public MetricStreamerStubConfig(String host, int port, int pollIntervalInSeconds,
            List<MetricStreamerStubStreamDefinition> metricStreams) {
        this.host = host;
        this.port = port;
        this.metricStreams = new ArrayList<>();
        this.metricStreams.addAll(metricStreams);
    }

    public void validate() throws IllegalArgumentException {
        try {
            checkArgument(this.host != null, "missing host");
            checkArgument(this.port > 0, "port number must be positive");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("metricStreamer: " + e.getMessage(), e);
        }
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public List<MetricStreamerStubStreamDefinition> getMetricStreams() {
        return this.metricStreams;
    }

    /**
     * Adds a {@link MetricStreamDefinition} to this
     * {@link MetricStreamerStubConfig}.
     *
     * @param streamDefinition
     * @return
     */
    public MetricStreamerStubConfig withMetricStream(MetricStreamerStubStreamDefinition streamDefinition) {
        this.metricStreams.add(streamDefinition);
        return this;
    }

}
