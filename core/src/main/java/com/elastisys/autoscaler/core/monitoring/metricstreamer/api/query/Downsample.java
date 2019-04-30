package com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query;

import java.util.Objects;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * A {@link QueryOptions} hint which suggests the {@link MetricStream} to
 * downsample the data points in the query result using the given sampling
 * interval and aggregation function.
 * <p/>
 * If supported by the {@link MetricStream}, at most a single data point will be
 * returned for each sampling interval, and for the cases where that sampling
 * interval contained several data points, all those data points are aggregated
 * using the sampling function.
 * <p/>
 * As an example, data can be down-sampled such that only the mean over each 10
 * minute period is reported.
 *
 */
public class Downsample {

    /** The sampling interval. */
    private final TimeInterval interval;
    /**
     * The function used to aggregate data points within each sampling interval
     * to a single value.
     */
    private final DownsampleFunction function;

    /**
     * Creates a {@link Downsample} query hint.
     *
     * @param interval
     *            The sampling interval.
     * @param function
     *            The function used to aggregate data points within each
     *            sampling interval to a single value.
     */
    public Downsample(TimeInterval interval, DownsampleFunction function) {
        this.interval = interval;
        this.function = function;
    }

    /**
     * The sampling interval.
     *
     * @return
     */
    public TimeInterval getInterval() {
        return this.interval;
    }

    /**
     * The function used to aggregate data points within each sampling interval
     * to a single value.
     *
     * @return
     */
    public DownsampleFunction getFunction() {
        return this.function;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.interval, this.function);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Downsample) {
            Downsample that = (Downsample) obj;
            return Objects.equals(this.interval, that.interval) && Objects.equals(this.function, that.function);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
