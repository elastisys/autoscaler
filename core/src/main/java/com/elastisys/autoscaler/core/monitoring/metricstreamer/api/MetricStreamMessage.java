package com.elastisys.autoscaler.core.monitoring.metricstreamer.api;

import java.util.List;
import java.util.Objects;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Represents a collection of {@link MetricValue}s that have been retrieved for
 * a certain {@link MetricStream} by a {@link MetricStreamer}.
 * <p/>
 * {@link MetricStreamMessage}s are sent onto the {@link AutoScaler}
 * {@link EventBus} by a {@link MetricStreamer} when new {@link MetricValue}s
 * have been fetched for one of its {@link MetricStream}s.
 *
 * @see MetricStreamer
 */
public class MetricStreamMessage {

    /**
     * The identifier of the {@link MetricStream} from which these
     * {@link MetricValue}s were collected.
     */
    private final String id;
    /**
     * A collection of {@link MetricValue}s ordered in increasing order of time
     * (oldest first).
     */
    private final List<MetricValue> metricValues;

    /**
     * Creates a {@link MetricStreamMessage}.
     *
     * @param id
     *            The identifier of the {@link MetricStream} from which these
     *            {@link MetricValue}s were collected.
     * @param metricValues
     *            A collection of {@link MetricValue}s ordered in increasing
     *            order of time (oldest first).
     *
     */
    public MetricStreamMessage(String id, List<MetricValue> metricValues) {
        this.id = id;
        this.metricValues = metricValues;
    }

    /**
     * The identifier of the {@link MetricStream} from which these
     * {@link MetricValue}s were collected.
     *
     * @return
     */
    public String getId() {
        return this.id;
    }

    /**
     * A collection of {@link MetricValue}s ordered in increasing order of time
     * (oldest first).
     *
     * @return
     */
    public List<MetricValue> getMetricValues() {
        return this.metricValues;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.metricValues);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetricStreamMessage) {
            MetricStreamMessage that = (MetricStreamMessage) obj;
            return Objects.equals(this.id, that.id) && Objects.equals(this.metricValues, that.metricValues);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
