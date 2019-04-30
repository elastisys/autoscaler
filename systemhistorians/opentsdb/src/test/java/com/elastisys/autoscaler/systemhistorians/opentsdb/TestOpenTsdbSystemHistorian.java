package com.elastisys.autoscaler.systemhistorians.opentsdb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.Health;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.autoscaler.systemhistorians.opentsdb.config.OpenTsdbSystemHistorianConfig;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.concurrent.Sleep;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link OpenTsdbSystemHistorian}.
 */
public class TestOpenTsdbSystemHistorian {

    private static final String AUTOSCALER_ID = "autoscaler1";
    private static final UUID AUTOSCALER_UUID = UUID.randomUUID();
    private static final Logger logger = LoggerFactory.getLogger(TestOpenTsdbSystemHistorian.class);
    private ScheduledThreadPoolExecutor executor;
    private static final EventBus eventBus = new SynchronousEventBus(logger);

    /** Object under test. */
    private OpenTsdbSystemHistorian historian;
    private static final TimeInterval PUSH_INTERVAL = TimeInterval.seconds(1);

    /** Socket server used to respond to inserter requests. */
    private FixedReplySocketServer server;
    private Thread serverThread;

    @Before
    public void onSetup() throws Exception {
        this.executor = new ScheduledThreadPoolExecutor(5);
        this.executor.setRemoveOnCancelPolicy(true);

        Optional<String> absent = Optional.empty();
        this.server = new FixedReplySocketServer(absent, 0);
        this.serverThread = new Thread(this.server);
        this.serverThread.start();
        this.server.awaitStartup();

        int serverPort = this.server.getListenPort();
        this.historian = new OpenTsdbSystemHistorian(AUTOSCALER_UUID, AUTOSCALER_ID, logger, this.executor, eventBus);

        OpenTsdbSystemHistorianConfig config = new OpenTsdbSystemHistorianConfig("localhost", serverPort,
                PUSH_INTERVAL);
        this.historian.validate(config);
        this.historian.configure(config);
        this.historian.start();
    }

    @After
    public void onTearDown() throws IOException, InterruptedException {
        this.server.close();
        this.serverThread.join();
        this.historian.stop();
    }

    /**
     * Verifies {@link OpenTsdbSystemHistorian} behavior on success to deliver a
     * data point. That is, when simulated opentsdb endpoint doesn't respond to
     * insert.
     */
    @Test
    public void onSuccess() {
        // prepare server (success) response
        Optional<String> absent = Optional.empty();
        this.server.setResponse(absent);

        // post event for historian to report
        Map<String, String> tags = Maps.of("host", "localhost");
        eventBus.post(new SystemMetricEvent(new MetricValue("metric", 1.0, UtcTime.now(), tags)));

        // wait for delivery to happen (and succeed)
        Sleep.forTime(PUSH_INTERVAL.getSeconds() * 2, TimeUnit.SECONDS);
        assertThat(this.historian.getStatus().getHealth(), is(Health.OK));
        assertThat(this.historian.getDeliveredDataPoints(), is(1));
    }

    /**
     * Verifies {@link OpenTsdbSystemHistorian} behavior on failure to deliver a
     * data point. That is, when simulated opentsdb endpoint responds to insert
     * with an (error) message.
     */
    @Test
    public void onFailure() {
        // prepare server (error) response
        this.server.setResponse(Optional.of("illegal argument: unrecognized metric"));

        // post event for historian to report
        Map<String, String> tags = Maps.of("host", "localhost");
        eventBus.post(new SystemMetricEvent(new MetricValue("metric", 1.0, UtcTime.now(), tags)));

        // wait for delivery to happen (and fail)
        Sleep.forTime(PUSH_INTERVAL.getSeconds() * 2, TimeUnit.SECONDS);
        assertThat(this.historian.getStatus().getHealth(), is(Health.NOT_OK));
        assertThat(this.historian.getDeliveredDataPoints(), is(0));
    }

    /**
     * When flush is called, any buffered {@link SystemMetricEvent}s are to be
     * written to the backend store.
     */
    @Test
    public void flush() {
        // prepare server (success) response
        Optional<String> absent = Optional.empty();
        this.server.setResponse(absent);
        // post event for historian to report
        eventBus.post(new SystemMetricEvent(new MetricValue("metric", 1.0, UtcTime.now())));
        eventBus.post(new SystemMetricEvent(new MetricValue("metric", 2.0, UtcTime.now())));

        // TODO
        assertThat(this.historian.getDeliveredDataPoints(), is(0));

        this.historian.flush();

        assertThat(this.historian.getDeliveredDataPoints(), is(2));
    }

    /**
     * Invoking {@link Service#start()} on an already started {@link Service}
     * should be a no-op.
     */
    @Test
    public void startWhenAlreadyStarted() throws Exception {
        // starting the should schedule one task: metric streaming loop
        assertThat(this.executor.getQueue().size(), is(1));

        // calling start() again should not start any new tasks
        this.historian.start();
        assertThat(this.executor.getQueue().size(), is(1));
    }

    /**
     * Any started tasks are to be stopped whenever {@link Service#stop()} is
     * called.
     */
    @Test
    public void stopShouldCancelStartedTasks() throws Exception {
        assertThat(this.executor.getQueue().size(), is(1));

        // verify that periodical tasks are stopped on stop()
        this.historian.stop();
        assertThat(this.executor.getQueue().size(), is(0));
        // calling stop on an already stopped service is a no-op
        this.historian.stop();
        assertThat(this.executor.getQueue().size(), is(0));
    }

    @Test(expected = IllegalStateException.class)
    public void flushBeforeConfigured() throws IOException {
        OpenTsdbSystemHistorian historian = createSystemHistorian();

        historian.flush();
    }

    @Test(expected = IllegalStateException.class)
    public void flushBeforeStarted() throws IOException {
        OpenTsdbSystemHistorian historian = createSystemHistorian();
        OpenTsdbSystemHistorianConfig config = new OpenTsdbSystemHistorianConfig("localhost", 4242, PUSH_INTERVAL);
        historian.configure(config);

        historian.flush();
    }

    private OpenTsdbSystemHistorian createSystemHistorian() {
        return new OpenTsdbSystemHistorian(AUTOSCALER_UUID, AUTOSCALER_ID, logger, this.executor, eventBus);
    }

}
