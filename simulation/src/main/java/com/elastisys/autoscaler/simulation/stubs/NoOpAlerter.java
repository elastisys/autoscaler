package com.elastisys.autoscaler.simulation.stubs;

import com.elastisys.autoscaler.core.alerter.api.Alerter;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.simulation.stubs.NoOpAlerter.NoOpAlerterConfig;
import com.elastisys.scale.commons.eventbus.Subscriber;
import com.elastisys.scale.commons.net.alerter.Alert;

public class NoOpAlerter implements Alerter<NoOpAlerterConfig> {

    private boolean started = false;
    private NoOpAlerterConfig config = null;

    @Override
    public void start() {
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
    public void validate(NoOpAlerterConfig configuration) throws IllegalArgumentException {
    }

    @Override
    public void configure(NoOpAlerterConfig configuration) throws IllegalArgumentException {
        this.config = configuration;
    }

    @Override
    public NoOpAlerterConfig getConfiguration() {
        return this.config;
    }

    @Override
    public Class<NoOpAlerterConfig> getConfigurationClass() {
        return NoOpAlerterConfig.class;
    }

    @Override
    @Subscriber
    public void onAlert(Alert alertMessage) {
        // noop
    }

    public static class NoOpAlerterConfig {
    }

}
