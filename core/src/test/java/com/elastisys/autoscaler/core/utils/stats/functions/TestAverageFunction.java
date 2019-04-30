package com.elastisys.autoscaler.core.utils.stats.functions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.Test;

import com.elastisys.autoscaler.core.utils.stats.functions.AverageFunction;

/**
 * Exercises the {@link AverageFunction} class.
 */
public class TestAverageFunction {

    @Test
    public void onEmptyInput() {
        Optional<Double> absent = Optional.empty();
        assertThat(new AverageFunction().apply(Collections.emptyList()), is(absent));
    }

    @Test
    public void onNonEmptyInput() {

        assertThat(new AverageFunction().apply(Arrays.asList(1.0)), is(Optional.of(1.0)));

        assertThat(new AverageFunction().apply(Arrays.asList(1.0, 2.0)), is(Optional.of(1.5)));

        assertThat(new AverageFunction().apply(Arrays.asList(1.0, 2.0, 3.0)), is(Optional.of(2.0)));
    }

    @Test(expected = NullPointerException.class)
    public void onNullInput() {
        new AverageFunction().apply(null);
    }
}
