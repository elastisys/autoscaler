package com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * {@link QueryOptions} are used as hints to a {@link MetricStream} on how to
 * execute a query.
 *
 * @see MetricStream#query(org.joda.time.Interval, QueryOptions)
 */
public class QueryOptions {

    /**
     * Hint to the {@link MetricStream} to downsample the data points in the
     * query result using the given sampling interval and aggregation function.
     * May be <code>null</code>.
     * <p/>
     * If supported by the {@link MetricStream}, at most a single data point
     * will be returned for each sampling interval, and for the cases where that
     * sampling interval contained several data points, all those data points
     * are aggregated using the sampling function.
     * <p/>
     * As an example, data can be down-sampled such that only the mean over each
     * 10 minute period is reported.
     *
     */
    private final Downsample downsample;

    /**
     * Creates an empty {@link QueryOptions} object.
     */
    public QueryOptions() {
        this(null);
    }

    /**
     * Creates a new {@link QueryOptions} object.
     *
     * @param downsample
     *            Hint to the {@link MetricStream} to downsample the data points
     *            in the query result using the given sampling interval and
     *            aggregation function. May be <code>null</code>.
     */
    public QueryOptions(Downsample downsample) {
        this.downsample = downsample;
    }

    /**
     * Hint to the {@link MetricStream} to downsample the data points in the
     * query result using the given sampling interval and aggregation function.
     * <p/>
     * If supported by the {@link MetricStream}, at most a single data point
     * will be returned for each sampling interval, and for the cases where that
     * sampling interval contained several data points, all those data points
     * are aggregated using the sampling function.
     * <p/>
     * As an example, data can be down-sampled such that only the mean over each
     * 10 minute period is reported.
     *
     * @return
     */
    public Optional<Downsample> getDownsample() {
        return Optional.ofNullable(this.downsample);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.downsample);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof QueryOptions) {
            QueryOptions that = (QueryOptions) obj;
            return Objects.equals(this.downsample, that.downsample);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
