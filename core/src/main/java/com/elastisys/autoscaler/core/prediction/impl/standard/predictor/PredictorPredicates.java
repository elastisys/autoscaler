package com.elastisys.autoscaler.core.prediction.impl.standard.predictor;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;

/**
 * A collection of {@link Predicate}s that operate on {@link Predictor}s.
 */
public class PredictorPredicates {

    private PredictorPredicates() {
        throw new UnsupportedOperationException(
                PredictorPredicates.class.getSimpleName() + " not intended to be instantiated.");
    }

    /**
     * {@link Predicate} that given a {@link Predictor} finds out if its started
     * or not.
     *
     * @return
     */
    public static Predicate<Predictor> isStarted() {
        return p -> p.getStatus().getState() == State.STARTED;
    }

    /**
     * Returns a {@link Predicate} function that, when applied to a
     * {@link PredictorConfig}, returns <code>true</code> if it has an
     * identifier that is included in the provided set of identifiers.
     *
     * @param ids
     *            The set of identifiers to match {@link PredictorConfig}s
     *            against.
     * @return The {@link Predicate} function.
     */
    public static Predicate<PredictorConfig> withIdIn(final Set<String> ids) {
        return predictorConfig -> ids.contains(predictorConfig.getId());
    }

    /**
     * Returns a {@link Predicate} function that, when applied to a
     * {@link Predictor}, returns <code>true</code> if the {@link Predictor} has
     * an identifier that is equal to a given identifier.
     *
     * @param ids
     *            The identifier to match {@link Predictor}s against.
     * @return The {@link Predicate} function.
     */
    public static Predicate<Predictor> withId(final String id) {
        return predictor -> predictor.getConfiguration().getId().equals(id);
    }

    /**
     * Returns a {@link Predicate} function that, when applied to a
     * {@link Predictor}, returns <code>true</code> if the {@link Predictor} has
     * a configuration that is included in the provided set of configurations.
     *
     * @param ids
     *            The set of configurations to match {@link Predictor}s against.
     * @return The {@link Predicate} function.
     */
    public static Predicate<Predictor> withConfigIn(final Collection<PredictorConfig> configurations) {
        return predictor -> configurations.contains(predictor.getConfiguration());
    }

}
