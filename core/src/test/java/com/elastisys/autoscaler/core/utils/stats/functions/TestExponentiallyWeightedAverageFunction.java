package com.elastisys.autoscaler.core.utils.stats.functions;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.core.utils.stats.functions.AggregationFunction;
import com.elastisys.autoscaler.core.utils.stats.functions.ExponentiallyWeightedAverageFunction;
import com.elastisys.autoscaler.core.utils.stats.functions.LinearlyWeightedAverageFunction;

/**
 * Exercises the {@link ExponentiallyWeightedAverageFunction} class.
 */
public class TestExponentiallyWeightedAverageFunction {

    /** Object under test. */
    private AggregationFunction<Double> function;

    @Before
    public void onSetup() {
        this.function = new ExponentiallyWeightedAverageFunction();
    }

    @Test
    public void onEmptyInput() {
        Optional<Double> absent = Optional.empty();
        assertThat(new LinearlyWeightedAverageFunction().apply(Collections.emptyList()), is(absent));
    }

    @Test
    public void onNonEmptyInput() {
        // decay factor is default: 0.5
        // (3.0*0.5^0) / (0.5^0)
        assertThat(this.function.apply(asList(3.0)).get(), is(3.0));
        // (2.0*0.5^1 + 3.0*0.5^0) / (0.5^1 + 0.5^0)
        assertThat(this.function.apply(asList(2.0, 3.0)).get(), is(4.0 / 1.5));
        // (1.0*0.5^3 + 2.0*0.5^2 + 3.0*0.5^1 + 8.0*0.5^0) / (0.5^3 + 0.5^2 +
        // 0.5^1 + 0.5^0)
        assertThat(this.function.apply(asList(1.0, 2.0, 3.0, 8.0)).get(), is(10.125 / 1.875));
    }

    @Test
    public void withNonDefaultDecayFactor() {
        double decayFactor = 0.4;
        this.function = new ExponentiallyWeightedAverageFunction(decayFactor);

        // (3.0*0.6^0) / (0.6^0)
        assertThat(this.function.apply(asList(3.0)).get(), is(3.0));
        // (2.0*0.6^1 + 3.0*0.6^0) / (0.6^1 + 0.6^0)
        assertThat(this.function.apply(asList(2.0, 3.0)).get(), is(4.2 / 1.6));
        // (1.0*0.6^2 + 2.0*0.6^1 + 3.0*0.6^0) / (0.6^2 + 0.6^1 + 0.6^0)
        assertThat(this.function.apply(asList(1.0, 2.0, 3.0)).get(), is(4.5600000000000005 / 1.96));

        // (1.0*0.6^3 + 2.0*0.6^2 + 3.0*0.6^1 + 8.0*0.6^0) / (0.6^0 + 0.6^1 +
        // 0.6^2 + 0.6^3)
        assertThat(this.function.apply(asList(1.0, 2.0, 3.0, 8.0)).get(), is(10.736 / 2.176));
    }

    @Test(expected = NullPointerException.class)
    public void onNullInput() {
        new LinearlyWeightedAverageFunction().apply(null);
    }
}
