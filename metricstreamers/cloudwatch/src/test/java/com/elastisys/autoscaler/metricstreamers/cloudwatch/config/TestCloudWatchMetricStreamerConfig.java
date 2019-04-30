package com.elastisys.autoscaler.metricstreamers.cloudwatch.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;

/**
 * Exercises the logic in the {@link CloudWatchMetricStreamerConfig} class.
 */
public class TestCloudWatchMetricStreamerConfig {

    /**
     * Verify behavior when explicit values are passed for every setting.
     */
    @Test
    public void completeConfig() {
        List<CloudWatchMetricStreamDefinition> streamDefinitions = Arrays.asList(new CloudWatchMetricStreamDefinition(
                "id", "namespace", "metric", CloudWatchStatistic.Average, TimeInterval.seconds(60), false,
                Maps.of("asgroup", "group1"), TimeInterval.seconds(120), TimeInterval.seconds(86400)));
        CloudWatchMetricStreamerConfig config = new CloudWatchMetricStreamerConfig("accessKey", "secretKey",
                "us-east-1", TimeInterval.seconds(60), streamDefinitions);
        config.validate();

        assertThat(config.getAccessKeyId(), is("accessKey"));
        assertThat(config.getSecretAccessKey(), is("secretKey"));
        assertThat(config.getRegion(), is("us-east-1"));
        assertThat(config.getPollInterval(), is(TimeInterval.seconds(60)));
        assertThat(config.getAccessKeyId(), is("accessKey"));
    }

