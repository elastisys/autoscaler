package com.elastisys.autoscaler.metricstreamers.opentsdb.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.DownsampleFunction;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.DownsamplingSpecification;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.MetricAggregator;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Verifies that parsing of {@link OpenTsdbMetricStreamerConfig}s from JSON
 * works as expected.
 */
public class TestOpenTsdbMetricStreamerConfigParsing {

    @Test
    public void parseMinimalConfig() throws Exception {
        OpenTsdbMetricStreamerConfig config = parseConfig("metricstreamer/opentsdb/minimal.json");
        config.validate();

        assertThat(config.getOpenTsdbHost(), is("1.2.3.4"));
        assertThat(config.getOpenTsdbPort(), is(4242));
        assertThat(config.getPollInterval(), is(OpenTsdbMetricStreamerConfig.DEFAULT_POLL_INTERVAL));
        assertThat(config.getMetricStreams(), is(Collections.emptyList()));
    }

    @Test
    public void parseConfigWithSingleStreamDefinition() throws Exception {
        OpenTsdbMetricStreamerConfig config = parseConfig("metricstreamer/opentsdb/singlestreamdef.json");
        config.validate();

        Map<String, List<String>> expectedTags = Maps.of("host", Arrays.asList("*"));
        List<OpenTsdbMetricStreamDefinition> expectedStreams = Arrays.asList(new OpenTsdbMetricStreamDefinition(
                "http.total.accesses.rate.stream", "http.total.accesses", MetricAggregator.SUM, true,
                new DownsamplingSpecification(new TimeInterval(5L, TimeUnit.MINUTES), DownsampleFunction.MEAN),
                expectedTags, new TimeInterval(30L, TimeUnit.SECONDS), null));

        assertThat(config.getOpenTsdbHost(), is("1.2.3.4"));
        assertThat(config.getOpenTsdbPort(), is(4242));
        assertThat(config.getPollInterval(), is(new TimeInterval(5L, TimeUnit.SECONDS)));
        assertThat(config.getMetricStreams(), is(expectedStreams));

    }

    @Test
    public void parseConfigWithTagInStreamDefinition() throws Exception {
        OpenTsdbMetricStreamerConfig config = parseConfig("metricstreamer/opentsdb/stream-with-tag.json");
        config.validate();

        Map<String, List<String>> expectedTags = Maps.of("source", Arrays.asList("BACKEND"));
        List<OpenTsdbMetricStreamDefinition> expectedStreams = Arrays.asList(new OpenTsdbMetricStreamDefinition(
                "haproxy.session.rate.stream", "haproxy.session_rate", MetricAggregator.SUM, false, null, expectedTags,
                new TimeInterval(30L, TimeUnit.SECONDS), null));

        assertThat(config.getOpenTsdbHost(), is("1.2.3.4"));
        assertThat(config.getOpenTsdbPort(), is(4242));
        assertThat(config.getPollInterval(), is(new TimeInterval(5L, TimeUnit.SECONDS)));
        assertThat(config.getMetricStreams(), is(expectedStreams));

    }

    @Test
    public void parseConfigWithMultipleStreamDefinition() throws Exception {
        OpenTsdbMetricStreamerConfig config = parseConfig("metricstreamer/opentsdb/multistreamdef.json");
        config.validate();

        List<OpenTsdbMetricStreamDefinition> expectedStreams = Arrays.asList(
                new OpenTsdbMetricStreamDefinition("http.total.accesses.rate.stream", "http.total.accesses",
                        MetricAggregator.SUM, true, null, null, null, null),
                new OpenTsdbMetricStreamDefinition("http.total.accesses.counter.stream", "http.total.accesses",
                        MetricAggregator.SUM, null, null, null, new TimeInterval(30L, TimeUnit.SECONDS), null));

        assertThat(config.getOpenTsdbHost(), is("1.2.3.4"));
        assertThat(config.getOpenTsdbPort(), is(4242));
        assertThat(config.getPollInterval(), is(new TimeInterval(5L, TimeUnit.SECONDS)));
        assertThat(config.getMetricStreams(), is(expectedStreams));

    }

    private OpenTsdbMetricStreamerConfig parseConfig(String resourceFile) throws IOException {
        JsonObject jsonConfig = JsonUtils.parseJsonResource(resourceFile).getAsJsonObject();
        OpenTsdbMetricStreamerConfig config = new Gson().fromJson(jsonConfig.get("metricStreamer"),
                OpenTsdbMetricStreamerConfig.class);
        return config;
    }

}
