package com.elastisys.autoscaler.metricstreamers.ceilometer.converters;

import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.metricstreamers.ceilometer.converters.SampleValueConverter;
import com.elastisys.autoscaler.metricstreamers.ceilometer.converters.StatisticValueConverter;

/**
 * Utility methods that make writing test cases for {@link SampleValueConverter}
 * and {@link StatisticValueConverter} less verbose.
 */
public class ConverterTestUtils {
    public static MetricValue value(String metric, DateTime timestamp, Double value, Map<String, String> tags) {
        return new MetricValue(metric, value, timestamp, tags);
    }

    public static MetricValue value(String metric, DateTime timestamp, Double value) {
        return new MetricValue(metric, value, timestamp);
    }

    public static DateTime time(int epoch) {
        return new DateTime(epoch, DateTimeZone.UTC);
    }

    public static Interval interval(int start, int end) {
        return new Interval(time(start), time(end));
    }
}
