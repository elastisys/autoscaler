package com.elastisys.autoscaler.core.monitoring.metricstreamer.reader;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.scale.commons.eventbus.EventBus;

/**
 * Exercises the {@link MetricStreamReader}.
 */
public class TestMetricStreamReader {

    private static final String METRIC = "http.requests";

    private final EventBus eventBus = mock(EventBus.class);
    private final MetricStream metricStream = mock(MetricStream.class);

    /** Object under test. */
    private MetricStreamReader reader = new MetricStreamReader(this.eventBus, this.metricStream);

    /**
     * On creation the {@link MetricStreamReader} should be in a stopped state
     * with an empty metric queue.
     */
    @Test
    public void initialState() {
        assertThat(this.reader.getEventBus(), is(this.eventBus));
        assertThat(this.reader.getMetricStream(), is(this.metricStream));

        assertThat(this.reader.isStarted(), is(false));
        assertThat(this.reader.isEmpty(), is(true));
    }

    /**
     * A {@link NoSuchElementException} should be thrown on an attempt to pop an
     * empty {@link MetricStreamReader}.
     */
    @Test(expected = NoSuchElementException.class)
    public void popWhenReaderIsEmpty() {
        this.reader.pop();
    }

    /**
     * Calling popTo on an empty {@link MetricStreamReader} should not change
     * the destination collection.
     */
    @Test
    public void popToWhenReaderIsEmpty() {
        List<MetricValue> destination = new ArrayList<>();
        assertThat(destination.isEmpty(), is(true));

        this.reader.popTo(destination);

        // should still be empty
        assertThat(destination.isEmpty(), is(true));
    }

    /**
     * When started, the {@link MetricStreamReader} should register on
     * {@link EventBus} to start listening for {@link MetricValue}s.
     */
    @Test
    public void start() {
        this.reader.start();

        verify(this.eventBus).register(this.reader);

        assertThat(this.reader.isStarted(), is(true));
    }

    /**
     * Start should be idempotent. No matter how many times it is called,
     * {@link MetricStreamReader} should only register once with
     * {@link EventBus}.
     */
    @Test
    public void startIsIdemPotent() {
        this.reader.start();
        this.reader.start();
        this.reader.start();

        verify(this.eventBus, times(1)).register(this.reader);
    }

    /**
     * When stopped, the {@link MetricStreamReader} should unregister from the
     * {@link EventBus} to stop listening for {@link MetricValue}s.
     */
    @Test
    public void stop() {
        this.reader.start();
        assertThat(this.reader.isStarted(), is(true));

        verify(this.eventBus, times(0)).unregister(this.reader);

        this.reader.stop();
        assertThat(this.reader.isStarted(), is(false));

        verify(this.eventBus, times(1)).unregister(this.reader);
    }

    /**
     * Should be okay to call stop without being started (should be a no-op).
     */
    @Test
    public void stopWhenAlreadyStopped() {
        assertThat(this.reader.isStarted(), is(false));
        this.reader.stop();
        assertThat(this.reader.isStarted(), is(false));
        this.reader.stop();

        verifyZeroInteractions(this.eventBus);
    }

    /**
     * Received {@link MetricValue}s are to be returned to the client in FIFO
     * order.
     */
    @Test
    public void popInFifoOrder() {
        this.reader.start();
        when(this.metricStream.getId()).thenReturn("stream.id");

        assertThat(this.reader.isEmpty(), is(true));

        this.reader.onMetricStreamMessage(new MetricStreamMessage("stream.id", values(value(1))));
        this.reader.onMetricStreamMessage(new MetricStreamMessage("stream.id", values(value(2), value(3))));
        this.reader.onMetricStreamMessage(new MetricStreamMessage("stream.id", values(value(4), value(5), value(6))));

        assertThat(this.reader.isEmpty(), is(false));
        assertThat(this.reader.pop(), is(value(1)));
        assertThat(this.reader.pop(), is(value(2)));
        assertThat(this.reader.pop(), is(value(3)));
        assertThat(this.reader.pop(), is(value(4)));
        assertThat(this.reader.pop(), is(value(5)));
        assertThat(this.reader.pop(), is(value(6)));
        assertThat(this.reader.isEmpty(), is(true));
    }

    /**
     * Received {@link MetricValue}s are to be returned to the client in FIFO
     * order.
     */
    @Test
    public void popToInFifoOrder() {
        this.reader.start();
        when(this.metricStream.getId()).thenReturn("stream.id");

        assertThat(this.reader.isEmpty(), is(true));

        this.reader.onMetricStreamMessage(new MetricStreamMessage("stream.id", values(value(1))));
        this.reader.onMetricStreamMessage(new MetricStreamMessage("stream.id", values(value(2), value(3))));
        this.reader.onMetricStreamMessage(new MetricStreamMessage("stream.id", values(value(4), value(5), value(6))));

        assertThat(this.reader.isEmpty(), is(false));
        List<MetricValue> destination = new ArrayList<>();
        this.reader.popTo(destination);
        assertThat(destination, is(values(value(1), value(2), value(3), value(4), value(5), value(6))));
        assertThat(this.reader.isEmpty(), is(true));
    }

    /**
     * The {@link MetricStreamReader} should only care about
     * {@link MetricValue}s published for its {@link MetricStream} (with the
     * stream's id).
     */
    @Test
    public void filterOutValuesFromOtherMetricStreams() {
        this.reader.start();
        when(this.metricStream.getId()).thenReturn("stream.id");

        assertThat(this.reader.isEmpty(), is(true));

        this.reader.onMetricStreamMessage(new MetricStreamMessage("stream.id", values(value(1))));
        // should ignore values from other streams
        this.reader.onMetricStreamMessage(new MetricStreamMessage("other.stream.id", values(value(4), value(5))));
        this.reader.onMetricStreamMessage(new MetricStreamMessage("stream.id", values(value(2), value(3))));

        assertThat(this.reader.isEmpty(), is(false));
        assertThat(this.reader.pop(), is(value(1)));
        assertThat(this.reader.pop(), is(value(2)));
        assertThat(this.reader.pop(), is(value(3)));
        assertThat(this.reader.isEmpty(), is(true));
    }

    /**
     * Any calls to onMetricStreamMessage should be ignored when in a stopped
     * state.
     */
    @Test
    public void ignoreMetricValuesWhenStopped() {
        assertThat(this.reader.isStarted(), is(false));
        assertThat(this.reader.isEmpty(), is(true));

        this.reader.onMetricStreamMessage(new MetricStreamMessage("stream.id", values(value(1))));

        assertThat(this.reader.isEmpty(), is(true));
    }

    /**
     * Should fail unless an {@link EventBus} gets passed at construction time.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createWithNullEventBus() {
        new MetricStreamReader(null, this.metricStream);
    }

    /**
     * Should fail unless an {@link EventBus} gets passed at construction time.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createWithNullMetricStream() {
        new MetricStreamReader(this.eventBus, null);
    }

    private static List<MetricValue> values(MetricValue... values) {
        return Arrays.asList(values);
    }

    private static MetricValue value(long value) {
        return new MetricValue(METRIC, value, new DateTime(value));
    }

}
