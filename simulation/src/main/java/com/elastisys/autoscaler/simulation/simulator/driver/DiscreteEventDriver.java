package com.elastisys.autoscaler.simulation.simulator.driver;

import java.util.Optional;

import org.joda.time.DateTime;

/**
 * A discrete event simulator that allows {@link Event}s, each with an
 * associated action, to be scheduled for future execution.
 * <p/>
 * Added {@link Event}s execute an associated {@link EventAction} which, besides
 * performing some action, will be passed a {@link EventContext} through which
 * it can access the {@link DiscreteEventDriver} to schedule additional
 * {@link Event}s for execution.
 *
 * @see #addEvent(Event)
 * @see DiscreteEventDriver#run()
 */
public interface DiscreteEventDriver {

    /**
     * Initializes/resets the simulator by clearing any events from the event
     * queue and setting the start time (and optionally the end time) of the
     * event driver.
     * <p/>
     * This methods needs to be invoked prior to adding events or running the
     * event driver.
     *
     * @param simulationStartTime
     *            The time to set the simulation time to at the start of the
     *            simulation. It will not be allowed to add {@link Event}s with
     *            a simulation time earlier than this time.
     * @param simulationEndTime
     *            A (optional) time at which the simulation will be forcefully
     *            ended, even if there are more {@link Event}s in the
     *            {@link #eventQueue}. Note that the simulation can end earlier
     *            if the {@link #eventQueue} becomes empty.
     */
    void initialize(DateTime simulationStartTime, Optional<DateTime> simulationEndTime);

    /**
     * Schedules an {@link Event} for future execution.
     *
     * @param event
     *            The {@link Event} to be executed.
     * @throws IllegalArgumentException
     *             If the {@link Event}'s time lies in the past.
     */
    void addEvent(Event event);

    /**
     * Executes all scheduled {@link Event}s in increasing order of scheduling
     * time until the event queue is empty or until the simulation end time is
     * reached (if one was set on {@link #initialize()}).
     *
     * @throws EventException
     */
    void run() throws EventException;

    /**
     * Returns <code>true</code> if there are more unprocessed {@link Event}s in
     * the event queue.
     *
     * @return
     */
    boolean hasMoreEvents();

    /**
     * Returns <code>true</code> if an {@link Event} with an event action of a
     * particular type is scheduled for future execution.
     *
     * @param eventActionClass
     *            An {@link EventAction} realization class.
     * @return <code>true</code> if such an {@link Event} is found in the event
     *         queue, <code>false</code> otherwise.
     */
    boolean containsEventAction(Class<? extends EventAction> eventActionClass);

    /**
     * Returns the number of {@link Event}s with a particular event action that
     * are currently scheduled for future execution.
     *
     * @param eventActionClass
     *            An {@link EventAction} realization class.
     * @return The number of {@link Event}s in the queue with a event action of
     *         the specified type.
     */
    int numQueuedEventActions(Class<? extends EventAction> eventActionClass);

    /**
     * Returns the current simulation time.
     *
     * @return the current time in the simulation.
     */
    DateTime getSimulationTime();

    /**
     * Returns the start time of the simulation.
     *
     * @return the simulation start time.
     */
    DateTime getSimulationStart();

}