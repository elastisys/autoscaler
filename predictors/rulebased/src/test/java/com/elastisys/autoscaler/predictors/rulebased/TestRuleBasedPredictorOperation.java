package com.elastisys.autoscaler.predictors.rulebased;

import static com.elastisys.autoscaler.predictors.rulebased.RuleBasedPredictorTestUtils.customConfig;
import static com.elastisys.autoscaler.predictors.rulebased.RuleBasedPredictorTestUtils.rule;
import static com.elastisys.autoscaler.predictors.rulebased.rule.Condition.ABOVE;
import static com.elastisys.autoscaler.predictors.rulebased.rule.Condition.BELOW;
import static com.elastisys.autoscaler.predictors.rulebased.rule.ResizeUnit.PERCENT;
import static com.elastisys.scale.commons.util.time.FrozenTime.now;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.prediction.api.PredictionException;
import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.api.types.PredictionUnit;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.autoscaler.predictors.rulebased.rule.Condition;
import com.elastisys.autoscaler.predictors.rulebased.rule.ResizeUnit;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonElement;

/**
 * Performs some basic exercising of the core logic of the
 * {@link RuleBasedPredictor}, by pushing {@link MetricValue}s into the
 * {@link Predictor}'s {@link MetricStream} and asking the {@link Predictor} to
 * make predictions.
 */
public class TestRuleBasedPredictorOperation {

    private static final Logger logger = LoggerFactory.getLogger(TestRuleBasedPredictorOperation.class);
    private static final EventBus eventBus = new SynchronousEventBus(logger);
    private static final MonitoringSubsystem mockedMonitoringSubsystem = mock(MonitoringSubsystem.class);

    private static final String METRIC_NAME = "metric";
    private static final String METRIC_STREAM_ID = METRIC_NAME + ".stream";

    private static final TimeInterval cooldown = TimeInterval.seconds(180);
    private static final TimeInterval evaluationPeriod = TimeInterval.seconds(300);

    /** Object under test. */
    private Predictor predictor;

    @Before
    public void onSetup() throws IOException {
        FrozenTime.setFixed(UtcTime.parse("2017-01-01T12:00:00.000Z"));

        prepareMockedMetricStream();

        this.predictor = new RuleBasedPredictor(logger, eventBus, mockedMonitoringSubsystem);
    }

    private void prepareMockedMetricStream() {
        MetricStreamer mockedMetricStreamer = mock(MetricStreamer.class);
        when(mockedMonitoringSubsystem.getMetricStreamers()).thenReturn(asList(mockedMetricStreamer));
        MetricStream mockedMetricStream = mock(MetricStream.class);
        when(mockedMetricStreamer.getMetricStream(Matchers.argThat(is(any(String.class)))))
                .thenReturn(mockedMetricStream);

        when(mockedMetricStream.getId()).thenReturn(METRIC_STREAM_ID);
        when(mockedMetricStream.getMetric()).thenReturn(METRIC_NAME);
    }

    @Test
    public void startAndStop() throws Exception {
        PredictorConfig config = config(
                customConfig(cooldown, rule(Condition.ABOVE, 80, evaluationPeriod, 20, ResizeUnit.PERCENT),
                        rule(Condition.BELOW, 20, evaluationPeriod, -20, ResizeUnit.PERCENT)));

        this.predictor.configure(config);
        assertThat(this.predictor.getStatus().getState(), is(State.STOPPED));
        this.predictor.start();
        assertThat(this.predictor.getStatus().getState(), is(State.STARTED));
        this.predictor.stop();
        assertThat(this.predictor.getStatus().getState(), is(State.STOPPED));
    }

    @Test(expected = IllegalStateException.class)
    public void startBeforeConfiguring() throws Exception {
        this.predictor.start();
    }

    @Test(expected = IllegalStateException.class)
    public void predictBeforeConfigured() throws Exception {
        predict(machinePool(1), 180);
    }

    @Test(expected = IllegalStateException.class)
    public void predictBeforeStarted() throws Exception {
        PredictorConfig config = config(
                customConfig(cooldown, rule(Condition.ABOVE, 80, evaluationPeriod, 20, ResizeUnit.PERCENT),
                        rule(Condition.BELOW, 20, evaluationPeriod, -20, ResizeUnit.PERCENT)));

        this.predictor.configure(config);
        predict(machinePool(1), 180);
    }

