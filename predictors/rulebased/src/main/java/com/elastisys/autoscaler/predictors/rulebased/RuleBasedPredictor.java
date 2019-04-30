package com.elastisys.autoscaler.predictors.rulebased;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
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
import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.BasicTimeSeries;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.MaxAgeTimeSeries;
import com.elastisys.autoscaler.predictors.rulebased.config.RuleBasedPredictorParams;
import com.elastisys.autoscaler.predictors.rulebased.rule.ScalingRule;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * A compute unit predictor that suggests capacity changes from a collection of
 * {@link ScalingRule}s, defining thresholds for the monitored metric and what
 * scaling actions to take when those thresholds are breached.
 * <p/>
 * When a threshold has been exceeded (for a sufficiently long time), causing a
 * {@link ScalingRule} to fire, the {@link RuleBasedPredictor} will enter a
 * "cool-down period" in order for the scaling decision to get some time to take
 * effect. During this cool-down period, no new {@link ScalingRule}s will be
 * allowed to fire, and the {@link RuleBasedPredictor} will just respond with
 * its latest "prediction".
 * <p/>
 * Since the {@link RuleBasedPredictor} merely reacts to numerical changes in
 * the monitored metric, it makes no particular assumptions on the unit of the
 * observed metric values. The {@link RuleBasedPredictor} does not need to be
 * aware of what the metric represents, it works the same regardless of if it
 * monitors a stream of observed response times or a stream of CPU utilization
 * pool average values.
 *
 * @see ScalingRule
 */
public class RuleBasedPredictor extends AbstractPredictor {
    /**
     * Event bus onto which {@link Alert}s and {@link SystemMetricEvent}s can be
     * posted.
     */
    private final EventBus eventBus;

    /** The {@link MetricStreamReader} from which metric values are read. */
    private MetricStreamReader metricReader;

    /**
     * The last "prediction" that was returned. When in a cooldown-phase, this
     * value will be immediately returned instead of evaluating the
     * {@link ScalingRule}s.
     */
    private Optional<Prediction> lastPrediction;

    /** The time at which the last {@link ScalingRule} was triggered. */
    private Optional<DateTime> lastCooldownStart;

    /**
     * A sliding window of {@link MetricValue} observations that only keeps
     * track of a limited history of {@link MetricValue}s read from the metric
     * stream. To make sure that sufficient metric values are available to
     * evaluate all scaling rules, a history is kept of twice the length of the
     * longest scaling rule period.
     */
    private final MaxAgeTimeSeries slidingMetricSeries;

    /** The currently set parameters. */
    private RuleBasedPredictorParams params;

    @Inject
    public RuleBasedPredictor(Logger logger, EventBus eventBus, MonitoringSubsystem monitoringSubsystem) {
        super(logger, eventBus, monitoringSubsystem);
        this.eventBus = eventBus;

        this.lastPrediction = Optional.empty();
        this.lastCooldownStart = Optional.empty();

        this.params = null;
        this.slidingMetricSeries = new MaxAgeTimeSeries(new BasicTimeSeries(), 0);
    }

