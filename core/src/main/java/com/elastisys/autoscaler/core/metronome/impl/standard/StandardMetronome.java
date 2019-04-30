package com.elastisys.autoscaler.core.metronome.impl.standard;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.Health;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.metronome.api.Metronome;
import com.elastisys.autoscaler.core.metronome.api.MetronomeEvent;
import com.elastisys.autoscaler.core.metronome.impl.standard.config.StandardMetronomeConfig;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;
import com.elastisys.scale.commons.eventbus.AllowConcurrentEvents;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;

/**
 * Standard {@link Metronome} implementation which carries out resize iterations
 * with a fixed delay and, on each iteration, predicts the future service load
 * at a specified point in the future (at the horizon), and resizes the
 * application appropriately.
 * <p/>
 * The {@link PredictionSubsystem} is delegated the task of predicting future
 * machine need and a {@link CloudPoolProxy} is delegated the task of carrying
 * out the resize action.
 *
 * @see PredictionSubsystem
 * @see CloudPoolProxy
 */
@SuppressWarnings("rawtypes")
public class StandardMetronome implements Metronome<StandardMetronomeConfig> {
    private final Logger logger;
    private final EventBus eventBus;
    private final ScheduledExecutorService executorService;

    /** The task that performs resize iterations. Runs in a separate thread. */
    private final ResizeLoop resizeLoop;
    /** Tracks the execution of the currently running {@link ResizeLoop}. */
    private ScheduledFuture<?> ongoingLoop = null;

    /** The task periodically reports the current pool membership. */
    private final MachinePoolReporter machinePoolReporter;
    /**
     * Tracks the execution of the currently running
     * {@link MachinePoolReporter}.
     */
    private ScheduledFuture<?> ongoingPoolReporter = null;

    /** Lock ensuring that resize iterations are never concurrent. */
    private final Object resizeLock = new Object();

    @Inject
    public StandardMetronome(Logger logger, EventBus eventBus, ScheduledExecutorService executor,
            PredictionSubsystem predictionSubsystem, CloudPoolProxy cloudPool) {
        this.logger = logger;
        this.eventBus = eventBus;
        this.executorService = executor;

        this.machinePoolReporter = new MachinePoolReporter(logger, eventBus, cloudPool);
        this.resizeLoop = new ResizeLoop(logger, eventBus, cloudPool, predictionSubsystem);
    }

    @Override
    public void validate(StandardMetronomeConfig configuration) throws IllegalArgumentException {
        checkArgument(configuration != null, "metronome: configuration cannot be null");
        configuration.validate();
    }

    @Override
    public synchronized void configure(StandardMetronomeConfig configuration) throws IllegalArgumentException {
        validate(configuration);
        this.resizeLoop.setConfig(configuration);

        if (isStarted()) {
            stop();
            start();
        }
    }

    @Override
    public synchronized void start() {
        checkState(this.resizeLoop.getConfig() != null, "cannot start resize loop before it is configured");

        if (isStarted()) {
            return;
        }

        this.eventBus.register(this);

        long delay = getConfiguration().getInterval().getTime();
        TimeUnit unit = getConfiguration().getInterval().getUnit();
        // start resize loop
        this.ongoingLoop = this.executorService.scheduleWithFixedDelay(() -> doResizeIteration(), delay, delay, unit);
        // start pool membership reporter
        this.ongoingPoolReporter = this.executorService.scheduleWithFixedDelay(this.machinePoolReporter, delay, delay,
                unit);

        this.logger.info(getClass().getSimpleName() + " started.");
    }

    @Override
    public synchronized void stop() {
        if (!isStarted()) {
            return;
        }

        // interrupt ongoing execution
        if (this.ongoingLoop != null) {
            this.ongoingLoop.cancel(true);
            this.ongoingLoop = null;
        }
        if (this.ongoingPoolReporter != null) {
            this.ongoingPoolReporter.cancel(true);
            this.ongoingPoolReporter = null;
        }

        this.eventBus.unregister(this);

        this.logger.info(getClass().getSimpleName() + " stopped.");
    }

    public boolean isStarted() {
        return this.ongoingLoop != null;
    }

    @Override
    public ServiceStatus getStatus() {
        Optional<String> detail = Optional.empty();
        State state = isStarted() ? State.STARTED : State.STOPPED;
        Optional<Throwable> lastFailure = this.resizeLoop.getLastFailure();
        if (lastFailure.isPresent()) {
            String faultMessage = lastFailure.get().getMessage();
            return new ServiceStatus(state, Health.NOT_OK, Optional.of(faultMessage));
        }
        return new ServiceStatus(state, Health.OK, detail);
    }

    @Override
    public synchronized StandardMetronomeConfig getConfiguration() {
        return this.resizeLoop.getConfig();
    }

    @Override
    public Class<StandardMetronomeConfig> getConfigurationClass() {
        return StandardMetronomeConfig.class;
    }

    /**
     * Carries out a resize iteration (a new round of predictions and a
     * cloudpool resize).
     */
    public void doResizeIteration() {
        synchronized (this.resizeLock) {
            this.resizeLoop.run();
        }
    }

    @Subscriber
    @AllowConcurrentEvents
    public void onResizeIterationTrigger(MetronomeEvent event) {
        if (event == MetronomeEvent.RESIZE_ITERATION) {
            this.logger.debug("resize iteration triggered over event bus");
            doResizeIteration();
        }
    }
}
