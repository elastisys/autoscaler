package com.elastisys.autoscaler.core.utils.stats.timeseries.impl;

import java.util.Collection;
import java.util.List;

import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeries;

/**
 * Represents a {@link TimeSeries} with limited capacity ("memory"). It will
 * only keep track of the {@code N} most recently observed {@link DataPoint}s,
 * where {@code N} is the capacity of the {@link TimeSeries}.
 * <p/>
 * The data series is sliding in the sense that when the it has reached its
 * capacity, the adding of another {@link DataPoint} will cause the oldest
 * {@link DataPoint} to be rotated out.
 *
 * @see DataPoint
 */
public class FixedCapacityTimeSeries implements TimeSeries {

    /**
     * The "memory" of the {@link TimeSeries}. That is, the maximum number of
     * {@link DataPoint}s that will be kept in the {@link TimeSeries} before
     * adding of additional {@link DataPoint}s will cause older
     * {@link DataPoint}s to be dropped.
     */
    private int capacity;

    /** The backing {@link TimeSeries}. */
    private final TimeSeries series;

    /**
     * Constructs a new {@link FixedCapacityTimeSeries}.
     *
     * @param timeSeries
     *            The backing {@link TimeSeries}.
     * @param capacity
     *            The "memory" of the {@link TimeSeries}. That is, the maximum
     *            number of {@link DataPoint}s that will be kept in the
     *            {@link TimeSeries} before adding of additional
     *            {@link DataPoint}s will cause older {@link DataPoint}s to be
     *            dropped.
     */
    public FixedCapacityTimeSeries(BasicTimeSeries timeSeries, int capacity) {
        this.series = timeSeries;
        this.capacity = capacity;
    }

    @Override
    public void add(DataPoint dataPoint) {
        this.series.add(dataPoint);
        rotate();
    }

    @Override
    public void addAll(Collection<? extends DataPoint> dataPoints) {
        this.series.addAll(dataPoints);
        rotate();
    }

    @Override
    public void remove(int index) throws IndexOutOfBoundsException {
        this.series.remove(index);
    }

    @Override
    public List<DataPoint> getDataPoints() {
        return this.series.getDataPoints();
    }

    @Override
    public boolean isEmpty() {
        return this.series.isEmpty();
    }

    @Override
    public int size() {
        return this.series.size();
    }

    /**
     * Rotates out the oldest data point(s) in case the collection holds more
     * {@link DataPoint}s than it is currently dimensioned for.
     */
    private void rotate() {
        if (this.series.size() <= this.capacity) {
            return;
        }

        while (this.series.size() > this.capacity) {
            this.series.remove(0);
        }
    }

    /**
     * Returns <code>true</code> if this data series has reached its capacity.
     *
     * @return
     */
    public boolean isFull() {
        return this.series.size() == this.capacity;
    }

    /**
     * Returns the "memory" size of the {@link TimeSeries}. That is, the maximum
     * number of {@link DataPoint}s that will be kept in the {@link TimeSeries}
     * before adding of additional {@link DataPoint}s will cause older
     * {@link DataPoint}s to be dropped.
     *
     * @return
     */
    public int getCapacity() {
        return this.capacity;
    }

    /**
     * Re-sizes this {@link TimeSeries} by setting its capacity. In case the
     * operation shrinks the capacity of the {@link TimeSeries}, the oldest
     * {@link DataPoint}s will be rotated out.
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
        rotate();
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
        return this.series.toString();
    }
}
