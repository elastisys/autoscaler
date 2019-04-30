package com.elastisys.autoscaler.metricstreamers.opentsdb.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.autoscaler.metricstreamers.opentsdb.config.OpenTsdbMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.opentsdb.config.OpenTsdbMetricStreamerConfig;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.MetricAggregator;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Exercises the logic in the {@link OpenTsdbMetricStreamerConfig} class.
 */
public class TestOpenTsdbMetricStreamerConfig {

    @Test
    public void validateValidConfig() {
        List<OpenTsdbMetricStreamDefinition> streams = new ArrayList<>();
        OpenTsdbMetricStreamerConfig config = new OpenTsdbMetricStreamerConfig("host", 4242,
                new TimeInterval(10L, TimeUnit.SECONDS), streams);
        config.validate();

        assertThat(config.getOpenTsdbHost(), is("host"));
        assertThat(config.getOpenTsdbPort(), is(4242));
        assertThat(config.getPollInterval(), is(new TimeInterval(10L, TimeUnit.SECONDS)));
        assertThat(config.getMetricStreams(), is(Collections.emptyList()));
    }

    /**
     * openTsdbPort, pollInterval and metricstreams is optional
     */
    @Test
    public void withDefaults() {
        Integer openTsdbPort = null;
        List<OpenTsdbMetricStreamDefinition> streams = null;
        TimeInterval pollInterval = null;
        OpenTsdbMetricStreamerConfig config = new OpenTsdbMetricStreamerConfig("host", openTsdbPort, pollInterval,
                streams);
        config.validate();

        assertThat(config.getOpenTsdbPort(), is(OpenTsdbMetricStreamerConfig.DEFAULT_OPENTSDB_PORT));
        assertThat(config.getPollInterval(), is(OpenTsdbMetricStreamerConfig.DEFAULT_POLL_INTERVAL));
        assertThat(config.getMetricStreams(), is(Collections.emptyList()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithMissingHost() {
        List<OpenTsdbMetricStreamDefinition> streams = new ArrayList<>();
        new OpenTsdbMetricStreamerConfig(null, 4242, new TimeInterval(10L, TimeUnit.SECONDS), streams).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithNegativePort() {
        List<OpenTsdbMetricStreamDefinition> streams = new ArrayList<>();
        new OpenTsdbMetricStreamerConfig("host", -1, new TimeInterval(10L, TimeUnit.SECONDS), streams).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithZeroPort() {
        List<OpenTsdbMetricStreamDefinition> streams = new ArrayList<>();
        new OpenTsdbMetricStreamerConfig("host", 0, new TimeInterval(10L, TimeUnit.SECONDS), streams).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithTooHighPort() {
        List<OpenTsdbMetricStreamDefinition> streams = new ArrayList<>();
        new OpenTsdbMetricStreamerConfig("host", 65354, new TimeInterval(10L, TimeUnit.SECONDS), streams).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithNegativePollInterval() {
        List<OpenTsdbMetricStreamDefinition> streams = new ArrayList<>();
        TimeInterval illegalPollInterval = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": -1, \"unit\": \"seconds\"}"), TimeInterval.class);
        new OpenTsdbMetricStreamerConfig("host", 4242, illegalPollInterval, streams).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithZeroPollInterval() {
        List<OpenTsdbMetricStreamDefinition> streams = new ArrayList<>();
        TimeInterval illegalPollInterval = new TimeInterval(0L, TimeUnit.SECONDS);
        new OpenTsdbMetricStreamerConfig("host", 4242, illegalPollInterval, streams).validate();
    }

    @Test
    public void testEqualsAndHashcode() {
        List<OpenTsdbMetricStreamDefinition> streams = new ArrayList<>();
        OpenTsdbMetricStreamerConfig config = new OpenTsdbMetricStreamerConfig("host", 4242,
                new TimeInterval(10L, TimeUnit.SECONDS), streams);

        OpenTsdbMetricStreamerConfig equal = new OpenTsdbMetricStreamerConfig("host", 4242,
                new TimeInterval(10L, TimeUnit.SECONDS), streams);
        OpenTsdbMetricStreamerConfig differentHost = new OpenTsdbMetricStreamerConfig("host2", 4242,
                new TimeInterval(10L, TimeUnit.SECONDS), streams);
        OpenTsdbMetricStreamerConfig differentPort = new OpenTsdbMetricStreamerConfig("host", 2424,
                new TimeInterval(10L, TimeUnit.SECONDS), streams);
        OpenTsdbMetricStreamerConfig differentPoll = new OpenTsdbMetricStreamerConfig("host", 4242,
                new TimeInterval(11L, TimeUnit.SECONDS), streams);
        OpenTsdbMetricStreamerConfig differentStreams = config.withMetricStream(new OpenTsdbMetricStreamDefinition("id",
                "metric", MetricAggregator.MAX, false, null, null, null, null));

        assertTrue(config.equals(equal));
        assertFalse(config.equals(differentHost));
        assertFalse(config.equals(differentPort));
        assertFalse(config.equals(differentPoll));
        assertFalse(config.equals(differentStreams));

        assertTrue(config.hashCode() == equal.hashCode());
        assertFalse(config.hashCode() == differentHost.hashCode());
        assertFalse(config.hashCode() == differentPort.hashCode());
        assertFalse(config.hashCode() == differentPoll.hashCode());
        assertFalse(config.hashCode() == differentStreams.hashCode());
    }

    /**
     * Tests the
     * {@link OpenTsdbMetricStreamerConfig#withMetricStream(OpenTsdbMetricStreamDefinition)}
     * copy method.
     */
    @Test
    public void testWithMetricStreamDefinition() {
        OpenTsdbMetricStreamerConfig original = config();
        assertThat(original, is(config()));

        // use withStreamDefinition copy method to create a config with an add
        // stream definition
        OpenTsdbMetricStreamDefinition stream1 = new OpenTsdbMetricStreamDefinition("id1", "metric",
                MetricAggregator.SUM, true, null, null, new TimeInterval(10L, TimeUnit.SECONDS), null);
        OpenTsdbMetricStreamerConfig extendedCopy = original.withMetricStream(stream1);
        // extended copy is original + the new stream definition
        assertThat(extendedCopy, is(config(stream1)));
        // original should remain unchanged
        assertThat(original, is(config()));

        // test chaining calls
        OpenTsdbMetricStreamDefinition stream2 = new OpenTsdbMetricStreamDefinition("id2", "metric2",
                MetricAggregator.SUM, true, null, null, new TimeInterval(20L, TimeUnit.SECONDS), null);
        extendedCopy = original.withMetricStream(stream1).withMetricStream(stream2);
        assertThat(extendedCopy, is(config(stream1, stream2)));
    }

    private OpenTsdbMetricStreamerConfig config(OpenTsdbMetricStreamDefinition... streams) {
        List<OpenTsdbMetricStreamDefinition> streamDefList = Arrays.asList(streams);
        return new OpenTsdbMetricStreamerConfig("host", 4242, new TimeInterval(10L, TimeUnit.SECONDS), streamDefList);
    }

}
