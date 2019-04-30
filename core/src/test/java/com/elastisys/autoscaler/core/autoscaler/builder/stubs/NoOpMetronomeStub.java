package com.elastisys.autoscaler.core.autoscaler.builder.stubs;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.metronome.api.Metronome;

public class NoOpMetronomeStub implements Metronome<Object> {

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

}
