package com.elastisys.autoscaler.core.monitoring.metricstreamer.commons;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.elastisys.autoscaler.core.api.types.MetricValue;

public class ConverterTestUtils {

    /**
     * Creates a {@link MetricValue} without tags of metric "metric".
     *
     * @param value
     *            value
     * @param time
     *            time stamp
     * @return
     */
    public static MetricValue value(double value, DateTime time) {
        Map<String, String> tags = Collections.emptyMap();
        return new MetricValue("metric", value, time, tags);
    }

    /**
     * Creates a {@link MetricValue} without tags.
     *
     * @param value
     *            value
     * @param time
     *            time in seconds since epoch
     * @return
     */
    public static MetricValue value(double value, long time) {
        Map<String, String> tags = Collections.emptyMap();
        return new MetricValue("metric", value, time(time), tags);
    }

    /**
     * Creates a time stamp.
     *
     * @param secondsSinceEpoch
     *            Seconds since epoch.
     * @return
     */
    public static DateTime time(long secondsSinceEpoch) {
        return new DateTime(secondsSinceEpoch * 1000, DateTimeZone.UTC);
    }

    /**
     * Creates a {@link MetricValue} {@link List}.
     *
     * @param values
     *            values
     * @return
     */
    public static List<MetricValue> values(MetricValue... values) {
        return Arrays.asList(values);
    }

}
