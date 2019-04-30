package com.elastisys.autoscaler.core.monitoring.systemhistorian.impl.noop;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorianFlushException;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.google.gson.JsonObject;

/**
 * A no-op {@link SystemHistorian} implementation.
 */
public class NoOpSystemHistorian implements SystemHistorian<JsonObject> {

    private final Logger logger;

    private JsonObject config;
    private boolean started;

    @Inject
    public NoOpSystemHistorian(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void start() throws IllegalStateException {
        checkState(this.config != null, "NoOpSystemHistorian: cannot start before being configured");
        this.started = true;
        this.logger.debug("{} started.", getClass().getSimpleName());
    }

    @Override
    public void stop() {
        this.started = false;
        this.logger.debug("{} stopped.", getClass().getSimpleName());
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
        // no-op
    }

    @Override
    public void flush() throws SystemHistorianFlushException {
        // no-op
    }
}
