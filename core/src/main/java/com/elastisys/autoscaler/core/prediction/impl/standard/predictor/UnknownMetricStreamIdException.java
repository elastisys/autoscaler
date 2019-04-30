package com.elastisys.autoscaler.core.prediction.impl.standard.predictor;

import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;

/**
 * Thrown by the {@link PredictorRegistry} on failure to validate the metric
 * stream subscription of a {@link Predictor}.
 */
class UnknownMetricStreamIdException extends IllegalArgumentException {

    /**
     * Constructs a new {@link UnknownMetricStreamIdException}.
     */
    public UnknownMetricStreamIdException() {
        super();
    }

    /**
     * Constructs a new {@link UnknownMetricStreamIdException}.
     *
     * @param message
     * @param cause
     */
    public UnknownMetricStreamIdException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link UnknownMetricStreamIdException}.
     *
     * @param message
     */
    public UnknownMetricStreamIdException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link UnknownMetricStreamIdException}.
     *
     * @param cause
     */
    public UnknownMetricStreamIdException(Throwable cause) {
        super(cause);
    }

}