    /**
     * A config neither needs to specify pollInterval nor metricStreams.
     */
    @Test
    public void defaults() {
        CloudWatchMetricStreamerConfig config = new CloudWatchMetricStreamerConfig("accessKey", "secretKey",
                "us-east-1", null, null);
        config.validate();

        assertThat(config.getPollInterval(), is(CloudWatchMetricStreamerConfig.DEFAULT_POLL_INTERVAL));
        assertThat(config.getMetricStreams(), is(Collections.emptyList()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithMissingAccessKeyId() {
        List<CloudWatchMetricStreamDefinition> streamDefinitions = new ArrayList<>();
        new CloudWatchMetricStreamerConfig(null, "secretKey", "us-east-1", TimeInterval.seconds(60), streamDefinitions)
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithMissingSecretAccessKey() {
        List<CloudWatchMetricStreamDefinition> streamDefinitions = new ArrayList<>();
        new CloudWatchMetricStreamerConfig("accessKey", null, "us-east-1", TimeInterval.seconds(60), streamDefinitions)
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithMissingRegion() {
        List<CloudWatchMetricStreamDefinition> streamDefinitions = new ArrayList<>();
        new CloudWatchMetricStreamerConfig("accessKey", "secretKey", null, TimeInterval.seconds(60), streamDefinitions)
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithNegativePollInterval() {
        List<CloudWatchMetricStreamDefinition> streamDefinitions = new ArrayList<>();
        TimeInterval negativePollInterval = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": -1, \"unit\": \"seconds\"}"), TimeInterval.class);
        new CloudWatchMetricStreamerConfig("accessKey", "secretKey", "us-east-1", negativePollInterval,
                streamDefinitions).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithZeroPollInterval() {
        List<CloudWatchMetricStreamDefinition> streamDefinitions = new ArrayList<>();
        new CloudWatchMetricStreamerConfig("accessKey", "secretKey", "us-east-1", TimeInterval.seconds(0),
                streamDefinitions).validate();
    }

    /**
     * Each metric stream definition should also be validated.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withIllegalMetricStreamDefinition() {
        List<CloudWatchMetricStreamDefinition> metricStreams = Arrays.asList(illegalStreamDef());
        new CloudWatchMetricStreamerConfig("accessKey", "secretKey", "us-east-1", TimeInterval.seconds(30),
                metricStreams).validate();
    }

    @Test
    public void testEqualsAndHashcode() {
        List<CloudWatchMetricStreamDefinition> streamDefinitions = new ArrayList<>();
        CloudWatchMetricStreamerConfig config = new CloudWatchMetricStreamerConfig("accessKey", "secretKey",
                "us-east-1", TimeInterval.seconds(60), streamDefinitions);

        CloudWatchMetricStreamerConfig equal = new CloudWatchMetricStreamerConfig("accessKey", "secretKey", "us-east-1",
                TimeInterval.seconds(60), streamDefinitions);
        CloudWatchMetricStreamerConfig differentAccessKey = new CloudWatchMetricStreamerConfig("accessKey2",
                "secretKey", "us-east-1", TimeInterval.seconds(60), streamDefinitions);
        CloudWatchMetricStreamerConfig differentSecretKey = new CloudWatchMetricStreamerConfig("accessKey",
                "secretKey2", "us-east-1", TimeInterval.seconds(60), streamDefinitions);
        CloudWatchMetricStreamerConfig differentRegion = new CloudWatchMetricStreamerConfig("accessKey", "secretKey",
                "us-east-2", TimeInterval.seconds(60), streamDefinitions);
        CloudWatchMetricStreamerConfig differentPeriod = new CloudWatchMetricStreamerConfig("accessKey", "secretKey",
                "us-east-1", TimeInterval.seconds(120), streamDefinitions);
        CloudWatchMetricStreamerConfig differentStreams = config
                .withStreamDefinition(new CloudWatchMetricStreamDefinition("id", "namespace", "metric",
                        CloudWatchStatistic.Average, TimeInterval.seconds(60), false, Maps.of("asgroup", "group1"),
                        TimeInterval.seconds(120), null));

        assertTrue(config.equals(equal));
        assertFalse(config.equals(differentAccessKey));
        assertFalse(config.equals(differentSecretKey));
        assertFalse(config.equals(differentRegion));
        assertFalse(config.equals(differentPeriod));
        assertFalse(config.equals(differentStreams));

        assertTrue(config.hashCode() == equal.hashCode());
        assertFalse(config.hashCode() == differentAccessKey.hashCode());
        assertFalse(config.hashCode() == differentSecretKey.hashCode());
        assertFalse(config.hashCode() == differentRegion.hashCode());
        assertFalse(config.hashCode() == differentPeriod.hashCode());
        assertFalse(config.hashCode() == differentStreams.hashCode());
    }

    /**
     * Tests the
     * {@link OpenTsdbMetricStreamerConfig#withStreamDefinition(OpenTsdbMetricStreamDefinition)}
     * copy method.
     */
    @Test
    public void testWithStreamDefinition() {
        CloudWatchMetricStreamerConfig original = config();
        assertThat(original, is(config()));

        // use withStreamDefinition copy method to create a config with an added
        // stream definition
        CloudWatchMetricStreamDefinition stream1 = new CloudWatchMetricStreamDefinition("id", "namespace", "metric",
                CloudWatchStatistic.Average, TimeInterval.seconds(120), false, Maps.of("asgroup", "group1"),
                TimeInterval.seconds(120), null);
        CloudWatchMetricStreamerConfig extendedCopy = original.withStreamDefinition(stream1);
        // extended copy is original + the new stream definition
        assertThat(extendedCopy, is(config(stream1)));
        // original should remain unchanged
        assertThat(original, is(config()));

        // test chaining calls
        CloudWatchMetricStreamDefinition stream2 = new CloudWatchMetricStreamDefinition("id2", "namespace", "metric2",
                CloudWatchStatistic.Sum, TimeInterval.seconds(120), false, Maps.of("asgroup", "group1"),
                TimeInterval.seconds(120), null);
        extendedCopy = original.withStreamDefinition(stream1).withStreamDefinition(stream2);
        assertThat(extendedCopy, is(config(stream1, stream2)));
    }

    private CloudWatchMetricStreamerConfig config(CloudWatchMetricStreamDefinition... streamDefinitions) {
        List<CloudWatchMetricStreamDefinition> streamDefList = Arrays.asList(streamDefinitions);
        return new CloudWatchMetricStreamerConfig("accessKey", "secretKey", "us-east-1", TimeInterval.seconds(60),
                streamDefList);
    }

    /**
     * Returns an illegal {@link CloudWatchMetricStreamDefinition} that fails to
     * specify a metric.
     *
     * @return
     */
    private CloudWatchMetricStreamDefinition illegalStreamDef() {
        String metric = null;
        return new CloudWatchMetricStreamDefinition("id", "namespace", metric, CloudWatchStatistic.Average,
                TimeInterval.seconds(110), false, null, null, null);
    }
}
