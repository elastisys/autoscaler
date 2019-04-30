package com.elastisys.autoscaler.core.prediction.impl.standard;

import static com.elastisys.autoscaler.core.api.types.ServiceStatus.State.STARTED;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.alerter.api.types.AlertTopics;
import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.metronome.impl.standard.AlertMatcher;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetric;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.autoscaler.core.prediction.api.PredictionException;
import com.elastisys.autoscaler.core.prediction.impl.standard.aggregator.AggregatorTestUtils;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
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
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonObject;

/**
 * Verifies the behavior of the {@link StandardPredictionSubsystem} with respect
 * to posting proper {@link AlertMessage}s and {@link SystemMetricEvent}s for
 * system metrics.
 */
@SuppressWarnings("rawtypes")
public class TestStandardPredictionSubsystemEventing {
    static Logger logger = LoggerFactory.getLogger(TestStandardPredictionSubsystemOperation.class);
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    /**
     * {@link EventBus} mock object used to capture events generated in the
     * tests by the {@link StandardPredictionSubsystem}.
     */
    private final EventBus eventBusMock = mock(EventBus.class);

    /** Object under test. */
    private StandardPredictionSubsystem predictionSubsystem;

    @Before
    public void onSetup() {
        MetricStreamer metricStreamer = PredictionTestUtils.createMetricStreamerStub("cpu.user.rate");
        MonitoringSubsystem monitoringSubsystem = PredictionTestUtils.createMonitoringSubsystemStub(metricStreamer);

        // Note: to see what events are posted on the bus, replace the mocked
        // eventbus with a real one and register this object as a listener.
        EventBus eventBus = new AsynchronousEventBus(this.executorService, logger);
        eventBus.register(this);

        this.predictionSubsystem = new StandardPredictionSubsystem(logger, this.eventBusMock, this.executorService,
                monitoringSubsystem, FileUtils.cwd());
    }

    @Subscriber
    public void onEvent(Object event) {
        logger.debug("event: " + event);
    }

    /**
     * Runs a (successful) prediction and verifies that the expected system
     * metric time-series values were reported on the {@link AutoScaler}
     * {@link EventBus}.
     *
     * @throws Exception
     */
    @Test
    public void predictAndVerifySystemMetricReporting() throws Exception {
        DateTime now = UtcTime.now();
        FrozenTime.setFixed(now);

        // configure
        JsonObject computeUnitPredictorConfig = JsonUtils.parseJsonString("{'constant.prediction': 5}")
                .getAsJsonObject();
        JsonObject capacityPredictorConfig = JsonUtils.parseJsonString("{'constant.prediction': 600}")
                .getAsJsonObject();
        StandardPredictionSubsystemConfig config = StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p1", ConstantComputeUnitPredictor.class, STARTED, "cpu.user.rate",
                        computeUnitPredictorConfig)
                .withPredictor("p2", ConstantCapacityPredictor.class, STARTED, "cpu.user.rate", capacityPredictorConfig)
                .withCapacityMapping("cpu.user.rate", 100.0)
                .withAggregator(AggregatorTestUtils.maxAggregatorExpression())
                .withCapacityLimit("l1", 1, "* * * * * ? *", 2, 4).build();
        configureAndStart(config);

        // predict

        Optional<Integer> prediction = this.predictionSubsystem.predict(emptyPool(), now);

        assertTrue(prediction.isPresent());
        assertThat(prediction.get(), is(4));

        // verify time-series events posted on the event bus
        // reported twice: first after prediction, second after capacity mapping
        verify(this.eventBusMock, times(2)).post(predictionEvent("p1", 5.0, "COMPUTE"));
        verify(this.eventBusMock).post(predictionEvent("p2", 600.0, "METRIC"));
        // after capacity mapping
        verify(this.eventBusMock).post(predictionEvent("p2", 6.0, "COMPUTE"));
        verify(this.eventBusMock).post(aggregatePredictionEvent(6.0));
        verify(this.eventBusMock).post(minLimitEvent("l1", 2));
        verify(this.eventBusMock).post(maxLimitEvent("l1", 4));
        verify(this.eventBusMock).post(boundedPredictionEvent(4.0));

