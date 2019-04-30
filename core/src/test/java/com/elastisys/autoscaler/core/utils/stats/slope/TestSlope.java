package com.elastisys.autoscaler.core.utils.stats.slope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.elastisys.autoscaler.core.utils.stats.slope.Slope;
import com.elastisys.autoscaler.core.utils.stats.slope.SlopeDirection;
import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeries;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.BasicDataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.BasicTimeSeries;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.FixedCapacityTimeSeries;

/**
 * Verifies the behavior of the {@link Slope} class.
 *
 *
 *
 */
public class TestSlope {

    /**
     * Verifies that the following sample is recognized as an downward
     * {@link Slope}:
     *
     * <pre>
     *     8| x
     *     7|x
     *     6|  xx x
     *     5|    x x x
     *     4|       x
     *     3|         x
     *     2|
     *     1|
     *      +-------------+
     * </pre>
     */
    @Test
    public void testDownwardSlope() {
        TimeSeries observations = new FixedCapacityTimeSeries(new BasicTimeSeries(), 10);
        DataPoint point1 = point(1, 7);
        DataPoint point2 = point(2, 8);
        DataPoint point3 = point(3, 6);
        DataPoint point4 = point(4, 6);
        DataPoint point5 = point(5, 5);
        DataPoint point6 = point(6, 6);
        DataPoint point7 = point(7, 5);
        DataPoint point8 = point(8, 4);
        DataPoint point9 = point(9, 5);
        DataPoint point10 = point(10, 3);
        observations.add(point1);
        observations.add(point2);
        observations.add(point3);
        observations.add(point4);
        observations.add(point5);
        observations.add(point6);
        observations.add(point7);
        observations.add(point8);
        observations.add(point9);
        observations.add(point10);

        Slope slope = new Slope(observations);
        assertFalse(slope.isHorizontal());
        assertFalse(slope.isUp());
        assertTrue(slope.isDown());
        assertEquals(SlopeDirection.DOWN, slope.getDirection());
        assertTrue(slope.slope() < 0);
    }

    /**
     * Verifies that the following sample is recognized as an upward
     * {@link Slope}:
     *
     * <pre>
     *      +
     *     8 |
     *     7 |                  x
     *     6 |                 x
     *     5 |              x    x
     *     4 |           x    x
     *     3 |
     *     2 |             x
     *     1 |          x
     *       +----------------------------------------+
     * </pre>
     */
    @Test
    public void testUpwardSlope() {
        TimeSeries observations = new FixedCapacityTimeSeries(new BasicTimeSeries(), 10);
        DataPoint point1 = point(1, 1);
        DataPoint point2 = point(2, 4);
        DataPoint point3 = point(3, 2);
        DataPoint point4 = point(4, 5);
        DataPoint point5 = point(5, 4);
        DataPoint point6 = point(6, 6);
        DataPoint point7 = point(7, 6);
        DataPoint point8 = point(8, 5);
        observations.add(point1);
        observations.add(point2);
        observations.add(point3);
        observations.add(point4);
        observations.add(point5);
        observations.add(point6);
        observations.add(point7);
        observations.add(point8);

        Slope slope = new Slope(observations);
        assertFalse(slope.isDown());
        assertFalse(slope.isHorizontal());
        assertTrue(slope.isUp());
        assertEquals(SlopeDirection.UP, slope.getDirection());
        assertTrue(slope.slope() > 0);
    }

    @Test
    public void testHorizontalSlope() {
        TimeSeries observations = new FixedCapacityTimeSeries(new BasicTimeSeries(), 10);
        DataPoint point1 = point(1, 1);
        DataPoint point2 = point(2, 1);
        DataPoint point3 = point(3, 1);
        DataPoint point4 = point(4, 1);
        observations.add(point1);
        observations.add(point2);
        observations.add(point3);
        observations.add(point4);

        Slope slope = new Slope(observations);
        assertFalse(slope.isDown());
        assertFalse(slope.isUp());
        assertTrue(slope.isHorizontal());
        assertEquals(SlopeDirection.HORIZONTAL, slope.getDirection());
        assertEquals(0.0, slope.slope(), 0.0000001);
    }

    @Test
    public void testSlopeOnNoObservations() {
        TimeSeries observations = new FixedCapacityTimeSeries(new BasicTimeSeries(), 0);

        Slope slope = new Slope(observations);
        assertFalse(slope.isDown());
        assertFalse(slope.isUp());
        assertTrue(slope.isHorizontal());
        assertEquals(SlopeDirection.HORIZONTAL, slope.getDirection());
        assertEquals(0.0, slope.slope(), 0.0000001);
    }

    @Test
    public void testSlopeOnSingleObservation() {
        TimeSeries observations = new FixedCapacityTimeSeries(new BasicTimeSeries(), 1);
        DataPoint point = point(4, 5.0);
        observations.add(point);

        Slope slope = new Slope(observations);
        assertFalse(slope.isDown());
        assertFalse(slope.isUp());
        assertTrue(slope.isHorizontal());
        assertEquals(SlopeDirection.HORIZONTAL, slope.getDirection());
        assertEquals(0.0, slope.slope(), 0.0000001);
    }

    private DataPoint point(long timeInSeconds, double value) {
        return new BasicDataPoint(new DateTime(timeInSeconds * 1000, DateTimeZone.UTC), value);
    }
}