    @Override
    public void validateConfig(PredictorConfig configuration) throws IllegalArgumentException {
        checkArgument(configuration.getParameters() != null, "predictor: missing parameters");
        try {
            // validate predictor-specific part of configuration
            RuleBasedPredictorParams params = parameters(configuration);
            params.validate();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("predictor %s: %s", configuration.getId(), e.getMessage()),
                    e);
        }
    }

    @Override
    public void applyConfig(PredictorConfig newConfig) throws IllegalArgumentException {
        validateConfig(newConfig);

        this.params = parameters(newConfig);

        // set history length to twice the longest evaluation period
        int historyLength = 2 * longestScalingRulePeriod(this.params.getScalingRules());
        this.slidingMetricSeries.setMaxAge(historyLength);
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
        popMetricStream();

        String metric = this.metricReader.getMetricStream().getMetric();

        List<DataPoint> metricValues = this.slidingMetricSeries.getDataPoints();
        this.logger.debug("metric values: {}", metricValues);
        if (!metricValues.isEmpty()) {
            reportLoadObservation(metric, metricValues.get(metricValues.size() - 1).getValue());
        }

        if (inCooldownPhase()) {
            this.logger.debug("no new prediction: in cool-down phase");
            return this.lastPrediction;
        }
        if (!poolSize.isPresent()) {
            this.logger.debug("no new prediction: unknown current pool size");
            return Optional.empty();
        }

        int currentDesiredSize = poolSize.get().getDesiredSize();
        this.logger.debug(format("desired pool size: %d", currentDesiredSize));
        this.logger.debug(format("active pool size: %d", poolSize.get().getActive()));

        // evaluate each scaling rule in turn: if anyone triggers, ignore the
        // rest and start a new cooldown period
        Optional<Prediction> prediction = Optional.of(prediction(currentDesiredSize, metric, predictionTime));
        List<ScalingRule> scalingRules = this.params.getScalingRules();
        for (ScalingRule scalingRule : scalingRules) {
            ScalingRule.RuleOutcome outcome = scalingRule.isSatisfiedBy(metricValues);
            this.logger.debug(format("Scaling rule %s %s satisifed by metrics: %s", scalingRule,
                    outcome.isSatisfied() ? "was not" : "was", outcome.getReason()));
            if (outcome.isSatisfied()) {
                int increment = scalingRule.getResizeIncrement(currentDesiredSize);
                if (increment != 0) {
                    prediction = Optional.of(prediction(currentDesiredSize + increment, metric, predictionTime));
                    this.logger.debug("scaling rule fired: resizing by {} to a pool size of {}", increment,
                            prediction.get().getValue());
                    startCooldownPhase();
                    break;
                }
            }
        }

        this.lastPrediction = prediction;
        return prediction;
    }

    /**
     * Reads any new metric value arrivals from the {@link MetricStream}.
     */
    private void popMetricStream() {
        List<MetricValue> newStreamValues = new ArrayList<>();
        this.metricReader.popTo(newStreamValues);
        this.slidingMetricSeries.addAll(newStreamValues);
    }

    private Prediction prediction(int poolSizePrediction, String metric, DateTime predictionTime) {
        return new Prediction(poolSizePrediction, PredictionUnit.COMPUTE, metric, predictionTime);
    }

    private void startCooldownPhase() {
        this.lastCooldownStart = Optional.of(UtcTime.now());
    }

    /**
     * Checks if the predictor is currently in a cooldown phase.
     *
     * @return <code>true</code> if in cooldown, <code>false</code> otherwise.
     */
    private boolean inCooldownPhase() {
        if (!lastCooldownStart().isPresent()) {
            // not in cooldown phase
            return false;
        }

        // in cooldown phase
        DateTime cooldownStart = lastCooldownStart().get();
        Duration cooldownDuration = new Duration(cooldownStart, UtcTime.now());
        TimeInterval cooldownPeriod = this.params.getCooldownPeriod();
        long elapsedCooldown = cooldownDuration.getStandardSeconds();
        if (elapsedCooldown < cooldownPeriod.getSeconds()) {
            // still in cooldown phase
            this.logger.debug("in cooldown phase: {} seconds remaining", cooldownPeriod.getSeconds() - elapsedCooldown);
            return true;
        }

        // cooldown phase over
        this.logger.debug("cooldown phase over");
        resetLastCooldownStart();
        return false;
    }

    private void resetLastCooldownStart() {
        this.lastCooldownStart = Optional.empty();
    }

    private Optional<DateTime> lastCooldownStart() {
        return this.lastCooldownStart;
    }

    /**
     * Returns the custom part of the configuration that is specific to this
     * {@link Predictor}.
     *
     * @param predictorConfig
     * @return
     */
    private RuleBasedPredictorParams parameters(PredictorConfig predictorConfig) {
        return RuleBasedPredictorParams.parse(predictorConfig.getParameters());
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
     * Returns the evaluation period (in seconds) for the {@link ScalingRule}
     * with longest evaluation period.
     *
     * @param scalingRules
     * @return
     */
    private int longestScalingRulePeriod(List<ScalingRule> scalingRules) {
        int longestEvaluationPeriod = 0;
        for (ScalingRule rule : scalingRules) {
            if (rule.getPeriod().getSeconds() > longestEvaluationPeriod) {
                longestEvaluationPeriod = (int) rule.getPeriod().getSeconds();
            }
        }
        return longestEvaluationPeriod;
    }

    /**
     * Returns the tracked time series of historical metric values against which
     * the scaling rules are evaluated.
     *
     * @return
     */
    MaxAgeTimeSeries metricSeries() {
        return this.slidingMetricSeries;
    }
}
