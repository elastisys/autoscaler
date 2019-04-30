package com.elastisys.autoscaler.core.prediction.impl.standard.stubs;

import java.util.Optional;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.reader.MetricStreamReader;
import com.elastisys.autoscaler.core.prediction.api.PredictionException;
import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.predictor.AbstractPredictor;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;

public class PredictorStub extends AbstractPredictor {

    @Inject
    public PredictorStub(Logger logger, EventBus eventBus, MonitoringSubsystem monitoringSubsystem) {
        super(logger, eventBus, monitoringSubsystem);
    }

    @Override
    public void validateConfig(PredictorConfig configuration) throws IllegalArgumentException {
    }

    @Override
    public void applyConfig(PredictorConfig newConfig) throws IllegalArgumentException {
    }

    @Override
    public void onStart(MetricStreamReader reader) {
    }

    @Override
    public void onStop() {
    }

    @Override
    public Optional<Prediction> doPrediction(Optional<PoolSizeSummary> poolSize, DateTime predictionTime)
            throws PredictionException {
        return Optional.empty();
    }
}