    /**
     * No rule should fire given that no {@link MetricValue}s have been
     * observed.
     *
     * @throws Exception
     */
    @Test
    public void predictionWithoutMetricValues() throws Exception {
        PredictorConfig config = config(
                customConfig(cooldown, rule(Condition.ABOVE, 80, evaluationPeriod, 20, ResizeUnit.PERCENT),
                        rule(Condition.BELOW, 20, evaluationPeriod, -20, ResizeUnit.PERCENT)));
        this.predictor.configure(config);
        this.predictor.start();

        // note: no metric values in metric stream at this point
        Optional<Prediction> prediction = predict(machinePool(1), 180);
        assertThat(prediction.isPresent(), is(true));
        assertThat(prediction.get().getValue(), is(1.0));
    }

    @Test
    public void predictionNotFiringAnyScalingRules() throws Exception {
        TimeInterval period = TimeInterval.seconds(60);
        PredictorConfig config = config(
                customConfig(cooldown, rule(ABOVE, 80, period, 20, PERCENT), rule(BELOW, 20, period, -20, PERCENT)));
        this.predictor.configure(config);
        this.predictor.start();

        streamMetric(50.0, now().minusSeconds(60));
        streamMetric(51.0, now().minusSeconds(45));
        streamMetric(45.0, now().minusSeconds(30));
        streamMetric(55.0, now().minusSeconds(15));
        Optional<Prediction> prediction = predict(machinePool(1), 180);
        verifyPrediction(prediction.get(), 1.0, 180);
    }

    @Test
    public void predictionFiringFirstScalingRule() throws Exception {
        TimeInterval cooldown = TimeInterval.seconds(60);
        TimeInterval period = TimeInterval.seconds(60);
        PredictorConfig config = config(
                customConfig(cooldown, rule(ABOVE, 80, period, 20, PERCENT), rule(BELOW, 20, period, -20, PERCENT)));
        this.predictor.configure(config);
        this.predictor.start();

        // iteration 1: no triggering yet: not sufficient threshold breaking
        // period (should be 60 seconds)
        streamMetric(81.0, now().minusSeconds(45));
        streamMetric(85.0, now().minusSeconds(30));
        streamMetric(83.0, now().minusSeconds(15));
        Optional<Prediction> prediction = predict(machinePool(1), 180);
        verifyPrediction(prediction.get(), 1.0, 180);

        // iteration 2: rule 1 should trigger (threshold broken for more than 60
        // seconds)
        FrozenTime.tick(30);
        streamMetric(82.0, now().minusSeconds(20));
        streamMetric(81.0, now().minusSeconds(10));
        prediction = predict(machinePool(1), 180);
        verifyPrediction(prediction.get(), 2.0, 180);
    }

    @Test
    public void predictionFiringSecondScalingRule() throws Exception {
        TimeInterval cooldown = TimeInterval.seconds(180);
        TimeInterval period = TimeInterval.seconds(60);
        PredictorConfig config = config(
                customConfig(cooldown, rule(ABOVE, 80, period, 20, PERCENT), rule(BELOW, 20, period, -20, PERCENT)));
        this.predictor.configure(config);
        this.predictor.start();

        // should not trigger second rule yet (not seen 60s of sub-threshold
        // levels)
        streamMetric(19.0, now().minusSeconds(45));
        streamMetric(18.0, now().minusSeconds(30));
        streamMetric(16.0, now().minusSeconds(15));
        Optional<Prediction> prediction = predict(machinePool(1), 180);
        verifyPrediction(prediction.get(), 1.0, 180);

        // should trigger second rule
        FrozenTime.tick(30);
        streamMetric(17.0, now().minusSeconds(15));
        prediction = predict(machinePool(2), 180);
        verifyPrediction(prediction.get(), 1.0, 180);
    }

    /**
     * Verify that during a cooldown phase, no scaling rules are evaluated but
     * rather, the last "prediction" is returned.
     *
     * @throws Exception
     */
    @Test
    public void verifyCooldownBehavior() throws Exception {
        TimeInterval cooldown = TimeInterval.seconds(60);
        TimeInterval period = TimeInterval.seconds(60);
        PredictorConfig config = config(
                customConfig(cooldown, rule(ABOVE, 80, period, 20, PERCENT), rule(BELOW, 20, period, -20, PERCENT)));
        this.predictor.configure(config);
        this.predictor.start();

        // scale-up
        streamMetric(81.0, now().minusSeconds(100));
        streamMetric(85.0, now().minusSeconds(30));
        Optional<Prediction> prediction = predict(machinePool(1), 180);
        verifyPrediction(prediction.get(), 2.0, 180);
        assertThat(prediction.get().getTimestamp(), is(now().plusSeconds(180)));

        // record time of latest prediction
        DateTime latestPredictionTime = prediction.get().getTimestamp();

        // in cooldown ...
        FrozenTime.tick(20);
        streamMetric(90.0, now().minusSeconds(10));
        // on cool-down no new prediction should be produced
        prediction = predict(machinePool(2), 180);
        assertThat(prediction.get().getTimestamp(), is(latestPredictionTime));

        // still in cooldown ...
        FrozenTime.tick(20);
        streamMetric(90.0, now().minusSeconds(10));
        prediction = predict(machinePool(2), 180);
        assertThat(prediction.get().getTimestamp(), is(latestPredictionTime));

        // cooldown over: predictor should start predicting again
        FrozenTime.tick(20);
        streamMetric(50.0, now().minusSeconds(10));
        prediction = predict(machinePool(2), 180);
        assertThat(prediction.get().getTimestamp(), is(now().plusSeconds(180)));
    }

