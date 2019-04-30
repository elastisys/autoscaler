package com.elastisys.autoscaler.core.monitoring.metricstreamer.commons;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.isA;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.metronome.api.MetronomeEvent;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.impl.EmptyResultSet;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.impl.SinglePageResultSet;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Verifies the behavior of the {@link MetricStreamingLoop}.
 * <p/>
 * The test is carried out by injecting a set of mocked {@link MetricStream}s
 * into the {@link MetricStreamingLoop}. The test then checks that the
 * {@link MetricStreamingLoop} makes proper calls to the {@link MetricStream}s
 * and handles retrieved values according to its contract.
 */
public class TestMetricStreamingLoop {

    private final Logger logger = LoggerFactory.getLogger(TestMetricStreamingLoop.class);
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final EventBus eventBusMock = mock(EventBus.class);

    /** Object under test. */
    private MetricStreamingLoop loop;

    @Before
    public void onSetup() {
        FrozenTime.setFixed(UtcTime.parse("2017-01-01T12:00:00.000Z"));
    }

    /**
     * Without {@link MetricStream}s, running the {@link MetricStreamingLoop} is
     * essentially a no-op.
     */
    @Test
    public void runWithoutMetricStreams() {
        this.loop = new MetricStreamingLoop(this.logger, this.executor, this.eventBusMock, Collections.emptyList());
        this.loop.run();

        verifyZeroInteractions(this.eventBusMock);
    }

    /**
     * On the first query to a {@link MetricStream}, the query start interval
     * should be set to a configurable lookback.
     */
    @Test
    public void initialQuery() {
        MetricStream stream = mockedStream("http.req.count");
        prepareResponse(stream, new EmptyResultSet());

        TimeInterval firstQueryLookback = new TimeInterval(5L, TimeUnit.MINUTES);
        this.loop = new MetricStreamingLoop(this.logger, this.executor, this.eventBusMock, asList(stream),
                firstQueryLookback);
        this.loop.run();

        // verify query was made to metric stream with 5 min lookback
        Interval expectedInterval = new Interval(minutesAgo(5), FrozenTime.now());
        verify(stream).query(expectedInterval, new QueryOptions());

        verifyZeroInteractions(this.eventBusMock);
    }

    /**
     * When no firstQueryLookback is specified, a default should be used.
     */
    @Test
    public void initialQueryWithDefaultLookback() {
        MetricStream stream = mockedStream("http.req.count");
        prepareResponse(stream, new EmptyResultSet());

        this.loop = new MetricStreamingLoop(this.logger, this.executor, this.eventBusMock, asList(stream));
        this.loop.run();

        // verify query was made to metric stream with default lookback on first
        // query
        Interval expectedInterval = new Interval(
                FrozenTime.now().minus(MetricStreamingLoop.DEFAULT_FIRST_QUERY_LOOKBACK.getMillis()), FrozenTime.now());
        verify(stream).query(expectedInterval, new QueryOptions());

        verifyZeroInteractions(this.eventBusMock);
    }

    /**
     * The query interval start for a given {@link MetricStream} should be set
     * to only include new values.
     */
    @Test
    public void queryIntervalExcludesObservedValues() {
        MetricStream stream = mockedStream("http.req.count");

        TimeInterval firstQueryLookback = new TimeInterval(5L, TimeUnit.MINUTES);
        this.loop = new MetricStreamingLoop(this.logger, this.executor, this.eventBusMock, asList(stream),
                firstQueryLookback);

        prepareResponse(stream, result(value(stream, 1.0, secondsAgo(30))));
        this.loop.run();
        // verify query was made to metric stream with lookback as start
        Interval expectedInterval = new Interval(minutesAgo(5), FrozenTime.now());
        verify(stream).query(expectedInterval, new QueryOptions());

        // now streaming loop has observed a value 30 seconds ago so next query
        // should start from there
        prepareResponse(stream, result());
        this.loop.run();
        expectedInterval = new Interval(secondsAgo(30), FrozenTime.now());
        verify(stream).query(expectedInterval, new QueryOptions());
    }

