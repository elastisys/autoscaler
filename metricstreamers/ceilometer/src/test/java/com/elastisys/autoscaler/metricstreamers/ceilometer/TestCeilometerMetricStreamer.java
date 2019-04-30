package com.elastisys.autoscaler.metricstreamers.ceilometer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerFunction;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerMetricStreamerConfig;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.Downsampling;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.openstack.AuthConfig;
import com.elastisys.scale.commons.openstack.AuthV2Credentials;

/**
 * Exercises the basic operations (configure/start/stop) of the
 * {@link CeilometerMetricStreamer}.
 */
public class TestCeilometerMetricStreamer {
    private static final Logger LOG = LoggerFactory.getLogger(TestCeilometerMetricStreamer.class);
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static final EventBus eventBus = new SynchronousEventBus(LOG);

    private static final AuthConfig AUTH = new AuthConfig("http://keystone.host.com:5000/v2.0",
            new AuthV2Credentials("tenant", "user", "password"), null);
    private static final String REGION = "RegionOne";
    private static final String METER = "network.services.lb.total.connections.rate";

    private static final String resourceId = null;
    private static final Downsampling downsampling = new Downsampling(CeilometerFunction.Average,
            TimeInterval.seconds(60));
    private static final Boolean rateConversion = false;
    private static final TimeInterval dataSettlingTime = null;
    private static final TimeInterval queryChunkSize = null;

    /** Object under test. */
    private CeilometerMetricStreamer metricStreamer;

    @Before
    public void beforeTestMethod() {
        this.metricStreamer = new CeilometerMetricStreamer(LOG, executor, eventBus);
    }

    /**
     * Make sure configuring produces the right {@link MetricStream}s.
     */
    @Test
    public void configure() throws Exception {
        assertThat(this.metricStreamer.getConfiguration(), is(nullValue()));
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));

        CeilometerMetricStreamerConfig config = config(minimalStreamDef(METER));
        this.metricStreamer.configure(config);
        assertThat(this.metricStreamer.getConfiguration(), is(config));
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));

        // check metric streams
        assertThat(this.metricStreamer.getMetricStreams().size(), is(1));
    }

    @Test
    public void reconfigure() throws Exception {
        CeilometerMetricStreamerConfig config = config(minimalStreamDef(METER));
        this.metricStreamer.configure(config);
        assertThat(this.metricStreamer.getConfiguration(), is(config));
        assertThat(this.metricStreamer.getMetricStreams().size(), is(1));

        CeilometerMetricStreamerConfig newConfig = config(minimalStreamDef("cpu.system.stream"),
                minimalStreamDef("cpu.user.stream"));
        this.metricStreamer.configure(newConfig);
        assertThat(this.metricStreamer.getConfiguration(), is(newConfig));
        assertThat(this.metricStreamer.getMetricStreams().size(), is(2));
    }

    @Test
    public void startAndStop() throws Exception {
        CeilometerMetricStreamerConfig config = config(minimalStreamDef(METER));
        this.metricStreamer.configure(config);
        assertThat(this.metricStreamer.getConfiguration(), is(config));
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));

        // start
        this.metricStreamer.start();
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STARTED));

        // stop
        this.metricStreamer.stop();
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));
        // stop should be idempotent
        this.metricStreamer.stop();

        // restart
        this.metricStreamer.start();
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STARTED));
        // start should be idempotent
        this.metricStreamer.start();
    }

    @Test(expected = IllegalStateException.class)
    public void startBeforeConfigured() {
        this.metricStreamer.start();
    }

    @Test(expected = IllegalStateException.class)
    public void getMetricStreamsBeforeConfigured() {
        this.metricStreamer.getMetricStreams();
    }

    @Test(expected = IllegalStateException.class)
    public void getMetricStreamBeforeConfigured() {
        this.metricStreamer.getMetricStream(METER);
    }

    @Test(expected = IllegalStateException.class)
    public void fetchBeforeConfigured() {
        this.metricStreamer.fetch();
    }

    @Test(expected = IllegalStateException.class)
    public void fetchBeforeStarted() {
        CeilometerMetricStreamerConfig config = config(minimalStreamDef(METER));
        this.metricStreamer.configure(config);

        this.metricStreamer.fetch();
    }

    private CeilometerMetricStreamerConfig config(CeilometerMetricStreamDefinition... metricStreams) {
        TimeInterval pollInterval = new TimeInterval(10L, TimeUnit.SECONDS);
        return new CeilometerMetricStreamerConfig(AUTH, REGION, pollInterval, Arrays.asList(metricStreams));
    }

    private CeilometerMetricStreamDefinition minimalStreamDef(String streamId) {
        return new CeilometerMetricStreamDefinition(streamId, streamId, resourceId, downsampling, rateConversion,
                dataSettlingTime, queryChunkSize);
    }
}
