package com.elastisys.autoscaler.core.utils.stats.functions;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * An {@link AggregationFunction} that calculates a <a href=
 * "https://en.wikipedia.org/wiki/Moving_average#Weighted_moving_average"
 * >weighted average</a> for a series of values with weights that decrease
 * linearly in arithmetical progression. For an n-point series, the latest point
 * has weight {@code n}, the second latest {@code n − 1}, etc., down to one.
 * <p>
 * {@code (p[N]*N + p[N-1]*(N-1) + ... + p[1]*1) / (N + (N-1) + ... + 1)} <br/>
 * Note that here, {@code pN} is assumed to be the most recent observation (to
 * which the greatest weight factor is applied).
 * </p>
 */
public class LinearlyWeightedAverageFunction implements AggregationFunction<Double> {

    @Override
    public Optional<Double> apply(Collection<Double> values) {
        Objects.requireNonNull(values, "value list cannot be null");
        if (values.isEmpty()) {
            return Optional.empty();
        }

        Double[] points = values.toArray(new Double[0]);
        double weightedSum = 0.0;
        int N = points.length;
        for (int i = N - 1; i >= 0; i--) {
            double weight = i + 1;
            weightedSum += points[i] * weight;
        }
        double weightSum = N * (N + 1) / 2;
        double weightedAverage = weightedSum / weightSum;

        return Optional.of(weightedAverage);
    }
}
