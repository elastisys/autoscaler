package com.elastisys.autoscaler.core.utils.stats.timeseries;

import java.util.Collection;
import java.util.List;

/**
 * A sequence of time-stamped value observations ({@link DataPoint}s) ordered in
 * increasing order of time. For any point in time, only a single
 * {@link DataPoint} may exist. Therefore, adding a {@link DataPoint} with the
 * same time as an existing {@link DataPoint} in the {@link TimeSeries} has the
 * effect of overwriting the existing {@link DataPoint}.
 *
 * @see DataPoint
 */
public interface TimeSeries {

    /**
     * Adds a {@link DataPoint} to the {@link TimeSeries}. For any point in
     * time, only a single {@link DataPoint} may exist. Therefore, adding a
     * {@link DataPoint} with the same time as an existing {@link DataPoint} in
     * the {@link TimeSeries} has the effect of overwriting the existing
     * {@link DataPoint}.
     *
     * @param dataPoint
     */
    public void add(DataPoint dataPoint);

    /**
     * Adds a collection of {@link DataPoint}s to the {@link TimeSeries}. For
     * any point in time, only a single {@link DataPoint} may exist. Therefore,
     * adding a {@link DataPoint} with the same time as an existing
     * {@link DataPoint} in the {@link TimeSeries} has the effect of overwriting
     * the existing {@link DataPoint}.
     *
     * @param dataPoints
     */
    public void addAll(Collection<? extends DataPoint> dataPoints);

    /**
     * Removes a {@link DataPoint} from this {@link TimeSeries}.
     *
     * @param index
     *            The index of the {@link DataPoint} to remove.
     * @throws IndexOutOfBoundsException
     *             On attempts to remove elements not within the
     *             {@link TimeSeries} bounds.
     */
    public void remove(int index) throws IndexOutOfBoundsException;

    /**
     * Returns the sequence of {@link DataPoint}s observed in this
     * {@link TimeSeries}, sorted in chronological order (oldest first).
     *
     * @return
     */
    public List<DataPoint> getDataPoints();

    /**
     * Returns <code>true</code> if this {@link TimeSeries} does not contain any
     * {@link DataPoint}s, <code>false</code> if it does.
     *
     * @return
     */
    public boolean isEmpty();

    /**
     * Return the number of {@link DataPoint}s in this {@link TimeSeries}.
     *
     * @return
     */
    public int size();

}
