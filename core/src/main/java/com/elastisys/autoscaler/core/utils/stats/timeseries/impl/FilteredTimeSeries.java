package com.elastisys.autoscaler.core.utils.stats.timeseries.impl;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeries;

/**
 * A {@link TimeSeries} that is applied around an existing {@link TimeSeries},
 * in order to filter out any {@link DataPoint}s that don't satisfy a certain
 * filter {@link Predicate}.
 * <p/>
 * Note that the {@link FilteredTimeSeries} does not modify the backing
 * {@link TimeSeries}, it only applies its filter when the data points are
 * requested via a call to the {@link #getDataPoints()} method.
 */
public class FilteredTimeSeries implements TimeSeries {

    /**
     * The wrapped {@link TimeSeries} whose {@link DataPoint}s will be filtered.
     */
    private final TimeSeries backingSeries;

    /**
     * The {@link Predicate} that needs to be satisfied by {@link DataPoint}s in
     * the backing {@link TimeSeries} that are to be visible to callers of
     * {@link #getDataPoints()}.
     */
    private final Predicate<? super DataPoint> inclusionFilter;

    /**
     * Constructs a new {@link FilteredTimeSeries} that wraps an existing
     * {@link TimeSeries}.
     *
     * @param backingSeries
     *            The backing {@link TimeSeries}.
     * @param inclusionFilter
     *            The {@link Predicate} that needs to be satisfied by
     *            {@link DataPoint}s in the backing {@link TimeSeries} that are
     *            to be visible to callers of {@link #getDataPoints()}.
     *
     */
    public FilteredTimeSeries(TimeSeries backingSeries, Predicate<? super DataPoint> inclusionFilter) {
        this.backingSeries = backingSeries;
        this.inclusionFilter = inclusionFilter;
    }

    @Override
    public void add(DataPoint dataPoint) {
        this.backingSeries.add(dataPoint);
    }

    @Override
    public void addAll(Collection<? extends DataPoint> dataPoints) {
        this.backingSeries.addAll(dataPoints);
    }

    @Override
    public void remove(int index) throws IndexOutOfBoundsException {
        this.backingSeries.remove(index);
    }

    /**
     * Returns only the {@link DataPoint}s that satisfy the filter
     * {@link Predicate}.
     *
     * @see com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeries#getDataPoints()
     */
    @Override
    public List<DataPoint> getDataPoints() {
        // filter out any data points not satisfying the inclusion filter
        // predicate
        List<DataPoint> allDataPoints = this.backingSeries.getDataPoints();
        return allDataPoints.stream().filter(this.inclusionFilter).collect(Collectors.toList());
    }

    @Override
    public boolean isEmpty() {
        return getDataPoints().isEmpty();
    }

    @Override
    public int size() {
        return getDataPoints().size();
    }

    @Override
    public String toString() {
        return this.backingSeries.toString();
    }
}
