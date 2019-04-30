package com.elastisys.autoscaler.core.prediction.impl.standard.stubs;

import java.util.Optional;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.prediction.api.PredictionException;
import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;

/**
 * A stubbed {@link Predictor}, intended for test use, that always fails (throws
 * a runtime exception) when asked to make a prediction.
 */
public class FailingPredictor extends ConstantCapacityPredictor {

    @Inject
    public FailingPredictor(Logger logger, EventBus eventBus, MonitoringSubsystem monitoringSubsystem) {
        super(logger, eventBus, monitoringSubsystem);
    }

    @Override
    public Optional<Prediction> doPrediction(Optional<PoolSizeSummary> poolSize, DateTime predictionTime)
            throws PredictionException {
        throw new NullPointerException("Does not compute");
    }
}
