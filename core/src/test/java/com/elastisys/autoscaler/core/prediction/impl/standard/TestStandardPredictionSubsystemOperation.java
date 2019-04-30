package com.elastisys.autoscaler.core.prediction.impl.standard;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.Health;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.prediction.api.PredictionException;
import com.elastisys.autoscaler.core.prediction.api.types.PredictionUnit;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.ScalingPoliciesConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.StandardPredictionSubsystemConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictionTestUtils;
import com.elastisys.autoscaler.core.prediction.impl.standard.stubs.AbsentComputeUnitPredictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.stubs.ConstantCapacityPredictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.stubs.ConstantComputeUnitPredictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.stubs.FailingPredictor;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;
import com.elastisys.scale.commons.eventbus.impl.AsynchronousEventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonObject;

/**
 * Verifies the behavior of the {@link StandardPredictionSubsystem} operation
 * logic.
 */
public class TestStandardPredictionSubsystemOperation {

    static Logger logger = LoggerFactory.getLogger(TestStandardPredictionSubsystemOperation.class);
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final EventBus bus = new AsynchronousEventBus(this.executorService, logger);
    /** Object under test. */
    private StandardPredictionSubsystem predictionSubsystem;

    @SuppressWarnings("rawtypes")
    @Before
    public void onSetup() {
        MetricStreamer metricStreamer = PredictionTestUtils.createMetricStreamerStub("cpu.user.rate");
        MonitoringSubsystem monitoringSubsystem = PredictionTestUtils.createMonitoringSubsystemStub(metricStreamer);

        this.predictionSubsystem = new StandardPredictionSubsystem(logger, this.bus, this.executorService,
                monitoringSubsystem, FileUtils.cwd());
        this.bus.register(this);
    }

    @Subscriber
    public void eventListener(Object event) {
        logger.debug("Event: " + event);
    }

