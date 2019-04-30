package com.elastisys.autoscaler.predictors.reactive;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.reader.MetricStreamReader;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetric;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.autoscaler.core.prediction.api.PredictionException;
import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.api.types.PredictionUnit;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.predictor.AbstractPredictor;
import com.elastisys.autoscaler.predictors.reactive.config.ReactivePredictorParams;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * A naive {@link Predictor} that simply uses the latest observed metric value
 * as the predicted future value (using a "tomorrow will probably be very
 * similar to today"-style heuristic) combined with adding some optional safety
 * margin to try and stay "above the load curve".
 * <p/>
 * To keep some margin to the load curve a {@code safetyMargin} can optionally
 * be set in the {@link ReactivePredictorParams}, which will add some extra
 * padding to every prediction
 * <p/>
 * If metric values are sparse, a sufficiently long lookbackWindow should be
 * specified so that the predictor can initialize on start-up.
 */
public class ReactivePredictor extends AbstractPredictor {

    /**
     * Event bus onto which {@link Alert}s and {@link SystemMetricEvent}s can be
     * posted.
     */
    private final EventBus eventBus;

    /** The {@link MetricStreamReader} from which metric values are read. */
    private MetricStreamReader metricReader;

    /**
     * The latest observed metric value, which is used as the prediction for
     * coming metric values.
     */
    private MetricValue lastReading;

    /** The currently set parameters. */
    private ReactivePredictorParams params;

    @Inject
    public ReactivePredictor(Logger logger, EventBus eventBus, MonitoringSubsystem monitoringSubsystem) {
        super(logger, eventBus, monitoringSubsystem);
        this.eventBus = eventBus;

        this.lastReading = null;
        this.params = null;
    }

    @Override
    public void validateConfig(PredictorConfig configuration) throws IllegalArgumentException {
        try {
            // validate predictor-specific part of configuration
            ReactivePredictorParams parameters = effectiveParameters(configuration);
            parameters.validate();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("predictor %s: %s", configuration.getId(), e.getMessage()),
                    e);
        }
    }

    @Override
    public void applyConfig(PredictorConfig newConfig) throws IllegalArgumentException {
        validateConfig(newConfig);

        this.params = effectiveParameters(newConfig);
        if (newConfig.getParameters() == null) {
            this.logger.debug("no predictor parameters given, using defaults: {}", this.params);
        }
    }

    @Override
    public void onStart(MetricStreamReader metricReader) {
        this.metricReader = metricReader;
        metricReader.start();
    }

    @Override
    public void onStop() {
        this.metricReader.stop();
    }

    @Override
    public Optional<Prediction> doPrediction(Optional<PoolSizeSummary> poolSize, DateTime predictionTime)
            throws PredictionException {
        if (poolSize.isPresent()) {
            int desiredSize = poolSize.get().getDesiredSize();
            this.logger.debug(format("desired pool size: %d", desiredSize));
            this.logger.debug(format("active pool size: %d", poolSize.get().getActive()));
        }

        this.lastReading = getLastMetricReading();
        if (this.lastReading == null) {
            this.logger.warn("no metric value has been read yet from metric stream '{}', cannot make prediction.",
                    this.metricReader.getMetricStream().getId());
            return Optional.empty();
        }

        this.logger.debug("latest metric value reading is: {}", this.lastReading);
        double load = this.lastReading.getValue();
        String metric = this.metricReader.getMetricStream().getMetric();
        reportLoadObservation(metric, load);

        double prediction = marginPaddedPrediction(load);
        return Optional.of(new Prediction(prediction, PredictionUnit.METRIC, metric, predictionTime));
    }

    private double marginPaddedPrediction(double load) {
        double marginCoefficient = 1.0 + this.params.getSafetyMargin() / 100.0;
        return load * marginCoefficient;
    }

    private MetricValue getLastMetricReading() {
        List<MetricValue> newReadings = new ArrayList<>();
        this.metricReader.popTo(newReadings);

        if (newReadings.isEmpty()) {
            // no new readings this iteration, return latest reading
            this.logger.debug("no new metric values received");
            return this.lastReading;
        }

        MetricValue latestReading = newReadings.get(newReadings.size() - 1);
        this.logger.debug("new metric value reading: {}", latestReading);
        return latestReading;
    }

    /**
     * Pushes a load observation event for the {@link Predictor}'s metric onto
     * the {@link AutoScaler} event bus to have the {@link SystemHistorian}
     * record the observation.
     *
     * @param metric
     *            The metric for which the load observation was made
     * @param load
     *            The load observation.
     */
    private void reportLoadObservation(String metric, double load) {
        try {
            String systemMetric = SystemMetric.CURRENT_LOAD.getMetricName();
            Map<String, String> tags = new HashMap<>();
            tags.put("predictor", getConfiguration().getId());
            tags.put("metric", metric);
            MetricValue dataPoint = new MetricValue(systemMetric, load, UtcTime.now(), tags);
            this.eventBus.post(new SystemMetricEvent(dataPoint));
        } catch (Exception e) {
            this.logger.error(
                    String.format("failed to push current load " + "observation onto event bus: %s", e.getMessage()),
                    e);
        }
    }

    /**
     * Returns the {@link ReactivePredictorParams} associated with a given
     * {@link PredictorConfig}, or returns a default
     * {@link ReactivePredictorParams} in case no parameters were set.
     *
     * @param parametersAsJson
     * @return
     */
    private ReactivePredictorParams effectiveParameters(PredictorConfig config) {
        if (config.getParameters() == null) {
            return ReactivePredictorParams.DEFAULT;
        }
        return ReactivePredictorParams.parse(config.getParameters());
    }
}
