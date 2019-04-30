package com.elastisys.autoscaler.core.api.types;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Verifies the behavior of the {@link MetricValue} class.
 *
 *
 */
public class TestMetricValue {

    /**
     * Verifies that {@link MetricValue}s are totally ordered in increasing
     * order of timestamp.
     */
    @Test
    public void testOrdering() {
        assertTrue(metricValue(time(1)).compareTo(metricValue(time(1))) == 0);
        assertTrue(metricValue(time(1)).compareTo(metricValue(time(2))) < 0);
        assertTrue(metricValue(time(2)).compareTo(metricValue(time(1))) > 0);
    }

    /**
     * Verfies that the {@link MetricValue#withTag(String, String)} "copy
     * method" works as expected.
     */
    @Test
    public void testWithTagCopyMethod() {
        DateTime time = UtcTime.now();

        MetricValue original = metricValue(1.0, time);

        MetricValue copy = original.withTag("tag", "value");
        assertNotSame(original, copy);
        assertThat(original, is(metricValue(1.0, time)));
        assertThat(copy, is(metricValue(1.0, time, "tag", "value")));

        // test overwriting an existing tag in original and verify that original
        // is untouched
        original = metricValue(2.0, time, "tag", "original-value");
        copy = original.withTag("tag", "new-value");
        assertThat(copy, is(metricValue(2.0, time, "tag", "new-value")));
        assertThat(original, is(metricValue(2.0, time, "tag", "original-value")));
    }

    /**
     * Verfies that the {@link MetricValue#withTags(Map)} "copy method" works as
     * expected.
     */
    @Test
    public void testWithTagsCopyMethod() {
        DateTime time = UtcTime.now();

        MetricValue original = metricValue(1.0, time);

        MetricValue copy = original.withTags(Maps.of("tag", "value"));
        assertNotSame(original, copy);
        assertThat(original, is(metricValue(1.0, time)));
        assertThat(copy, is(metricValue(1.0, time, "tag", "value")));

        // test overwriting an existing tag in original and verify that original
        // is untouched
        original = metricValue(2.0, time, "tag", "original-value");
        copy = original.withTags(Maps.of("tag", "new-value", //
                "tag2", "value2"));

        assertThat(copy, is(metricValue(2.0, time, //
                Maps.of("tag", "new-value", //
                        "tag2", "value2"))));
        assertThat(original, is(metricValue(2.0, time, "tag", "original-value")));
    }

    /**
     * Verfies that the {@link MetricValue#withValue(double)} "copy method"
     * works as expected.
     */
    @Test
    public void testWithValueCopyMethod() {
        DateTime time = UtcTime.now();
        MetricValue original = metricValue(1.0, time);
        MetricValue copy = original.withValue(2.0);
        assertNotSame(original, copy);
        assertThat(original, is(metricValue(1.0, time)));
        assertThat(copy, is(metricValue(2.0, time)));
    }

    public static DateTime time(long millisSinceEpoch) {
        return new DateTime(millisSinceEpoch, DateTimeZone.UTC);
    }

    public static MetricValue metricValue(DateTime timestamp) {
        Map<String, String> tags = Maps.of();
        return new MetricValue("metric", Math.random(), timestamp, tags);
    }

    /**
     * Creates a {@link MetricValue} without tags.
     *
     * @param value
     * @param time
     * @return
     */
    public static MetricValue metricValue(double value, DateTime time) {
        Map<String, String> tags = Maps.of();
        return new MetricValue("metric", value, time, tags);
    }

    /**
     * Creates a {@link MetricValue} with a tag.
     *
     * @param value
     * @param time
     * @param tag
     * @param tagValue
     * @return
     */
    public static MetricValue metricValue(double value, DateTime time, String tag, String tagValue) {
        Map<String, String> tags = Maps.of(tag, tagValue);
        return new MetricValue("metric", value, time, tags);
    }

    public static MetricValue metricValue(double value, DateTime time, Map<String, String> tags) {
        return new MetricValue("metric", value, time, tags);
    }
}
