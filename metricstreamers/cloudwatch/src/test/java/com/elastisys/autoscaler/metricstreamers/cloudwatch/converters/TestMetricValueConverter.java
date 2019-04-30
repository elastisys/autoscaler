package com.elastisys.autoscaler.metricstreamers.cloudwatch.converters;

import static com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchStatistic.Average;
import static com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchStatistic.Maximum;
import static com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchStatistic.Minimum;
import static com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchStatistic.SampleCount;
import static com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchStatistic.Sum;
import static com.elastisys.autoscaler.metricstreamers.cloudwatch.converters.ConverterTestUtils.datapoint;
import static com.elastisys.autoscaler.metricstreamers.cloudwatch.converters.ConverterTestUtils.datapoints;
import static com.elastisys.autoscaler.metricstreamers.cloudwatch.converters.ConverterTestUtils.time;
import static com.elastisys.autoscaler.metricstreamers.cloudwatch.converters.ConverterTestUtils.value;
import static com.elastisys.autoscaler.metricstreamers.cloudwatch.converters.ConverterTestUtils.values;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.joda.time.DateTime;
import org.junit.Test;

import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchStatistic;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.converters.MetricValueConverter;

/**
 * Exercises the {@link MetricValueConverter} class.
 */
public class TestMetricValueConverter {

    @Test(expected = NullPointerException.class)
    public void applyOnNull() {
        MetricValueConverter converter = new MetricValueConverter(CloudWatchStatistic.Average);
        converter.apply(null);
    }

    @Test
    public void applyOnEmpty() {
        MetricValueConverter converter = new MetricValueConverter(Average);

        GetMetricStatisticsResult empty = new GetMetricStatisticsResult().withDatapoints().withLabel("metric");
        assertThat(converter.apply(empty), is(values()));
    }

    @Test
    public void applyOnSingleValue() {
        MetricValueConverter converter = new MetricValueConverter(Average);
        DateTime time = time(1);
        GetMetricStatisticsResult stats = new GetMetricStatisticsResult()
                .withDatapoints(datapoints(datapoint(Average, 1.0, time))).withLabel("metric");

        assertThat(converter.apply(stats), is(values(value(1.0, time))));

    }

    @Test
    public void applyOnMultipleValues() {
        MetricValueConverter converter = new MetricValueConverter(Average);
        GetMetricStatisticsResult stats = new GetMetricStatisticsResult()
                .withDatapoints(datapoints(datapoint(Average, 1.0, time(1)), datapoint(Average, 2.0, time(2)),
                        datapoint(Average, 3.0, time(3))))
                .withLabel("metric");

        assertThat(converter.apply(stats), is(values(value(1.0, time(1)), value(2.0, time(2)), value(3.0, time(3)))));
    }

    /**
     * Verify behavior when an attempt is made to extract a statistic that isn't
     * available from the input data points.
     */
    @Test(expected = IllegalArgumentException.class)
    public void applyToValuesOfWrongStatistic() {
        MetricValueConverter converter = new MetricValueConverter(Average);
        GetMetricStatisticsResult stats = new GetMetricStatisticsResult()
                .withDatapoints(datapoints(datapoint(Sum, 1.0, time(1)))).withLabel("metric");

        converter.apply(stats);
    }

    @Test
    public void extractSumStatistic() {
        MetricValueConverter converter = new MetricValueConverter(Sum);
        GetMetricStatisticsResult stats = new GetMetricStatisticsResult()
                .withDatapoints(datapoints(datapoint(Sum, 1.0, time(1)))).withLabel("metric");

        assertThat(converter.apply(stats), is(values(value(1.0, time(1)))));
    }

    @Test
    public void extractMaximumStatistic() {
        MetricValueConverter converter = new MetricValueConverter(Maximum);
        GetMetricStatisticsResult stats = new GetMetricStatisticsResult()
                .withDatapoints(datapoints(datapoint(Maximum, 1.0, time(1)))).withLabel("metric");

        assertThat(converter.apply(stats), is(values(value(1.0, time(1)))));
    }

    @Test
    public void extractMinimumStatistic() {
        MetricValueConverter converter = new MetricValueConverter(Minimum);
        GetMetricStatisticsResult stats = new GetMetricStatisticsResult()
                .withDatapoints(datapoints(datapoint(Minimum, 1.0, time(1)))).withLabel("metric");

        assertThat(converter.apply(stats), is(values(value(1.0, time(1)))));
    }

    @Test
    public void extractSampleCountStatistic() {
        MetricValueConverter converter = new MetricValueConverter(SampleCount);
        GetMetricStatisticsResult stats = new GetMetricStatisticsResult()
                .withDatapoints(datapoints(datapoint(SampleCount, 1.0, time(1)))).withLabel("metric");

        assertThat(converter.apply(stats), is(values(value(1.0, time(1)))));
    }
}
