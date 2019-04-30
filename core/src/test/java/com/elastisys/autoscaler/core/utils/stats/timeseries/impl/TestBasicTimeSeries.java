package com.elastisys.autoscaler.core.utils.stats.timeseries.impl;

import static com.elastisys.autoscaler.core.utils.stats.timeseries.impl.TimeSeriesTestUtils.dataPoint;
import static com.elastisys.autoscaler.core.utils.stats.timeseries.impl.TimeSeriesTestUtils.list;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeries;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.BasicDataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.BasicTimeSeries;

/**
 * Exercises the {@link BasicTimeSeries} class.
 */
public class TestBasicTimeSeries {

    /** {@link TimeSeries} under test. */
    private BasicTimeSeries series;

    @Before
    public void onSetup() {
        this.series = new BasicTimeSeries();
    }

    @Test
    public void testEmptySeries() {
        assertTrue(this.series.isEmpty());
        assertThat(this.series.size(), is(0));
        assertThat(this.series.getDataPoints(), is(list()));
    }

    /**
     * Added {@link DataPoint} should always be inserted to preserve the
     * increasing order of {@link DataPoint} timestamps.
     */
    @Test
    public void add() {
        assertTrue(this.series.isEmpty());
        assertThat(this.series.size(), is(0));
        assertThat(this.series.getDataPoints(), is(list()));

        DataPoint p0 = dataPoint(0, 0.0);
        DataPoint p1 = dataPoint(0, 1.0);
        DataPoint p2 = dataPoint(2, 2.0);
        DataPoint p3 = dataPoint(3, 3.0);
        DataPoint p4 = dataPoint(4, 4.0);
        DataPoint p5 = dataPoint(5, 5.0);
        DataPoint p6 = dataPoint(6, 6.0);

        // insert last
        this.series.add(p2);
        assertFalse(this.series.isEmpty());
        assertThat(this.series.size(), is(1));
        assertThat(this.series.getDataPoints(), is(list(p2)));

        // insert last
        this.series.add(p4);
        assertFalse(this.series.isEmpty());
        assertThat(this.series.size(), is(2));
        assertThat(this.series.getDataPoints(), is(list(p2, p4)));

        // insert first
        this.series.add(p1);
        assertFalse(this.series.isEmpty());
        assertThat(this.series.size(), is(3));
        assertThat(this.series.getDataPoints(), is(list(p1, p2, p4)));

        // insert in middle
        this.series.add(p3);
        assertFalse(this.series.isEmpty());
        assertThat(this.series.size(), is(4));
        assertThat(this.series.getDataPoints(), is(list(p1, p2, p3, p4)));

        // overwrite
        this.series.add(p0);
        assertThat(this.series.size(), is(4));
        assertThat(this.series.getDataPoints(), is(list(p0, p2, p3, p4)));

        // insert last
        this.series.add(p6);
        assertThat(this.series.size(), is(5));
        assertThat(this.series.getDataPoints(), is(list(p0, p2, p3, p4, p6)));

        // insert in middle
        this.series.add(p5);
        assertThat(this.series.size(), is(6));
        assertThat(this.series.getDataPoints(), is(list(p0, p2, p3, p4, p5, p6)));
    }

    /**
     * {@link TimeSeries#addAll(java.util.Collection)} should preserve the
     * increasing timestamp ordering of {@link DataPoint}s.
     */
    @Test
    public void addAll() {
        assertTrue(this.series.isEmpty());
        assertThat(this.series.getDataPoints(), is(list()));

        DataPoint p0 = dataPoint(0, 0.0);
        DataPoint p1 = dataPoint(0, 1.0);
        DataPoint p2 = dataPoint(2, 2.0);
        DataPoint p3 = dataPoint(3, 3.0);
        DataPoint p4 = dataPoint(4, 4.0);
        DataPoint p5 = dataPoint(5, 5.0);
        DataPoint p6 = dataPoint(6, 6.0);

        // add empty set
        this.series.addAll(Collections.emptyList());
        assertThat(this.series.getDataPoints(), is(list()));

        // add singleton set
        this.series.addAll(asList(p2));
        assertThat(this.series.getDataPoints(), is(list(p2)));

        // add set
        this.series.addAll(asList(p0, p4));
        assertThat(this.series.getDataPoints(), is(list(p0, p2, p4)));

        // add set with overwrites
        this.series.addAll(asList(p1, p3, p5));
        assertThat(this.series.getDataPoints(), is(list(p1, p2, p3, p4, p5)));

    }

    /**
     * Adding a {@link DataPoint} with the same time as an existing
     * {@link DataPoint} should cause the new {@link DataPoint} to overwrite the
     * old {@link DataPoint}.
     */
    @Test
    public void addDuplicateDataPoint() {
        DataPoint point1 = new BasicDataPoint(new DateTime(1), 1.0);
        this.series.add(point1);
        assertEquals(asList(point1), this.series.getDataPoints());

        DataPoint point1Updated = new BasicDataPoint(new DateTime(1), 11.0);
        this.series.add(point1Updated);
        assertEquals(asList(point1Updated), this.series.getDataPoints());
    }

    @Test
    public void remove() {
        DataPoint p0 = dataPoint(0, 0.0);
        DataPoint p1 = dataPoint(1, 1.0);
        DataPoint p2 = dataPoint(2, 2.0);

        this.series.addAll(asList(p0, p1, p2));

        assertThat(this.series.getDataPoints(), is(list(p0, p1, p2)));

        // remove in middle
        this.series.remove(1);
        assertThat(this.series.getDataPoints(), is(list(p0, p2)));
        // remove last
        this.series.remove(1);
        assertThat(this.series.getDataPoints(), is(list(p0)));
        // remove first
        this.series.remove(0);
        assertThat(this.series.getDataPoints(), is(list()));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void removeIndexTooSmall() {
        this.series.remove(0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void removeIndexTooLarge() {
        DataPoint p0 = dataPoint(0, 0.0);
        DataPoint p1 = dataPoint(1, 1.0);
        this.series.addAll(asList(p0, p1));

        this.series.remove(2);
    }

}
