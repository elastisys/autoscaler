package com.elastisys.autoscaler.core.prediction.impl.standard;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetric;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Posts values for different {@link SystemMetric} time-series to the
 * {@link AutoScaler} {@link EventBus}.
 *
 * @see StandardPredictionSubsystem
 */
public class SystemMetricPoster {

    /**
     * The {@link AutoScaler} {@link EventBus} on which to post
     * {@link SystemMetricEvent}s.
     */
    private final EventBus eventBus;

    @Inject
    public SystemMetricPoster(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Posts a collection of capacity predictions (either expressed in raw
     * metric unit or in compute units) on the {@link AutoScaler}
     * {@link EventBus}.
     *
     * @param predictionResults
     */
    public void postPredictionResults(Map<Predictor, Optional<Prediction>> predictionResults) {
        for (Predictor predictor : predictionResults.keySet()) {
            Optional<Prediction> result = predictionResults.get(predictor);
            if (!result.isPresent()) {
                continue;
            }
            Prediction prediction = result.get();
            Map<String, String> tags = Maps.of(//
                    "predictor", predictor.getConfiguration().getId(), //
                    "metric", prediction.getMetric(), //
                    "unit", prediction.getUnit().name());
            MetricValue value = new MetricValue(SystemMetric.PREDICTION.getMetricName(), prediction.getValue(),
                    UtcTime.now(), tags);
            postSystemMetric(value);
        }
    }

    /**
     * Posts a <i>aggregate prediction</i> on the {@link AutoScaler}
     * {@link EventBus}.
     *
     * @param aggregatePrediction
     */
    public void postAggregatePrediction(Optional<Double> aggregatePrediction) {
        if (!aggregatePrediction.isPresent()) {
            return;
        }
        Map<String, String> tags = Maps.of();
        postSystemMetric(new MetricValue(SystemMetric.AGGREGATE_PREDICTION.getMetricName(), aggregatePrediction.get(),
                UtcTime.now(), tags));
    }

    /**
     * Posts a <i>bounded prediction</i> on the {@link AutoScaler}
     * {@link EventBus}.
     *
     * @param boundedPrediction
     */
    public void postBoundedPrediction(Optional<Integer> boundedPrediction) {
        if (!boundedPrediction.isPresent()) {
            return;
        }
        Map<String, String> tags = Maps.of();
        postSystemMetric(new MetricValue(SystemMetric.BOUNDED_PREDICTION.getMetricName(), boundedPrediction.get(),
                UtcTime.now(), tags));
    }

    /**
     * Posts a {@link MetricValue} on the {@link AutoScaler} {@link EventBus}
     * for later handling by the {@link SystemHistorian}.
     *
     * @param value
     */
    private void postSystemMetric(MetricValue value) {
        this.eventBus.post(new SystemMetricEvent(value));
    }
}
