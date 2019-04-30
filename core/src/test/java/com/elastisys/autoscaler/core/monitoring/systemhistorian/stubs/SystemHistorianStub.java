package com.elastisys.autoscaler.core.monitoring.systemhistorian.stubs;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;

/**
 * A {@link SystemHistorian} implementation intended for testing purposes.
 */
public class SystemHistorianStub implements SystemHistorian<SystemHistorianStubConfig> {

    private SystemHistorianStubConfig config;
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
    public void validate(SystemHistorianStubConfig configuration) throws IllegalArgumentException {
        configuration.validate();
    }

    @Override
    public void configure(SystemHistorianStubConfig configuration) throws IllegalArgumentException {
        this.config = configuration;
    }

    @Override
    public SystemHistorianStubConfig getConfiguration() {
        return this.config;
    }

    @Override
    public Class<SystemHistorianStubConfig> getConfigurationClass() {
        return SystemHistorianStubConfig.class;
    }

    @Override
    public void onEvent(SystemMetricEvent event) {
        // do nothing
    }

    @Override
    public void flush() {
        // do nothing
    }
}
