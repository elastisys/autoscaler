package com.elastisys.autoscaler.core.prediction.impl.standard.stubs;

import java.util.Optional;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.prediction.api.PredictionException;
import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.api.types.PredictionUnit;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;

/**
 * A predictor that always predicts the same value. This predictor predicts
 * value in terms of compute units (not in raw capacity). The constant
 * prediction vaule is passed as a configuration parameter named
 * {@code constant.prediction}.
 */
public class ConstantComputeUnitPredictor extends ConstantCapacityPredictor {

    @Inject
    public ConstantComputeUnitPredictor(Logger logger, EventBus eventBus, MonitoringSubsystem monitoringSubsystem) {
        super(logger, eventBus, monitoringSubsystem);
    }

    @Override
    public Optional<Prediction> doPrediction(Optional<PoolSizeSummary> poolSize, DateTime predictionTime)
            throws PredictionException {
        Optional<Prediction> prediction = super.doPrediction(poolSize, predictionTime);
        if (!prediction.isPresent()) {
            return prediction;
        }

        return Optional.of(prediction.get().withUnit(PredictionUnit.COMPUTE));
    }
}
