package com.elastisys.autoscaler.core.prediction.impl.standard.aggregator;

import com.elastisys.autoscaler.core.prediction.api.types.Prediction;

/**
 * Thrown by an {@link Aggregator} to indicate a problem aggregating
 * {@link Prediction}s.
 * 
 * 
 * 
 */
public class AggregatorException extends Exception {

    /**
     * Constructs a new {@link AggregatorException}.
     */
    public AggregatorException() {
        super();
    }

    /**
     * Constructs a new {@link AggregatorException}.
     * 
     * @param message
     * @param cause
     */
    public AggregatorException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link AggregatorException}.
     * 
     * @param message
     */
    public AggregatorException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link AggregatorException}.
     * 
     * @param cause
     */
    public AggregatorException(Throwable cause) {
        super(cause);
    }
}
