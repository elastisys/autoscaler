package com.elastisys.autoscaler.systemhistorians.opentsdb;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetric;
import com.elastisys.autoscaler.systemhistorians.opentsdb.OpenTsdbDataPointRepresenter;

/**
 * Exercises the {@link OpenTsdbDataPointRepresenter}.
 */
public class TestOpenTsdbDataPointRepresenter {

    @Test
    public void validWithSingleTag() {
        final String metric = SystemMetric.PREDICTION.getMetricName();
        final String expected = metric + " 0 1.234567 host=foo";
        final Map<String, String> tags = new TreeMap<>();
        tags.put("host", "foo");

        MetricValue dataPoint = new MetricValue(metric, 1.234567d, new DateTime(0), tags);
        assertEquals(expected, OpenTsdbDataPointRepresenter.representDataPoint(dataPoint));
    }

    @Test
    public void validWithMultipleTags() {
        final String metric = SystemMetric.PREDICTION.getMetricName();
        final String expected = metric + " 0 1.234567 host=foo some.tag=some.value type=bar";
        final Map<String, String> tags = new TreeMap<>();

        tags.put("host", "foo");
        tags.put("some.tag", "some.value");
        tags.put("type", "bar");

        MetricValue dataPoint = new MetricValue(metric, 1.234567d, new DateTime(0), tags);
        assertEquals(expected, OpenTsdbDataPointRepresenter.representDataPoint(dataPoint));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidMissingTags() {
        Map<String, String> emptyTags = new TreeMap<>();
        MetricValue dataPoint = new MetricValue(SystemMetric.PREDICTION.getMetricName(), 1.234567d, new DateTime(0),
                emptyTags);
        OpenTsdbDataPointRepresenter.representDataPoint(dataPoint);
    }
}
