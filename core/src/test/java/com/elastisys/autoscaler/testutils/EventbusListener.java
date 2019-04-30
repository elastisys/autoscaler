package com.elastisys.autoscaler.testutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;

/**
 * Can be registered with an {@link EventBus} to record all events posted on the
 * {@link EventBus}.
 */
public class EventbusListener {
    private static Logger LOG = LoggerFactory.getLogger(EventbusListener.class);

    private final Queue<Object> events = new ConcurrentLinkedQueue<>();

    @Subscriber
    public void onEvent(Object event) {
        LOG.debug("got event: {}", event);
        this.events.add(event);
    }

    /**
     * Return all recorded events thus far of a given type.
     *
     * @param eventType
     * @return
     */
    public <T> List<T> getEventsOfType(Class<T> eventType) {
        List<T> tTypeEvents = new ArrayList<>();
        for (Object event : this.events) {
            if (eventType.isAssignableFrom(event.getClass())) {
                tTypeEvents.add(eventType.cast(event));
            }
        }
        return tTypeEvents;
    }

    /**
     * Total number of currently recorded events (of any type).
     *
     * @return
     */
    public int size() {
        return this.events.size();
    }

    /**
     * Indicates if any events exist are currently recorded by returning
     * <code>true</code>. If no events are currently recorded,
     * <code>false</code> is returned.
     *
     * @return
     */
    public boolean isEmpty() {
        return this.events.isEmpty();
    }

    /**
     * Clears all recorded events.
     */
    public void clear() {
        this.events.clear();
    }
}
