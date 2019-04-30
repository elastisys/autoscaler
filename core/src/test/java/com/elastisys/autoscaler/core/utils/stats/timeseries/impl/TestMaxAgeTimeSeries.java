package com.elastisys.autoscaler.core.utils.stats.timeseries.impl;

import static com.elastisys.autoscaler.core.utils.stats.timeseries.impl.TimeSeriesTestUtils.list;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeries;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.BasicDataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.BasicTimeSeries;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.MaxAgeTimeSeries;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link MaxAgeTimeSeries}.
 */
public class TestMaxAgeTimeSeries {
    @Before
    public void onSetup() {
        DateTime now = FrozenTime.now();
        FrozenTime.setFixed(now);
    }

    /**
     * Verifies that adding {@link DataPoint}s outside of the time window
     * covered by the {@link MaxAgeTimeSeries} fails.
     */
    @Test
    public void testAddValuesOlderThanWindow() {
        int maxAge = 10;
        MaxAgeTimeSeries slidingSeries = getSeries(maxAge);

        // add(): should not succeed
        slidingSeries.add(agedDataPoint(maxAge + 1, 1.0));
        assertThat(slidingSeries.size(), is(0));

        // addAll(): should not succeed
        slidingSeries.addAll(list(agedDataPoint(maxAge + 1, 1.0), agedDataPoint(maxAge + 2, 2.0)));
        assertThat(slidingSeries.size(), is(0));
    }

    /**
     * Verifies that {@link DataPoint}s with future time stamps can be added.
     */
    @Test
    public void testAddFutureValues() {
        int maxAge = 10;
        MaxAgeTimeSeries series = getSeries(maxAge);

        // add(): should succeed
        DataPoint futureValue1 = agedDataPoint(-1, 1.0);
        DataPoint futureValue2 = agedDataPoint(-2, 2.0);
        series.add(futureValue1);
        assertThat(series.getDataPoints(), is(list(futureValue1)));
        series.add(futureValue2);
        assertThat(series.getDataPoints(), is(list(futureValue1, futureValue2)));

        // addAll(): should succeed
        series = getSeries(maxAge);
        series.addAll(list(futureValue1, futureValue2));
        assertThat(series.getDataPoints(), is(list(futureValue1, futureValue2)));
    }

    /**
     * Verify that added values are sorted in increasing order of time
     * (decreasing age) irrespective of insertion order.
     */
    @Test
    public void testSortingBehavior() {
        int maxAge = 3;
        TimeSeries series = getSeries(maxAge);

        DataPoint value1 = agedDataPoint(maxAge - 0, 1.0); // oldest
        DataPoint value2 = agedDataPoint(maxAge - 1, 2.0);
        DataPoint value3 = agedDataPoint(maxAge - 2, 3.0);
        DataPoint value4 = agedDataPoint(maxAge - 3, 4.0); // youngest
        series.add(value4);
        assertThat(series.getDataPoints(), is(list(value4)));
        series.add(value2);
        assertThat(series.getDataPoints(), is(list(value2, value4)));
        series.add(value1);
        assertThat(series.getDataPoints(), is(list(value1, value2, value4)));
        series.add(value3);
        assertThat(series.getDataPoints(), is(list(value1, value2, value3, value4)));
    }

    /**
     * Test eviction behavior: verify that {@link DataPoint}s are rotated out as
     * time progresses.
     */
    @Test
    public void testWindowSliding() {
        int maxAge = 3;
        TimeSeries series = getSeries(maxAge);

        DataPoint value1 = agedDataPoint(maxAge - 0, 1.0);
        DataPoint value2 = agedDataPoint(maxAge - 1, 2.0);
        DataPoint value3 = agedDataPoint(maxAge - 2, 3.0);
        DataPoint value4 = agedDataPoint(maxAge - 3, 4.0);
        series.add(value1);
        series.add(value2);
        series.add(value3);
        series.add(value4);
        assertThat(series.getDataPoints(), is(list(value1, value2, value3, value4)));

        // advance time and make sure that values are rotated out (when
        // toArray() is implicitly called).
        FrozenTime.tick();
        assertThat(series.getDataPoints(), is(list(value2, value3, value4)));
        FrozenTime.tick();
        assertThat(series.getDataPoints(), is(list(value3, value4)));
        FrozenTime.tick();
        assertThat(series.getDataPoints(), is(list(value4)));
        FrozenTime.tick();
        assertThat(series.getDataPoints(), is(list()));
    }

    @Test
    public void size() {
        int maxAge = 1;
        TimeSeries series = getSeries(maxAge);
        DataPoint value1 = agedDataPoint(maxAge - 0, 1.0);
        DataPoint value2 = agedDataPoint(maxAge - 1, 2.0);
        series.add(value1);
        series.add(value2);
        assertThat(series.getDataPoints(), is(list(value1, value2)));

        // advance time and make sure that values are rotated out
        assertThat(series.size(), is(2));
        FrozenTime.tick(); // rotate out value1
        assertThat(series.size(), is(1));
        FrozenTime.tick(); // rotate out value2
        assertThat(series.size(), is(0));
        FrozenTime.tick();
        assertThat(series.size(), is(0));
    }

