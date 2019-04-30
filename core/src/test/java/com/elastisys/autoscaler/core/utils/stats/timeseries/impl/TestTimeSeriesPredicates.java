package com.elastisys.autoscaler.core.utils.stats.timeseries.impl;

import static com.elastisys.autoscaler.core.api.types.TestMetricValue.metricValue;
import static com.elastisys.autoscaler.core.api.types.TestMetricValue.time;
import static com.elastisys.autoscaler.core.utils.stats.timeseries.impl.TimeSeriesTestUtils.dataPoint;
import static com.elastisys.autoscaler.core.utils.stats.timeseries.impl.TimeSeriesTestUtils.list;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Test;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeriesFunctions;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeriesPredicates;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeriesPredicates.DataPointOlderThan;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeriesPredicates.IsOutlier;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.BasicTimeSeries;

/**
 * Tests the {@link Predicate}s in the {@link TimeSeriesPredicates} class.
 */
public class TestTimeSeriesPredicates {
    /**
     * Verifies the behavior of the {@link DataPointOlderThan} predicate.
     */
    @Test
    public void testOlderThan() {
        Predicate<? super MetricValue> olderThan10 = TimeSeriesPredicates.olderThan(time(10));

        assertTrue(olderThan10.test(metricValue(time(1))));
        assertTrue(olderThan10.test(metricValue(time(9))));
        assertFalse(olderThan10.test(metricValue(time(10))));
        assertFalse(olderThan10.test(metricValue(time(11))));
    }

    /**
     * Verifies the behavior of the {@link MetricValueYoungerThan}
     * {@link Predicate}.
     */
    @Test
    public void testYoungerThan() {
        Predicate<? super MetricValue> youngerThan10 = TimeSeriesPredicates.youngerThan(time(10));

        assertFalse(youngerThan10.test(metricValue(time(1))));
        assertFalse(youngerThan10.test(metricValue(time(9))));
        assertFalse(youngerThan10.test(metricValue(time(10))));
        assertTrue(youngerThan10.test(metricValue(time(11))));
        assertTrue(youngerThan10.test(metricValue(time(12))));
    }

    /**
     * Verifies the behavior of the {@link MetricValueInInterval}
     * {@link Predicate}.
     */
    @Test
    public void testWithin() {
        Predicate<? super MetricValue> between5And9 = TimeSeriesPredicates.within(new Interval(time(5), time(10)));

        assertFalse(between5And9.test(metricValue(time(1))));
        assertFalse(between5And9.test(metricValue(time(4))));
        assertTrue(between5And9.test(metricValue(time(5))));
        assertTrue(between5And9.test(metricValue(time(6))));
        assertTrue(between5And9.test(metricValue(time(8))));
        assertTrue(between5And9.test(metricValue(time(9))));
        assertFalse(between5And9.test(metricValue(time(10))));
    }

    /**
     * Verifies the behavior of the {@link IsOutlier} {@link Predicate}.
     */
    @Test
    public void testIsOutlier() {
        // In this time series, only 50.0 is an outlier according to Chauvenet's
        // criterion: http://en.wikipedia.org/wiki/Chauvenet%27s_criterion
        BasicTimeSeries timeSeries = new BasicTimeSeries();
        List<DataPoint> datapoints = list(dataPoint(new DateTime(1), 9.0), dataPoint(new DateTime(2), 10.0),
                dataPoint(new DateTime(3), 10.0), dataPoint(new DateTime(4), 10.0), dataPoint(new DateTime(5), 11.0),
                dataPoint(new DateTime(6), 50.0));
        timeSeries.addAll(datapoints);
        SummaryStatistics stats = TimeSeriesFunctions.timeSeriesStats().apply(timeSeries);

        Predicate<? super DataPoint> isOutlier = TimeSeriesPredicates.isOutlier(stats.getMean(),
                stats.getStandardDeviation(), stats.getN());

        assertFalse(isOutlier.test(dataPoint(new DateTime(1), 9.0)));
        assertFalse(isOutlier.test(dataPoint(new DateTime(2), 10.0)));
        assertFalse(isOutlier.test(dataPoint(new DateTime(4), 11.0)));
        assertTrue(isOutlier.test(dataPoint(new DateTime(6), 50.0)));
    }

    /**
     * Verifies the behavior of the {@link IsOutlier} {@link Predicate}.
     */
    @Test
    public void testIsOutlier2() {
        // In this time series, only 50.0 is an outlier according to Chauvenet's
        // criterion: http://en.wikipedia.org/wiki/Chauvenet%27s_criterion
        BasicTimeSeries timeSeries = new BasicTimeSeries();
        List<DataPoint> datapoints = list(dataPoint(new DateTime(1), 9.0), dataPoint(new DateTime(2), 10.0),
                dataPoint(new DateTime(3), 10.0), dataPoint(new DateTime(4), 10.0), dataPoint(new DateTime(5), 11.0),
                dataPoint(new DateTime(6), 50.0));
        timeSeries.addAll(datapoints);
        SummaryStatistics stats = TimeSeriesFunctions.timeSeriesStats().apply(timeSeries);

        Predicate<? super DataPoint> isOutlier = TimeSeriesPredicates.isOutlier(stats.getMean(),
                stats.getStandardDeviation(), stats.getN());

        assertFalse(isOutlier.test(dataPoint(new DateTime(1), 9.0)));
        assertFalse(isOutlier.test(dataPoint(new DateTime(2), 10.0)));
        assertFalse(isOutlier.test(dataPoint(new DateTime(4), 11.0)));
        assertTrue(isOutlier.test(dataPoint(new DateTime(6), 50.0)));
    }

}
