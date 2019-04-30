package com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types;

import java.util.Objects;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * A {@link SystemMetricEvent} can be posted on the {@link AutoScaler}'s
 * {@link EventBus} to notify the {@link SystemHistorian} of a new system
 * {@link MetricValue} that needs to be stored.
 * <p/>
 * Different {@link SystemMetricEvent}s are generated and reported by the
 * different {@link AutoScaler} sub-systems. Each {@link SystemMetricEvent}
 * posted on the {@link AutoScaler} 's {@link EventBus} is captured by the
 * {@link SystemHistorian}, which takes care of recording them in a backing
 * store.
 *
 * @see SystemHistorian
 * @see EventBus
 */
public class SystemMetricEvent {
    /** The {@link MetricValue} being reported by this event. */
    private final MetricValue value;

    /**
     * Constructs a new {@link MetricEvent}.
     *
     * @param value
     *            The {@link MetricValue} being reported by this event.
     */
    public SystemMetricEvent(MetricValue value) {
        this.value = value;
    }

    /**
     * Returns the {@link MetricValue} being reported by this event.
     *
     * @return
     */
    public MetricValue getValue() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SystemMetricEvent) {
            SystemMetricEvent that = (SystemMetricEvent) obj;
            return Objects.equals(this.value, that.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
