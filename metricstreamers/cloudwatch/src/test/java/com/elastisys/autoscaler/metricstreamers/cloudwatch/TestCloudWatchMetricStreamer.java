package com.elastisys.autoscaler.metricstreamers.cloudwatch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
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
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchMetricStreamerConfig;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchStatistic;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Exercises the basic operations (configure/start/stop) of the
 * {@link CloudWatchMetricStreamer}.
 */
public class TestCloudWatchMetricStreamer {
    private static final Logger LOG = LoggerFactory.getLogger(TestCloudWatchMetricStreamer.class);
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static final EventBus eventBus = new SynchronousEventBus(LOG);

    /** Object under test. */
    private CloudWatchMetricStreamer metricStreamer;

    @Before
    public void beforeTestMethod() {
        this.metricStreamer = new CloudWatchMetricStreamer(LOG, executor, eventBus);
    }

    /**
     * Make sure configuring produces the right {@link MetricStream}s.
     */
    @Test
    public void configure() throws Exception {
        assertThat(this.metricStreamer.getConfiguration(), is(nullValue()));
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));

        CloudWatchMetricStreamerConfig config = config(minimalStreamDef("requests.stream", "AWS/ELB", "RequestCount",
                CloudWatchStatistic.Sum, TimeInterval.seconds(60)));
        this.metricStreamer.configure(config);
        assertThat(this.metricStreamer.getConfiguration(), is(config));
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));

        // check metric streams
        assertThat(this.metricStreamer.getMetricStreams().size(), is(1));
    }

    @Test
    public void reconfigure() throws Exception {
        CloudWatchMetricStreamerConfig config = config(minimalStreamDef("requests.stream", "AWS/ELB", "RequestCount",
                CloudWatchStatistic.Sum, TimeInterval.seconds(60)));
        this.metricStreamer.configure(config);
        assertThat(this.metricStreamer.getConfiguration(), is(config));
        assertThat(this.metricStreamer.getMetricStreams().size(), is(1));

        CloudWatchMetricStreamerConfig newConfig = config(
                minimalStreamDef("cpu.avg", "AWS/EC2", "CPUUtilization", CloudWatchStatistic.Average,
                        TimeInterval.seconds(60)),
                minimalStreamDef("cpu.sum", "AWS/EC2", "CPUUtilization", CloudWatchStatistic.Sum,
                        TimeInterval.seconds(60)));
        this.metricStreamer.configure(newConfig);
        assertThat(this.metricStreamer.getConfiguration(), is(newConfig));
        assertThat(this.metricStreamer.getMetricStreams().size(), is(2));
    }

    @Test
    public void startAndStop() throws Exception {
        CloudWatchMetricStreamerConfig config = config(minimalStreamDef("requests.stream", "AWS/ELB", "RequestCount",
                CloudWatchStatistic.Sum, TimeInterval.seconds(60)));
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
        CloudWatchMetricStreamerConfig config = config(minimalStreamDef("requests.stream", "AWS/ELB", "RequestCount",
                CloudWatchStatistic.Sum, TimeInterval.seconds(60)));
        this.metricStreamer.configure(config);

        this.metricStreamer.fetch();
    }

    private CloudWatchMetricStreamerConfig config(CloudWatchMetricStreamDefinition... metricStreams) {
        TimeInterval pollInterval = new TimeInterval(10L, TimeUnit.SECONDS);
        return new CloudWatchMetricStreamerConfig("awsAccessKeyId", "awsSecretAccessKey", "eu-west-1", pollInterval,
                Arrays.asList(metricStreams));
    }

    /**
     * Creates an {@link MetricStreamDefinition} set up to query CloudWatch with
     * required parameters only.
     *
     * @param streamId
     * @param namespace
     * @param metric
     * @param statistic
     * @param period
     * @return
     */
    private CloudWatchMetricStreamDefinition minimalStreamDef(String streamId, String namespace, String metric,
            CloudWatchStatistic statistic, TimeInterval period) {
        boolean convertToRate = false;
        Map<String, String> dimensions = null;

        TimeInterval dataSettlingTime = null;
        TimeInterval queryChunkSize = null;

        return new CloudWatchMetricStreamDefinition(streamId, namespace, metric, statistic, period, convertToRate,
                dimensions, dataSettlingTime, queryChunkSize);
    }

}
