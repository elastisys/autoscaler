package com.elastisys.autoscaler.metricstreamers.opentsdb;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.metricstreamers.opentsdb.config.OpenTsdbMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.opentsdb.config.OpenTsdbMetricStreamerConfig;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.DownsamplingSpecification;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.MetricAggregator;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Exercises the basic operations (configure/start/stop) of the
 * {@link OpenTsdbMetricStreamer}.
 */
public class TestOpenTsdbMetricStreamer {
    private static final Logger LOG = LoggerFactory.getLogger(TestOpenTsdbMetricStreamer.class);
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static final EventBus eventBus = new SynchronousEventBus(LOG);

    /** Object under test. */
    private OpenTsdbMetricStreamer metricStreamer;

    @Before
    public void beforeTestMethod() {
        this.metricStreamer = new OpenTsdbMetricStreamer(LOG, executor, eventBus);
    }

    /**
     * Make sure configuring produces the right {@link MetricStream}s.
     */
    @Test
    public void configure() throws Exception {
        assertThat(this.metricStreamer.getConfiguration(), is(nullValue()));
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));

        OpenTsdbMetricStreamerConfig config = config(minimalStreamDef("requests.stream"));
        this.metricStreamer.configure(config);
        assertThat(this.metricStreamer.getConfiguration(), is(config));
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));

        // check metric streams
        assertThat(this.metricStreamer.getMetricStreams().size(), is(1));
    }

    @Test
    public void reconfigure() throws Exception {
        OpenTsdbMetricStreamerConfig config = config(minimalStreamDef("requests.stream"));
        this.metricStreamer.configure(config);
        assertThat(this.metricStreamer.getConfiguration(), is(config));
        assertThat(this.metricStreamer.getMetricStreams().size(), is(1));

        OpenTsdbMetricStreamerConfig newConfig = config(minimalStreamDef("cpu.system.stream"),
                minimalStreamDef("cpu.user.stream"));
        this.metricStreamer.configure(newConfig);
        assertThat(this.metricStreamer.getConfiguration(), is(newConfig));
        assertThat(this.metricStreamer.getMetricStreams().size(), is(2));
    }

    @Test
    public void startAndStop() throws Exception {
        OpenTsdbMetricStreamerConfig config = config(minimalStreamDef("requests.stream"));
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
        OpenTsdbMetricStreamerConfig config = config(minimalStreamDef("requests.stream"));
        this.metricStreamer.configure(config);

        this.metricStreamer.fetch();
    }

    private OpenTsdbMetricStreamerConfig config(OpenTsdbMetricStreamDefinition... metricStreams) {
        TimeInterval pollInterval = new TimeInterval(10L, TimeUnit.SECONDS);
        return new OpenTsdbMetricStreamerConfig("opentsdb", 4242, pollInterval, Arrays.asList(metricStreams));
    }

    private OpenTsdbMetricStreamDefinition minimalStreamDef(String streamId) {
        DownsamplingSpecification downsampling = null;
        boolean convertToRate = false;
        Map<String, List<String>> tags = null;
        TimeInterval dataSettlingTime = null;
        TimeInterval queryChunkSize = null;

        return new OpenTsdbMetricStreamDefinition(streamId, "metric", MetricAggregator.SUM, convertToRate, downsampling,
                tags, dataSettlingTime, queryChunkSize);
    }

}
