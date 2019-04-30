package com.elastisys.autoscaler.core.metronome.impl.standard;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.metronome.api.MetronomeEvent;
import com.elastisys.autoscaler.core.metronome.impl.standard.config.StandardMetronomeConfig;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.Gson;

/**
 * Verifies the behavior of the {@link StandardMetronome} {@link Service}.
 */
@SuppressWarnings("rawtypes")
public class TestStandardMetronome {
    static Logger logger = LoggerFactory.getLogger(TestStandardMetronome.class);

    /** The prediction horizon. */
    private static final TimeInterval PREDICTION_HORIZON = TimeInterval.seconds(600);
    /** The interval between two resize iterations. */
    private static final TimeInterval METRONOME_PERIOD = TimeInterval.seconds(60);

    private EventBus eventBus = new SynchronousEventBus(logger);
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private PredictionSubsystem predictionSubsystemMock = mock(PredictionSubsystem.class);
    private CloudPoolProxy cloudPoolMock = mock(CloudPoolProxy.class);

    /** Object under test. */
    private StandardMetronome metronome;

    @Before
    public void onSetup() {
        this.executor.setRemoveOnCancelPolicy(true);

        this.metronome = new StandardMetronome(logger, this.eventBus, this.executor, this.predictionSubsystemMock,
                this.cloudPoolMock);
    }

    @Test
    public void configureWithCorrectConfig() {
        StandardMetronomeConfig config = new StandardMetronomeConfig(PREDICTION_HORIZON, METRONOME_PERIOD, false);
        this.metronome.validate(config);
        this.metronome.configure(config);
        assertThat(this.metronome.getConfiguration(), is(config));
    }

