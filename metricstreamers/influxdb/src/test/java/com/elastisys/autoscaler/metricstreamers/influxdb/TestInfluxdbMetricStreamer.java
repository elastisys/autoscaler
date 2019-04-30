package com.elastisys.autoscaler.metricstreamers.influxdb;

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
import com.elastisys.autoscaler.metricstreamers.influxdb.config.InfluxdbMetricStreamerConfig;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.MetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.Query;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.SecurityConfig;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Exercises the basic operations (configure/start/stop) of the
 * {@link InfluxdbMetricStreamer}.
 */
public class TestInfluxdbMetricStreamer {
    private static final Logger LOG = LoggerFactory.getLogger(TestInfluxdbMetricStreamer.class);
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static final EventBus eventBus = new SynchronousEventBus(LOG);

    /** Object under test. */
    private InfluxdbMetricStreamer metricStreamer;

    @Before
    public void beforeTestMethod() {
        this.metricStreamer = new InfluxdbMetricStreamer(LOG, executor, eventBus);
    }

    /**
     * Make sure configuring produces the right {@link MetricStream}s.
     */
    @Test
    public void configure() throws Exception {
        assertThat(this.metricStreamer.getConfiguration(), is(nullValue()));
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));

        Query query = Query.builder().select("value").from("requests").build();
        InfluxdbMetricStreamerConfig config = config(minimalStreamDef("requests.stream", "db", query));
        this.metricStreamer.configure(config);
        assertThat(this.metricStreamer.getConfiguration(), is(config));
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));

        // check metric streams
        assertThat(this.metricStreamer.getMetricStreams().size(), is(1));
    }

    @Test
    public void reconfigure() throws Exception {
        Query query = Query.builder().select("value").from("requests").build();
        InfluxdbMetricStreamerConfig config = config(minimalStreamDef("requests.stream", "db", query));
        this.metricStreamer.configure(config);
        assertThat(this.metricStreamer.getConfiguration(), is(config));
        assertThat(this.metricStreamer.getMetricStreams().size(), is(1));

        Query query2 = Query.builder().select("system").from("cpu").build();
        Query query3 = Query.builder().select("user").from("cpu").build();
        InfluxdbMetricStreamerConfig newConfig = config(minimalStreamDef("cpu.system.stream", "db", query2),
                minimalStreamDef("cpu.user.stream", "db", query3));
        this.metricStreamer.configure(newConfig);
        assertThat(this.metricStreamer.getConfiguration(), is(newConfig));
        assertThat(this.metricStreamer.getMetricStreams().size(), is(2));
    }

    @Test
    public void startAndStop() throws Exception {
        Query query = Query.builder().select("value").from("requests").build();
        InfluxdbMetricStreamerConfig config = config(minimalStreamDef("requests.stream", "db", query));
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
        this.metricStreamer.getMetricStream("requests.stream");
    }

    @Test(expected = IllegalStateException.class)
    public void fetchBeforeConfigured() {
        this.metricStreamer.fetch();
    }

    @Test(expected = IllegalStateException.class)
    public void fetchBeforeStarted() {
        Query query = Query.builder().select("value").from("requests").build();
        InfluxdbMetricStreamerConfig config = config(minimalStreamDef("requests.stream", "db", query));
        this.metricStreamer.configure(config);

        this.metricStreamer.fetch();
    }

    @Test
    public void fetch() {
        Query query = Query.builder().select("value").from("requests").build();
        InfluxdbMetricStreamerConfig config = config(minimalStreamDef("requests.stream", "db", query));
        this.metricStreamer.configure(config);
        this.metricStreamer.start();

        this.metricStreamer.fetch();
    }

    private InfluxdbMetricStreamerConfig config(MetricStreamDefinition... metricStreams) {
        SecurityConfig security = null;
        TimeInterval pollInterval = new TimeInterval(10L, TimeUnit.SECONDS);
        return new InfluxdbMetricStreamerConfig("opentsdb", 4242, security, pollInterval, Arrays.asList(metricStreams));
    }

    /**
     * Creates an {@link MetricStreamDefinition} set up to query InfluxDB with
     * required parameters only.
     *
     * @param streamId
     * @param db
     * @param query
     * @return
     */
    private MetricStreamDefinition minimalStreamDef(String streamId, String db, Query query) {
        TimeInterval dataSettlingTime = null;
        TimeInterval queryChunkSize = null;

        return new MetricStreamDefinition(streamId, null, db, query, dataSettlingTime, queryChunkSize);
    }

}
