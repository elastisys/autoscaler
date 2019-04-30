package com.elastisys.autoscaler.core.prediction.impl.standard.predictor;

import static com.elastisys.autoscaler.core.api.types.ServiceStatus.State.STARTED;
import static com.elastisys.autoscaler.core.api.types.ServiceStatus.State.STOPPED;
import static com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictionTestUtils.configs;
import static com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictionTestUtils.predictorConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.stubs.PredictorStub;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.AsynchronousEventBus;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.google.gson.JsonObject;

/**
 * Tests that verify the (re)configuration behavior of the
 * {@link PredictorRegistry}.
 */
public class TestPredictorRegistryConfiguration {
    static Logger logger = LoggerFactory.getLogger(TestPredictorRegistryConfiguration.class);

    private static final PredictorConfig p1Stopped = predictorConfig("p1", PredictorStub.class, STOPPED,
            "metric1.stream", new JsonObject());
    private static final PredictorConfig p2Stopped = predictorConfig("p2", PredictorStub.class, STOPPED,
            "metric2.stream", new JsonObject());
    private static final PredictorConfig p3Stopped = predictorConfig("p3", PredictorStub.class, STOPPED,
            "metric3.stream", new JsonObject());

    /**
     * Slightly modified variant of p1 config with a different metric stream
     * subscription.
     */
    private static final PredictorConfig p1Stream2 = predictorConfig("p1", PredictorStub.class, STOPPED,
            "metric2.stream", new JsonObject());
    /** Slightly modified variant of p2 config with state started. */
    private static final PredictorConfig p2Started = predictorConfig("p2", PredictorStub.class, STARTED,
            "metric2.stream", new JsonObject());

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final EventBus eventBus = new AsynchronousEventBus(this.executor, logger);
    /** Test stub for {@link MetricStreamer}. */
    private MetricStreamer metricStreamer;

    /** Object under test. */
    private PredictorRegistry predictorRegistry;

