package com.elastisys.autoscaler.predictors.rulebased;

import static com.elastisys.autoscaler.core.api.types.ServiceStatus.State.STOPPED;
import static com.elastisys.autoscaler.predictors.rulebased.RuleBasedPredictorTestUtils.customConfig;
import static com.elastisys.autoscaler.predictors.rulebased.RuleBasedPredictorTestUtils.rule;
import static com.elastisys.autoscaler.predictors.rulebased.rule.Condition.ABOVE;
import static com.elastisys.autoscaler.predictors.rulebased.rule.ResizeUnit.PERCENT;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.autoscaler.predictors.rulebased.rule.Condition;
import com.elastisys.autoscaler.predictors.rulebased.rule.ResizeUnit;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.google.gson.JsonElement;

/**
 * Verifies proper behavior when validating and applying configurations of
 * different kinds to the {@link RuleBasedPredictor}.
 */
public class TestRuleBasedPredictorConfig {

    private static final Logger logger = LoggerFactory.getLogger(TestRuleBasedPredictorConfig.class);
    private static final EventBus eventBusMock = mock(EventBus.class);
    private static final MonitoringSubsystem mockedMonitoringSubsystem = mock(MonitoringSubsystem.class);

    private static final String METRIC_NAME = "metric";
    private static final String METRIC_STREAM_ID = METRIC_NAME + ".stream";

    private static final TimeInterval cooldown = TimeInterval.seconds(180);
    private static final TimeInterval evaluationPeriod = TimeInterval.seconds(300);

    /** Object under test. */
    private RuleBasedPredictor predictor;

    @Before
    public void onSetup() throws IOException {
        prepareMockedMetricStream();
        this.predictor = new RuleBasedPredictor(logger, eventBusMock, mockedMonitoringSubsystem);
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

    @Test(expected = IllegalArgumentException.class)
    public void validateNullConfig() {
        this.predictor.validate(null);
    }

    /**
     * {@link RuleBasedPredictor} requires input parameters (its rules).
     */
    @Test(expected = IllegalArgumentException.class)
    public void configureWithNullParameters() {
        JsonElement nullParameters = null;
        this.predictor.configure(new PredictorConfig("p1", RuleBasedPredictor.class.getName(), State.STARTED,
                METRIC_STREAM_ID, nullParameters));
    }

    @Test
    public void configureWithValidConfigs() {
        // no scaling rules
        PredictorConfig config = config(customConfig(cooldown));
        this.predictor.validate(config);
        this.predictor.configure(config);
        assertThat(this.predictor.getConfiguration(), is(config));

        // single scaling rule
        config = config(customConfig(cooldown, rule(Condition.ABOVE, 80, evaluationPeriod, 20, ResizeUnit.PERCENT)));
        this.predictor.validate(config);
        this.predictor.configure(config);
        assertThat(this.predictor.getConfiguration(), is(config));

        // multiple scaling rules
        config = config(customConfig(cooldown, rule(Condition.ABOVE, 80, evaluationPeriod, 20, ResizeUnit.PERCENT),
                rule(Condition.BELOW, 20, evaluationPeriod, -20, ResizeUnit.PERCENT)));
        this.predictor.validate(config);
        this.predictor.configure(config);
        assertThat(this.predictor.getConfiguration(), is(config));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithNegativeCooldown() {
        TimeInterval negativeCooldown = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": -1, \"unit\": \"seconds\"}"), TimeInterval.class);
        PredictorConfig config = config(customConfig(negativeCooldown));
        this.predictor.validate(config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithNegativeRuleEvaluationPeriod() {
        TimeInterval negativeRuleEvaluation = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": -1, \"unit\": \"seconds\"}"), TimeInterval.class);
        PredictorConfig config = config(
                customConfig(cooldown, rule(Condition.ABOVE, 80, negativeRuleEvaluation, 20, ResizeUnit.PERCENT)));
        this.predictor.validate(config);
    }

