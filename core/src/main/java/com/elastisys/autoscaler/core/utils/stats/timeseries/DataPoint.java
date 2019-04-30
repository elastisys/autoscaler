package com.elastisys.autoscaler.core.utils.stats.timeseries;

import java.util.Objects;

import org.joda.time.DateTime;

/**
 * Represents an individual value observation in a {@link TimeSeries}.
 * <p/>
 * It is advised that a {@link DataPoint} implementation provides a
 * {@link #compareTo(DataPoint)} method that sorts {@link DataPoint}s in
 * increasing time-order. This is what the default {@link #compareTo(DataPoint)}
 * implementation does.
 *
 * @see TimeSeries
 */
public interface DataPoint extends Comparable<DataPoint> {

    /**
     * The observed value.
     *
     * @return
     */
    public double getValue();

    /**
     * The time at which the value was observed.
     *
     * @return
     */
    public DateTime getTime();

    @Override
    default int compareTo(DataPoint other) {
        Objects.requireNonNull(other, "cannot compare DataPoint to a null DataPoint");
        Objects.requireNonNull(getTime(), "cannot compare DataPoint without time field");
        Objects.requireNonNull(other.getTime(), "cannot compare DataPoint without time field");
        return getTime().compareTo(other.getTime());
    }
}
