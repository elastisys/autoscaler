package com.elastisys.autoscaler.core.utils.stats.timeseries.impl;

import java.util.Objects;

import org.joda.time.DateTime;

import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeries;
import com.elastisys.scale.commons.util.precond.Preconditions;

/**
 * Represents a time-stamped value observation.
 *
 * @see TimeSeries
 */
public class BasicDataPoint implements DataPoint {
    /** The timestamp at which value was observed. */
    private final DateTime time;
    /** The observed value. */
    private final double value;

    /**
     * Constructs a {@link BasicDataPoint}.
     *
     * @param time
     *            The time at which value was observed.
     * @param value
     *            The observed value.
     */
    public BasicDataPoint(DateTime time, double value) {
        Preconditions.checkArgument(time != null, "time cannot be null");
        this.time = time;
        this.value = value;
    }

    @Override
    public DateTime getTime() {
        return this.time;
    }

    @Override
    public double getValue() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.time, this.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BasicDataPoint) {
            BasicDataPoint that = (BasicDataPoint) obj;
            return Objects.equals(this.time, that.time) && Objects.equals(this.value, that.value);

        }
        return false;
    }

    @Override
    public String toString() {
        return "(" + this.time + ": " + this.value + ")";
    }
}