    /**
     * Query interval start should remain unchanged until a new value has been
     * observed on stream.
     */
    @Test
    public void queryIntervalStartUnchangedUntilNewObservation() {
        MetricStream stream = mockedStream("http.req.count");

        TimeInterval firstQueryLookback = new TimeInterval(5L, TimeUnit.MINUTES);
        this.loop = new MetricStreamingLoop(this.logger, this.executor, this.eventBusMock, asList(stream),
                firstQueryLookback);

        prepareResponse(stream, result());
        this.loop.run();
        // verify query was made to metric stream with lookback as start
        DateTime expectedQueryStart = minutesAgo(5);
        Interval expectedInterval = new Interval(expectedQueryStart, FrozenTime.now());
        verify(stream).query(expectedInterval, new QueryOptions());

        // advance time ...
        FrozenTime.tick(60);

        // same query start should still be used
        prepareResponse(stream, result(value(stream, 1.0, secondsAgo(10))));
        this.loop.run();
        expectedInterval = new Interval(expectedQueryStart, FrozenTime.now());
        verify(stream).query(expectedInterval, new QueryOptions());

        // a value has been observed, so next query start should be advanced
        FrozenTime.tick(60);
        this.loop.run();
        // query interval should start on last observation
        expectedInterval = new Interval(secondsAgo(60 + 10), FrozenTime.now());
        verify(stream).query(expectedInterval, new QueryOptions());
    }

    /**
     * Observed {@link MetricValue} are to be delivered on the {@link EventBus}.
     */
    @Test
    public void deliverMetricValuesOnEventBus() {
        MetricStream stream = mockedStream("http.req.count");

        this.loop = new MetricStreamingLoop(this.logger, this.executor, this.eventBusMock, asList(stream));

        MetricValue datapoint1 = value(stream, 1.0, secondsAgo(40));
        MetricValue datapoint2 = value(stream, 2.0, secondsAgo(20));
        prepareResponse(stream, result(datapoint1, datapoint2));
        this.loop.run();

        // verify that the values were reported onto the event bus
        verify(this.eventBusMock).post(new MetricStreamMessage(stream.getId(), asList(datapoint1, datapoint2)));
    }

    /**
     * Even if the {@link MetricStream} delivers values out of order, values
     * should be delivered to the {@link EventBus} in chronological order.
     */
    @Test
    public void deliverMetricValuesInOrder() {
        MetricStream stream = mockedStream("http.req.count");

        this.loop = new MetricStreamingLoop(this.logger, this.executor, this.eventBusMock, asList(stream));

        MetricValue datapoint1 = value(stream, 1.0, secondsAgo(45));
        MetricValue datapoint2 = value(stream, 2.0, secondsAgo(30));
        MetricValue datapoint3 = value(stream, 2.0, secondsAgo(15));
        // metric stream delivers in wrong order (youngest datapoint first)
        prepareResponse(stream, result(datapoint3, datapoint1, datapoint2));
        this.loop.run();

        // verify that the values were reported onto the event bus in
        // chronological order
        verify(this.eventBusMock)
                .post(new MetricStreamMessage(stream.getId(), asList(datapoint1, datapoint2, datapoint3)));
    }

    /**
     * The {@link MetricStreamingLoop} must only deliver datapoints that are
     * newer than ones already delivered. So if a {@link MetricStream} reports a
     * metric value that arrives late, that value must be suppressed from
     * delivery.
     */
    @Test
    public void filterOutLateArrivals() {
        MetricStream stream = mockedStream("http.req.count");

        this.loop = new MetricStreamingLoop(this.logger, this.executor, this.eventBusMock, asList(stream));

        MetricValue datapoint1 = value(stream, 1.0, secondsAgo(30));
        prepareResponse(stream, result(datapoint1));
        this.loop.run();
        verify(this.eventBusMock).post(new MetricStreamMessage(stream.getId(), asList(datapoint1)));

        FrozenTime.tick(60);

        // datapoint is late
        MetricValue lateDatapoint = value(stream, 1.0, datapoint1.getTime().minusSeconds(1));
        MetricValue datapoint2 = value(stream, 1.0, secondsAgo(30));
        prepareResponse(stream, result(lateDatapoint, datapoint2));
        this.loop.run();
        // verify that late datapoint was filtered out
        verify(this.eventBusMock).post(new MetricStreamMessage(stream.getId(), asList(datapoint2)));

    }

    /**
     * When multiple streams are managed, the {@link MetricStreamMessage} must
     * clearly indicate from which {@link MetricStream} a certain collection of
     * {@link MetricValue}s originates.
     */
    @Test
    public void distinguishMultipleStreams() {
        MetricStream stream1 = mockedStream("http.req.count");
        MetricStream stream2 = mockedStream("avg.cpu");

        this.loop = new MetricStreamingLoop(this.logger, this.executor, this.eventBusMock, asList(stream1, stream2));

        MetricValue stream1Point1 = value(stream1, 11.0, secondsAgo(30));
        prepareResponse(stream1, result(stream1Point1));

        MetricValue stream2Point1 = value(stream2, 21.0, secondsAgo(40));
        MetricValue stream2Point2 = value(stream2, 22.0, secondsAgo(20));
        prepareResponse(stream2, result(stream2Point1, stream2Point2));

        this.loop.run();

        // verify that two separate messages are sent on metric stream and that
        // stream identifier is clearly stated to indicate the metric stream
        // source
        verify(this.eventBusMock).post(new MetricStreamMessage(stream1.getId(), asList(stream1Point1)));
        verify(this.eventBusMock).post(new MetricStreamMessage(stream2.getId(), asList(stream2Point1, stream2Point2)));

    }

