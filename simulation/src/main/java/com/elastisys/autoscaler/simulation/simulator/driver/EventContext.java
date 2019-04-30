package com.elastisys.autoscaler.simulation.simulator.driver;

import org.joda.time.DateTime;

/**
 * Carries contextual data for the execution of a particular
 * {@link EventAction}.
 */
public class EventContext {
    /** The simulation time at which the {@link Event} occurs. */
    private DateTime eventTime;
    /**
     * The driver of simulation events. The {@link EventAction} can use this to
     * schedule new {@link Event}s in the simulation.
     */
    private DiscreteEventDriver eventDriver;

    /** The simulation time at which the simulation started. */
    private DateTime simulationStart;

    /**
     * @param simulationStart
     * @param eventTime
     * @param eventDriver
     */
    public EventContext(DateTime simulationStart, DateTime eventTime, DiscreteEventDriver eventDriver) {
        this.simulationStart = simulationStart;
        this.eventTime = eventTime;
        this.eventDriver = eventDriver;
    }

    /**
     * The simulation time at which the {@link Event} occurs.
     *
     * @return the eventTime
     */
    public DateTime getEventTime() {
        return this.eventTime;
    }

    /**
     * The driver of simulation events. The {@link EventAction} can use this to
     * schedule new {@link Event}s in the simulation.
     *
     * @return the eventDriver
     */
    public DiscreteEventDriver getEventDriver() {
        return this.eventDriver;
    }

    /**
     * The simulation time at which the simulation started.
     * 
     * @return the simulationStart
     */
    public DateTime getSimulationStart() {
        return this.simulationStart;
    }

}
