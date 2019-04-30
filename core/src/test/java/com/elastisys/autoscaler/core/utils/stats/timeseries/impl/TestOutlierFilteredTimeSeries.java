package com.elastisys.autoscaler.core.utils.stats.timeseries.impl;

import static com.elastisys.autoscaler.core.utils.stats.timeseries.impl.TimeSeriesTestUtils.dataPoint;
import static com.elastisys.autoscaler.core.utils.stats.timeseries.impl.TimeSeriesTestUtils.list;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeries;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.BasicTimeSeries;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.OutlierFilteredTimeSeries;

/**
 * Exercises the {@link OutlierFilteredTimeSeries}.
 */
public class TestOutlierFilteredTimeSeries {

    private BasicTimeSeries backingSeries;

    @Before
    public void onSetup() {
        this.backingSeries = new BasicTimeSeries();
    }

    @Test
    public void testFilteredTimeSeries() {
        // The example of Chauvenet's criterion that is illustrated here:
        // http://en.wikipedia.org/wiki/Chauvenet%27s_criterion
        TimeSeries filteredSeries = new OutlierFilteredTimeSeries(this.backingSeries);
        DataPoint p1 = dataPoint(1, 9.0);
        DataPoint p2 = dataPoint(2, 10.0);
        DataPoint p3 = dataPoint(3, 10.0);
        DataPoint p4 = dataPoint(4, 10.0);
        DataPoint p5 = dataPoint(5, 11.0);
        DataPoint p6 = dataPoint(6, 50.0);
        filteredSeries.addAll(list(p1, p2, p3, p4, p5, p6));

        // make sure the outlier value (50.0) gets filtered out
        assertThat(filteredSeries.getDataPoints(), is(list(p1, p2, p3, p4, p5)));

        // make sure that the backing time series has not been modified (that
        // is, filtered out values are not discarded from the underlying
        // series, only hidden from the caller).
        assertThat(this.backingSeries.getDataPoints(), is(list(p1, p2, p3, p4, p5, p6)));

        // if additional "extreme" values keep entering the time series,
        // eventually the outlier becomes a "normal" value in the data set
        DataPoint p7 = dataPoint(7, 35.0);
        filteredSeries.add(p7);
        // ... p6 is still filtered out (but less of an outlier now)
        assertThat(filteredSeries.getDataPoints(), is(list(p1, p2, p3, p4, p5, p7)));
        DataPoint p8 = dataPoint(8, 32.0);
        filteredSeries.add(p8);
        // ... the shape of the series has changed enough so that p6 is no
        // longer an outlier
        assertThat(filteredSeries.getDataPoints(), is(list(p1, p2, p3, p4, p5, p6, p7, p8)));
    }

    /**
     * Adding a {@link DataPoint} with the same time as an existing
     * {@link DataPoint} should cause the new {@link DataPoint} to overwrite the
     * old {@link DataPoint}.
     */
    @Test
    public void addDuplicateDataPoint() {
        TimeSeries series = new OutlierFilteredTimeSeries(this.backingSeries);
        DataPoint point1 = dataPoint(1, 9.0);

        series.add(point1);
        assertEquals(asList(point1), series.getDataPoints());

        DataPoint point1Updated = dataPoint(1, 99.0);
        series.add(point1Updated);
        assertEquals(asList(point1Updated), series.getDataPoints());
    }

}
