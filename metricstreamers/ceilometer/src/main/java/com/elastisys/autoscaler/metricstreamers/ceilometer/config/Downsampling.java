package com.elastisys.autoscaler.metricstreamers.ceilometer.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Used to configure a {@link CeilometerMetricStreamDefinition} to ask for
 * downsampled meter values, referred to as <i>statistics</i> in Ceilometer
 * lingo. Statistics are downsampled meter data where an aggregation function is
 * applied to data points over a certain downsampling period.
 *
 * @see CeilometerMetricStreamDefinition
 */
public class Downsampling {

    /**
     * A Ceilometer aggregation function to apply to data points within each
     * {@link #period} in a query interval.
     */
    private final CeilometerFunction function;

    /**
     * The downsampling period. It determines the distance between aggregated
     * data points in the result set. The aggregation function {@link #function}
     * will be applied to all datapoints within each period in the requested
     * query interval.
     */
    private final TimeInterval period;

    /**
     * Creates a {@link Downsampling}.
     *
     * @param function
     *            A Ceilometer aggregation function to apply to data points
     *            within each {@link #period} in a query interval.
     * @param period
     *            The downsampling period. It determines the distance between
     *            aggregated data points in the result set. The aggregation
     *            function {@link #function} will be applied to all datapoints
     *            within each period in the requested query interval.
     */
    public Downsampling(CeilometerFunction function, TimeInterval period) {
        this.function = function;
        this.period = period;
    }

    /**
     * A Ceilometer aggregation function to apply to data points within each
     * {@link #period} in a query interval.
     *
     * @return
     */
    public CeilometerFunction getFunction() {
        return this.function;
    }

    /**
     * The downsampling period. It determines the distance between aggregated
     * data points in the result set. The aggregation function {@link #function}
     * will be applied to all datapoints within each period in the requested
     * query interval.
     *
     * @return
     */
    public TimeInterval getPeriod() {
        return this.period;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.function, this.period);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Downsampling) {
            Downsampling that = (Downsampling) obj;
            return Objects.equals(this.function, that.function) && Objects.equals(this.period, that.period);
        }
        return false;
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.function != null, "statistics: missing function");
        checkArgument(this.period != null, "statistics: missing period");

        try {
            this.period.validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("statistics: period: " + e.getMessage(), e);
        }
        checkArgument(this.period.getSeconds() > 0, "statistics: period must be a positive duration");
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }

}