    @Test
    public void configureWithJsonConfig() {
        StandardMetronomeConfig config = jsonConfig(
                "{interval: {\"time\": 1, \"unit\": \"minutes\"}, horizon: {\"time\": 10, \"unit\": \"minutes\"}}");
        this.metronome.validate(config);
        this.metronome.configure(config);
        assertThat(this.metronome.getConfiguration(),
                is(new StandardMetronomeConfig(PREDICTION_HORIZON, METRONOME_PERIOD, null)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithMissingHorizon() {
        StandardMetronomeConfig metronomeConfig = jsonConfig("{interval: {\"time\": 1, \"unit\": \"minutes\"}}");
        this.metronome.validate(metronomeConfig);
        // fault expected
    }

    /**
     * interval is an optional argument
     */
    @Test
    public void configureWithMissingInterval() {
        StandardMetronomeConfig metronomeConfig = jsonConfig("{horizon: {\"time\": 10, \"unit\": \"minutes\"}}");
        this.metronome.validate(metronomeConfig);

        // default should be provided
        assertThat(metronomeConfig.getInterval(), is(StandardMetronomeConfig.DEFAULT_METRONOME_INTERVAL));
    }

    @Test
    public void testStartAndStop() {
        assertStopped();

        StandardMetronomeConfig config = new StandardMetronomeConfig(PREDICTION_HORIZON, METRONOME_PERIOD, false);
        this.metronome.validate(config);
        this.metronome.configure(config);
        assertStopped();

        this.metronome.start();
        assertStarted();

        this.metronome.stop();
        assertStopped();
    }

    @Test
    public void testRestart() {
        StandardMetronomeConfig config = new StandardMetronomeConfig(PREDICTION_HORIZON, METRONOME_PERIOD, false);
        this.metronome.validate(config);
        this.metronome.configure(config);
        this.metronome.start();
        assertStarted();

        this.metronome.stop();
        assertStopped();

        this.metronome.start();
        assertStarted();
    }

    @Test(expected = IllegalStateException.class)
    public void testStartBeforeConfigure() throws Exception {
        this.metronome.start();
    }

    @Test
    public void testReconfigure() throws Exception {
        StandardMetronomeConfig oldConfig = new StandardMetronomeConfig(PREDICTION_HORIZON, METRONOME_PERIOD, false);
        this.metronome.validate(oldConfig);
        this.metronome.configure(oldConfig);
        this.metronome.start();
        assertStarted();
        assertThat(this.metronome.getConfiguration(), is(oldConfig));

        StandardMetronomeConfig newConfig = new StandardMetronomeConfig(TimeInterval.seconds(300),
                TimeInterval.seconds(30), false);
        this.metronome.validate(newConfig);
        this.metronome.configure(newConfig);
        assertStarted();
        assertThat(this.metronome.getConfiguration(), is(newConfig));
    }

    /**
     * Invoking {@link Service#start()} on an already started {@link Service}
     * should be a no-op.
     */
    @Test
    public void startWhenAlreadyStarted() throws Exception {
        assertThat(this.executor.getTaskCount(), is(0L));

        StandardMetronomeConfig config = new StandardMetronomeConfig(PREDICTION_HORIZON, METRONOME_PERIOD, false);
        this.metronome.configure(config);
        this.metronome.start();

        // starting the metronome should schedule two tasks: resize loop and
        // pool reporter
        assertThat(this.executor.getQueue().size(), is(2));

        // calling start() again should not start any new tasks
        this.metronome.start();
        assertThat(this.executor.getQueue().size(), is(2));
    }

    /**
     * Any started tasks are to be stopped whenever {@link Service#stop()} is
     * called.
     */
    @Test
    public void stopShouldCancelStartedTasks() throws Exception {
        assertThat(this.executor.getTaskCount(), is(0L));

        StandardMetronomeConfig config = new StandardMetronomeConfig(PREDICTION_HORIZON, METRONOME_PERIOD, false);
        this.metronome.configure(config);
        this.metronome.start();
        assertThat(this.executor.getQueue().size(), is(2));

        // verify that periodical tasks are stopped on stop()
        this.metronome.stop();
        assertThat(this.executor.getQueue().size(), is(0));
        // calling stop on an already stopped service is a no-op
        this.metronome.stop();
        assertThat(this.executor.getQueue().size(), is(0));
    }

    /**
     * Whenever a {@link MetronomeEvent#RESIZE_ITERATION} is received over the
     * {@link EventBus} (typically, sent by a {@link MetricStreamer} after
     * delivering a new batch of metrics) the {@link StandardMetronome} should
     * start a resize iteration.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void shouldRunNewResizeIterationOnEvent() throws Exception {
        StandardMetronomeConfig config = new StandardMetronomeConfig(PREDICTION_HORIZON, METRONOME_PERIOD, false);
        this.metronome.configure(config);
        this.metronome.start();

        // when called, prediction subsystem will produce a prediction
        Optional<Integer> computeUnitPrediction = Optional.of(1);
        when(this.predictionSubsystemMock.predict(argThat(is(any(Optional.class))), argThat(is(any(DateTime.class)))))
                .thenReturn(computeUnitPrediction);

        verifyZeroInteractions(this.predictionSubsystemMock, this.cloudPoolMock);

        // trigger resize iteration via event on the event bus
        this.eventBus.post(MetronomeEvent.RESIZE_ITERATION);

        // verify that the resize iteration was carried out
        verify(this.predictionSubsystemMock).predict(argThat(is(any(Optional.class))),
                argThat(is(any(DateTime.class))));
        verify(this.cloudPoolMock).setDesiredSize(1);
    }

    /**
     * Ensure that resize iterations are never carried out concurrently.
     */
    @Test
    public void shouldNeverExecuteResizeIterationsConcurrently() throws Exception {
        StandardMetronomeConfig config = new StandardMetronomeConfig(PREDICTION_HORIZON, METRONOME_PERIOD, false);
        this.metronome.configure(config);
        this.metronome.start();

        // tracks the execution times of resize iterations
        List<Interval> executionTimings = new ArrayList<>();

        // run prediction after some delay and record when it was executed
        Optional<Integer> computeUnitPrediction = Optional.of(1);
        Callable<Optional<Integer>> predictExecution = () -> {
            DateTime start = UtcTime.now();
            try {
                Thread.sleep(50);
                return computeUnitPrediction;
            } finally {
                DateTime end = UtcTime.now();
                executionTimings.add(new Interval(start, end));
            }
        };
        when(this.predictionSubsystemMock.predict(argThat(is(any(Optional.class))), argThat(is(any(DateTime.class)))))
                .then(invocation -> predictExecution.call());

        verifyZeroInteractions(this.predictionSubsystemMock);

        // run concurrent resize iterations
        Thread thread1 = new Thread(() -> this.metronome.doResizeIteration());
        Thread thread2 = new Thread(() -> this.metronome.doResizeIteration());
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // verify that both resize iterations were carried out and did not
        // overlap in time
        verify(this.predictionSubsystemMock, times(2)).predict(argThat(is(any(Optional.class))),
                argThat(is(any(DateTime.class))));
        verify(this.cloudPoolMock, times(2)).setDesiredSize(1);
        assertThat(executionTimings.size(), is(2));
        assertTrue(executionTimings.get(0).isBefore(executionTimings.get(1)));
    }

    private StandardMetronomeConfig jsonConfig(String json) {
        return new Gson().fromJson(json, StandardMetronomeConfig.class);
    }

    /**
     * Asserts that the metronome under test is started.
     */
    private void assertStarted() {
        assertThat(this.metronome.getStatus().getState(), is(State.STARTED));
        assertTrue(this.metronome.isStarted());
    }

    /**
     * Asserts that the metronome under test is stopped.
     */
    private void assertStopped() {
        assertThat(this.metronome.getStatus().getState(), is(State.STOPPED));
        assertFalse(this.metronome.isStarted());
    }
}
