package com.elastisys.autoscaler.core.monitoring.metricstreamer.commons;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.impl.SinglePageResultSet;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Verifies that the {@link MetricStreamDriver} correctly starts and stops its
 * {@link MetricStreamingLoop}.
 */
public class TestMetricStreamDriver {

    private static final Logger logger = LoggerFactory.getLogger(TestMetricStreamDriver.class);
    private ScheduledThreadPoolExecutor executor;
    private final EventBus eventBus = new SynchronousEventBus(logger);
    private final TimeInterval pollInterval = new TimeInterval(15L, TimeUnit.SECONDS);
    private final TimeInterval firstQueryLookback = new TimeInterval(1L, TimeUnit.MINUTES);

    private MetricStream mockMetricStream = mockedStream("cpu.usage");

    /** Object under test. */
    private MetricStreamDriver metricStreamDriver;

    @Before
    public void beforeTestMethod() {
        this.executor = new ScheduledThreadPoolExecutor(1);
        this.executor.setRemoveOnCancelPolicy(true);

        this.metricStreamDriver = new MetricStreamDriver(logger, this.executor, this.eventBus,
                Arrays.asList(this.mockMetricStream), this.pollInterval, this.firstQueryLookback);
    }

    /**
     * Verifies that starting the {@link MetricStreamDriver} also starts its
     * {@link MetricStreamingLoop} task.
     */
    @Test
    public void start() {
        assertThat(this.executor.getQueue().size(), is(0));

        this.metricStreamDriver.start();

        assertThat(this.executor.getQueue().size(), is(1));

        // start is idempotent, restarting is a no-op
        this.metricStreamDriver.start();

        assertThat(this.executor.getQueue().size(), is(1));
    }

    /**
     * Verifies that stopping the {@link MetricStreamDriver} also stops its
     * {@link MetricStreamingLoop} task.
     */
    @Test
    public void stop() {
        this.metricStreamDriver.start();
        assertThat(this.executor.getQueue().size(), is(1));

        this.metricStreamDriver.stop();
        assertThat(this.executor.getQueue().size(), is(0));

        // start is idempotent, restarting is a no-op
        this.metricStreamDriver.stop();
        assertThat(this.executor.getQueue().size(), is(0));
    }

    /**
     * When {@link MetricStreamDriver#fetch()} is called, it should fetch
     * metrics for all {@link MetricStream}s.
     */
    @Test
    public void fetch() {
        this.metricStreamDriver.start();

        verifyZeroInteractions(this.mockMetricStream);

        // when called, metric stream will return a single metric value
        prepareResponse(this.mockMetricStream, result(value(this.mockMetricStream, 1.0, UtcTime.now())));

        this.metricStreamDriver.fetch();

        // verify that fetch actually tried to collect metrics for the
        // MetricStream
        verify(this.mockMetricStream).query(argThat(instanceOf(Interval.class)),
                argThat(instanceOf(QueryOptions.class)));
    }

    /**
     * One cannot call {@link MetricStreamDriver#fetch()} before having started
     * it.
     */
    @Test(expected = IllegalStateException.class)
    public void fetchOnStoppedMetricStreamDriver() {
        this.metricStreamDriver.fetch();
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
