package com.elastisys.autoscaler.systemhistorians.influxdb;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.systemhistorians.influxdb.InfluxdbReporter;
import com.elastisys.autoscaler.systemhistorians.influxdb.inserter.InfluxdbInserter;
import com.elastisys.autoscaler.systemhistorians.influxdb.inserter.InfluxdbInserterException;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link InfluxdbReporter} on a mocked {@link InfluxdbInserter}.
 */
public class TestInfluxdbReporter {
    private static final Logger LOG = LoggerFactory.getLogger(TestInfluxdbReporter.class);

    /**
     * The max batch size to use for the {@link InfluxdbReporter} when sending
     * data points.
     */
    private static final int MAX_BATCH_SIZE = 3;

    /**
     * The mocked {@link InfluxdbInserter} that the {@link InfluxdbReporter}
     * delegates writes to.
     */
    private final InfluxdbInserter mockedInserter = mock(InfluxdbInserter.class);

    /**
     * The send queue of data points that the {@link InfluxdbReporter} reads
     * values from.
     */
    private ConcurrentLinkedQueue<SoftReference<MetricValue>> unreportedDatapoints;

    /** Object under test. */
    private InfluxdbReporter reporter;

    @Before
    public void beforeTestMethod() {
        this.unreportedDatapoints = new ConcurrentLinkedQueue<>();

        this.reporter = new InfluxdbReporter(LOG, this.mockedInserter, this.unreportedDatapoints, MAX_BATCH_SIZE);
    }

    /**
     * When the send queue is empty, the {@link InfluxdbReporter} should not
     * take any action.
     */
    @Test
    public void onEmptySendQueue() {
        this.reporter.run();
        verifyZeroInteractions(this.mockedInserter);
    }

    /**
     * Sent {@link MetricValue}s are to be popped off the send queue.
     */
    @Test
    public void onNonEmptySendQueue() {
        // push values onto send queue
        MetricValue point1 = metricValue(1, UtcTime.parse("2016-01-01T12:00:00.000Z"));
        this.unreportedDatapoints.add(ref(point1));

        assertThat(this.unreportedDatapoints.size(), is(1));

        this.reporter.run();

        // verify that a call was made to the inserter
        verify(this.mockedInserter).insert(asList(point1));

        // verify that the data point was popped off send queue
        assertThat(this.unreportedDatapoints.size(), is(0));
    }

    /**
     * When the send queue exceeds the batch size, the datapoints should be sent
     * in several batches.
     */
    @Test
    public void sendQueueExceedsBatchSize() {
        InOrder callOrder = inOrder(this.mockedInserter);

        // send queue size exceeds MAX_BATCH_SIZE (3)
        MetricValue point1 = metricValue(1, UtcTime.parse("2016-01-01T12:00:00.000Z"));
        MetricValue point2 = metricValue(2, UtcTime.parse("2016-01-01T12:00:01.000Z"));
        MetricValue point3 = metricValue(3, UtcTime.parse("2016-01-01T12:00:02.000Z"));
        MetricValue point4 = metricValue(4, UtcTime.parse("2016-01-01T12:00:03.000Z"));
        MetricValue point5 = metricValue(5, UtcTime.parse("2016-01-01T12:00:04.000Z"));
        this.unreportedDatapoints.addAll(asList(ref(point1), ref(point2), ref(point3), ref(point4), ref(point5)));

        assertThat(this.unreportedDatapoints.size(), is(5));

        this.reporter.run();

        // verify sending of first batch
        callOrder.verify(this.mockedInserter).insert(asList(point1, point2, point3));
        // verify sending of second batch
        callOrder.verify(this.mockedInserter).insert(asList(point4, point5));

        // verify that the data points were popped off send queue
        assertThat(this.unreportedDatapoints.size(), is(0));
    }

    /**
     * On a failure to write data points, they should be kept in the send queue
     * (for retrying later).
     */
    @Test
    public void onSendFailure() {
        // push values onto send queue
        MetricValue point1 = metricValue(1, UtcTime.parse("2016-01-01T12:00:00.000Z"));
        MetricValue point2 = metricValue(2, UtcTime.parse("2016-01-01T12:00:01.000Z"));
        this.unreportedDatapoints.addAll(asList(ref(point1), ref(point2)));
        assertThat(this.unreportedDatapoints.size(), is(2));

        // inserter will fail
        doThrow(new InfluxdbInserterException("connection refused")).when(this.mockedInserter)
                .insert(asList(point1, point2));

        assertThat(this.reporter.getLastError().isPresent(), is(false));
        // reporting should fail
        this.reporter.run();
        assertThat(this.reporter.getLastError().isPresent(), is(true));

        // verify that a call was made to the inserter
        verify(this.mockedInserter).insert(asList(point1, point2));

        // verify that the data points are kept around in send queue
        assertThat(this.unreportedDatapoints.size(), is(2));
    }

    /**
     * When the reporting fails part-way through (batch 1 succeeds, but batch 2
     * fails), the first (successful) batch should still be removed from send
     * queue while the second (failed) should be kept.
     */
    @Test
    public void onPartialFailure() {
        InOrder callOrder = inOrder(this.mockedInserter);

        // send queue size exceeds MAX_BATCH_SIZE (3)
        MetricValue point1 = metricValue(1, UtcTime.parse("2016-01-01T12:00:00.000Z"));
        MetricValue point2 = metricValue(2, UtcTime.parse("2016-01-01T12:00:01.000Z"));
        MetricValue point3 = metricValue(3, UtcTime.parse("2016-01-01T12:00:02.000Z"));
        MetricValue point4 = metricValue(4, UtcTime.parse("2016-01-01T12:00:03.000Z"));
        MetricValue point5 = metricValue(5, UtcTime.parse("2016-01-01T12:00:04.000Z"));
        SoftReference<MetricValue> pointRef1 = ref(point1);
        SoftReference<MetricValue> pointRef2 = ref(point2);
        SoftReference<MetricValue> pointRef3 = ref(point3);
        SoftReference<MetricValue> pointRef4 = ref(point4);
        SoftReference<MetricValue> pointRef5 = ref(point5);
        this.unreportedDatapoints.addAll(asList(pointRef1, pointRef2, pointRef3, pointRef4, pointRef5));
        assertThat(this.unreportedDatapoints.size(), is(5));

        // inserter will complete first batch but fail on the second
        doNothing().when(this.mockedInserter).insert(asList(point1, point2, point3));
        doThrow(new InfluxdbInserterException("connection refused")).when(this.mockedInserter)
                .insert(asList(point4, point5));

        this.reporter.run();

        // verify sending of first batch
        callOrder.verify(this.mockedInserter).insert(asList(point1, point2, point3));
        // verify attempt to send second batch
        callOrder.verify(this.mockedInserter).insert(asList(point4, point5));

        // verify that only the first batch was popped off send queue
        assertThat(this.unreportedDatapoints.size(), is(2));
        assertTrue(this.unreportedDatapoints.containsAll(asList(pointRef4, pointRef5)));
    }

    private MetricValue metricValue(int value, DateTime time) {
        return new MetricValue("metric", value, time);
    }

    private SoftReference<MetricValue> ref(MetricValue metricValue) {
        return new SoftReference<MetricValue>(metricValue);
    }

}
