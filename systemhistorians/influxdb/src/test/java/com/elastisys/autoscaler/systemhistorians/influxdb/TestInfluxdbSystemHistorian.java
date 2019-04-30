package com.elastisys.autoscaler.systemhistorians.influxdb;

import static com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetric.CLOUDPOOL_SIZE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.autoscaler.AutoScalerMetadata;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetric;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.autoscaler.systemhistorians.influxdb.config.InfluxdbSystemHistorianConfig;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link InfluxdbSystemHistorian}.
 */
public class TestInfluxdbSystemHistorian {
    private static final String AUTOSCALER_ID = "autoscaler1";
    private static final UUID AUTOSCALER_UUID = UUID.randomUUID();
    private static final Logger logger = LoggerFactory.getLogger(TestInfluxdbSystemHistorian.class);
    private ScheduledThreadPoolExecutor executor;
    private static final EventBus eventBus = new SynchronousEventBus(logger);

    /** Object under test. */
    private InfluxdbSystemHistorian historian;

    @Before
    public void beforeTestMethod() {
        this.executor = new ScheduledThreadPoolExecutor(1);
        this.executor.setRemoveOnCancelPolicy(true);

        this.historian = new InfluxdbSystemHistorian(AUTOSCALER_UUID, AUTOSCALER_ID, logger, this.executor, eventBus);
        assertThat(this.historian.getConfiguration(), is(nullValue()));
        assertThat(this.historian.getStatus().getState(), is(State.STOPPED));
    }

    @Test
    public void configure() {
        assertThat(this.historian.getConfiguration(), is(nullValue()));
        assertThat(this.historian.getStatus().getState(), is(State.STOPPED));

        this.historian.validate(validConfig());
        this.historian.configure(validConfig());

        assertThat(this.historian.getConfiguration(), is(validConfig()));
        assertThat(this.historian.getStatus().getState(), is(State.STOPPED));
    }

    /**
     * Validation should fail with a {@link IllegalArgumentException} when the
     * configuration is illegal in some way.
     */
    @Test(expected = IllegalArgumentException.class)
    public void configureWithIllegalConfig() {
        this.historian.validate(invalidConfig());
    }

    /**
     * When started, the {@link InfluxdbSystemHistorian} should listen for
     * {@link SystemMetricEvent}s and push them onto its send queue. While
     * stopped, any {@link SystemMetricEvent}s are to be ignored.
     */
    @Test
    public void listenForSystemMetrics() {
        assertThat(this.historian.sendQueue().size(), is(0));
        this.historian.configure(validConfig());

        // not started: should not register metric
        eventBus.post(metric(CLOUDPOOL_SIZE, 0));
        assertThat(this.historian.sendQueue().size(), is(0));

        this.historian.start();

        eventBus.post(metric(CLOUDPOOL_SIZE, 1));
        assertThat(this.historian.sendQueue().size(), is(1));
        eventBus.post(metric(CLOUDPOOL_SIZE, 2));
        assertThat(this.historian.sendQueue().size(), is(2));
    }

    /**
     * Verify that the system historian places discriminating meta data tags on
     * received metrics to help distinguish data reported by different
     * autoscaler instances.
     */
    @Test
    public void qualifyMetricValues() {
        assertThat(this.historian.sendQueue().size(), is(0));
        this.historian.configure(validConfig());
        this.historian.start();

        eventBus.post(metric(CLOUDPOOL_SIZE, 1));
        assertThat(this.historian.sendQueue().size(), is(1));

        // check that autoscaler id is added as a tag
        List<SoftReference<MetricValue>> queue = new ArrayList<>(this.historian.sendQueue());
        assertTrue(queue.get(0).get().getTags().containsKey(AutoScalerMetadata.AUTOSCALER_ID_TAG));
        assertTrue(queue.get(0).get().getTags().containsKey(AutoScalerMetadata.AUTOSCALER_UUID_TAG));
    }

    /**
     * When started, the {@link InfluxdbSystemHistorian} should start its
     * reporting loop.
     */
    @Test
    public void start() {
        // reporting loop not started
        assertThat(this.executor.getQueue().size(), is(0));
        this.historian.configure(validConfig());

        this.historian.start();

        // reporting loop started
        assertThat(this.executor.getQueue().size(), is(1));
    }

    /**
     * When stopped, the {@link InfluxdbSystemHistorian} should stop its
     * reporting loop.
     */
    @Test
    public void stop() {
        // reporting loop stopped
        assertThat(this.executor.getQueue().size(), is(0));
        this.historian.configure(validConfig());

        this.historian.start();

        // reporting loop started
        assertThat(this.executor.getQueue().size(), is(1));

        this.historian.stop();
        // reporting loop stopped
        assertThat(this.executor.getQueue().size(), is(0));
    }

    /**
     * Re-configuring a stopped {@link InfluxdbSystemHistorian} should leave it
     * in a stopped state.
     */
    @Test
    public void reconfigureWhenStopped() {
        this.historian.configure(validConfig());

        assertThat(this.historian.getConfiguration(), is(validConfig()));
        assertThat(this.historian.getStatus().getState(), is(State.STOPPED));

        this.historian.configure(validConfig2());
        assertThat(this.historian.getConfiguration(), is(validConfig2()));
        assertThat(this.historian.getStatus().getState(), is(State.STOPPED));
    }

    /**
     * Re-configuring a started {@link InfluxdbSystemHistorian} should leave it
     * in a started state.
     */
    @Test
    public void reconfigureWhenStarted() {
        this.historian.configure(validConfig());
        this.historian.start();

        assertThat(this.historian.getConfiguration(), is(validConfig()));
        assertThat(this.historian.getStatus().getState(), is(State.STARTED));

        this.historian.configure(validConfig2());
        assertThat(this.historian.getConfiguration(), is(validConfig2()));
        assertThat(this.historian.getStatus().getState(), is(State.STARTED));
    }

    @Test(expected = IllegalStateException.class)
    public void flushBeforeConfigured() throws IOException {
        this.historian.flush();
    }

    @Test(expected = IllegalStateException.class)
    public void flushBeforeStarted() throws IOException {
        this.historian.configure(validConfig());
        this.historian.flush();
    }

    private InfluxdbSystemHistorianConfig validConfig() {
        return new InfluxdbSystemHistorianConfig("host", 8086, "mydb", null, null, null);
    }

    private InfluxdbSystemHistorianConfig validConfig2() {
        return new InfluxdbSystemHistorianConfig("host2", 8888, "mydb2", null, null, null);
    }

    private InfluxdbSystemHistorianConfig invalidConfig() {
        // missing databas
        String database = null;
        return new InfluxdbSystemHistorianConfig("host", 8086, database, null, null, null);
    }

    private SystemMetricEvent metric(SystemMetric metric, int value) {
        return new SystemMetricEvent(new MetricValue(metric.getMetricName(), 1, UtcTime.now()));
    }

}
