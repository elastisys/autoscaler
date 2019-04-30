package com.elastisys.autoscaler.core.utils.stats.timeseries.impl;

import static com.elastisys.autoscaler.core.utils.stats.timeseries.impl.TimeSeriesTestUtils.dataPoint;
import static com.elastisys.autoscaler.core.utils.stats.timeseries.impl.TimeSeriesTestUtils.list;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeries;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.BasicTimeSeries;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.FilteredTimeSeries;

/**
 * Exercises the {@link FilteredTimeSeries}.
 */
public class TestFilteredTimeSeries {

    private BasicTimeSeries backingSeries;

    @Before
    public void onSetup() {
        this.backingSeries = new BasicTimeSeries();
    }

    @Test
    public void testFilteredTimeSeries() {
        // Apply a filter that only returns even numbered data points
        EvenPredicate evenFilter = new EvenPredicate();
        TimeSeries evenOnlySeries = new FilteredTimeSeries(this.backingSeries, evenFilter);
        DataPoint p1 = dataPoint(1, 1.0);
        DataPoint p2 = dataPoint(2, 2.0);
        DataPoint p3 = dataPoint(3, 3.0);
        DataPoint p4 = dataPoint(4, 4.0);
        DataPoint p5 = dataPoint(5, 5.0);
        DataPoint p6 = dataPoint(6, 6.0);
        evenOnlySeries.addAll(list(p1, p2, p3, p4, p5, p6));

        // odd numbers are to be filtered out
        assertThat(evenOnlySeries.getDataPoints(), is(list(p2, p4, p6)));

        // the backing time series has not been modified
        assertThat(this.backingSeries.getDataPoints(), is(list(p1, p2, p3, p4, p5, p6)));
    }

    private static class EvenPredicate implements Predicate<DataPoint> {

        @Override
        public boolean test(DataPoint point) {
            return Double.valueOf(point.getValue()).intValue() % 2 == 0;
        }
    }
}
