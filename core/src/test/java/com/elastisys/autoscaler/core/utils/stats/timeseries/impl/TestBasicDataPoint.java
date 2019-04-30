package com.elastisys.autoscaler.core.utils.stats.timeseries.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.BasicDataPoint;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link BasicDataPoint} class.
 */
public class TestBasicDataPoint {

    @Test
    public void create() {
        DateTime time = UtcTime.now();

        BasicDataPoint dataPoint = new BasicDataPoint(time, 0.0);
        assertThat(dataPoint.getTime(), is(time));
        assertThat(dataPoint.getValue(), is(0.0));
    }

    @Test
    public void equality() {
        DateTime time = UtcTime.now();

        BasicDataPoint p1 = new BasicDataPoint(time, 0.0);
        BasicDataPoint p1Copy = new BasicDataPoint(time, 0.0);

        BasicDataPoint differentTime = new BasicDataPoint(time.plus(1), 0.0);
        BasicDataPoint differentTimeCopy = new BasicDataPoint(time.plus(1), 0.0);

        BasicDataPoint differentValue = new BasicDataPoint(time, 1.0);
        BasicDataPoint differentValueCopy = new BasicDataPoint(time, 1.0);

        assertTrue(p1.equals(p1Copy));
        assertTrue(differentTime.equals(differentTimeCopy));
        assertTrue(differentValue.equals(differentValueCopy));

        assertFalse(p1.equals(null));
        assertFalse(p1.equals(differentTime));
        assertFalse(p1.equals(differentValue));
        assertFalse(differentTime.equals(p1));
        assertFalse(differentTime.equals(differentValue));
        assertFalse(differentValue.equals(p1));
        assertFalse(differentValue.equals(differentTime));

    }

    @Test
    public void hashcode() {
        DateTime time = UtcTime.now();

        BasicDataPoint p1 = new BasicDataPoint(time, 0.0);
        BasicDataPoint p1Copy = new BasicDataPoint(time, 0.0);

        BasicDataPoint differentTime = new BasicDataPoint(time.plus(1), 0.0);
        BasicDataPoint differentTimeCopy = new BasicDataPoint(time.plus(1), 0.0);

        BasicDataPoint differentValue = new BasicDataPoint(time, 1.0);
        BasicDataPoint differentValueCopy = new BasicDataPoint(time, 1.0);

        assertTrue(p1.hashCode() == p1Copy.hashCode());
        assertTrue(differentTime.hashCode() == differentTimeCopy.hashCode());
        assertTrue(differentValue.hashCode() == differentValueCopy.hashCode());

        assertTrue(p1.hashCode() != differentTime.hashCode());
        assertTrue(p1.hashCode() != differentValue.hashCode());
        assertTrue(differentTime.hashCode() != differentValue.hashCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithNullTime() {
        new BasicDataPoint(null, 0.0);
    }

    @Test
    public void testOrdering() {
        DateTime t0 = UtcTime.now();
        DateTime t1 = t0.plus(1);
        DateTime t2 = t0.plus(2);

        BasicDataPoint p0 = new BasicDataPoint(t0, 0.0);
        BasicDataPoint p1 = new BasicDataPoint(t1, 1.0);
        BasicDataPoint p2 = new BasicDataPoint(t2, 2.0);

        assertTrue(p0.compareTo(p0) == 0);
        assertTrue(p0.compareTo(p1) < 0);
        assertTrue(p0.compareTo(p2) < 0);

        assertTrue(p1.compareTo(p1) == 0);
        assertTrue(p1.compareTo(p0) > 0);
        assertTrue(p1.compareTo(p2) < 0);

        assertTrue(p2.compareTo(p2) == 0);
        assertTrue(p2.compareTo(p0) > 0);
        assertTrue(p2.compareTo(p1) > 0);
    }

    @Test(expected = NullPointerException.class)
    public void testCompareWithNull() {
        new BasicDataPoint(UtcTime.now(), 0.0).compareTo(null);
    }
}
