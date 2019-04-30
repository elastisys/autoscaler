package com.elastisys.autoscaler.metricstreamers.cloudwatch.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Verifies that parsing of {@link CloudWatchMetricStreamerConfig}s from JSON
 * works as expected.
 */
public class TestCloudWatchMetricStreamerConfigParsing {

    @Test
    public void parseSingleStreamDefinition() throws Exception {
        TimeInterval pollInterval = TimeInterval.seconds(30);
        TimeInterval period = TimeInterval.seconds(60);
        TimeInterval dataSettlingTime = new TimeInterval(2L, TimeUnit.MINUTES);
        TimeInterval queryChunkSize = new TimeInterval(1440L, TimeUnit.MINUTES);

        CloudWatchMetricStreamerConfig expected = new CloudWatchMetricStreamerConfig("accessKey", "secretKey",
                "us-east-1", pollInterval,
                Arrays.asList(new CloudWatchMetricStreamDefinition("autoscaling.group.cpu.utilization.stream",
                        "AWS/EC2", "CPUUtilization", CloudWatchStatistic.Sum, period, false,
                        Maps.of("AutoScalingGroupName", "group1"), dataSettlingTime, queryChunkSize)));

        CloudWatchMetricStreamerConfig actual = parse("metricstreamer/single-stream.json");
        actual.validate();

        assertThat(actual, is(expected));
    }

    @Test
    public void parseMultipleStreamDefinitions() throws Exception {
        TimeInterval pollInterval = TimeInterval.seconds(30);
        TimeInterval period = TimeInterval.seconds(60);
        TimeInterval dataSettlingTime = new TimeInterval(2L, TimeUnit.MINUTES);

        CloudWatchMetricStreamDefinition stream1 = new CloudWatchMetricStreamDefinition(
                "autoscaling.group.cpu.utilization.stream", "AWS/EC2", "CPUUtilization", CloudWatchStatistic.Sum,
                period, false, Maps.of("AutoScalingGroupName", "group1"), dataSettlingTime, null);
        CloudWatchMetricStreamDefinition stream2 = new CloudWatchMetricStreamDefinition(
                "cpu.utilization.average.stream", "AWS/EC2", "CPUUtilization", CloudWatchStatistic.Average, period,
                false, Maps.of("Name", "i-123456", "InstanceType", "m1.small"), null, null);

        CloudWatchMetricStreamerConfig expected = new CloudWatchMetricStreamerConfig("accessKey", "secretKey",
                "us-east-1", pollInterval, Arrays.asList(stream1, stream2));

        CloudWatchMetricStreamerConfig actual = parse("metricstreamer/multiple-streams.json");
        actual.validate();

        assertThat(actual, is(expected));
    }

    @Test
    public void parseMinimalConfig() throws Exception {
        CloudWatchMetricStreamerConfig expected = new CloudWatchMetricStreamerConfig("accessKey", "secretKey",
                "us-east-1", null, null);

        CloudWatchMetricStreamerConfig actual = parse("metricstreamer/minimal.json");
        actual.validate();

        assertThat(actual, is(expected));
    }

    private CloudWatchMetricStreamerConfig parse(String resourceFile) throws IOException {
        JsonObject jsonConfig = JsonUtils.parseJsonResource(resourceFile).getAsJsonObject();
        CloudWatchMetricStreamerConfig config = new Gson().fromJson(jsonConfig.get("metricStreamer"),
                CloudWatchMetricStreamerConfig.class);
        return config;
    }
}
