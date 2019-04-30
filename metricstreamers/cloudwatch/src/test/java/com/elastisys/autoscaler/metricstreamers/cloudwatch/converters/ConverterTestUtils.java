package com.elastisys.autoscaler.metricstreamers.cloudwatch.converters;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchStatistic;

public class ConverterTestUtils {
    /**
     * Constructs a CloudWatch {@link Datapoint}.
     *
     * @param statistic
     *            The statistic for which the value is provided.
     * @param value
     *            The value.
     * @param time
     *            The timestamp.
     * @return
     */
    public static Datapoint datapoint(CloudWatchStatistic statistic, double value, DateTime time) {

        switch (statistic) {
        case Sum:

            return new Datapoint().withTimestamp(time.toDate()).withSum(value);
        case Average:
            return new Datapoint().withAverage(value).withTimestamp(time.toDate());
        case Minimum:
            return new Datapoint().withMinimum(value).withTimestamp(time.toDate());
        case Maximum:
            return new Datapoint().withMaximum(value).withTimestamp(time.toDate());
        case SampleCount:
            return new Datapoint().withSampleCount(value).withTimestamp(time.toDate());
        default:
            throw new IllegalArgumentException(String.format("unrecognized statistic '%s'", statistic.name()));

        }
    }

    public static List<Datapoint> datapoints(Datapoint... datapoints) {
        return Arrays.asList(datapoints);
    }

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
