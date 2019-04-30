package com.elastisys.autoscaler.core.utils.stats.functions;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Optional;

/**
 * An {@link AggregationFunction} that calculates a <a href=
 * "https://en.wikipedia.org/wiki/Moving_average#Weighted_moving_average"
 * >weighted average</a> for a series of values with weights that decrease
 * exponentially. For an n-point series, the latest point has weight
 * {@code (1-α)^0}, the second latest {@code (1-α)^1}), etc., down to one.
 * <p>
 * {@code (p1(1-α)^0 + p2(1-α)^1 + ... + pN(1-α)^(N-1)/((1-α)^0 + (1-α)^1 + ... + (1-α)^(N - 1))}
 * <br/>
 * Note that here, {@code pN} is assumed to be the oldest observation (to which
 * the biggest decay factor is applied). α is the decay factor: a value between
 * {@code 0} and {@code 1} determining how quickly to "forget" old values (a
 * high value discounts older observations faster).
 * </p>
 */
public class ExponentiallyWeightedAverageFunction implements AggregationFunction<Double> {

    public static final double DEFALT_DECAY_FACTOR = 0.5;

    /**
     * A coefficient that represents the degree of weighting decrease, a
     * constant smoothing factor between 0 and 1. A higher value discounts older
     * observations faster.
     */
    private final double decayFactor;

    /**
     * Constructs a new {@link ExponentiallyWeightedAverageFunction} with a
     * default decay factor of {@link #DEFALT_DECAY_FACTOR}.
     */
    public ExponentiallyWeightedAverageFunction() {
        this(DEFALT_DECAY_FACTOR);
    }

    /**
     * Constructs a new {@link ExponentiallyWeightedAverageFunction} with a
     * given decay factor.
     *
     * @param decayFactor
     *            A coefficient that represents the degree of weighting
     *            decrease, a constant smoothing factor between 0 and 1. A
     *            higher value discounts older observations faster.
     */
    public ExponentiallyWeightedAverageFunction(double decayFactor) {
        checkArgument(0 <= decayFactor && decayFactor <= 1, "decay factor must lie between 0 and 1");
        this.decayFactor = decayFactor;
    }

    @Override
    public Optional<Double> apply(Collection<Double> values) {
        requireNonNull(values, "value list cannot be null");
        if (values.isEmpty()) {
            return Optional.empty();
        }

        Double[] points = values.toArray(new Double[0]);
        double weightedSum = 0.0; // weighted sum of values
        double weightSum = 0.0; // sum of weights
        int N = points.length;
        for (int i = 0; i < N; i++) {
            // biggest decay factor applied to oldest value (i.e., first value)
            double weight = Math.pow(1 - this.decayFactor, N - 1 - i);
            weightedSum += points[i] * weight;
            weightSum += weight;
        }
        double weightedAverage = weightedSum / weightSum;
        return Optional.of(weightedAverage);
    }

}