    @Test
    public void isEmpty() {
        int maxAge = 1;
        TimeSeries series = getSeries(maxAge);
        DataPoint value1 = agedDataPoint(maxAge - 0, 1.0);
        DataPoint value2 = agedDataPoint(maxAge - 1, 2.0);
        series.add(value1);
        series.add(value2);
        assertThat(series.getDataPoints(), is(list(value1, value2)));

        // advance time and make sure that values are rotated out
        assertFalse(series.isEmpty());
        FrozenTime.tick(); // rotate out value1
        assertFalse(series.isEmpty());
        FrozenTime.tick(); // rotate out value2
        assertTrue(series.isEmpty());
    }

    @Test
    public void addAll() {
        int maxAge = 1;
        TimeSeries series = getSeries(maxAge);
        DataPoint value1 = agedDataPoint(maxAge - 0, 1.0);
        DataPoint value2 = agedDataPoint(maxAge - 1, 2.0);
        series.addAll(list(value1, value2));
        assertThat(series.getDataPoints(), is(list(value1, value2)));

        // add one value outside of window and one within
        DataPoint oldValue = agedDataPoint(maxAge + 1, 0.0);
        DataPoint value3 = agedDataPoint(maxAge - 2, 3.0);
        series.addAll(list(oldValue, value3));
        assertThat(series.getDataPoints(), is(list(value1, value2, value3)));

        // advance time and make sure that values are rotated out when method is
        // called
        FrozenTime.tick(); // rotate out value1
        series.addAll(list(value1)); // cannot add old value
        assertThat(series.getDataPoints(), is(list(value2, value3)));
        FrozenTime.tick(); // rotate out value2
        series.addAll(list(value1, value2));
        assertThat(series.getDataPoints(), is(list(value3)));
    }

    @Test
    public void testShrinkCapacity() {
        int maxAge = 2;
        MaxAgeTimeSeries series = getSeries(maxAge);
        DataPoint value1 = agedDataPoint(maxAge - 0, 1.0);
        DataPoint value2 = agedDataPoint(maxAge - 1, 2.0);
        DataPoint value3 = agedDataPoint(maxAge - 2, 3.0);
        series.addAll(list(value1, value2));
        assertThat(series.getDataPoints(), is(list(value1, value2)));
        assertThat(series.getMaxAge(), is(2));

        // make sure old value1 is rotated out when maxAge is decreased
        series.setMaxAge(1);
        assertThat(series.getMaxAge(), is(1));
        assertThat(series.getDataPoints(), is(list(value2)));

        series.add(value3);
        assertThat(series.getDataPoints(), is(list(value2, value3)));
        FrozenTime.tick();
        assertThat(series.getDataPoints(), is(list(value3)));
    }

    @Test
    public void testGrowCapacity() {
        int maxAge = 2;
        MaxAgeTimeSeries series = getSeries(maxAge);
        DataPoint value1 = agedDataPoint(maxAge - 0, 1.0);
        DataPoint value2 = agedDataPoint(maxAge - 1, 2.0);
        DataPoint value3 = agedDataPoint(maxAge - 2, 3.0);
        series.addAll(list(value1, value2));
        assertThat(series.getDataPoints(), is(list(value1, value2)));
        assertThat(series.getMaxAge(), is(2));

        // make sure no values are rotated out when maxAge is increased
        series.setMaxAge(3);
        assertThat(series.getMaxAge(), is(3));
        assertThat(series.getDataPoints(), is(list(value1, value2)));

        series.add(value3);
        assertThat(series.getDataPoints(), is(list(value1, value2, value3)));
        FrozenTime.tick();
        assertThat(series.getDataPoints(), is(list(value1, value2, value3)));
        FrozenTime.tick();
        assertThat(series.getDataPoints(), is(list(value2, value3)));
    }

    /**
     * Adding a {@link DataPoint} with the same time as an existing
     * {@link DataPoint} should cause the new {@link DataPoint} to overwrite the
     * old {@link DataPoint}.
     */
    @Test
    public void addDuplicateDataPoint() {
        int maxAge = 2;
        MaxAgeTimeSeries series = getSeries(maxAge);

        DataPoint point1 = agedDataPoint(maxAge - 0, 1.0);
        series.add(point1);
        assertEquals(asList(point1), series.getDataPoints());

        DataPoint point1Updated = agedDataPoint(maxAge - 0, 11.0);
        series.add(point1Updated);
        assertEquals(asList(point1Updated), series.getDataPoints());
    }

    private MaxAgeTimeSeries getSeries(int maxAge) {
        MaxAgeTimeSeries slidingSeries = new MaxAgeTimeSeries(new BasicTimeSeries(), maxAge);
        assertThat(slidingSeries.getMaxAge(), is(maxAge));
        assertTrue(slidingSeries.isEmpty());
        return slidingSeries;
    }

    /**
     * Creates a {@link DataPoint} of a given age and value.
     *
     * @param ageInSeconds
     *            Age in seconds. Negative numbers produce {@link DataPoint}s
     *            with time stamps in the future.
     * @param value
     * @return
     */
    public static DataPoint agedDataPoint(int ageInSeconds, double value) {
        return new BasicDataPoint(UtcTime.now().minusSeconds(ageInSeconds), value);
    }
}
