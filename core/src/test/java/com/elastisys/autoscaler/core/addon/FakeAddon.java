package com.elastisys.autoscaler.core.addon;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;

public class FakeAddon implements Service<FakeAddonConfig> {

    private FakeAddonConfig config;

    private boolean started = false;

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
        return new ServiceStatus(this.started ? State.STARTED : State.STOPPED, ServiceStatus.Health.OK);
    }

    @Override
    public void validate(FakeAddonConfig configuration) throws IllegalArgumentException {
    }

    @Override
    public void configure(FakeAddonConfig configuration) throws IllegalArgumentException {
        this.config = configuration;
    }

    @Override
    public FakeAddonConfig getConfiguration() {
        return this.config;
    }

    @Override
    public Class<FakeAddonConfig> getConfigurationClass() {
        return FakeAddonConfig.class;
    }

}
