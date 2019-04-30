package com.elastisys.autoscaler.core.utils.stats.functions;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

/**
 * {@link AggregationFunction} that calculates the arithmetic mean of each value
 * series it is passed.
 * <p/>
 * The arithmetic mean of a collection of values is defined as:
 *
 * <pre>
 * mean = sum(x_i) / n
 * </pre>
 *
 * with {@code n} being the number of observations.
 * <p/>
 * <i>Note: when passed an empty collection, {@link Optional#absent()} is
 * returned.</i>
 * <p/>
 * <i>Note: when passed a collection containing one or more {@link Double#NaN}
 * values, {@link Double#NaN} is returned.</i>
 *
 *
 *
 */
public class AverageFunction implements AggregationFunction<Double> {

    @Override
    public Optional<Double> apply(Collection<Double> values) {
        Objects.requireNonNull(values, "value sequence cannot be null");
        if (values.isEmpty()) {
            return Optional.empty();
        }
        double[] array = values.stream().mapToDouble(d -> d).toArray();
        double mean = new Mean().evaluate(array);
        return Optional.of(mean);
    }

}
