package com.elastisys.autoscaler.core.monitoring.streammonitor;

import static com.elastisys.autoscaler.core.api.types.ServiceStatus.State.STARTED;
import static com.elastisys.autoscaler.core.api.types.ServiceStatus.State.STOPPED;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamMonitorConfig;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Exercises and verifies the operational behavior of the
 * {@link MetricStreamMonitor}.
 */
@SuppressWarnings({ "rawtypes" })
public class TestMetricStreamMonitorOperation {
    private final Logger logger = LoggerFactory.getLogger(TestMetricStreamMonitorOperation.class);
    private final EventBus eventBus = new SynchronousEventBus(this.logger);
    private ScheduledThreadPoolExecutor executor;

    private final MonitoringSubsystem fakeMonitoringSubsystem = MetricStreamMonitorUtils
            .setupFakeMetricStreams(Arrays.asList(new DummyMetricStream("http.rate")));

    /** Object under test. */
    private MetricStreamMonitor streamMonitor;

    @Before
    public void onSetup() {
        this.executor = new ScheduledThreadPoolExecutor(2);
        this.executor.setRemoveOnCancelPolicy(true);

        this.streamMonitor = new MetricStreamMonitor(this.logger, this.eventBus, this.executor,
                this.fakeMonitoringSubsystem);
    }

    @Test
    public void testStartAndStop() {
        configure(config1());
        assertThat(this.streamMonitor.getStatus().getState(), is(STOPPED));
        start();
        assertThat(this.streamMonitor.getStatus().getState(), is(STARTED));
        stop();
        assertThat(this.streamMonitor.getStatus().getState(), is(STOPPED));
    }

    /**
     * Invoking {@link Service#start()} on an already started {@link Service}
     * should be a no-op.
     */
    @Test
    public void startWhenAlreadyStarted() throws Exception {
        assertThat(this.executor.getTaskCount(), is(0L));

        TimeInterval checkInterval = new TimeInterval(1L, TimeUnit.MINUTES);
        TimeInterval maxTolerableInactivity = new TimeInterval(30L, TimeUnit.MINUTES);
        MetricStreamMonitorConfig config = new MetricStreamMonitorConfig(checkInterval, maxTolerableInactivity);
        this.streamMonitor.configure(config);
        this.streamMonitor.start();

        // starting the should schedule one task: metric streaming loop
        assertThat(this.executor.getQueue().size(), is(1));

        // calling start() again should not start any new tasks
        this.streamMonitor.start();
        assertThat(this.executor.getQueue().size(), is(1));
    }

    /**
     * Any started tasks are to be stopped whenever {@link Service#stop()} is
     * called.
     */
    @Test
    public void stopShouldCancelStartedTasks() throws Exception {
        assertThat(this.executor.getTaskCount(), is(0L));

        TimeInterval checkInterval = new TimeInterval(1L, TimeUnit.MINUTES);
        TimeInterval maxTolerableInactivity = new TimeInterval(30L, TimeUnit.MINUTES);
        MetricStreamMonitorConfig config = new MetricStreamMonitorConfig(checkInterval, maxTolerableInactivity);
        this.streamMonitor.configure(config);
        this.streamMonitor.start();
        assertThat(this.executor.getQueue().size(), is(1));

        // verify that periodical tasks are stopped on stop()
        this.streamMonitor.stop();
        assertThat(this.executor.getQueue().size(), is(0));
        // calling stop on an already stopped service is a no-op
        this.streamMonitor.stop();
        assertThat(this.executor.getQueue().size(), is(0));
    }

    @Test(expected = IllegalStateException.class)
    public void testStartWithoutConfiguration() {
        assertThat(this.streamMonitor.getStatus().getState(), is(STOPPED));
        start();
    }

    @Test
    public void testReconfigure() {
        MetricStreamMonitorConfig config1 = config1();
        MetricStreamMonitorConfig config2 = config2();

        assertThat(this.streamMonitor.getStatus().getState(), is(STOPPED));
        // configure
        configure(config1);
        start();
        assertThat(this.streamMonitor.getConfiguration(), is(config1));
        assertThat(this.streamMonitor.getStatus().getState(), is(STARTED));

        // reconfigure
        configure(config2);
        assertThat(this.streamMonitor.getConfiguration(), is(config2));
        assertThat(this.streamMonitor.getStatus().getState(), is(STARTED));
    }

    /**
     * Re-configures the {@link MetricStreamMonitor} under test with a given
     * config.
     *
     * @param config
     *            The new configuration.
     */
    private void configure(MetricStreamMonitorConfig config) {
        this.streamMonitor.validate(config);
        this.streamMonitor.configure(config);
    }

    /**
     * Creates a {@link MetricStreamMonitorConfig}.
     *
     * @return
     */
    private MetricStreamMonitorConfig config1() {
        TimeInterval checkInterval = new TimeInterval(1L, TimeUnit.MINUTES);
        TimeInterval maxTolerableInactivity = new TimeInterval(3L, TimeUnit.MINUTES);
        return new MetricStreamMonitorConfig(checkInterval, maxTolerableInactivity);
    }

    /**
     * Creates a {@link MetricStreamMonitorConfig}.
     *
     * @return
     */
    private MetricStreamMonitorConfig config2() {
        TimeInterval checkInterval = new TimeInterval(2L, TimeUnit.MINUTES);
        TimeInterval maxTolerableInactivity = new TimeInterval(4L, TimeUnit.MINUTES);
        return new MetricStreamMonitorConfig(checkInterval, maxTolerableInactivity);
    }

    /**
     * Starts the {@link MetricStreamMonitor} under test.
     */
    private void start() {
        this.streamMonitor.start();
    }

    /**
     * Stops the {@link MetricStreamMonitor} under test.
     */
    private void stop() {
        this.streamMonitor.stop();
    }
}
