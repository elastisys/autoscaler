package com.elastisys.autoscaler.metricstreamers.ceilometer.converters;

import java.util.concurrent.Callable;
import java.util.function.Function;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.openstack4j.model.telemetry.Statistics;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerFunction;
import com.google.common.base.Preconditions;

/**
 * A {@link Callable} task that takes a {@link Statistics}, as input and
 * converts to {@link MetricValue}.
 */
public class StatisticValueConverter implements Function<Statistics, MetricValue> {

    private final String meter;
    private final CeilometerFunction statistic;

    public StatisticValueConverter(String meter, CeilometerFunction statistic) {
        this.meter = meter;
        this.statistic = statistic;
    }

    @Override
    public MetricValue apply(Statistics input) {
        Preconditions.checkNotNull(input, "statistics cannot be null");

        // the period end date is to be interpreted as UTC time, not the local
        // time zone
        DateTime timeWithUtcZone = new DateTime(input.getPeriodStart().getTime(), DateTimeZone.UTC);
        return new MetricValue(this.meter, getValue(input), timeWithUtcZone);
    }

    private Double getValue(Statistics input) {
        switch (this.statistic) {
        case Sum:
            return input.getSum();
        case Average:
            return input.getAvg();
        case Maximum:
            return input.getMax();
        case Minimum:
            return input.getMin();
        case SampleCount:
            return new Double(input.getCount());
        default:
            throw new IllegalArgumentException(String.format("unrecognized statistic", this.statistic));
        }
    }
}
