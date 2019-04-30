package com.elastisys.autoscaler.simulation.simulator.driver;

import org.joda.time.DateTime;

/**
 * An event to be executed at a particular point in time as part of a discrete
 * event simulation. {@link Event}s are scheduled for exeuction with a
 * {@link DiscreteEventDriver}. The {@link Event} has an associated
 * {@link EventAction} that captures any action that the {@link Event} performs.
 * 
 * @see EventAction
 * @see DiscreteEventDriver
 * 
 * 
 */
public class Event implements Comparable<Event> {

    /**
     * The time at which the {@link Event} is scheduled for execution by the
     * {@link DiscreteEventDriver}.
     */
    private DateTime scheduledTime;

    /**
     * The action that will be performed when this {@link Event} is triggered.
     */
    private EventAction eventAction;

    /**
     * 
     * @param scheduledTime
     * @param eventAction
     */
    public Event(DateTime scheduledTime, EventAction eventAction) {
        this.scheduledTime = scheduledTime;
        this.eventAction = eventAction;
    }

    public DateTime getScheduledTime() {
        return this.scheduledTime;
    }

    public EventAction getEventAction() {
        return this.eventAction;
    }

    @Override
    public int compareTo(Event other) {
        return this.scheduledTime.compareTo(other.scheduledTime);
    }
}
