package com.elastisys.autoscaler.simulation.simulator.driver;

import static java.lang.String.format;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.commons.util.time.FrozenTime;

/**
 * A simple {@link DiscreteEventDriver} implementation.
 *
 * @see DiscreteEventDriver
 */
public class StandardEventDriver implements DiscreteEventDriver {
    /** {@link Logger} instance. */
    private static Logger LOG = LoggerFactory.getLogger(StandardEventDriver.class);

    /** Queue of events ordered in increasing order of scheduled time. */
    private Queue<Event> eventQueue = new PriorityQueue<Event>();

    /** The current simulated time. */
    private DateTime simulationTime = null;
    /**
     * The time to set the simulation time to at the start of the simulation. It
     * will not be allowed to add {@link Event}s with a simulation time earlier
     * than this time.
     */
    private DateTime simulationStartTime = null;
    /**
     * A (optional) time at which the simulation will be forcefully ended, even
     * if there are more {@link Event}s in the {@link #eventQueue}. Note that
     * the simulation can end earlier if the {@link #eventQueue} becomes empty.
     */
    private Optional<DateTime> simulationEndTime = Optional.empty();

    /**
     * If <code>true</code>, the event driver will introduce delays to mimic the
     * real delays between events. So if one event is scheduled at time
     * <code>t</code> and another at time <code>t+3</code>, the event driver
     * will wait for three seconds until executing the second event. If
     * <code>false</code>, events will be processed as fast as possible without
     * delays.
     */
    private Boolean playbackWithRealDelays;

    /** The real system (wallclock) time at the start of the simulation. */
    private long systemTimeAtStart = 0;

    /**
     * Constructs a new {@link StandardEventDriver}.
     *
     * @param playbackWithRealDelays
     *            If <code>true</code>, the event driver will introduce delays
     *            to mimic the real delays between events. So if one event is
     *            scheduled at time <code>t</code> and another at time
     *            <code>t+3</code>, the event driver will wait for (roughly)
     *            three seconds until executing the second event. If
     *            <code>false</code>, events will be processed as fast as
     *            possible without delays.
     */
    public StandardEventDriver(Boolean playbackWithRealDelays) {
        this.playbackWithRealDelays = playbackWithRealDelays;
    }

    @Override
    public void initialize(DateTime simulationStartTime, Optional<DateTime> simulationEndTime) {
        if (simulationStartTime == null) {
            throw new IllegalArgumentException(
                    "a non-null simulation start time must " + "be passed to the " + this.getClass().getSimpleName());
        }
        this.eventQueue.clear();
        this.simulationStartTime = simulationStartTime;
        this.simulationEndTime = simulationEndTime;
        updateSimulationTime(simulationStartTime);
    }

    private void updateSimulationTime(DateTime simulationTime) {
        this.simulationTime = simulationTime;
        FrozenTime.setFixed(this.simulationTime);
    }

    private boolean isInitialized() {
        return this.simulationStartTime != null;
    }

    @Override
    public void addEvent(Event event) {
        Objects.requireNonNull(event, "attempt to add null event");
        if (!isInitialized()) {
            throw new IllegalStateException("initialize() needs to be invoked prior to addEvent()");
        }

        if (event.getScheduledTime().isBefore(this.simulationTime)) {
            String message = String.format("An attempt was made to schedule an event %s"
                    + " at a time (%s) that has already passed. " + "Current simulation time is %s.", event,
                    event.getScheduledTime(), this.simulationTime);
            throw new IllegalArgumentException(message);
        }
        this.eventQueue.add(event);
    }

    @Override
    public void run() throws EventException {
        if (!isInitialized()) {
            throw new IllegalStateException("initialize() needs to be invoked prior to run()");
        }

        LOG.debug("starting simulation at simulation start time {} (end time: {}) ...", this.simulationStartTime,
                this.simulationEndTime);
        this.systemTimeAtStart = System.currentTimeMillis();
        while (!this.eventQueue.isEmpty() && !simulationEndTimeReached()) {
            Event event = this.eventQueue.remove();
            if (this.playbackWithRealDelays) {
                waitForEvent(event);
            }
            updateSimulationTime(event.getScheduledTime());
            EventContext eventContext = new EventContext(this.simulationStartTime, this.simulationTime, this);

            EventAction action = event.getEventAction();
            if (LOG.isTraceEnabled()) {
                LOG.trace(format("{%s} executing event: %s", this.simulationTime, action.getClass().getSimpleName()));
            }
            try {
                action.execute(eventContext);
            } catch (Exception e) {
                throw new EventException(String.format("failed to execute event %s: %s",
                        event.getEventAction().getClass().getName(), e.getMessage()), e);
            }
        }
        LOG.debug("simulation done at simulation time {}. events left in queue: {}", this.simulationTime,
                this.eventQueue.size());
    }

    private boolean simulationEndTimeReached() {
        if (!this.simulationEndTime.isPresent()) {
            return false;
        }

        if (!this.simulationTime.isBefore(this.simulationEndTime.get())) {
            LOG.info("simulation has reached the end time {}", this.simulationEndTime.get());
            return true;
        }
        return false;
    }

    /**
     * Waits until the simulation time offset of an event
     *
     * <pre>
     * eventTime - simulationStartTime
     * </pre>
     *
     * is equal to the current real time offset:
     *
     * <pre>
     * now - startTime
     * </pre>
     *
     * This is used to introduce delays between events when run in
     * {@link #playbackWithRealDelays} mode.
     *
     * @param event
     *            The event whose scheduled time we are to wait for.
     */
    private void waitForEvent(Event event) {
        // time (in ms) into the simulation when event is to occur
        long eventSimulationTimeOffset = event.getScheduledTime().getMillis() - this.simulationStartTime.getMillis();
        // the current time (in ms) into the simulation
        long currentRealTimeOffset = System.currentTimeMillis() - this.systemTimeAtStart;
        if (currentRealTimeOffset < eventSimulationTimeOffset) {
            try {
                // wait for the remaining time until the event is to take place
                Thread.sleep(eventSimulationTimeOffset - currentRealTimeOffset);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting to execute next event: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean hasMoreEvents() {
        return !this.eventQueue.isEmpty();
    }

    @Override
    public boolean containsEventAction(Class<? extends EventAction> eventActionClass) {
        Iterator<Event> iterator = this.eventQueue.iterator();
        while (iterator.hasNext()) {
            Event event = iterator.next();
            if (event.getEventAction().getClass() == eventActionClass) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int numQueuedEventActions(Class<? extends EventAction> eventActionClass) {
        int counter = 0;
        Iterator<Event> iterator = this.eventQueue.iterator();
        while (iterator.hasNext()) {
            Event event = iterator.next();
            if (event.getEventAction().getClass() == eventActionClass) {
                counter++;
            }
        }
        return counter;
    }

    @Override
    public DateTime getSimulationTime() {
        return this.simulationTime;
    }

    @Override
    public DateTime getSimulationStart() {
        return this.simulationStartTime;
    }
}
