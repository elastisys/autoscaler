package com.elastisys.autoscaler.core.monitoring.metricstreamer.reader;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;

/**
 * A {@link MetricStreamReader} collects {@link MetricValue}s for a certain
 * {@link MetricStream} by listening for {@link MetricStreamMessage}s sent onto
 * an {@link EventBus} that originate from the given {@link MetricStream}.
 * <p/>
 * The {@link #start()} method must be invoked before use. Until it is called
 * the {@link MetricStreamReader} is in a passive state.
 */
public class MetricStreamReader {
    /** The {@link EventBus} that this {@link MetricStreamReader} listens to. */
    private final EventBus eventBus;
    /**
     * The {@link MetricStream} for which this {@link MetricStreamReader} reads
     * {@link MetricValue}s.
     */
    private final MetricStream metricStream;

    /**
     * Queue holding {@link MetricValue}s read but not yet consumed (popped) by
     * the client of this {@link MetricStreamReader}.
     */
    private final Queue<MetricValue> metricValueQueue;
    private boolean started;

    /**
     * Creates a new {@link MetricStreamReader}.
     *
     * @param eventBus
     *            The {@link EventBus} that this {@link MetricStreamReader}
     *            listens to.
     * @param metricStream
     */
    public MetricStreamReader(EventBus eventBus, MetricStream metricStream) {
        checkArgument(eventBus != null, "eventBus cannot be null");
        checkArgument(metricStream != null, "metricStream cannot be null");
        this.eventBus = eventBus;
        this.metricStream = metricStream;
        this.metricValueQueue = new ConcurrentLinkedQueue<>();

        this.started = false;
    }

    /**
     * Returns the {@link EventBus} that this {@link MetricStreamReader} listens
     * to.
     *
     * @return
     */
    public EventBus getEventBus() {
        return this.eventBus;
    }

    /**
     * Returns the {@link MetricStream} that this {@link MetricStreamReader}
     * reads metric values from.
     *
     * @return
     */
    public MetricStream getMetricStream() {
        return this.metricStream;
    }

    /**
     * Starts listening to the {@link EventBus} for values published by the
     * {@link MetricStream}.
     */
    public void start() {
        if (this.started) {
            return;
        }
        this.eventBus.register(this);
        this.started = true;
    }

    /**
     * Starts listening to the {@link EventBus} for {@link MetricStream} values.
     */
    public void stop() {
        if (!this.started) {
            return;
        }
        this.eventBus.unregister(this);
        this.started = false;
    }

    /**
     * Returns <code>true</code> if the {@link MetricStreamReader} has
     * {@link MetricValue}s that have been read but not yet consumed (popped).
     *
     * @return
     */
    public boolean isEmpty() {
        return this.metricValueQueue.isEmpty();
    }

    /**
     * Consumes the oldest {@link MetricValue} from the reader's buffer. A
     * {@link NoSuchElementException} will be thrown if the buffer is empty.
     *
     * @return
     * @throws NoSuchElementException
     *             if the buffer is empty.
     */
    public MetricValue pop() throws NoSuchElementException {
        if (isEmpty()) {
            throw new NoSuchElementException("cannot pop: MetricStreamReader is empty");
        }
        return this.metricValueQueue.remove();
    }

    /**
     * Consumes all {@link MetricValue}s in the reader's buffer. Values are
     * added to the destination in chronological order (oldest first). If the
     * buffer is empty nothing gets written to the destination collection.
     *
     * @param destination
     *            Destination collection.
     */
    public void popTo(Collection<MetricValue> destination) {
        while (!this.metricValueQueue.isEmpty()) {
            destination.add(this.metricValueQueue.remove());
        }
    }

    /**
     * Indicates if this {@link MetricStreamReader} has been started or not.
     * When in a stopped state the {@link MetricStreamReader} will be
     * unregistered from the {@link EventBus} and therefore will not catch new
     * metric values from the {@link MetricStream}.
     *
     * @return
     */
    public boolean isStarted() {
        return this.started == true;
    }

    /**
     * When started, this method will be called whenever metric values are
     * posted to the {@link EventBus}.
     *
     * @param message
     */
    @Subscriber
    public void onMetricStreamMessage(MetricStreamMessage message) {
        if (!isStarted()) {
            // we should be unregistered from event bus when stopped and should
            // therefore never receive these calls. if we nevertheless do, we
            // just ignore
            return;
        }

        if (this.metricStream.getId().equals(message.getId())) {
            this.metricValueQueue.addAll(message.getMetricValues());
        }
    }
}
