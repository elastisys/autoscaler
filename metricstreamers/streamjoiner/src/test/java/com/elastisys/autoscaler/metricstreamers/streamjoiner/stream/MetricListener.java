package com.elastisys.autoscaler.metricstreamers.streamjoiner.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;

/**
 * Can be registered with an {@link EventBus} to record all
 * {@link MetricStreamMessage} events posted on the {@link EventBus}.
 */
public class MetricListener {
    private static Logger LOG = LoggerFactory.getLogger(MetricListener.class);

    private final Queue<MetricStreamMessage> metricsMessages = new ConcurrentLinkedQueue<>();

    @Subscriber
    public void onMetricBatch(MetricStreamMessage metricsMessage) {
        LOG.debug("metrics message on event bus: {}", metricsMessage);
        this.metricsMessages.add(metricsMessage);
    }

    /**
     * Return all recorded {@link MetricValue}s thus far of a given metric
     * stream.
     *
     * @param metricStreamId
     * @return
     */
    public List<MetricValue> getMetricsByStreamId(String metricStreamId) {
        List<MetricValue> metricValues = new ArrayList<>();
        for (MetricStreamMessage metricMessage : this.metricsMessages) {
            if (metricMessage.getId().equals(metricStreamId)) {
                metricValues.addAll(metricMessage.getMetricValues());
            }
        }
        return metricValues;
    }

    /**
     * Return the latest recorded {@link MetricValue}s posted on the
     * {@link EventBus} by a given metric stream.
     *
     * @param metricStreamId
     * @return
     */
    public MetricValue getLatestMetricByStreamId(String metricStreamId) {
        List<MetricValue> values = getMetricsByStreamId(metricStreamId);
        return values.get(values.size() - 1);
    }

    /**
     * Total number of currently recorded events (of any type).
     *
     * @return
     */
    public int size() {
        return this.metricsMessages.size();
    }

    /**
     * Indicates if any events exist are currently recorded by returning
     * <code>true</code>. If no events are currently recorded,
     * <code>false</code> is returned.
     *
     * @return
     */
    public boolean isEmpty() {
        return this.metricsMessages.isEmpty();
    }

    /**
     * Clears all recorded events.
     */
    public void clear() {
        this.metricsMessages.clear();
    }
}
