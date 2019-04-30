package com.elastisys.autoscaler.metricstreamers.cloudwatch.converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchStatistic;

/**
 * A {@link Callable} task that takes a collection of CloudWatch
 * {@link Datapoint}s, represented as a {@link GetMetricStatisticsResponse}
 * object, as input and converts them to a {@link List} of {@link MetricValue}s.
 */
public class MetricValueConverter implements Function<GetMetricStatisticsResult, List<MetricValue>> {

    /** The statistic to extract from each {@link Datapoint}. */
    private final CloudWatchStatistic statistic;

    /**
     * Constructs a new {@link MetricValueConverter} that, for each in a
     * collection of CloudWatch {@link Datapoint}s, will extract a particular
     * statistic and convert it to a {@link MetricValue}.
     *
     * @param statistic
     *            The statistic to extract from each {@link Datapoint}.
     */
    public MetricValueConverter(CloudWatchStatistic statistic) {
        this.statistic = statistic;
    }

    @Override
    public List<MetricValue> apply(GetMetricStatisticsResult input) {
        Objects.requireNonNull(input, "metric statistics cannot be null");
        List<MetricValue> values = new ArrayList<>();

        String metric = input.getLabel();
        for (Datapoint dataPoint : input.getDatapoints()) {
            DateTime timestamp = new DateTime(dataPoint.getTimestamp(), DateTimeZone.UTC);
            Double statisticValue = extractStatisticValue(dataPoint);
            if (statisticValue == null) {
                throw new IllegalArgumentException(String.format(
                        "failed to extract statistic '%s' from " + "CloudWatch data points: field is missing",
                        this.statistic));
            }
            values.add(new MetricValue(metric, statisticValue, timestamp));
        }
        return values;
    }

    private Double extractStatisticValue(Datapoint dataPoint) {
        switch (this.statistic) {
        case Sum:
            return dataPoint.getSum();
        case Average:
            return dataPoint.getAverage();
        case Maximum:
            return dataPoint.getMaximum();
        case Minimum:
            return dataPoint.getMinimum();
        case SampleCount:
            return dataPoint.getSampleCount();
        default:
            throw new IllegalArgumentException(String.format("unrecognized statistic", this.statistic));
        }
    }
}