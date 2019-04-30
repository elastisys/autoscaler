package com.elastisys.autoscaler.core.utils.stats.timeseries.impl;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.BasicDataPoint;

public class TimeSeriesTestUtils {
    public static List<DataPoint> list(DataPoint... dataPoints) {
        return Arrays.asList(dataPoints);
    }

    /**
     * Creates a {@link DataPoint} observation with a given time and value.
     *
     * @param time
     *            The time stamp of the {@link DataPoint}.
     * @param value
     *            The observed value
     * @return
     */
    public static DataPoint dataPoint(DateTime time, double value) {
        return new BasicDataPoint(time, value);
    }

    /**
     * Creates a {@link DataPoint} of a given age and value.
     *
     * @param epochOffsetSeconds
     *            Time of the data point observation as an offset (in seconds)
     *            from epoch.
     * @param value
     * @return
     */
    public static DataPoint dataPoint(long epochOffsetSeconds, double value) {
        return new BasicDataPoint(new DateTime(epochOffsetSeconds * 1000, DateTimeZone.UTC), value);
    }
}