    @Test
    public void reconfigureWhileStopped() {
        PredictorConfig config = config(
                customConfig(evaluationPeriod, rule(Condition.ABOVE, 80, evaluationPeriod, 20, ResizeUnit.PERCENT)));

        this.predictor.validate(config);
        this.predictor.configure(config);
        assertThat(this.predictor.getConfiguration(), is(config));
        assertThat(this.predictor.getStatus().getState(), is(State.STOPPED));

        PredictorConfig newConfig = config(
                customConfig(cooldown, rule(Condition.ABOVE, 80, evaluationPeriod, 20, ResizeUnit.PERCENT),
                        rule(Condition.BELOW, 20, evaluationPeriod, -20, ResizeUnit.PERCENT)));
        this.predictor.configure(newConfig);
        assertThat(this.predictor.getConfiguration(), is(newConfig));
        assertThat(this.predictor.getStatus().getState(), is(State.STOPPED));
    }

    @Test
    public void reconfigureWhileStarted() {
        PredictorConfig config = config(
                customConfig(cooldown, rule(Condition.ABOVE, 80, evaluationPeriod, 20, ResizeUnit.PERCENT)));
        this.predictor.validate(config);
        this.predictor.configure(config);
        this.predictor.start();
        assertThat(this.predictor.getConfiguration(), is(config));
        assertThat(this.predictor.getStatus().getState(), is(State.STARTED));

        PredictorConfig newConfig = config(
                customConfig(cooldown, rule(Condition.ABOVE, 80, evaluationPeriod, 20, ResizeUnit.PERCENT),
                        rule(Condition.BELOW, 20, evaluationPeriod, -20, ResizeUnit.PERCENT)));
        this.predictor.configure(newConfig);
        assertThat(this.predictor.getConfiguration(), is(newConfig));
        assertThat(this.predictor.getStatus().getState(), is(State.STARTED));
    }

    /**
     * Stop a started predictor by setting its state to STOPPED
     */
    @Test
    public void deactivateStartedPredictor() {
        PredictorConfig config = config(
                customConfig(cooldown, rule(Condition.ABOVE, 80, evaluationPeriod, 20, ResizeUnit.PERCENT)));
        this.predictor.validate(config);
        this.predictor.configure(config);
        this.predictor.start();
        assertThat(this.predictor.getConfiguration(), is(config));
        assertThat(this.predictor.getStatus().getState(), is(State.STARTED));

        // set to STOPPED
        PredictorConfig newConfig = config(STOPPED,
                customConfig(cooldown, rule(ABOVE, 80, evaluationPeriod, 20, PERCENT)));
        this.predictor.configure(newConfig);
        assertThat(this.predictor.getConfiguration(), is(newConfig));
        // should no longer be running
        assertThat(this.predictor.getStatus().getState(), is(State.STOPPED));
    }

    /**
     * The {@link RuleBasedPredictor} should keep track of a tail of metric
     * values that are twice the length of the longest evaluation period.
     */
    @Test
    public void trackMetricHistoryOfTwiceTheLenghtOfLongestEvaluationPeriod() {
        TimeInterval evaluationPeriod1 = TimeInterval.seconds(300);
        TimeInterval evaluationPeriod2 = TimeInterval.seconds(240);
        PredictorConfig newConfig = config(
                customConfig(cooldown, rule(Condition.ABOVE, 80, evaluationPeriod1, 20, ResizeUnit.PERCENT),
                        rule(Condition.BELOW, 20, evaluationPeriod2, -20, ResizeUnit.PERCENT)));
        this.predictor.configure(newConfig);
        long expectedMaxMetricAge = evaluationPeriod1.getSeconds() * 2;
        assertThat(this.predictor.metricSeries().getMaxAge(), is((int) expectedMaxMetricAge));

        // when a new config is set, the window should adapt
        TimeInterval evaluationPeriod3 = TimeInterval.seconds(120);
        newConfig = config(
                customConfig(cooldown, rule(Condition.ABOVE, 80, evaluationPeriod3, 20, ResizeUnit.PERCENT)));
        this.predictor.configure(newConfig);
        expectedMaxMetricAge = evaluationPeriod3.getSeconds() * 2;
        assertThat(this.predictor.metricSeries().getMaxAge(), is((int) expectedMaxMetricAge));
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
