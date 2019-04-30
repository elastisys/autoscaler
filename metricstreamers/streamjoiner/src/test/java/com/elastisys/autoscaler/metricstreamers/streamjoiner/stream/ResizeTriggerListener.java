package com.elastisys.autoscaler.metricstreamers.streamjoiner.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.metronome.api.MetronomeEvent;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;

/**
 * Can be registered with an {@link EventBus} to record all
 * {@link MetronomeEvent#RESIZE_ITERATION} trigger events posted on the
 * {@link EventBus}.
 */
public class ResizeTriggerListener {
    private static Logger LOG = LoggerFactory.getLogger(ResizeTriggerListener.class);

    private final Queue<MetronomeEvent> resizeTriggers = new ConcurrentLinkedQueue<>();

    @Subscriber
    public void onAlert(MetronomeEvent event) {
        if (event == MetronomeEvent.RESIZE_ITERATION) {
            LOG.debug("resize trigger on event bus: {}", event);
            this.resizeTriggers.add(event);
        }
    }

    /**
     * Return all recorded resize trigger events thus far.
     *
     * @return
     */
    public List<MetronomeEvent> getResizeTriggers() {
        return new ArrayList<>(this.resizeTriggers);
    }

    /**
     * Total number of currently recorded resize triggers.
     *
     * @return
     */
    public int size() {
        return this.resizeTriggers.size();
    }

    /**
     * Indicates if any events exist are currently recorded by returning
     * <code>true</code>. If no events are currently recorded,
     * <code>false</code> is returned.
     *
     * @return
     */
    public boolean isEmpty() {
        return this.resizeTriggers.isEmpty();
    }

    /**
     * Clears all recorded events.
     */
    public void clear() {
        this.resizeTriggers.clear();
    }
}
