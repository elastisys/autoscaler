package com.elastisys.autoscaler.core.metronome.impl.standard;

import static com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetric.COMPUTE_UNIT_PREDICTION;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.alerter.api.types.AlertTopics;
import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxyException;
import com.elastisys.autoscaler.core.metronome.impl.standard.config.StandardMetronomeConfig;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.autoscaler.core.prediction.api.PredictionException;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.util.time.FrozenTime;

/**
 * Verifies the behavior of the {@link ResizeLoop} class.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class TestResizeLoop {

    /** The prediction horizon. */
    private static final TimeInterval PREDICTION_HORIZON = TimeInterval.seconds(600);
    /** The interval between two resize iterations. */
    private static final TimeInterval METRONOME_PERIOD = TimeInterval.seconds(30);

    static Logger logger = LoggerFactory.getLogger(TestResizeLoop.class);

    private static DateTime NOW = FrozenTime.now();

    /** Object under test. */
    private ResizeLoop resizeLoop;

    // Mocks
    private EventBus eventBusMock = mock(EventBus.class);
    private CloudPoolProxy cloudPoolMock = mock(CloudPoolProxy.class);
    private PredictionSubsystem predictionSubsystemMock = mock(PredictionSubsystem.class);

    @Before
    public void onSetup() {
        this.resizeLoop = new ResizeLoop(logger, this.eventBusMock, this.cloudPoolMock, this.predictionSubsystemMock);
        this.resizeLoop.setConfig(new StandardMetronomeConfig(PREDICTION_HORIZON, METRONOME_PERIOD, false));
    }

    /**
     * Verifies that the {@link ResizeLoop} can be configured.
     */
    @Test
    public void testConfigure() {
        ResizeLoop loop = new ResizeLoop(logger, this.eventBusMock, this.cloudPoolMock, this.predictionSubsystemMock);
        assertThat(loop.getConfig(), is(nullValue()));
        StandardMetronomeConfig config = new StandardMetronomeConfig(PREDICTION_HORIZON, METRONOME_PERIOD, false);
        loop.setConfig(config);
        assertThat(loop.getConfig(), is(config));
    }

    /**
     * Verify that the {@link ResizeLoop} cannot be started prior to being
     * assigned a configuration.
     */
    @Test
    public void startBeforeConfigured() {
        FrozenTime.setFixed(NOW);

        this.resizeLoop = new ResizeLoop(logger, this.eventBusMock, this.cloudPoolMock, this.predictionSubsystemMock);
        // note: no config set up

        assertFalse(this.resizeLoop.getLastFailure().isPresent());
        this.resizeLoop.run();
        assertLoopFailure(IllegalStateException.class);

        // no calls to mock objects should have been made
        verifyNoMoreInteractions(this.predictionSubsystemMock);
        verifyNoMoreInteractions(this.eventBusMock);
        verifyNoMoreInteractions(this.cloudPoolMock);
    }

    /**
     * Verify behavior on successful invocation of both prediction subsystem and
     * cloud pool.
     */
    @Test
    public void testSuccessfulIteration() throws Exception {
        FrozenTime.setFixed(NOW);
        // set up mocked responses
        Optional<PoolSizeSummary> poolSize = poolSize(5, 5, 5);
        when(this.cloudPoolMock.getPoolSize()).thenReturn(poolSize.get());
        int computeUnitPrediction = 2;
        whenPredicting().thenReturn(Optional.of(computeUnitPrediction));

        // execute test
        assertFalse(this.resizeLoop.getLastFailure().isPresent());
        this.resizeLoop.run();
        assertFalse(this.resizeLoop.getLastFailure().isPresent());

        // verify calls to mock objects
        verify(this.predictionSubsystemMock).predict(poolSize, NOW.plus(PREDICTION_HORIZON.getMillis()));
        verifyNoMoreInteractions(this.predictionSubsystemMock);
        verify(this.cloudPoolMock).setDesiredSize(computeUnitPrediction);
        verify(this.cloudPoolMock).getPoolSize();
        verifyNoMoreInteractions(this.cloudPoolMock);
        // verify that a system metric event was produced with the prediction
        SystemMetricEvent expectedEvent = new SystemMetricEvent(
                new MetricValue(COMPUTE_UNIT_PREDICTION.getMetricName(), computeUnitPrediction, NOW));
        verify(this.eventBusMock).post(argThat(is(expectedEvent)));
        verifyNoMoreInteractions(this.eventBusMock);
    }

    /**
     * Verify that only the prediction subsystem and <b>not</b> the cloud pool
     * is called when running in log-only mode.
     */
    @Test
    public void testSuccessfulIterationInLogOnlyMode() throws Exception {
        boolean logOnlyMode = true;
        this.resizeLoop.setConfig(new StandardMetronomeConfig(PREDICTION_HORIZON, METRONOME_PERIOD, logOnlyMode));

        FrozenTime.setFixed(NOW);
        // set up mocked responses
        Optional<PoolSizeSummary> poolSize = poolSize(5, 5, 5);
        when(this.cloudPoolMock.getPoolSize()).thenReturn(poolSize.get());
        int computeUnitPrediction = 2;
        whenPredicting().thenReturn(Optional.of(computeUnitPrediction));

        // execute test
        this.resizeLoop.run();

        // verify calls to mock objects
        verify(this.predictionSubsystemMock).predict(poolSize, NOW.plus(PREDICTION_HORIZON.getMillis()));
        verifyNoMoreInteractions(this.predictionSubsystemMock);
        // note: explicitly verify that cloud pool is never asked to resize
        verify(this.cloudPoolMock).getPoolSize();
        verifyNoMoreInteractions(this.cloudPoolMock);
        // verify that a system metric event was produced with the prediction
        SystemMetricEvent expectedEvent = new SystemMetricEvent(
                new MetricValue(COMPUTE_UNIT_PREDICTION.getMetricName(), computeUnitPrediction, NOW));
        verify(this.eventBusMock).post(argThat(is(expectedEvent)));
        verifyNoMoreInteractions(this.eventBusMock);
    }

    /**
     * Verify behavior when {@link PredictionSubsystem} fails.
     *
     * @throws Exception
     */
    @Test
    public void testIterationWithFailingPrediction() throws Exception {
        FrozenTime.setFixed(NOW);
        Optional<PoolSizeSummary> poolSize = poolSize(5, 5, 5);
        when(this.cloudPoolMock.getPoolSize()).thenReturn(poolSize.get());
        whenPredicting().thenThrow(new PredictionException("couldn't predict"));

        // execute test
        assertFalse(this.resizeLoop.getLastFailure().isPresent());
        this.resizeLoop.run();
        assertLoopFailure(PredictionException.class);

        // verify calls to mock objects
        verify(this.cloudPoolMock).getPoolSize();
        // explicitly validate that cloud pool was not asked to resize
        verifyNoMoreInteractions(this.cloudPoolMock);
        verify(this.predictionSubsystemMock).predict(poolSize, NOW.plus(PREDICTION_HORIZON.getMillis()));
        verifyNoMoreInteractions(this.predictionSubsystemMock);
        // verify that no system metric event was sent with the prediction
        verifyNoMoreInteractions(this.eventBusMock);
    }

    /**
     * Verify behavior when {@link PredictionSubsystem} produces an
     * {@link Optional#absent()} prediction.
     *
     * @throws Exception
     */
    @Test
    public void testIterationWithAbsentPrediction() throws Exception {
        FrozenTime.setFixed(NOW);
        Optional<PoolSizeSummary> poolSize = poolSize(5, 5, 5);
        when(this.cloudPoolMock.getPoolSize()).thenReturn(poolSize.get());
        whenPredicting().thenReturn(Optional.empty());

        // execute test
        assertFalse(this.resizeLoop.getLastFailure().isPresent());
        this.resizeLoop.run();
        assertFalse(this.resizeLoop.getLastFailure().isPresent());

        // verify calls to mock objects
        verify(this.cloudPoolMock).getPoolSize();
        // explicitly validate that cloud pool was not asked to resize
        verifyNoMoreInteractions(this.cloudPoolMock);
        verify(this.predictionSubsystemMock).predict(poolSize, NOW.plus(PREDICTION_HORIZON.getMillis()));
        verifyNoMoreInteractions(this.predictionSubsystemMock);
        // verify that no system metric event was sent with prediction
        verifyNoMoreInteractions(this.eventBusMock);
    }

    /**
     * A failure to retrieve the pool size from the {@link CloudPoolProxy}
     * should not result in a failure of the resize iteration, but rather in
     * that an empty pool size value is passed to the
     * {@link PredictionSubsystem}.
     */
    @Test
    public void testIterationWhenFailingToFetchPoolSize() throws Exception {
        FrozenTime.setFixed(NOW);
        // set up mocked responses
        when(this.cloudPoolMock.getPoolSize()).thenThrow(new CloudPoolProxyException("couldn't connect"));
        // prediction subsystem should be called with an absent pool size value
        Optional<PoolSizeSummary> absentPoolSize = Optional.empty();
        int computeUnitPrediction = 2;
        when(this.predictionSubsystemMock.predict(absentPoolSize, NOW.plus(PREDICTION_HORIZON.getMillis())))
                .thenReturn(Optional.of(computeUnitPrediction));

        // execute test
        assertFalse(this.resizeLoop.getLastFailure().isPresent());
        this.resizeLoop.run();
        assertFalse(this.resizeLoop.getLastFailure().isPresent());

        // verify calls to mock objects
        verify(this.cloudPoolMock).getPoolSize();
        verify(this.predictionSubsystemMock).predict(absentPoolSize, NOW.plus(PREDICTION_HORIZON.getMillis()));
        verify(this.cloudPoolMock).setDesiredSize(computeUnitPrediction);
        // verify that a system metric event was produced with the prediction
        SystemMetricEvent expectedEvent = new SystemMetricEvent(
                new MetricValue(COMPUTE_UNIT_PREDICTION.getMetricName(), computeUnitPrediction, NOW));
        verify(this.eventBusMock).post(argThat(is(expectedEvent)));
        verifyNoMoreInteractions(this.eventBusMock);
    }

    /**
     * Verify behavior when cloud pool fails to resize application.
     *
     * @throws Exception
     */
    @Test
    public void testIterationWithFailingResize() throws Exception {
        FrozenTime.setFixed(NOW);
        // set up mocked responses
        Optional<PoolSizeSummary> poolSize = poolSize(5, 5, 5);
        when(this.cloudPoolMock.getPoolSize()).thenReturn(poolSize.get());
        int computeUnitPrediction = 3;
        whenPredicting().thenReturn(Optional.of(computeUnitPrediction));
        doThrow(new CloudPoolProxyException("couldn't resize")).when(this.cloudPoolMock)
                .setDesiredSize(computeUnitPrediction);

        // execute test
        assertFalse(this.resizeLoop.getLastFailure().isPresent());
        this.resizeLoop.run();
        assertLoopFailure(CloudPoolProxyException.class);

        // verify calls to mock objects
        verify(this.predictionSubsystemMock).predict(poolSize, NOW.plus(PREDICTION_HORIZON.getMillis()));
        verifyNoMoreInteractions(this.predictionSubsystemMock);
        verify(this.cloudPoolMock).getPoolSize();
        verify(this.cloudPoolMock).setDesiredSize(computeUnitPrediction);
        verifyNoMoreInteractions(this.cloudPoolMock);
        // verify that a system metric event was produced with the prediction
        SystemMetricEvent expectedEvent = new SystemMetricEvent(
                new MetricValue(COMPUTE_UNIT_PREDICTION.getMetricName(), computeUnitPrediction, NOW));
        verify(this.eventBusMock).post(argThat(is(expectedEvent)));
        verifyNoMoreInteractions(this.eventBusMock);
    }

    private OngoingStubbing<Optional> whenPredicting() throws PredictionException {
        return when(this.predictionSubsystemMock.predict(isA(Optional.class), isA(DateTime.class)));
    }

    /**
     * Asserts that the last {@link ResizeLoop} execution failed due to the an
     * error of a given type.
     *
     * @param cause
     *            The {@link Exception} class that caused the resize loop to
     *            fail.
     */
    private void assertLoopFailure(Class<? extends Exception> cause) {
        assertTrue(this.resizeLoop.getLastFailure().isPresent());
        assertThat(this.resizeLoop.getLastFailure().get(), instanceOf(cause));
        verify(this.eventBusMock).post(argThat(
                is(AlertMatcher.alert(AlertTopics.RESIZE_ITERATION_FAILURE.getTopicPath(), AlertSeverity.ERROR))));
    }

    private Optional<PoolSizeSummary> poolSize(int desiredSize, int allocated, int active) {
        return Optional.of(new PoolSizeSummary(desiredSize, allocated, active));
    }
}
