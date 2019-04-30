package com.elastisys.autoscaler.predictors.reactive;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.elastisys.autoscaler.core.monitoring.metricstreamer.reader.MetricStreamReader;
import com.elastisys.autoscaler.core.prediction.api.PredictionException;
import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.api.types.PredictionUnit;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.autoscaler.predictors.reactive.config.ReactivePredictorParams;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonObject;

/**
 * * Performs some basic exercising of the core logic of the
 * {@link ReactivePredictor}, by pushing {@link MetricValue}s onto the
 * {@link Predictor}'s {@link MetricStream} and asking the {@link Predictor} to
 * make predictions.
 */
public class TestLastReadingPredictorOperation {

    private static final Logger logger = LoggerFactory.getLogger(TestLastReadingPredictorOperation.class);

    private static final String METRIC_NAME = "http.request.rate";
    private static final String METRIC_STREAM_ID = METRIC_NAME + ".stream";

    private static final EventBus eventBus = new SynchronousEventBus(logger);
    private static final MonitoringSubsystem mockedMonitoringSubsystem = mock(MonitoringSubsystem.class);

    /** Object under test. */
    private Predictor predictor;

    @Before
    public void onSetup() {
        FrozenTime.setFixed(UtcTime.parse("2014-05-20T12:00:00.000Z"));
        prepareMockedMetricStream();

        this.predictor = new ReactivePredictor(logger, eventBus, mockedMonitoringSubsystem);
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

    /**
     * The {@link ReactivePredictor} can run with <code>null</code> parameters.
     * If so, it uses a default config.
     */
    @Test
    public void configureWithNullParameters() {
        JsonObject nullParameters = null;
        this.predictor.configure(new PredictorConfig("p1", ReactivePredictor.class.getName(), State.STARTED,
                "metric.stream", nullParameters));
    }

    @Test
    public void startAndStop() throws Exception {
        this.predictor.configure(config(0.0));
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
        int HORIZON = 180;
        this.predictor.predict(machinePool(0), UtcTime.now().plusSeconds(HORIZON));
    }

    @Test(expected = IllegalStateException.class)
    public void predictBeforeStarted() throws Exception {
        int HORIZON = 180;

        this.predictor.configure(config(0.0));
        this.predictor.predict(machinePool(0), UtcTime.now().plusSeconds(HORIZON));
    }

    /**
     * No prediction returned unless at least one {@link MetricValue} has been
     * observed.
     */
    @Test
    public void predictWithoutObservingAnyMetricValues() throws Exception {
        int HORIZON = 180;
        this.predictor.configure(config(0.0));
        this.predictor.start();

        // note: no metric values in metric stream at this point
        Optional<Prediction> prediction = this.predictor.predict(machinePool(0), UtcTime.now().plusSeconds(HORIZON));

        Optional<Prediction> absent = Optional.empty();
        assertThat(prediction, is(absent));
    }

    /**
     * Verify that predictions are made based on the most recently observed
     * metric value.
     */
    @Test
    public void predict() throws PredictionException {
        DateTime now = UtcTime.now();

        int HORIZON = 180;
        this.predictor.configure(config(0.0));
        this.predictor.start();

        // stream some values to predictor
        streamLoadMetric(new MetricValue(METRIC_NAME, 1.0, now.minus(100)));
        streamLoadMetric(new MetricValue(METRIC_NAME, 2.0, now.minus(50)));
        streamLoadMetric(new MetricValue(METRIC_NAME, 3.0, now.minus(1)));

        // note: new metric values available in stream
        DateTime predictionTime = UtcTime.now().plusSeconds(HORIZON);
        Optional<Prediction> prediction = this.predictor.predict(machinePool(0), predictionTime);
        assertThat(prediction.isPresent(), is(true));
        assertThat(prediction.get().getTimestamp(), is(predictionTime));
        assertThat(prediction.get().getMetric(), is(METRIC_NAME));
        assertThat(prediction.get().getUnit(), is(PredictionUnit.METRIC));
        assertThat(prediction.get().getValue(), is(3.0));

        // make a new prediction that relies on historical observation
        // (stream has been emptied in last resize iteration)
        FrozenTime.tick(60);
        predictionTime = UtcTime.now().plusSeconds(HORIZON);
        prediction = this.predictor.predict(machinePool(0), predictionTime);
        assertThat(prediction.isPresent(), is(true));
        assertThat(prediction.get().getTimestamp(), is(predictionTime));
        assertThat(prediction.get().getValue(), is(3.0));

    }

    /**
     * Predict when a {@code safetyMargin} has been specified.
     */
    @Test
    public void predictWithSafetyMargin() throws PredictionException {
        DateTime now = UtcTime.now();

        int HORIZON = 180;
        double safetyMargin = 20.0; // in percent
        this.predictor.configure(config(safetyMargin));
        this.predictor.start();

        // stream some values to predictor
        streamLoadMetric(new MetricValue(METRIC_NAME, 1.0, now.minus(100)));
        streamLoadMetric(new MetricValue(METRIC_NAME, 5.0, now.minus(50)));
        streamLoadMetric(new MetricValue(METRIC_NAME, 10.0, now.minus(1)));

        // note: new metric values available in stream
        DateTime predictionTime = UtcTime.now().plusSeconds(HORIZON);
        Optional<Prediction> prediction = this.predictor.predict(machinePool(0), predictionTime);
        assertThat(prediction.isPresent(), is(true));
        // verify that safety margin is added to prediction
        assertThat(prediction.get().getValue(), is(10.0 * 1.20));
    }

    /**
     * The predictor does not rely on the current pool size and should be able
     * to handle situations where it is missing (for example, due to the cloud
     * pool/cloud provider temporarily being unreachable).
     */
    @Test
    public void predictWithUnknownPoolSize() throws Exception {
        DateTime now = UtcTime.now();
        int HORIZON = 180;
        this.predictor.configure(config(0.0));
        this.predictor.start();

        // stream some values to predictor
        streamLoadMetric(new MetricValue(METRIC_NAME, 1.0, now.minus(100)));

        Optional<PoolSizeSummary> absentPoolSize = Optional.empty();
        DateTime predictionTime = UtcTime.now().plusSeconds(HORIZON);
        Optional<Prediction> prediction = this.predictor.predict(absentPoolSize, predictionTime);
        assertThat(prediction.isPresent(), is(true));
    }

    private PredictorConfig config(double safetyMargin) {
        return new PredictorConfig("p1", ReactivePredictor.class.getName(), State.STARTED, METRIC_STREAM_ID,
                JsonUtils.toJson(new ReactivePredictorParams(safetyMargin)).getAsJsonObject());
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
     * Pushes a {@link MetricValue} onto the {@link EventBus}, which the
     * {@link Predictor}'s {@link MetricStreamReader} will capture.
     *
     * @param requestRate
     *            The load to be reported.
     * @param timestamp
     *            The time stamp of the metric value.
     */
    private void streamLoadMetric(MetricValue value) {
        eventBus.post(new MetricStreamMessage(METRIC_STREAM_ID, asList(value)));
    }
}
