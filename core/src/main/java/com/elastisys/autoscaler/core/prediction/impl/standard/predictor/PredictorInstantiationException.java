package com.elastisys.autoscaler.core.prediction.impl.standard.predictor;

import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;

/**
 * Thrown by the {@link PredictorFactory} on failure to instantiate a
 * {@link Predictor}.
 *
 * @see PredictorFactory
 */
class PredictorInstantiationException extends RuntimeException {

    /**
     * Constructs a new {@link PredictorInstantiationException}.
     */
    public PredictorInstantiationException() {
        super();
    }

    /**
     * Constructs a new {@link PredictorInstantiationException}.
     *
     * @param message
     * @param cause
     */
    public PredictorInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link PredictorInstantiationException}.
     *
     * @param message
     */
    public PredictorInstantiationException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link PredictorInstantiationException}.
     *
     * @param cause
     */
    public PredictorInstantiationException(Throwable cause) {
        super(cause);
    }

}
