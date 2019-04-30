package com.elastisys.autoscaler.core.api.types;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.joda.time.DateTime;

import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeries;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.BasicDataPoint;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Represents a value observation for a certain metric (a metric is essentially
 * a {@link TimeSeries} of value observations).
 * <p/>
 * Besides a metric category, a value and a time stamp, a {@link MetricValue}
 * can be decorated with a set of tags. Tags are name-value pairs that can be
 * thought of as a means of categorizing/sub-grouping metric time-series. Each
 * tag essentially produces a separate, more specialized, time-series for the
 * metric. Tags help you distinguish metric values from each other (for example,
 * metric values reported by different hosts) and can be used to filter result
 * sets (for example, to only retrieve metric values reported by a certain
 * host).
 * <p/>
 * {@link MetricValue}s are {@link Comparable} and a total ordering (in
 * increasing order of time stamp) is imposed on the {@link MetricValue}s.
 *
 * @see DataPoint
 * @see TimeSeries
 */
public class MetricValue extends BasicDataPoint {
    /** The metric that this {@link MetricValue} concerns. */
    private final String metric;
    /**
     * A set of tags that further describe the {@link MetricValue}.
     * <p/>
     * Tags are name-value pairs that can be thought of as a means of
     * categorizing/sub-grouping metric time-series. Each tag essentially
     * produces a separate, more specialized, time-series for the metric. Tags
     * help you distinguish metric values from each other (for example, metric
     * values reported by different hosts) and can be used to filter result sets
     * (for example, to only retrieve metric values reported by a certain host).
     */
    private final Map<String, String> tags;

    /**
     * Creates a {@link MetricValue} without tags.
     *
     * @param metric
     *            The metric that this {@link MetricValue} concerns.
     * @param value
     *            The value of the {@link MetricValue}.
     * @param timestamp
     *            The time-stamp of the {@link MetricValue}.
     */
    public MetricValue(String metric, double value, DateTime timestamp) {
        this(metric, value, timestamp, new HashMap<String, String>());
    }

    /**
     * Creates a {@link MetricValue} with the given parameters.
     *
     * @param metric
     *            The metric that this {@link MetricValue} concerns.
     * @param value
     *            The value of the {@link MetricValue}.
     * @param timestamp
     *            The time-stamp of the {@link MetricValue}.
     * @param tags
     *            A set of tags that further describe the {@link MetricValue}.
     *            <p/>
     *            Tags are name-value pairs that can be thought of as a means of
     *            categorizing/sub-grouping metric time-series. Each tag
     *            essentially produces a separate, more specialized, time-series
     *            for the metric. Tags help you distinguish metric values from
     *            each other (for example, metric values reported by different
     *            hosts) and can be used to filter result sets (for example, to
     *            only retrieve metric values reported by a certain host).
     */
    public MetricValue(String metric, double value, DateTime timestamp, Map<String, String> tags) {
        super(timestamp, value);

        requireNonNull(metric, "Metric cannot be null");
        requireNonNull(timestamp, "Time stamp cannot be null");
        requireNonNull(tags, "Tags cannot be null");

        this.metric = metric;
        this.tags = tags;
    }

    /**
     * Returns the metric that this {@link MetricValue} concerns.
     *
     * @return
     */
    public String getMetric() {
        return this.metric;
    }

    /**
     * Returns a set of tags that further describe the {@link MetricValue}.
     * <p/>
     * Tags are name-value pairs that can be thought of as a means of
     * categorizing/sub-grouping metric time-series. Each tag essentially
     * produces a separate, more specialized, time-series for the metric. Tags
     * help you distinguish metric values from each other (for example, metric
     * values reported by different hosts) and can be used to filter result sets
     * (for example, to only retrieve metric values reported by a certain host).
     *
     * @return
     */
    public Map<String, String> getTags() {
        return Collections.unmodifiableMap(this.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.metric, getValue(), getTime(), this.tags);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetricValue) {
            MetricValue that = (MetricValue) obj;
            return Objects.equals(this.metric, that.metric) && Objects.equals(getValue(), that.getValue())
                    && Objects.equals(getTime(), that.getTime()) && Objects.equals(this.tags, that.tags);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }

    /**
     * Returns a copy of this {@link MetricValue} with an additional tag value.
     * <p/>
     * If the tag already exists on the {@link MetricValue}, the tag will be
     * overwritten on the returned copy. The original remains unchanged though.
     *
     * @param tag
     *            The tag to add.
     * @param value
     *            The value for the tag.
     * @return
     */
    public MetricValue withTag(String tag, String value) {
        Map<String, String> newTags = new HashMap<>(getTags());
        newTags.put(tag, value);
        return new MetricValue(getMetric(), getValue(), getTime(), newTags);
    }

    /**
     * Returns a copy of this {@link MetricValue} with additional tags.
     * <p/>
     * If any of the tags already exists on the {@link MetricValue}, they will
     * be overwritten on the returned copy. The original remains unchanged
     * though.
     *
     * @param tag
     *            The tag to add.
     * @param value
     *            The value for the tag.
     * @return
     */
    public MetricValue withTags(Map<String, String> tags) {
        Map<String, String> newTags = new HashMap<>(getTags());
        newTags.putAll(tags);
        return new MetricValue(getMetric(), getValue(), getTime(), newTags);
    }

    /**
     * Returns a copy of this {@link MetricValue} with all fields identical
     * except for the {@link #value} field which is assigned a new value. The
     * method is side-effect free. That is, the {@link MetricValue} instance
     * that the invocation is made against is not modified by the call.
     *
     * @param value
     *            The value to assign to the {@link #value} field of the
     *            returned copy.
     * @return A copy of this {@link MetricValue} with a different
     *         {@link #value} field.
     */
    public MetricValue withValue(double value) {
        return new MetricValue(getMetric(), value, getTime(), getTags());
    }

}
