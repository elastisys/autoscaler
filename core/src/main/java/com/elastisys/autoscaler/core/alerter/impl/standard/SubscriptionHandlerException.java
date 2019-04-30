package com.elastisys.autoscaler.core.alerter.impl.standard;

/**
 * Thrown by a {@link SubscriptionHandler} to indicate an error condition.
 * 
 * @see SubscriptionHandler
 * 
 */
public class SubscriptionHandlerException extends Exception {

    /**
     * Constructs a new {@link SubscriptionHandlerException}.
     */
    public SubscriptionHandlerException() {
        super();
    }

    /**
     * Constructs a new {@link SubscriptionHandlerException}.
     * 
     * @param message
     * @param cause
     */
    public SubscriptionHandlerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link SubscriptionHandlerException}.
     * 
     * @param message
     */
    public SubscriptionHandlerException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link SubscriptionHandlerException}.
     * 
     * @param cause
     */
    public SubscriptionHandlerException(Throwable cause) {
        super(cause);
    }

}