    @Test
    public void startAndStop() throws Exception {
        this.predictionSubsystem.validate(createConfig());
        this.predictionSubsystem.configure(createConfig());
        assertThat(this.predictionSubsystem.getStatus(), is(new ServiceStatus(State.STOPPED, Health.OK)));
        this.predictionSubsystem.start();
        assertThat(this.predictionSubsystem.getStatus(), is(new ServiceStatus(State.STARTED, Health.OK)));
        this.predictionSubsystem.stop();
        assertThat(this.predictionSubsystem.getStatus(), is(new ServiceStatus(State.STOPPED, Health.OK)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void predictWithMissingMachinePool() throws Exception {
        this.predictionSubsystem.validate(createConfig());
        this.predictionSubsystem.configure(createConfig());
        this.predictionSubsystem.predict(null, UtcTime.now().plusMinutes(5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void predictWithMissingPredictionTime() throws Exception {
        this.predictionSubsystem.validate(createConfig());
        this.predictionSubsystem.configure(createConfig());
        DateTime now = UtcTime.now();
        this.predictionSubsystem.predict(emptyPool(), null);
    }

    /**
     * Uses a "raw" metric predictor that estimates a capacity need in terms of
     * {@link PredictionUnit#METRIC}.
     *
     * @throws Exception
     */
    @Test
    public void predictWithMetricPredictor() throws Exception {
        // configure
        // use metric predictor always predicting a "raw" capacity need of 500
        JsonObject predictorConfig = JsonUtils.parseJsonString("{'constant.prediction': 500}").getAsJsonObject();
        StandardPredictionSubsystemConfig config = StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p1", ConstantCapacityPredictor.class, State.STARTED, "cpu.user.rate", predictorConfig)
                .withCapacityMapping("cpu.user.rate", 100.0).withAggregator("p1.prediction;").build();
        configureAndStart(config);

        // predict
        Optional<Integer> prediction = this.predictionSubsystem.predict(emptyPool(), UtcTime.now());

        assertTrue(prediction.isPresent());
        assertThat(prediction.get(), is(5));
    }

    /**
     * Run a prediction that makes use of a predictor that predicts capacity in
     * Compute Units ({@link PredictionUnit#COMPUTE} rather than
     * {@link PredictionUnit#METRIC}) and make sure that capacity mapping isn't
     * applied to the prediction.
     *
     * @throws Exception
     */
    @Test
    public void predictWithComputeUnitPredictor() throws Exception {
        // configure
        // use predictor always predicting a capacity need of 5 machines
        JsonObject predictorConfig = parseJsonString("{'constant.prediction': 5}");
        StandardPredictionSubsystemConfig config = StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p1", ConstantComputeUnitPredictor.class, State.STARTED, "cpu.user.rate",
                        predictorConfig)
                .withCapacityMapping("cpu.user.rate", 1.0).withAggregator("p1.prediction;").build();
        configureAndStart(config);

        // predict
        Optional<Integer> prediction = this.predictionSubsystem.predict(emptyPool(), UtcTime.now());

        assertTrue(prediction.isPresent());
        assertThat(prediction.get(), is(5));
    }

    @Test
    public void predictWithCapacityLimit() throws Exception {
        // configure
        // use a constant predictor always predicting a capacity need of 500
        JsonObject predictorConfig = parseJsonString("{'constant.prediction': 500}");
        StandardPredictionSubsystemConfig config = StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p1", ConstantCapacityPredictor.class, State.STARTED, "cpu.user.rate", predictorConfig)
                // machine capacity: 100
                .withCapacityMapping("cpu.user.rate", 100.0).withAggregator("p1.prediction;")
                .withCapacityLimit("limit", 1, "* * * * * ? *", 2, 3).build();
        configureAndStart(config);

        // predict
        DateTime now = UtcTime.now();
        Optional<Integer> prediction = this.predictionSubsystem.predict(emptyPool(), now);
        logger.info("prediction: " + prediction.get());

        assertTrue(prediction.isPresent());
        // predicted value (5) should be capped to max limit (3)
        assertThat(prediction.get(), is(3));
    }

    /**
     * Test the situation when a prediction that is lower than the minimum
     * capacity limit is given and make sure that in the end the minimum
     * capacity is honored.
     */
    @Test
    public void predictWithMinCapacityLimitKickingIn() throws Exception {
        // configure use a constant predictor always predicting a capacity need
        // of 100 => 1 machine
        JsonObject predictorConfig = parseJsonString("{'constant.prediction': 100}");
        StandardPredictionSubsystemConfig config = StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p1", ConstantCapacityPredictor.class, State.STARTED, "cpu.user.rate", predictorConfig)
                // machine capacity: 100
                .withCapacityMapping("cpu.user.rate", 100.0).withAggregator("p1.prediction;")
                .withCapacityLimit("limit", 1, "* * * * * ? *", 2, 3).build();
        configureAndStart(config);

        // predict
        DateTime now = UtcTime.now();
        Optional<Integer> prediction = this.predictionSubsystem.predict(emptyPool(), now);
        logger.info("prediction: " + prediction.get());

        assertTrue(prediction.isPresent());
        // predicted value (1) should be set to min limit (2)
        assertThat(prediction.get(), is(2));
    }

    /**
     * Attempts to make a prediction for a metric where no capacity mapping is
     * available.
     *
     * @throws IOException
     */
    @Test(expected = PredictionException.class)
    public void predictWithMissingCapacityMapping() throws Exception {
        // configure
        // use a constant predictor always predicting a capacity need of 500
        JsonObject predictorConfig = parseJsonString("{'constant.prediction': 500}");
        StandardPredictionSubsystemConfig config = StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p1", ConstantCapacityPredictor.class, State.STARTED, "cpu.user.rate", predictorConfig)
                .withAggregator("p1.prediction").build();
        configureAndStart(config);

        // predict
        DateTime now = UtcTime.now();
        this.predictionSubsystem.predict(emptyPool(), now);
    }

    /**
     * Uses a JavaScript function as aggregation method.
     *
     * @throws Exception
     */
    @Test
    public void predictWithAggregatorExpression() throws Exception {
        JsonObject p1Config = parseJsonString("{'constant.prediction': 500}");
        JsonObject p2Config = parseJsonString("{'constant.prediction': 600}");
        StandardPredictionSubsystemConfig config = StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p1", ConstantCapacityPredictor.class, State.STARTED, "cpu.user.rate", p1Config)
                .withPredictor("p2", ConstantCapacityPredictor.class, State.STARTED, "cpu.user.rate", p2Config)
                .withCapacityMapping("cpu.user.rate", 100.0).withAggregator("Math.max(p1.prediction, p2.prediction)")
                .build();
        configureAndStart(config);

        // predict
        DateTime now = UtcTime.now();
        Optional<Integer> prediction = this.predictionSubsystem.predict(emptyPool(), now);
        assertTrue(prediction.isPresent());
        assertThat(prediction.get(), is(6));
    }

    /**
     * Execute prediction where one of the predictors unexpectedly fails (and
     * throws a runtime error).
     *
     * @throws Exception
     */
    @Test
    public void predictWithUnexpectedPredictorFailure() throws Exception {
        // configure
        // use a constant predictor always predicting a capacity need of 500
        JsonObject predictorConfig = parseJsonString("{'constant.prediction': 500}");
        StandardPredictionSubsystemConfig config = StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p1", ConstantCapacityPredictor.class, State.STARTED, "cpu.user.rate", predictorConfig)
                .withPredictor("p2", FailingPredictor.class, State.STARTED, "cpu.user.rate", predictorConfig)
                .withCapacityMapping("cpu.user.rate", 100.0).withAggregator("Math.max(p1.prediction,p2.prediction)")
                .build();
        configureAndStart(config);

        // predict
        DateTime now = UtcTime.now();
        try {
            this.predictionSubsystem.predict(emptyPool(), now);
            fail("prediction should fail");
        } catch (PredictionException e) {
            // expected
        }
        ServiceStatus status = this.predictionSubsystem.getStatus();
        assertThat(status.getHealth(), is(Health.NOT_OK));
        assertThat(status.getHealthDetail(), is(notNullValue()));
    }

    /**
     * Execute prediction where the aggregator expression fails.
     *
     * @throws Exception
     */
    @Test
    public void predictWithUnexpectedAggregatorFailure() throws Exception {
        // configure
        StandardPredictionSubsystemConfig config = StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p1", ConstantCapacityPredictor.class, State.STARTED, "cpu.user.rate",
                        parseJsonString("{'constant.prediction': 500}"))
                .withCapacityMapping("cpu.user.rate", 100.0)
                // note: aggregator expression refers to unknown predictor
                .withAggregator("Math.max(p1.prediction,p2.prediction)").build();
        configureAndStart(config);

        // predict
        DateTime now = UtcTime.now();
        try {
            this.predictionSubsystem.predict(emptyPool(), now);
            fail("prediction should fail");
        } catch (PredictionException e) {
            // expected
        }
        ServiceStatus status = this.predictionSubsystem.getStatus();
        assertThat(status.getHealth(), is(Health.NOT_OK));
        assertThat(status.getHealthDetail(), is(notNullValue()));
    }

    /**
     * When no {@link Predictor} can produce a prediction (for example, due to
     * lack of metrics) the current pool size should be used as prediction if
     * known.
     */
    @Test
    public void verifyThatCurrentPoolSizeIsKeptWhenNoPredictionCanBeProduced() throws Exception {
        // use predictor that always answers with an absent prediction
        JsonObject predictorConfig = parseJsonString("{}");
        StandardPredictionSubsystemConfig config = StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p1", AbsentComputeUnitPredictor.class, State.STARTED, "cpu.user.rate", predictorConfig)
                .withCapacityMapping("cpu.user.rate", 1.0).withAggregator("p1.prediction;").build();

        configureAndStart(config);

        // predict on a machine pool containing a mix of pending, running,
        // terminated and inactive machines
        Optional<PoolSizeSummary> poolSize = poolSize(2, 2, 1);
        // MachinePool pool = ScalingPolicyTestUtils.pool(1, 1, 1, 1);
        Optional<Integer> prediction = this.predictionSubsystem.predict(poolSize, UtcTime.now());

        // verify that the current (effective) pool size is used as prediction
        assertTrue(prediction.isPresent());
        assertThat(prediction.get(), is(2));
    }

    /**
     * Predictions are to be performed also when the the current pool size is
     * unknown (predictors still get the chance to predict).
     */
    @Test
    public void verifyThatPredictionsAreProducedDespitePoolSizeBeingUnknown() throws Exception {
        // use predictor that always answers with an absent prediction
        StandardPredictionSubsystemConfig config = StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p1", ConstantCapacityPredictor.class, State.STARTED, "cpu.user.rate",
                        parseJsonString("{'constant.prediction': 500}"))
                .withCapacityMapping("cpu.user.rate", 100.0).withAggregator("p1.prediction;").build();

        configureAndStart(config);

        // pool size unknown
        Optional<PoolSizeSummary> absentPoolSize = Optional.empty();
        // MachinePool pool = ScalingPolicyTestUtils.pool(1, 1, 1, 1);
        Optional<Integer> prediction = this.predictionSubsystem.predict(absentPoolSize, UtcTime.now());

        // verify that the predictor was still asked to predict
        assertTrue(prediction.isPresent());
        assertThat(prediction.get(), is(5));
    }

    /**
     * When no {@link Predictor} can produce a prediction (for example, due to
     * lack of metrics) the current pool size should be used as prediction.
     */
    @Test
    public void verifyAbsentResultWhenNoPredictionsAndPoolSizeUnknown() throws Exception {
        // use predictor that always answers with an absent prediction
        JsonObject predictorConfig = parseJsonString("{}");
        StandardPredictionSubsystemConfig config = StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p1", AbsentComputeUnitPredictor.class, State.STARTED, "cpu.user.rate", predictorConfig)
                .withCapacityMapping("cpu.user.rate", 1.0).withAggregator("p1.prediction;").build();

        configureAndStart(config);

        // pool size unknown
        Optional<PoolSizeSummary> absentPoolSize = Optional.empty();
        // MachinePool pool = ScalingPolicyTestUtils.pool(1, 1, 1, 1);
        Optional<Integer> prediction = this.predictionSubsystem.predict(absentPoolSize, UtcTime.now());

        // verify that an absent prediction was produced
        assertFalse(prediction.isPresent());
    }

    // TODO: include scaling policies

    private StandardPredictionSubsystemConfig createConfig() throws IOException {
        StandardPredictionSubsystemConfig config = StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p1", ConstantCapacityPredictor.class, State.STARTED, "cpu.user.rate",
                        parseJsonString("{'constant.prediction': 500}"))
                .withCapacityMapping("cpu.user.rate", 100.0).withAggregator("p1.prediction;")
                .withScalingPolicies(new ScalingPoliciesConfig(null, null)).build();
        return config;
    }

    private void configureAndStart(StandardPredictionSubsystemConfig config) {
        this.predictionSubsystem.validate(config);
        this.predictionSubsystem.configure(config);
        this.predictionSubsystem.start();
    }

    /**
     * A {@link PoolSizeSummary} for an empty pool with desired size 0.
     *
     * @return
     */
    private Optional<PoolSizeSummary> emptyPool() {
        return Optional.of(new PoolSizeSummary(0, 0, 0));
    }

    private Optional<PoolSizeSummary> poolSize(int desiredSize, int allocated, int active) {
        return Optional.of(new PoolSizeSummary(desiredSize, allocated, active));
    }

    private JsonObject parseJsonString(String jsonString) {
        return JsonUtils.parseJsonString(jsonString).getAsJsonObject();
    }
}
