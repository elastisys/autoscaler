package com.elastisys.autoscaler.core.utils.stats.timeseries.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeries;

/**
 * A basic implementation of a {@link TimeSeries}, backed by a {@link List}.
 */
public class BasicTimeSeries implements TimeSeries {
    /**
     * The backing collection of {@link DataPoint}s.
     */
    private final List<DataPoint> dataPoints = new ArrayList<>();

    @Override
    public void add(DataPoint dataPoint) {
        // keep the backing list sorted at all times
        insert(dataPoint);
    }

    @Override
    public void addAll(Collection<? extends DataPoint> dataPoints) {
        // keep the backing list sorted at all times
        for (DataPoint dataPoint : dataPoints) {
            add(dataPoint);
        }
    }

    /**
     * Removes a {@link DataPoint} from this {@link TimeSeries}.
     *
     * @param index
     *            The index of the {@link DataPoint} to remove.
     * @throws IndexOutOfBoundsException
     *             On attempts to remove elements not within the
     *             {@link TimeSeries} bounds.
     */
    @Override
    public void remove(int index) throws IndexOutOfBoundsException {
        this.dataPoints.remove(index);
    }

    /**
     * Insert in sorted order.
     *
     * @param dataPoint
     */
    private void insert(DataPoint dataPoint) {
        int index = Collections.binarySearch(this.dataPoints, dataPoint);
        if (index >= 0) {
            // overwrite an existing data point
            this.dataPoints.set(index, dataPoint);
        } else {
            // insert a new data point
            int insertionIndex = -index - 1;
            this.dataPoints.add(insertionIndex, dataPoint);
        }
    }

    @Override
    public List<DataPoint> getDataPoints() {
        // defensive copy so that caller cannot change contents of backing list
        return new ArrayList<>(this.dataPoints);
    }

    @Override
    public boolean isEmpty() {
        return this.dataPoints.isEmpty();
    }

    @Override
    public int size() {
        return this.dataPoints.size();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.dataPoints);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BasicTimeSeries) {
            BasicTimeSeries that = (BasicTimeSeries) obj;
            return Objects.equals(this.dataPoints, that.dataPoints);

        }
        return false;
    }

    @Override
    public String toString() {
        return this.dataPoints.stream().map(DataPoint::toString).collect(Collectors.joining(", ", "[", "]"));
    }
}
