package com.elastisys.autoscaler.core.prediction.impl.standard.predictor;

import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;

/**
 * An enumeration of type aliases that can be used with the
 * {@link PredictorFactory} as a short-hand to refer to a certain
 * {@link Predictor} implementation class.
 */
public enum PredictorTypeAlias {
    /**
     * A compute unit predictor that suggests capacity changes from a collection
     * of rules, defining thresholds for the monitored metric and what scaling
     * actions to take when those thresholds are breached.
     */
    RuleBasedPredictor("com.elastisys.autoscaler.predictors.rulebased.RuleBasedPredictor"),
    /**
     * A naive, purely reactive, {@link Predictor} that simply repeats the
     * latest observed metric value as the predicted future value (using a
     * "tomorrow will probably be very similar to today"-style heuristic).
     */
    ReactivePredictor("com.elastisys.autoscaler.predictors.reactive.ReactivePredictor");

    /**
     * The full {@link Predictor} class name that this
     * {@link PredictorTypeAlias} refers to.
     */
    private final String fullClassName;

    private PredictorTypeAlias(String fullClassName) {
        this.fullClassName = fullClassName;
    }

    /**
     * The full {@link Predictor} class name that this
     * {@link PredictorTypeAlias} refers to.
     *
     * @return
     */
    public String getFullClassName() {
        return this.fullClassName;
    }
}