    /**
     * No messages are to be sent out on {@link EventBus} when no new metric
     * values are available on the streams.
     */
    @Test
    public void streamsWithoutNewValues() {
        MetricStream stream1 = mockedStream("http.req.count");
        MetricStream stream2 = mockedStream("avg.cpu");

        this.loop = new MetricStreamingLoop(this.logger, this.executor, this.eventBusMock, asList(stream1, stream2));

        prepareResponse(stream1, result());
        prepareResponse(stream2, result());

        this.loop.run();

        // no messages are sent out on eventbus when no new metric values were
        // read from streams
        verifyZeroInteractions(this.eventBusMock);
    }

    /**
     * After having fetched a non-empty batch of {@link MetricValue}s, the
     * {@link MetricStreamingLoop} should trigger a new resize iteration (a new
     * round of predictions on the metric values) by posting a
     * {@link MetronomeEvent#RESIZE_ITERATION} over the {@link EventBus}.
     */
    @Test
    public void shouldTriggerNewResizeIterationAfterFetch() {
        MetricStream stream = mockedStream("http.req.count");
        prepareResponse(stream, result(value(stream, 1.0, secondsAgo(30))));

        verifyZeroInteractions(this.eventBusMock);

        this.loop = new MetricStreamingLoop(this.logger, this.executor, this.eventBusMock, asList(stream));
        this.loop.run();

        verify(this.eventBusMock).post(MetronomeEvent.RESIZE_ITERATION);
    }

    /**
     * If no *new* values are fetched (that is, the fetch only got values that
     * have already been delivered), the {@link MetricStreamingLoop} should not
     * trigger a new resize iteration.
     */
    @Test
    public void shouldNotTriggerResizeIterationOnFetchWithAlreadyObservedValues() {
        MetricStream stream = mockedStream("http.req.count");
        MetricValue metricValue1 = value(stream, 1.0, secondsAgo(30));
        prepareResponse(stream, result(metricValue1));

        verifyZeroInteractions(this.eventBusMock);

        this.loop = new MetricStreamingLoop(this.logger, this.executor, this.eventBusMock, asList(stream));

        // a resize iteration should be triggered on first run, since we will
        // deliver a new value
        this.loop.run();
        verify(this.eventBusMock, times(1)).post(MetronomeEvent.RESIZE_ITERATION);

        // this time, no resize iteration should be triggered since no new value
        // is delivered
        prepareResponse(stream, result(metricValue1));
        this.loop.run();
        verify(this.eventBusMock, times(1)).post(MetronomeEvent.RESIZE_ITERATION);
    }

    /**
     * Creates a mock {@link MetricStream} collecting values for a given metric.
     *
     * @param metricName
     * @return
     */
    private MetricStream mockedStream(String metricName) {
        MetricStream stream = mock(MetricStream.class);
        when(stream.getId()).thenReturn(metricName + ".stream");
        when(stream.getMetric()).thenReturn(metricName);
        return stream;
    }

    /**
     * Sets up the next query result that will be returned when a given
     * {@link MetricStream} mock is queried.
     *
     * @param metricStream
     *            A mock {@link MetricStream}.
     * @param result
     * @return
     */
    private MetricStream prepareResponse(MetricStream metricStream, QueryResultSet result) {
        when(metricStream.query(argThat(isA(Interval.class)), argThat(isA(QueryOptions.class)))).thenReturn(result);
        return metricStream;
    }

    /**
     * Returns the current test time minus a given number of seconds.
     *
     * @param seconds
     * @return
     */
    private DateTime secondsAgo(int seconds) {
        return FrozenTime.now().minusSeconds(seconds);
    }

    /**
     * Returns the current test time minus a given number of minutes.
     *
     * @param minutes
     * @return
     */
    private DateTime minutesAgo(int minutes) {
        return FrozenTime.now().minusMinutes(minutes);
    }

    private QueryResultSet result(MetricValue... metricValues) {
        return new SinglePageResultSet(asList(metricValues));
    }

    /**
     * Creates a {@link MetricValue} from a given {@link MetricStream}.
     *
     * @param stream
     * @param value
     * @param timestamp
     * @return
     */
    private MetricValue value(MetricStream stream, double value, DateTime timestamp) {
        return new MetricValue(stream.getMetric(), value, timestamp);
    }
}
