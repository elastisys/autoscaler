package com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.impl.SinglePageResultSet;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercise {@link SinglePageResultSet}.
 */
public class TestSinglePageResultSet {

    @Test
    public void empty() {
        SinglePageResultSet empty = new SinglePageResultSet(Collections.emptyList());
        assertTrue(empty.hasNext());
        assertTrue(empty.fetchNext().getMetricValues().isEmpty());
        assertFalse(empty.hasNext());
    }

    @Test
    public void withValues() {
        MetricValue value = new MetricValue("metric", 1, UtcTime.now());
        SinglePageResultSet empty = new SinglePageResultSet(Arrays.asList(value));
        assertTrue(empty.hasNext());
        assertThat(empty.fetchNext().getMetricValues(), is(Arrays.asList(value)));
        assertFalse(empty.hasNext());
    }

    /**
     * Not allowed to pass <code>null</code>
     */
    @Test(expected = IllegalArgumentException.class)
    public void withNull() {
        new SinglePageResultSet(null);
    }

}