    /**
     * The predictor makes adjustments relative to the current pool size and
     * should not produce any prediction in case it is missing (for example, due
     * to the cloud pool/cloud provider temporarily being unreachable).
     */
    @Test
    public void predictWithUnknownPoolSize() throws Exception {
        TimeInterval cooldown = TimeInterval.seconds(60);
        TimeInterval period = TimeInterval.seconds(60);
        PredictorConfig config = config(
                customConfig(cooldown, rule(ABOVE, 80, period, 20, PERCENT), rule(BELOW, 20, period, -20, PERCENT)));
        this.predictor.configure(config);
        this.predictor.start();

        streamMetric(81.0, now().minusSeconds(45));
        Optional<PoolSizeSummary> absentPoolSize = Optional.empty();
        Optional<Prediction> prediction = predict(absentPoolSize, 180);
        assertFalse(prediction.isPresent());
    }

    /**
     * Verifies that a given {@link Prediction} contains expected values.
     *
     * @param actualPrediction
     * @param expectedPredictionRange
     * @param expectedHorizon
     */
    private void verifyPrediction(Prediction actualPrediction, double expectedPredictionRange, int expectedHorizon) {
        assertThat(actualPrediction.getValue(), is(expectedPredictionRange));
        assertThat(actualPrediction.getMetric(), is(METRIC_NAME));
        assertThat(actualPrediction.getTimestamp(), is(FrozenTime.now().plusSeconds(expectedHorizon)));
        assertThat(actualPrediction.getUnit(), is(PredictionUnit.COMPUTE));
    }

    /**
     * Pushes a {@link MetricValue} onto the {@link EventBus}.
     *
     * @param requestRate
     *            The load to be reported.
     * @param timestamp
     *            The time stamp of the metric value.
     */
    private void streamMetric(double requestRate, DateTime timestamp) {
        eventBus.post(new MetricStreamMessage(METRIC_STREAM_ID, Arrays.asList(value(requestRate, timestamp))));
    }

    private MetricValue value(double value, DateTime timestamp) {
        return new MetricValue(METRIC_NAME, value, timestamp);
    }

    /**
     * Perform a prediction against the predictor under test with a given
     * prediction horizon (taken to be relative to present time).
     *
     * @param machinePool
     *            The current machine pool in the (simulated) application.
     * @param horizon
     *            The prediction horizon (in seconds).
     * @return
     * @throws PredictionException
     */
    private Optional<Prediction> predict(Optional<PoolSizeSummary> poolSize, int horizon) throws PredictionException {
        DateTime now = FrozenTime.now();
        return this.predictor.predict(poolSize, now.plusSeconds(horizon));
    }

    /**
     * Creates a machine pool snapshot of a given size.
     *
     * @param size
     *            The number of desired and active machines in the pool.
     * @return
     */
    private Optional<PoolSizeSummary> machinePool(int size) {
        return Optional.of(new PoolSizeSummary(size, size, size));
    }

    /**
     * Creates a {@link PredictorConfig} in started state with a given custom
     * predictor config.
     *
     * @param customConfig
     * @return
     */
    private static PredictorConfig config(JsonElement customConfig) {
        return new PredictorConfig("p1", RuleBasedPredictor.class.getName(), State.STARTED, METRIC_STREAM_ID,
                customConfig);
    }

    /**
     * Creates a {@link PredictorConfig} in a given state with a given custom
     * predictor config.
     *
     * @param state
     * @param customConfig
     * @return
     */
    private static PredictorConfig config(State state, JsonElement customConfig) {
        return new PredictorConfig("p1", RuleBasedPredictor.class.getName(), state, METRIC_STREAM_ID, customConfig);
    }
}
