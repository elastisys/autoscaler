package com.elastisys.autoscaler.simulation.simulator.driver;

/**
 * An event thrown by the {@link DiscreteEventDriver} to signal a problem to
 * execute an {@link Event} action.
 * 
 * 
 * 
 */
public class EventException extends Exception {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    public EventException() {
        super();
    }

    /**
     * @param message
     * @param cause
     */
    public EventException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     */
    public EventException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public EventException(Throwable cause) {
        super(cause);
    }
}
