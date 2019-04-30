package com.elastisys.autoscaler.metricstreamers.streamjoiner.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;
import com.elastisys.scale.commons.net.alerter.Alert;

/**
 * Can be registered with an {@link EventBus} to record all {@link Alert} events
 * posted on the {@link EventBus}.
 */
public class AlertListener {
    private static Logger LOG = LoggerFactory.getLogger(AlertListener.class);

    private final Queue<Alert> alerts = new ConcurrentLinkedQueue<>();

    @Subscriber
    public void onAlert(Alert metricsMessage) {
        LOG.debug("alert on event bus: {}", metricsMessage);
        this.alerts.add(metricsMessage);
    }

    /**
     * Return all recorded {@link Alert}s thus far.
     *
     * @return
     */
    public List<Alert> getAlerts() {
        return new ArrayList<>(this.alerts);
    }

    /**
     * Total number of currently recorded events (of any type).
     *
     * @return
     */
    public int size() {
        return this.alerts.size();
    }

    /**
     * Indicates if any events exist are currently recorded by returning
     * <code>true</code>. If no events are currently recorded,
     * <code>false</code> is returned.
     *
     * @return
     */
    public boolean isEmpty() {
        return this.alerts.isEmpty();
    }

    /**
     * Clears all recorded events.
     */
    public void clear() {
        this.alerts.clear();
    }
}
