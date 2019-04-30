package com.elastisys.autoscaler.core.autoscaler.builder.stubs;

import com.elastisys.autoscaler.core.alerter.api.Alerter;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.scale.commons.net.alerter.Alert;

public class NoOpAlerterStub implements Alerter<Object> {

    @Override
    public void start() throws IllegalStateException {
    }

    @Override
    public void stop() {
    }

    @Override
    public ServiceStatus getStatus() {
        return null;
    }

    @Override
    public void validate(Object configuration) throws IllegalArgumentException {

    }

    @Override
    public void configure(Object configuration) throws IllegalArgumentException {

    }

    @Override
    public Object getConfiguration() {
        return null;
    }

    @Override
    public Class<Object> getConfigurationClass() {
        return null;
    }

    @Override
    public void onAlert(Alert alert) {

    }

}