        verifyNoMoreInteractions(this.eventBusMock);
    }

    /**
     * Verifies that a proper {@link AlertMessage} is sent on the
     * {@link AutoScaler} {@link EventBus} when a prediction fails.
     *
     * @throws Exception
     */
    @Test
    public void predictWithUnexpectedPredictorFailureAndVerifyAlertMessage() throws Exception {
        DateTime now = UtcTime.now();
        FrozenTime.setFixed(now);

        // configure
        JsonObject p1Config = JsonUtils.parseJsonString("{'constant.prediction': 5}").getAsJsonObject();
        StandardPredictionSubsystemConfig config = StandardPredictionSubsystemConfig.Builder.create()
                // note: predictor will throw runtime exception
                .withPredictor("p1", FailingPredictor.class, STARTED, "cpu.user.rate", p1Config)
                .withCapacityMapping("cpu.user.rate", 100.0)
                .withAggregator(AggregatorTestUtils.maxAggregatorExpression())
                .withCapacityLimit("l1", 1, "* * * * * ? *", 2, 4).build();
        configureAndStart(config);

        // predict
        try {
            this.predictionSubsystem.predict(emptyPool(), now);
            fail("prediction expected to fail");
        } catch (PredictionException e) {
            // expected
        }

        // verify that alert message was thrown on failure
        verify(this.eventBusMock)
                .post(argThat(is(AlertMatcher.alertMessage(AlertTopics.PREDICTION_FAILURE, AlertSeverity.ERROR))));

        verifyNoMoreInteractions(this.eventBusMock);
    }

    /**
     * When the current pool size is unknown and the {@link Predictor} does not
     * produce a prediction the prediction pipeline should produce an absent
     * value and no system metric events should be emitted.
     */
    @Test
    public void systemMetricReportingWithUnknownPoolSizeAndAbsentPrediction() throws Exception {
        DateTime now = UtcTime.now();
        FrozenTime.setFixed(now);

        // configure
        JsonObject computeUnitPredictorConfig = JsonUtils.parseJsonString("{}").getAsJsonObject();
        StandardPredictionSubsystemConfig config = StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p1", AbsentComputeUnitPredictor.class, STARTED, "cpu.user.rate",
                        computeUnitPredictorConfig)
                .withCapacityMapping("cpu.user.rate", 100.0)
                .withAggregator(AggregatorTestUtils.maxAggregatorExpression())
                .withCapacityLimit("l1", 1, "* * * * * ? *", 2, 4).build();
        configureAndStart(config);

        // predict
        Optional<PoolSizeSummary> absentPoolSize = Optional.empty();
        Optional<Integer> prediction = this.predictionSubsystem.predict(absentPoolSize, now);

        assertFalse(prediction.isPresent());

        // verify that no system metrics are reported when no prediction is
        // produced
        verifyNoMoreInteractions(this.eventBusMock);
    }

    /**
     * When the current pool size is known and the {@link Predictor} does not
     * produce a prediction, the prediction pipeline should base the
     * "prediction" on the current desired size and apply its capacity limits to
     * the current desired size.
     */
    @Test
    public void systemMetricReportingWithKnownPoolSizeAndAbsentPrediction() throws Exception {
        DateTime now = UtcTime.now();
        FrozenTime.setFixed(now);

        // configure
        JsonObject computeUnitPredictorConfig = JsonUtils.parseJsonString("{}").getAsJsonObject();
        StandardPredictionSubsystemConfig config = StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p1", AbsentComputeUnitPredictor.class, STARTED, "cpu.user.rate",
                        computeUnitPredictorConfig)
                .withCapacityMapping("cpu.user.rate", 100.0)
                .withAggregator(AggregatorTestUtils.maxAggregatorExpression())
                .withCapacityLimit("l1", 1, "* * * * * ? *", 2, 4).build();
        configureAndStart(config);

        // predict
        Optional<PoolSizeSummary> poolSize = poolSize(1, 1, 1);
        Optional<Integer> prediction = this.predictionSubsystem.predict(poolSize, now);

        assertTrue(prediction.isPresent());
        assertThat(prediction.get(), is(2));

        // capacity limits should be applied to the current pool size
        verify(this.eventBusMock).post(minLimitEvent("l1", 2));
        verify(this.eventBusMock).post(maxLimitEvent("l1", 4));
        verifyNoMoreInteractions(this.eventBusMock);
    }

    /**
     * Creates a {@link SystemMetric#MIN_CAPACITY_LIMIT} event.
     *
     * @param limitId
     * @param min
     * @return
     */
    private SystemMetricEvent minLimitEvent(String limitId, long min) {
        return new SystemMetricEvent(new MetricValue(SystemMetric.MIN_CAPACITY_LIMIT.getMetricName(), min,
                UtcTime.now(), Maps.of("limit", limitId)));
    }

    /**
     * Creates a {@link SystemMetric#MAX_CAPACITY_LIMIT} event.
     *
     * @param limitId
     * @param max
     * @return
     */
    private SystemMetricEvent maxLimitEvent(String limitId, long max) {
        return new SystemMetricEvent(new MetricValue(SystemMetric.MAX_CAPACITY_LIMIT.getMetricName(), max,
                UtcTime.now(), Maps.of("limit", limitId)));
    }

    /**
     * Creates a {@link SystemMetric#PREDICTION} event.
     *
     * @param predictorId
     * @param value
     * @param unit
     * @return
     */
    private SystemMetricEvent predictionEvent(String predictorId, double value, String unit) {
        return new SystemMetricEvent(new MetricValue(SystemMetric.PREDICTION.getMetricName(), value, UtcTime.now(),
                Maps.of( //
                        "predictor", predictorId, //
                        "metric", "cpu.user.rate", //
                        "unit", unit)));
    }

    /**
     * Creates a {@link SystemMetric#BOUNDED_PREDICTION} event.
     *
     * @param value
     * @return
     */
    private SystemMetricEvent boundedPredictionEvent(double value) {
        return new SystemMetricEvent(new MetricValue(SystemMetric.BOUNDED_PREDICTION.getMetricName(), value,
                UtcTime.now(), new HashMap<String, String>()));
    }

    /**
     * Creates a {@link SystemMetric#AGGREGATE_PREDICTION} event.
     *
     * @param value
     * @return
     */
    private SystemMetricEvent aggregatePredictionEvent(double value) {
        return new SystemMetricEvent(new MetricValue(SystemMetric.AGGREGATE_PREDICTION.getMetricName(), value,
                UtcTime.now(), new HashMap<String, String>()));
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
}
