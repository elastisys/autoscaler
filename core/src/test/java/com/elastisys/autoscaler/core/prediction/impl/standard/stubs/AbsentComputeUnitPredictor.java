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
 * A predictor that always fails to produce a {@link Prediction}. This could,
 * for example, be the case if a {@link Predictor} doesn't have any metric
 * values yet.
 */
public class AbsentComputeUnitPredictor extends PredictorStub {

    @Inject
    public AbsentComputeUnitPredictor(Logger logger, EventBus eventBus, MonitoringSubsystem monitoringSubsystem) {
        super(logger, eventBus, monitoringSubsystem);
    }

    @Override
    public Optional<Prediction> doPrediction(Optional<PoolSizeSummary> poolSize, DateTime predictionTime)
            throws PredictionException {
        return Optional.empty();
    }
}
