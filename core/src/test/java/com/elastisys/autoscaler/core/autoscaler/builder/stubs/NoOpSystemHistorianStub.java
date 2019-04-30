package com.elastisys.autoscaler.core.autoscaler.builder.stubs;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.google.gson.JsonObject;

public class NoOpSystemHistorianStub implements SystemHistorian<JsonObject> {

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
    public void onEvent(SystemMetricEvent event) {
    }

    @Override
    public void flush() {
    }
}
