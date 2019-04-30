package com.elastisys.autoscaler.core.utils.stats.functions;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.core.utils.stats.functions.LinearlyWeightedAverageFunction;

/**
 * Exercises the {@link LinearlyWeightedAverageFunction} class.
 */
public class TestLinearlyWeightedAverageFunction {

    /** Object under test. */
    private LinearlyWeightedAverageFunction function;

    @Before
    public void onSetup() {
        this.function = new LinearlyWeightedAverageFunction();
    }

    @Test
    public void onEmptyInput() {
        Optional<Double> absent = Optional.empty();
        assertThat(new LinearlyWeightedAverageFunction().apply(Collections.emptyList()), is(absent));
    }

    @Test
    public void onNonEmptyInput() {
        // (3.0 * 1) / 1
        assertThat(this.function.apply(asList(3.0)).get(), is(3.0));
        // (3.0 * 2 + 2.0 * 1) / (2 + 1)
        assertThat(this.function.apply(asList(2.0, 3.0)).get(), is(8.0 / 3));
        // (3.0 * 3 + 2.0 * 2 + 1.0 * 1) / (3 + 2 + 1)
        assertThat(this.function.apply(asList(1.0, 2.0, 3.0)).get(), is(14.0 / 6));

        // (8.0 * 4 + 3.0 * 3 + 2.0 * 2 + 1.0 * 1) / (4 + 3 + 2 + 1)
        assertThat(this.function.apply(asList(1.0, 2.0, 3.0, 8.0)).get(), is(4.6));
    }

    @Test(expected = NullPointerException.class)
    public void onNullInput() {
        new LinearlyWeightedAverageFunction().apply(null);
    }
}
