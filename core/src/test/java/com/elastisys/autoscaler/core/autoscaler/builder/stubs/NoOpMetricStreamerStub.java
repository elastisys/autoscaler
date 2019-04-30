package com.elastisys.autoscaler.core.autoscaler.builder.stubs;

import java.util.Collections;
import java.util.List;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamException;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.google.gson.JsonObject;

public class NoOpMetricStreamerStub implements MetricStreamer<JsonObject> {

    private JsonObject config;
    private boolean started;

    @Override
    public void start() throws IllegalStateException {
        this.started = true;
    }

    @Override
    public void stop() {
        this.started = false;
    }

    @Override
    public ServiceStatus getStatus() {
        return new ServiceStatus.Builder().started(this.started).build();
    }

    @Override
    public void validate(JsonObject configuration) throws IllegalArgumentException {

    }

    @Override
    public void configure(JsonObject configuration) throws IllegalArgumentException {
        this.config = configuration;
    }

    @Override
    public JsonObject getConfiguration() {
        return this.config;
    }

    @Override
    public Class<JsonObject> getConfigurationClass() {
        return JsonObject.class;
    }

    @Override
    public MetricStream getMetricStream(String metricStreamId) throws IllegalArgumentException {
        throw new IllegalArgumentException("unrecognized metric stream: " + metricStreamId);
    }

    @Override
    public List<MetricStream> getMetricStreams() {
        return Collections.emptyList();
    }

    @Override
    public void fetch() throws MetricStreamException, IllegalStateException {
    }
}