    @Before
    public void onSetup() {
        this.metricStreamer = PredictionTestUtils.createMetricStreamerStub("metric1.stream", "metric2.stream",
                "metric3.stream");
        MonitoringSubsystem monitoringSubsystem = PredictionTestUtils
                .createMonitoringSubsystemStub(this.metricStreamer);
        // make sure monitoring subsystem is in a configured state before
        // starting test
        monitoringSubsystem.configure(new Object());
        this.predictorRegistry = new PredictorRegistry(logger, this.eventBus, this.executor, monitoringSubsystem,
                FileUtils.cwd());

        // pre-test sanity check
        assertTrue(this.predictorRegistry.getConfiguration().isEmpty());
        assertTrue(this.predictorRegistry.getPredictors().isEmpty());
        assertTrue(this.predictorRegistry.getStartedPredictors().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithNull() throws Exception {
        this.predictorRegistry.validate(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithMissingId() throws Exception {
        List<PredictorConfig> configuration = configs(
                predictorConfig(null, PredictorStub.class, STARTED, "cpu.user.rate", new JsonObject()));
        this.predictorRegistry.validate(configuration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithMissingPredictorClass() throws Exception {
        List<PredictorConfig> configuration = configs(
                predictorConfig("p1", (String) null, STARTED, "cpu.user.rate", new JsonObject()));
        this.predictorRegistry.validate(configuration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithMissingMetric() throws Exception {
        List<PredictorConfig> configuration = configs(
                predictorConfig("p1", PredictorStub.class, STARTED, null, new JsonObject()));
        this.predictorRegistry.validate(configuration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithUnrecognizedPredictorType() throws Exception {
        List<PredictorConfig> configuration = configs(
                predictorConfig("p1", "un.recognized.PredictorClass", STARTED, "cpu.user.rate", new JsonObject()));
        this.predictorRegistry.validate(configuration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithUnrecognizedMetricStream() {
        List<PredictorConfig> configuration = configs(
                predictorConfig("p1", PredictorStub.class, STARTED, "unrecognized.metric.stream", new JsonObject()));
        this.predictorRegistry.validate(configuration);
        this.predictorRegistry.configure(configuration);
    }

    @Test
    public void configureSingleStoppedPredictor() {
        // configure with a single stopped predictor instance
        List<PredictorConfig> config = configs(p1Stopped);
        this.predictorRegistry.validate(config);
        this.predictorRegistry.configure(config);

        // post: verify predictor and its state
        verifyPredictors(p1Stopped);
    }

    @Test
    public void configureSingleStartedPredictor() {
        // configure with a single started predictor instance
        List<PredictorConfig> config = configs(p2Stopped);
        this.predictorRegistry.validate(config);
        this.predictorRegistry.configure(config);

        // post: verify predictor and its state
        verifyPredictors(p2Stopped);
    }

    @Test
    public void configureMultiplePredictors() {
        List<PredictorConfig> config = configs(p1Stopped, p2Started);
        this.predictorRegistry.validate(config);
        this.predictorRegistry.configure(config);

        verifyPredictors(p1Stopped, p2Started);
    }

    /**
     * Verifies that re-configuring the {@link PredictorRegistry} with an
     * unchanged configuration does not introduce any changes at all.
     */
    @Test
    public void reconfigureWithUnmodifiedConfig() {
        // configure
        List<PredictorConfig> config = configs(p1Stopped, p2Stopped, p3Stopped);
        this.predictorRegistry.configure(config);
        verifyPredictors(p1Stopped, p2Stopped, p3Stopped);

        Predictor p1 = this.predictorRegistry.getPredictors().get(0);
        Predictor p2 = this.predictorRegistry.getPredictors().get(1);
        Predictor p3 = this.predictorRegistry.getPredictors().get(2);

        // re-configure with unmodified config
        this.predictorRegistry.configure(config);
        verifyPredictors(p1Stopped, p2Stopped, p3Stopped);
        List<Predictor> predictors = this.predictorRegistry.getPredictors();
        // neither predictors nor their configs should be changed
        assertSame(p1, predictors.get(0));
        assertSame(p1.getConfiguration(), predictors.get(0).getConfiguration());
        assertSame(p2, predictors.get(1));
        assertSame(p2.getConfiguration(), predictors.get(1).getConfiguration());
        assertSame(p3, predictors.get(2));
        assertSame(p3.getConfiguration(), predictors.get(2).getConfiguration());
    }

    /**
     * Verifies proper reconfiguration behavior when the new configuration adds
     * additional predictor configurations: the new configuration(s) should be
     * added and the existing ones be left untouched.
     */
    @Test
    public void reconfigureWithAddedConfig() {
        // configure
        this.predictorRegistry.configure(configs(p1Stopped));
        verifyPredictors(p1Stopped);
        List<Predictor> oldPredictors = this.predictorRegistry.getPredictors();

        // re-configure with addition predictor config
        this.predictorRegistry.configure(configs(p1Stopped, p2Stopped));
        List<Predictor> newPredictors = this.predictorRegistry.getPredictors();

        assertThat(newPredictors.size(), is(oldPredictors.size() + 1));
        verifyPredictors(p1Stopped, p2Stopped);
        // p1 should remain untouched
        assertSame(oldPredictors.get(0), newPredictors.get(0));
        assertSame(oldPredictors.get(0).getConfiguration(), newPredictors.get(0).getConfiguration());
    }

    /**
     * Verifies proper reconfiguration behavior when the new configuration
     * deletes a predictor configuration (by leaving it out).
     */
    @Test
    public void reconfigureWithDeletedConfig() {
        // configure
        this.predictorRegistry.configure(configs(p1Stopped, p2Stopped));
        verifyPredictors(p1Stopped, p2Stopped);
        List<Predictor> oldPredictors = this.predictorRegistry.getPredictors();

        // re-configure with p1 deleted
        this.predictorRegistry.configure(configs(p2Stopped));
        verifyPredictors(p2Stopped);
        List<Predictor> newPredictors = this.predictorRegistry.getPredictors();
        // p2 should remain untouched
        assertSame(oldPredictors.get(1), newPredictors.get(0));
        assertSame(oldPredictors.get(1).getConfiguration(), newPredictors.get(0).getConfiguration());
    }

    /**
     * Verifies proper reconfiguration behavior when the new configuration
     * modified an existing predictor configuration.
     */
    @Test
    public void reconfigureWithModifiedConfig() {
        // configure
        this.predictorRegistry.configure(configs(p1Stopped, p2Stopped));
        verifyPredictors(p1Stopped, p2Stopped);
        List<Predictor> oldPredictors = this.predictorRegistry.getPredictors();

        // re-configure with p2 modified
        this.predictorRegistry.configure(configs(p1Stopped, p2Started));
        verifyPredictors(p1Stopped, p2Started);
        List<Predictor> newPredictors = this.predictorRegistry.getPredictors();
        // p1 should remain untouched
        assertSame(oldPredictors.get(0), newPredictors.get(0));
        assertSame(oldPredictors.get(0).getConfiguration(), newPredictors.get(0).getConfiguration());
        // p2 should be untouched (although its config object has changed)
        assertSame(oldPredictors.get(1), newPredictors.get(1));
    }

    /**
     * Tests reconfiguring the {@link PredictorRegistry} with several changes at
     * once: adding predictor(s), deleting predictor(s) and updating
     * predictor(s).
     */
    @Test
    public void reconfigureWithMultipleDifferentChanges() {
        // configure
        this.predictorRegistry.configure(configs(p1Stopped, p2Stopped));
        verifyPredictors(p1Stopped, p2Stopped);
        List<Predictor> oldPredictors = this.predictorRegistry.getPredictors();

        // re-configure with p1 deleted, p2 modified and p3 deleted
        this.predictorRegistry.configure(configs(p2Started, p3Stopped));

        verifyPredictors(p2Started, p3Stopped);
        List<Predictor> newPredictors = this.predictorRegistry.getPredictors();
        // p2 should be untouched (although its config object has changed)
        assertSame(oldPredictors.get(1), newPredictors.get(0));
    }

    /**
     * Re-configures a {@link Predictor} with a new metric stream subscription
     * and verifies that the metric subscription is re-subscribed (delete old,
     * add new).
     */
    @Test
    public void reconfigureWithNewMetricStreamSubscription() {
        // configure
        this.predictorRegistry.configure(configs(p1Stopped));
        verifyPredictors(p1Stopped);

        // re-configure with p1 subscribing to a new metric stream
        this.predictorRegistry.configure(configs(p1Stream2));
        // verify that metric subscriptions have been properly updated
        verifyPredictors(p1Stream2);
    }

    /**
     * Verifies that the {@link PredictorRegistry} under test is properly
     * configured according to an expected set of {@link PredictorConfig}s. That
     * is, that it contains the proper number of predictors, with the expected
     * configurations, and in the expected states.
     *
     * @param expectedPredictorConfigs
     *            The {@link PredictorConfig}s that the
     *            {@link PredictorRegistry} is expected to be configured with.
     */
    private void verifyPredictors(PredictorConfig... expectedPredictorConfigs) {
        int expectedNumPredictors = expectedPredictorConfigs.length;
        assertThat(this.predictorRegistry.getPredictors().size(), is(expectedNumPredictors));

        for (int i = 0; i < expectedNumPredictors; i++) {
            PredictorConfig predictorConfig = expectedPredictorConfigs[i];
            Predictor actualPredictor = this.predictorRegistry.getPredictors().get(i);
            assertThat(actualPredictor.getConfiguration(), is(predictorConfig));
            assertThat(actualPredictor.getStatus().getState(), is(State.STOPPED));
        }
        assertThat(this.predictorRegistry.getStartedPredictors().size(), is(0));
        // verify the full configuration object returned by registry
        assertThat(this.predictorRegistry.getConfiguration(), is(configs(expectedPredictorConfigs)));
    }

}