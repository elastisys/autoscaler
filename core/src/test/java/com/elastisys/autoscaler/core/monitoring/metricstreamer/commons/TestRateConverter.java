package com.elastisys.autoscaler.core.monitoring.metricstreamer.commons;

import static com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.ConverterTestUtils.value;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.RateConverter;

/**
 * Exercises the {@link RateConverter} class.
 */
public class TestRateConverter {

    @Test(expected = NullPointerException.class)
    public void applyOnNull() {
        new RateConverter().apply(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void applyOnEmptyList() {
        List<MetricValue> empty = new ArrayList<>();
        new RateConverter().apply(empty);
    }

    @Test(expected = IllegalArgumentException.class)
    public void applyOnSingletonList() {
        List<MetricValue> singleValue = Arrays.asList(value(1.0, 1));
        new RateConverter().apply(singleValue);
    }

    @Test
    public void applyOnMultiValuedLists() {
        List<MetricValue> values = asList(value(1.0, 1), value(2.0, 2));
        // rate series will contain N - 1 data points
        assertThat(new RateConverter().apply(values), is(asList(value(1.0, 2))));

        // rate is 2.0 over first second, 4.0 over next second
        values = asList(value(2.0, 1), value(4.0, 2), value(8.0, 3));
        assertThat(new RateConverter().apply(values), is(asList(value(2.0, 2), value(4.0, 3))));

        // rate is 2.0 over first two seconds, 5.0 over next 4 seconds
        values = asList(value(2.0, 1), value(6.0, 3), value(26.0, 7));
        assertThat(new RateConverter().apply(values), is(asList(value(2.0, 3), value(5.0, 7))));
    }
}
