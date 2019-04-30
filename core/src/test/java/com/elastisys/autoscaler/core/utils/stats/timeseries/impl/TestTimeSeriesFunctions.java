package com.elastisys.autoscaler.core.utils.stats.timeseries.impl;

import static com.elastisys.autoscaler.core.utils.stats.timeseries.impl.TimeSeriesTestUtils.dataPoint;
import static com.elastisys.autoscaler.core.utils.stats.timeseries.impl.TimeSeriesTestUtils.list;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.autoscaler.core.utils.stats.functions.AverageFunction;
import com.elastisys.autoscaler.core.utils.stats.functions.ExponentiallyWeightedAverageFunction;
import com.elastisys.autoscaler.core.utils.stats.functions.LinearlyWeightedAverageFunction;
import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeries;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeriesFunctions;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeriesFunctions.DataPointToValueTransformer;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.BasicDataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.BasicTimeSeries;

/**
 * Exercises the {@link Function}s in {@link TimeSeriesFunctions}.
 */
public class TestTimeSeriesFunctions {

    @Test
    public void testToValue() {
        DataPointToValueTransformer toValueTransformer = new TimeSeriesFunctions.DataPointToValueTransformer();

        DataPoint p1 = new BasicDataPoint(new DateTime(1), 1.0);
        DataPoint p2 = new BasicDataPoint(new DateTime(2), 2.0);
        DataPoint p3 = new BasicDataPoint(new DateTime(3), 3.0);

        assertThat(toValueTransformer.apply(p1), is(1.0));
        assertThat(toValueTransformer.apply(p2), is(2.0));
        assertThat(toValueTransformer.apply(p3), is(3.0));

        List<DataPoint> dataPoints = asList(p1, p2, p3);
        List<Double> values = asList(1.0, 2.0, 3.0);
        assertThat(dataPoints.stream().map(DataPoint::getValue).collect(Collectors.toList()), is(values));
    }

    /**
     * Test the {@link TimeSeriesFunctions#timeSeriesStats()} function.
     */
    @Test
    public void testTimeSeriesStats() {
        // empty time series
        BasicTimeSeries series = new BasicTimeSeries();
        SummaryStatistics stats = TimeSeriesFunctions.timeSeriesStats().apply(series);
        assertThat(stats.getN(), is(0L));
        assertThat(stats.getMean(), is(Double.NaN));
        assertThat(stats.getStandardDeviation(), is(Double.NaN));

        // single value time series
        series = new BasicTimeSeries();
        series.addAll(list(dataPoint(new DateTime(1), 1.0)));
        stats = TimeSeriesFunctions.timeSeriesStats().apply(series);
        assertThat(stats.getN(), is(1L));
        assertThat(stats.getMean(), is(1.0));
        assertThat(stats.getStandardDeviation(), is(0.0));

        // multi-value time series
        series = new BasicTimeSeries();
        series.addAll(Arrays.asList(dataPoint(0, 2), dataPoint(1, 4), dataPoint(2, 4), dataPoint(3, 4), dataPoint(4, 5),
                dataPoint(5, 5), dataPoint(6, 7), dataPoint(7, 9)));
        stats = TimeSeriesFunctions.timeSeriesStats().apply(series);
        assertThat(stats.getN(), is(8L));
        assertThat(stats.getMean(), is(5.0));
        assertThat(Math.round(stats.getStandardDeviation()), is(2L));
    }

    /**
     * Test applying
     * {@link TimeSeriesFunctions#aggregate(TimeSeries, com.elastisys.autoscaler.core.utils.stats.functions.AggregationFunction)}
     * on an empty {@link TimeSeries}.
     */
    @Test
    public void testAggregateOnEmptySeries() {
        TimeSeries emptySeries = new BasicTimeSeries();
        Optional<Double> absent = Optional.empty();

        assertThat(TimeSeriesFunctions.aggregate(emptySeries, new AverageFunction()), is(absent));

        assertThat(TimeSeriesFunctions.aggregate(emptySeries, new LinearlyWeightedAverageFunction()), is(absent));

        assertThat(TimeSeriesFunctions.aggregate(emptySeries, new ExponentiallyWeightedAverageFunction()), is(absent));
    }

    /**
     * Test applying
     * {@link TimeSeriesFunctions#aggregate(TimeSeries, com.elastisys.autoscaler.core.utils.stats.functions.AggregationFunction)}
     * on an empty {@link TimeSeries}.
     */
    @Test
    public void testAggregateWithoutOutlierFiltering() {
        // The example of Chauvenet's criterion that is illustrated here:
        // http://en.wikipedia.org/wiki/Chauvenet%27s_criterion
        TimeSeries timeSeries = new BasicTimeSeries();
        DataPoint p1 = dataPoint(1, 9.0);
        DataPoint p2 = dataPoint(2, 10.0);
        DataPoint p3 = dataPoint(3, 10.0);
        DataPoint p4 = dataPoint(4, 10.0);
        DataPoint p5 = dataPoint(5, 11.0);
        DataPoint p6 = dataPoint(6, 50.0);
        timeSeries.addAll(list(p1, p2, p3, p4, p5, p6));

        // Since no outlier filtering is done, 50.0 is included in the average
        boolean filterOutliers = false;
        Optional<Double> average = TimeSeriesFunctions.aggregate(timeSeries, new AverageFunction(), filterOutliers);
        assertThat(average.isPresent(), is(true));
        assertThat(Math.round(average.get()), is(17L));
    }

    /**
     * Test applying the aggregate function with outlier filtering on a
     * {@link TimeSeries}.
     */
    @Test
    public void testAggregateWithOutlierFiltering() {
        // The example of Chauvenet's criterion that is illustrated here:
        // http://en.wikipedia.org/wiki/Chauvenet%27s_criterion

        TimeSeries timeSeries = new BasicTimeSeries();
        DataPoint p1 = dataPoint(1, 9.0);
        DataPoint p2 = dataPoint(2, 10.0);
        DataPoint p3 = dataPoint(3, 10.0);
        DataPoint p4 = dataPoint(4, 10.0);
        DataPoint p5 = dataPoint(5, 11.0);
        DataPoint p6 = dataPoint(6, 50.0);
        timeSeries.addAll(list(p1, p2, p3, p4, p5, p6));

        // Note that the 50.0 data point will be filtered out, and not be
        // included in the average, as it is an outlier.
        boolean filterOutliers = true;
        Optional<Double> average = TimeSeriesFunctions.aggregate(timeSeries, new AverageFunction(), filterOutliers);
        assertThat(average.isPresent(), is(filterOutliers));
        assertThat(average.get(), is(10.0));
    }

}
