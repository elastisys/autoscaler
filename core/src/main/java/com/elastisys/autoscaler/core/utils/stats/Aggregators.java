package com.elastisys.autoscaler.core.utils.stats;

import java.util.Collection;

import com.elastisys.autoscaler.core.utils.stats.functions.AverageFunction;

/**
 * A utility class with methods for calculating various aggregation functions on
 * value sequences.
 *
 *
 */
public class Aggregators {

    private Aggregators() {
        throw new UnsupportedOperationException(Aggregators.class.getName() + " is not instantiable.");
    }

    /**
     * Returns the arithmetic mean of a collection of values.
     *
     * <pre>
     * mean = sum(x_i) / n
     * </pre>
     *
     * with {@code n} being the number of observations.
     * <p/>
     * <i>Note: when passed an empty collection, {@link Double#NaN} is
     * returned.</i>
     * <p/>
     * <i>Note: when passed a collection containing one or more
     * {@link Double#NaN} values, {@link Double#NaN} is returned.</i>
     *
     * @param values
     *            The values for which to calculate the mean.
     * @return The mean of the values.
     */
    public static double average(Collection<Double> values) {
        return new AverageFunction().apply(values).orElse(Double.NaN);
    }
}
