package com.elastisys.autoscaler.core.utils.stats;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Verifies the behavior of the {@link Aggregators} class.
 */
public class TestAggregators {

    @Test
    public void average() {
        assertThat(Aggregators.average(asList(1.0)), is(1.0));
        assertThat(Aggregators.average(asList(1.0, 2.0)), is(1.5));
        assertThat(Aggregators.average(asList(1.0, 2.0, 3.0)), is(2.0));

        assertThat(Aggregators.average(asList(1.0, 2.0, 1.0)), is(4.0 / 3.0));
        assertThat(Aggregators.average(asList(1.0, -1.0)), is(0.0));
    }

    @Test
    public void averageOnEmptyList() {
        assertThat(Aggregators.average(asList()), is(Double.NaN));
    }

    @Test
    public void averageOnListContainingNaN() {
        assertThat(Aggregators.average(asList(1.0, Double.NaN, 2.0)), is(Double.NaN));
    }

    @Test(expected = NullPointerException.class)
    public void averageOnNullInput() {
        Aggregators.average(null);
    }
}
