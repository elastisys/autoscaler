package com.elastisys.autoscaler.core.utils.stats.timeseries.impl;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeries;

/**
 * Exercises the {@link FixedCapacityTimeSeries} class.
 */
public class TestFixedCapacityTimeSeries {

    /**
     * A {@link FixedCapacityTimeSeries} with zero capacity should be possible
     * to create. It should never contain any elements.
     */
    @Test
    public void zeroCapacitySeries() {
        FixedCapacityTimeSeries zeroCapacitySeries = new FixedCapacityTimeSeries(new BasicTimeSeries(), 0);
        assertTrue(zeroCapacitySeries.isEmpty());
        assertTrue(zeroCapacitySeries.isFull());
        assertThat(zeroCapacitySeries.size(), is(0));

        DataPoint point1 = new BasicDataPoint(new DateTime(1), 1.0);
        DataPoint point2 = new BasicDataPoint(new DateTime(2), 2.0);

        zeroCapacitySeries.add(point1);
        assertThat(zeroCapacitySeries.size(), is(0));
        assertTrue(zeroCapacitySeries.isEmpty());
        assertTrue(zeroCapacitySeries.isFull());

        zeroCapacitySeries.add(point2);
        assertThat(zeroCapacitySeries.size(), is(0));
        assertTrue(zeroCapacitySeries.isEmpty());
        assertTrue(zeroCapacitySeries.isFull());
    }

    @Test
    public void testEmptySeries() {
        FixedCapacityTimeSeries dataSeries = new FixedCapacityTimeSeries(new BasicTimeSeries(), 3);
        assertTrue(dataSeries.isEmpty());
        assertFalse(dataSeries.isFull());
        assertThat(dataSeries.size(), is(0));
    }

    @Test
    public void testGetOldestAndGetLatest() {
        FixedCapacityTimeSeries dataSeries = new FixedCapacityTimeSeries(new BasicTimeSeries(), 3);

        DataPoint point1 = new BasicDataPoint(new DateTime(1), 1.0);
        DataPoint point2 = new BasicDataPoint(new DateTime(2), 2.0);
        DataPoint point3 = new BasicDataPoint(new DateTime(3), 3.0);

        dataSeries.add(point1);
        assertThat(dataSeries.size(), is(1));
        assertFalse(dataSeries.isEmpty());
        assertFalse(dataSeries.isFull());
        assertEquals(getOldest(dataSeries), point1);
        assertEquals(getLatest(dataSeries), point1);

        dataSeries.add(point2);
        assertThat(dataSeries.size(), is(2));
        assertFalse(dataSeries.isEmpty());
        assertFalse(dataSeries.isFull());
        assertEquals(getOldest(dataSeries), point1);
        assertEquals(getLatest(dataSeries), point2);

        dataSeries.add(point3);
        assertThat(dataSeries.size(), is(3));
        assertFalse(dataSeries.isEmpty());
        assertTrue(dataSeries.isFull());
        assertEquals(getOldest(dataSeries), point1);
        assertEquals(getLatest(dataSeries), point3);
    }

    /**
     * Verifies that the {@link FixedCapacityTimeSeries} rotates out old values
     * when capacity is full.
     */
    @Test
    public void testSlidingBehavior() {
        DataPoint dataPoint1 = new BasicDataPoint(new DateTime(1), 1.0);
        DataPoint dataPoint2 = new BasicDataPoint(new DateTime(2), 2.1);
        DataPoint dataPoint3 = new BasicDataPoint(new DateTime(3), 3.5);
        DataPoint dataPoint4 = new BasicDataPoint(new DateTime(4), 4.8);
        DataPoint dataPoint5 = new BasicDataPoint(new DateTime(5), 5.9);

        FixedCapacityTimeSeries dataSeries = new FixedCapacityTimeSeries(new BasicTimeSeries(), 3);
        assertEquals(0, dataSeries.getDataPoints().size());
        dataSeries.add(dataPoint1);
        assertEquals(Arrays.asList(dataPoint1), dataSeries.getDataPoints());
        dataSeries.add(dataPoint2);
        assertEquals(Arrays.asList(dataPoint1, dataPoint2), dataSeries.getDataPoints());
        dataSeries.add(dataPoint3);
        assertEquals(Arrays.asList(dataPoint1, dataPoint2, dataPoint3), dataSeries.getDataPoints());
        // as series is now full, oldest value should be rotated out
        dataSeries.add(dataPoint4);
        assertEquals(Arrays.asList(dataPoint2, dataPoint3, dataPoint4), dataSeries.getDataPoints());
        // as series is now full, oldest value should be rotated out
        dataSeries.add(dataPoint5);
        assertEquals(Arrays.asList(dataPoint3, dataPoint4, dataPoint5), dataSeries.getDataPoints());
    }

