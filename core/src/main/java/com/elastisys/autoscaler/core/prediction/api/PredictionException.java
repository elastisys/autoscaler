package com.elastisys.autoscaler.core.prediction.api;

/**
 * Thrown by the {@link PredictionSubsystem} to indicate a problem with
 * performing predictions.
 * 
 * @see PredictionSubsystem
 * 
 * 
 */
public class PredictionException extends Exception {
    /** Default serial UID */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@link PredictionException}.
     */
    public PredictionException() {
        super();
    }

    /**
     * Constructs a new {@link PredictionException}.
     * 
     * @param message
     * @param cause
     */
    public PredictionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link PredictionException}.
     * 
     * @param message
     */
    public PredictionException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link PredictionException}.
     * 
     * @param cause
     */
    public PredictionException(Throwable cause) {
        super(cause);
    }

}
