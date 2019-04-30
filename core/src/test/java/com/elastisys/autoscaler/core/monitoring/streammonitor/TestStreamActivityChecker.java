package com.elastisys.autoscaler.core.monitoring.streammonitor;

import static com.elastisys.autoscaler.core.monitoring.streammonitor.IsInactiveStreamAlert.isInactiveStreamAlert;
import static com.elastisys.autoscaler.core.monitoring.streammonitor.IsRecoveredStreamAlert.isRecoveredStreamAlert;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Arrays;
import java.util.List;

import org.joda.time.Duration;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.impl.EmptyResultSet;
import com.elastisys.autoscaler.testutils.EventbusListener;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link StreamActivityChecker}.
 */
public class TestStreamActivityChecker {

    static final Logger logger = LoggerFactory.getLogger(TestStreamActivityChecker.class);

    /**
     * One of the streams that the mocked {@link MetricStreamer} is set up with.
     */
    private static final DummyMetricStream stream1 = new DummyMetricStream("http.request.count");
    /**
     * One of the streams that the mocked {@link MetricStreamer} is set up with.
     */
    private static final DummyMetricStream stream2 = new DummyMetricStream("avg.cpu");

    /**
     * {@link EventBus} onto which {@link StreamActivityChecker} listens for
     * stream values and sends alerts.
     */
    private EventBus eventBus = new SynchronousEventBus(logger);

    /**
     * Listens to the {@link EventBus} during each test and records the events
     * sent.
     */
    private EventbusListener eventBusListener;

    /** Object under test. */
    private StreamActivityChecker streamChecker;

    @Before
    public void onSetup() {
        FrozenTime.setFixed(UtcTime.parse("2017-01-01T12:00:00.000Z"));

        this.eventBusListener = new EventbusListener();
        this.eventBus.register(this.eventBusListener);

        MonitoringSubsystem monitoringSubsystem = MetricStreamMonitorUtils
                .setupFakeMetricStreams(Arrays.asList(stream1));

        this.streamChecker = new StreamActivityChecker(logger, monitoringSubsystem, this.eventBus);
    }

    /**
     * {@link StreamActivityChecker} should unregister from {@link EventBus} on
     * stop.
     */
    @Test
    public void unregisterFromEventBusWhenStopped() {
        EventBus mockedEventBus = mock(EventBus.class);
        this.streamChecker = new StreamActivityChecker(logger, mock(MonitoringSubsystem.class), mockedEventBus);

        verifyZeroInteractions(mockedEventBus);

        // should register with eventbus on start
        this.streamChecker.start();
        verify(mockedEventBus).register(this.streamChecker);

        this.streamChecker.stop();
        verify(mockedEventBus).unregister(this.streamChecker);
    }

    /**
     * {@link StreamActivityChecker} must be stared before use.
     */
    @Test(expected = IllegalStateException.class)
    public void useBeforeStarted() {
        this.streamChecker.alertOnActivityChange(Duration.standardMinutes(30));
    }

    /**
     * Verify that a warning alert is sent out after
     * {@link #maxTolerableInactivity} seconds of inactivity.
     */
    @Test
    public void verifyAlertOnInactivity() {
        this.streamChecker.start();

        // stream has only been inactive for 0 seconds
        this.streamChecker.alertOnActivityChange(Duration.standardMinutes(30));
        assertThat(this.streamChecker.timeOfInactivity(stream1), is(Duration.standardSeconds(0)));
        // no alert sent out (yet)
        assertThat(this.eventBusListener.size(), is(0));

        FrozenTime.tick(10 * 60);
        // stream has only been inactive for 10 minutes
        assertThat(this.streamChecker.timeOfInactivity(stream1), is(Duration.standardMinutes(10)));
        this.streamChecker.alertOnActivityChange(Duration.standardMinutes(30));
        assertThat(this.eventBusListener.size(), is(0));

        FrozenTime.tick(20 * 60);
        // stream has only been inactive for 30:00 minutes
        assertThat(this.streamChecker.timeOfInactivity(stream1), is(Duration.standardMinutes(30)));
        this.streamChecker.alertOnActivityChange(Duration.standardMinutes(30));
        assertThat(this.eventBusListener.size(), is(0));

        FrozenTime.tick(1);
        // stream has now been inactive for 30:01 minutes
        this.streamChecker.alertOnActivityChange(Duration.standardMinutes(30));
        assertThat(this.streamChecker.timeOfInactivity(stream1), is(Duration.standardSeconds(1801)));
        assertThat(this.eventBusListener.size(), is(1));
        List<Alert> alerts = this.eventBusListener.getEventsOfType(Alert.class);
        assertThat(alerts.get(0), isInactiveStreamAlert(stream1, FrozenTime.now()));
    }

    /**
     * When activity is observed on a {@link MetricStream}, its inactivity timer
     * should be reset.
     */
    @Test
    public void metricStreamActivityShouldResetInactivityTimer() {
        this.streamChecker.start();
        this.streamChecker.alertOnActivityChange(Duration.standardMinutes(30));

        FrozenTime.tick(10 * 60);
        assertThat(this.streamChecker.timeOfInactivity(stream1), is(Duration.standardMinutes(10)));

        // send out a value for metric stream, which should reset the inactivity
        // timer
        FrozenTime.tick(10 * 60);
        this.eventBus.post(new MetricStreamMessage(stream1.getId(),
                asList(new MetricValue(stream1.getMetric(), 1.0, FrozenTime.now()))));

        // verify that inactivity timer has been reset
        assertThat(this.streamChecker.timeOfInactivity(stream1), is(Duration.standardMinutes(0)));
    }

