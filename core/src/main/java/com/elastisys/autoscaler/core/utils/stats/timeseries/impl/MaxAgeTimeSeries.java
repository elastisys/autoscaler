package com.elastisys.autoscaler.core.utils.stats.timeseries.impl;

import java.util.Collection;
import java.util.List;

import org.joda.time.DateTime;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeries;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeriesPredicates;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Represents a sliding window of {@link DataPoint}s that only tracks recently
 * observed values up to a certain maximum age.
 * <p/>
 * {@link DataPoint}s with a time stamp older than {@code maxAge} seconds are
 * automatically evicted (rotated out) from the collection.
 */
public class MaxAgeTimeSeries implements TimeSeries {

    /**
     * Maximum age of {@link MetricValue}s (in seconds). Values older than this
     * will be evicted from the time series.
     */
    private int maxAge;

    /** The backing {@link TimeSeries}. */
    private final TimeSeries series;

    /**
     * Constructs a new {@link MaxAgeTimeSeries} with a given maximum allowed
     * {@link DataPoint} age, relative to the current time.
     *
     * @param timeSeries
     *            The backing {@link TimeSeries}.
     * @param maxAge
     *            Maximum age of {@link DataPoint}s (in seconds). Values older
     *            than this will be evicted from the time series.
     */
    public MaxAgeTimeSeries(TimeSeries timeSeries, int maxAge) {
        this.series = timeSeries;
        this.maxAge = maxAge;
    }

    /**
     * Returns the maximum age of {@link DataPoint}s (in seconds). Values older
     * than this will be evicted from the time series.
     *
     * @return
     */
    public int getMaxAge() {
        return this.maxAge;
    }

    /**
     * Resizes this {@link MaxAgeTimeSeries} by setting the maximum allowed age
     * of {@link DataPoint}s (in seconds). In case the operation shrinks the
     * capacity of the time series, {@link MetricValue}s older than the new
     * {@code maxAge} are rotated out.
     *
     * @param maxAge
     *            The new maximum age of {@link MetricValue}s (in seconds).
     */
    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
        evictOldMetricValues();
    }

    /**
     * Evicts any {@link MetricValue}s older than {@code maxAge} from the
     * backing list of {@link MetricValue}s.
     *
     * @return <code>true</code> if any {@link MetricValue}s were evicted.
     */
    private boolean evictOldMetricValues() {
        boolean didEvict = false;

        for (int i = 0; i < this.series.size(); i++) {
            DataPoint oldest = this.series.getDataPoints().get(0);
            if (TimeSeriesPredicates.olderThan(oldestAllowedTimestamp()).test(oldest)) {
                this.series.remove(0);
                didEvict = true;
            } else {
                break;
            }
        }

        return didEvict;
    }

    /**
     * Returns the oldest allowed {@link MetricValue} time stamp, given the
     * current time as defined by the {@link TimeSource}.
     *
     * @return
     */
    private DateTime oldestAllowedTimestamp() {
        return UtcTime.now().minusSeconds(this.maxAge);
    }

    @Override
    public int size() {
        evictOldMetricValues();
        return this.series.size();
    }

    @Override
    public boolean isEmpty() {
        evictOldMetricValues();
        return this.series.isEmpty();
    }

    @Override
    public void add(DataPoint dataPoint) {
        this.series.add(dataPoint);
        evictOldMetricValues();
    }

    @Override
    public void addAll(Collection<? extends DataPoint> dataPoints) {
        this.series.addAll(dataPoints);
        evictOldMetricValues();
    }

    @Override
    public void remove(int index) throws IndexOutOfBoundsException {
        this.series.remove(index);
        evictOldMetricValues();
    }

    @Override
    public List<DataPoint> getDataPoints() {
        evictOldMetricValues();
        return this.series.getDataPoints();
    }

    @Override
    public int hashCode() {
        return this.series.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.series.equals(obj);
    }

    @Override
    public String toString() {
        evictOldMetricValues();
        return this.series.toString();
    }

}
