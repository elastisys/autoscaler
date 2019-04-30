package com.elastisys.autoscaler.core.prediction.impl.standard.stubs;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Optional;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.reader.MetricStreamReader;
import com.elastisys.autoscaler.core.prediction.api.PredictionException;
import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.api.types.PredictionUnit;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.predictor.AbstractPredictor;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A predictor that always predicts the same value. The constant prediction
 * vaule is passed as a configuration parameter named
 * {@code constant.prediction}.
 */
public class ConstantCapacityPredictor extends AbstractPredictor {

    @Inject
    public ConstantCapacityPredictor(Logger logger, EventBus eventBus, MonitoringSubsystem monitoringSubsystem) {
        super(logger, eventBus, monitoringSubsystem);
    }

    @Override
    public void validateConfig(PredictorConfig configuration) throws IllegalArgumentException {
        JsonObject jsonParameters = (JsonObject) configuration.getParameters();
        JsonElement constantPrediction = jsonParameters.get("constant.prediction");
        checkArgument(constantPrediction != null, "parameters missing constant.prediction field");
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
        int constantPrediction = getConstantPrediction(getConfiguration());
        String metricName = getConfiguration().getMetricStream();
        this.logger.debug(String.format("predicting that value will be %d for metric '%s' at time %s",
                constantPrediction, metricName, predictionTime.toString()));
        Prediction prediction = new Prediction(constantPrediction, PredictionUnit.METRIC, metricName, predictionTime);
        return Optional.of(prediction);
    }

    private int getConstantPrediction(PredictorConfig config) {
        JsonObject jsonParameters = (JsonObject) config.getParameters();
        JsonElement constantPrediction = jsonParameters.get("constant.prediction");
        return constantPrediction.getAsInt();
    }

}