    /**
     * After a {@link MetricStream} has been inactive, a recovery alert should
     * be sent when the {@link MetricStream} gives a new sign of life.
     */
    @Test
    public void recoveryAlertWhenInactivityEnds() {
        this.streamChecker.start();
        this.streamChecker.alertOnActivityChange(Duration.standardMinutes(30));

        // stream is now inactive
        FrozenTime.tick(30 * 60 + 1);
        this.streamChecker.alertOnActivityChange(Duration.standardMinutes(30));

        assertThat(this.streamChecker.timeOfInactivity(stream1), is(Duration.standardSeconds(1801)));
        assertThat(this.eventBusListener.size(), is(1));
        List<Alert> alerts = this.eventBusListener.getEventsOfType(Alert.class);
        assertThat(alerts.get(0), isInactiveStreamAlert(stream1, FrozenTime.now()));

        FrozenTime.tick(60);

        //
        // end inactivity by sending a metric value for metric stream
        //
        this.eventBus.post(new MetricStreamMessage(stream1.getId(),
                asList(new MetricValue(stream1.getMetric(), 1.0, FrozenTime.now()))));

        this.eventBusListener.clear();
        assertThat(this.eventBusListener.size(), is(0));

        this.streamChecker.alertOnActivityChange(Duration.standardMinutes(30));
        // verify that a recovery alert was sent
        assertThat(this.eventBusListener.size(), is(1));
        assertThat(this.eventBusListener.getEventsOfType(Alert.class).get(0),
                isRecoveredStreamAlert(stream1, FrozenTime.now()));

    }

    /**
     * {@link StreamActivityChecker} should be capable of tracking multiple
     * {@link MetricStream}s.
     */
    @Test
    public void monitorMultipleMetricStreams() {
        MonitoringSubsystem monitoringSubsystem = MetricStreamMonitorUtils
                .setupFakeMetricStreams(Arrays.asList(stream1, stream2));
        this.streamChecker = new StreamActivityChecker(logger, monitoringSubsystem, this.eventBus);
        this.streamChecker.start();

        assertThat(this.streamChecker.timeOfInactivity(stream1), is(Duration.standardSeconds(0)));
        assertThat(this.streamChecker.timeOfInactivity(stream2), is(Duration.standardSeconds(0)));
        this.streamChecker.alertOnActivityChange(Duration.standardMinutes(30));
        // no alert sent out (yet)
        assertThat(this.eventBusListener.size(), is(0));

        // make sure both streams are seen as inactive
        FrozenTime.tick(30 * 60 + 1);
        assertThat(this.streamChecker.timeOfInactivity(stream1), is(Duration.standardSeconds(1801)));
        assertThat(this.streamChecker.timeOfInactivity(stream2), is(Duration.standardSeconds(1801)));
        this.streamChecker.alertOnActivityChange(Duration.standardMinutes(30));
        assertThat(this.eventBusListener.size(), is(2));
        List<Alert> alerts = this.eventBusListener.getEventsOfType(Alert.class);
        assertThat(alerts.get(0), isInactiveStreamAlert(stream1, FrozenTime.now()));
        assertThat(alerts.get(1), isInactiveStreamAlert(stream2, FrozenTime.now()));

        FrozenTime.tick(60);

        //
        // end inactivity for stream1
        //
        this.eventBus.post(new MetricStreamMessage(stream1.getId(),
                asList(new MetricValue(stream1.getMetric(), 1.0, FrozenTime.now()))));

        this.eventBusListener.clear();
        this.streamChecker.alertOnActivityChange(Duration.standardMinutes(30));
        assertThat(this.eventBusListener.size(), is(2));
        alerts = this.eventBusListener.getEventsOfType(Alert.class);
        assertThat(alerts.get(0), isRecoveredStreamAlert(stream1, FrozenTime.now()));
        assertThat(alerts.get(1), isInactiveStreamAlert(stream2, FrozenTime.now()));

        FrozenTime.tick(60);

        //
        // end inactivity for stream2
        //
        this.eventBus.post(new MetricStreamMessage(stream2.getId(),
                asList(new MetricValue(stream2.getMetric(), 1.0, FrozenTime.now()))));

        this.eventBusListener.clear();
        this.streamChecker.alertOnActivityChange(Duration.standardMinutes(30));
        assertThat(this.eventBusListener.size(), is(1));
        alerts = this.eventBusListener.getEventsOfType(Alert.class);
        assertThat(alerts.get(0), isRecoveredStreamAlert(stream2, FrozenTime.now()));
    }

    /**
     * Dummy {@link MetricStream} implementation used in test.
     */
    private static class DummyMetricStream implements MetricStream {

        private String metric;

        public DummyMetricStream(String metricName) {
            this.metric = metricName;
        }

        @Override
        public String getId() {
            return this.metric + ".stream";
        }

        @Override
        public String getMetric() {
            return this.metric;
        }

        @Override
        public QueryResultSet query(Interval timeInterval, QueryOptions options) {
            return new EmptyResultSet();
        }
    }

}
