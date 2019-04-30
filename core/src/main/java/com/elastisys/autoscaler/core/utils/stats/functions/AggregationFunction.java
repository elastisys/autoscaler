package com.elastisys.autoscaler.core.utils.stats.functions;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A function that, when applied to a {@link List} of values, calculates a
 * single aggregate value.
 *
 * @param <T>
 *            The type of values being aggregated.
 */
public interface AggregationFunction<T> extends Function<Collection<T>, Optional<T>> {
    /**
     * Returns a single aggregate value from a {@link Collection} of values.
     *
     * @param values
     *            The values to be aggregated. Assumed to be ordered in
     *            chronological order where the value at index {@code 0} is the
     *            oldest (first) observation and the last index holds the most
     *            recently observed value.
     * @return The aggregate value, if one can be calculated, or
     *         {@link Optional#absent()} if no aggregate value could be
     *         calculated.
     */
    @Override
    Optional<T> apply(Collection<T> values);
}
