package com.elastisys.autoscaler.core.autoscaler.builder.stubs;

import java.util.Optional;

import org.joda.time.DateTime;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.prediction.api.PredictionException;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;

public class NoOpPredictionSubsystemStub implements PredictionSubsystem<Object> {

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
    public Optional<Integer> predict(Optional<PoolSizeSummary> poolSize, DateTime predictionTime)
            throws PredictionException {
        return Optional.empty();
    }

}