    @Test
    public void testShrinkCapacity() {
        DataPoint point1 = new BasicDataPoint(new DateTime(1), 1.0);
        DataPoint point2 = new BasicDataPoint(new DateTime(2), 2.0);
        DataPoint point3 = new BasicDataPoint(new DateTime(3), 3.0);
        DataPoint point4 = new BasicDataPoint(new DateTime(4), 4.0);

        FixedCapacityTimeSeries series = new FixedCapacityTimeSeries(new BasicTimeSeries(), 3);
        series.add(point1);
        series.add(point2);
        series.add(point3);
        assertEquals(3, series.getDataPoints().size());
        assertEquals(asList(point1, point2, point3), series.getDataPoints());

        series.setCapacity(2);
        assertEquals(2, series.getCapacity());
        assertEquals(2, series.getDataPoints().size());
        assertEquals(asList(point2, point3), series.getDataPoints());

        series.add(point4);
        assertEquals(asList(point3, point4), series.getDataPoints());
    }

    @Test
    public void testGrowCapacity() {
        DataPoint point1 = new BasicDataPoint(new DateTime(1), 1.0);
        DataPoint point2 = new BasicDataPoint(new DateTime(2), 2.0);
        DataPoint point3 = new BasicDataPoint(new DateTime(3), 3.0);
        DataPoint point4 = new BasicDataPoint(new DateTime(4), 4.0);
        DataPoint point5 = new BasicDataPoint(new DateTime(5), 5.0);

        FixedCapacityTimeSeries series = new FixedCapacityTimeSeries(new BasicTimeSeries(), 3);
        series.add(point1);
        series.add(point2);
        series.add(point3);
        assertEquals(3, series.getDataPoints().size());
        assertEquals(asList(point1, point2, point3), series.getDataPoints());

        series.setCapacity(4);
        assertEquals(4, series.getCapacity());
        assertEquals(3, series.getDataPoints().size());
        assertEquals(asList(point1, point2, point3), series.getDataPoints());

        series.add(point4);
        assertEquals(asList(point1, point2, point3, point4), series.getDataPoints());
        series.add(point5);
        assertEquals(asList(point2, point3, point4, point5), series.getDataPoints());

    }

    /**
     * Adding a {@link DataPoint} with the same time as an existing
     * {@link DataPoint} should cause the new {@link DataPoint} to overwrite the
     * old {@link DataPoint}.
     */
    @Test
    public void addDuplicateDataPoint() {
        FixedCapacityTimeSeries series = new FixedCapacityTimeSeries(new BasicTimeSeries(), 3);

        DataPoint point1 = new BasicDataPoint(new DateTime(1), 1.0);
        series.add(point1);
        assertEquals(asList(point1), series.getDataPoints());

        DataPoint point1Updated = new BasicDataPoint(new DateTime(1), 11.0);
        series.add(point1Updated);
        assertEquals(asList(point1Updated), series.getDataPoints());
    }

    private DataPoint getLatest(TimeSeries series) {
        return series.getDataPoints().get(series.size() - 1);
    }

    private DataPoint getOldest(TimeSeries dataSeries) {
        return dataSeries.getDataPoints().get(0);
    }

}
