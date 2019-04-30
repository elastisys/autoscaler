package com.elastisys.autoscaler.metricstreamers.cloudwatch.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;

/**
 * Verifies the behavior of the {@link CloudWatchMetricStreamDefinition} class.
 */
public class TestCloudWatchMetricStreamDefinition {
    private static final String streamId = "cpu.stream";
    private static final String namespace = "AWS/EC2";
    private static final String metricId = "CPUUtilization";
    private static final CloudWatchStatistic statistic = CloudWatchStatistic.Average;
    private static final TimeInterval period = TimeInterval.seconds(60);
    private static final boolean RATE_CONVERSION = false;
    private static final Map<String, String> dimensions = Maps.of("InstanceId", "i-1234567");
    private static final TimeInterval dataSettlingTime = TimeInterval.seconds(120);
    private static final TimeInterval queryChunkSize = TimeInterval.seconds(86400);

    /**
     * Should be possible to specify each field explicitly.
     */
    @Test
    public void completeDefinition() {

        CloudWatchMetricStreamDefinition stream = new CloudWatchMetricStreamDefinition(streamId, namespace, metricId,
                statistic, period, RATE_CONVERSION, dimensions, dataSettlingTime, queryChunkSize);
        stream.validate();

        assertThat(stream.getId(), is(streamId));
        assertThat(stream.getNamespace(), is(namespace));
        assertThat(stream.getMetric(), is(metricId));
        assertThat(stream.getStatistic(), is(statistic));
        assertThat(stream.getPeriod(), is(period));
        assertThat(stream.isConvertToRate(), is(RATE_CONVERSION));
        assertThat(stream.getDimensions(), is(dimensions));
        assertThat(stream.getDataSettlingTime(), is(dataSettlingTime));
        assertThat(stream.getQueryChunkSize(), is(queryChunkSize));
    }

    /**
     * Defaults are provided for some parameters, which may therefore be
     * <code>null</code>.
     */
    @Test
    public void defaults() {

        Map<String, String> dimensions = null;
        TimeInterval dataSettlingTime = null;
        TimeInterval queryChunkSize = null;

        CloudWatchMetricStreamDefinition stream = new CloudWatchMetricStreamDefinition(streamId, namespace, metricId,
                statistic, period, RATE_CONVERSION, dimensions, dataSettlingTime, queryChunkSize);
        stream.validate();

        assertThat(stream.getDimensions(), is(Collections.emptyMap()));
        assertThat(stream.getDataSettlingTime(), is(TimeInterval.seconds(0)));
        // default query chunk size is 1,440 data points
        TimeInterval expectedDefaultChunkSize = TimeInterval.seconds(period.getSeconds() * 1440);
        assertThat(stream.getQueryChunkSize(), is(expectedDefaultChunkSize));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithMissingId() {
        new CloudWatchMetricStreamDefinition(null, namespace, metricId, statistic, period, RATE_CONVERSION, dimensions,
                dataSettlingTime, queryChunkSize).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithMissingNamespace() {
        new CloudWatchMetricStreamDefinition(streamId, null, metricId, statistic, period, RATE_CONVERSION, dimensions,
                dataSettlingTime, queryChunkSize).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithMissingMetric() {
        new CloudWatchMetricStreamDefinition(streamId, namespace, null, statistic, period, RATE_CONVERSION, dimensions,
                dataSettlingTime, queryChunkSize).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithMissingStatistic() {
        new CloudWatchMetricStreamDefinition(streamId, namespace, metricId, null, period, RATE_CONVERSION, dimensions,
                dataSettlingTime, queryChunkSize).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithMissingPeriod() {
        new CloudWatchMetricStreamDefinition(streamId, namespace, metricId, statistic, null, RATE_CONVERSION,
                dimensions, dataSettlingTime, queryChunkSize).validate();
    }

    /**
     * CloudWatch API requires period to be a multiple of 60 seconds.
     */
    @Test(expected = IllegalArgumentException.class)
    public void validateWithTooShortPeriod() {
        TimeInterval shortPeriod = TimeInterval.seconds(45);
        new CloudWatchMetricStreamDefinition(streamId, namespace, metricId, statistic, shortPeriod, RATE_CONVERSION,
                dimensions, dataSettlingTime, queryChunkSize).validate();
    }

    /**
     * CloudWatch API requires period to be a multiple of 60 seconds.
     */
    @Test(expected = IllegalArgumentException.class)
    public void validateWithPeriodNotMultipleOf60Seconds() {
        TimeInterval notEvenMinutes = TimeInterval.seconds(90);
        new CloudWatchMetricStreamDefinition(streamId, namespace, metricId, statistic, notEvenMinutes, RATE_CONVERSION,
                dimensions, dataSettlingTime, queryChunkSize).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithNegativeDataSettlingTime() {
        TimeInterval negativeDataSettlingTime = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": -1, \"unit\": \"seconds\"}"), TimeInterval.class);
        new CloudWatchMetricStreamDefinition(streamId, namespace, metricId, statistic, period, RATE_CONVERSION,
                dimensions, negativeDataSettlingTime, queryChunkSize).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithNegativeQueryChunkSize() {
        TimeInterval negativeChunkSize = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": -1, \"unit\": \"seconds\"}"), TimeInterval.class);
        new CloudWatchMetricStreamDefinition(streamId, namespace, metricId, statistic, period, RATE_CONVERSION,
                dimensions, dataSettlingTime, negativeChunkSize).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithQueryChunkSizeNotMultipleOf60Seconds() {
        TimeInterval chunkSize = TimeInterval.seconds(90);
        new CloudWatchMetricStreamDefinition(streamId, namespace, metricId, statistic, period, RATE_CONVERSION,
                dimensions, dataSettlingTime, chunkSize).validate();
    }

    /**
     * The maximum number of data points returned from a single call is 1,440.
     */
    @Test
    public void validateWithTooLongQueryChunk() {
        TimeInterval period = TimeInterval.seconds(5 * 60);
        TimeInterval allowedMaxChunkSize = TimeInterval.seconds(5 * 60 * 1440);

        // should be okay
        new CloudWatchMetricStreamDefinition(streamId, namespace, metricId, statistic, period, RATE_CONVERSION,
                dimensions, dataSettlingTime, allowedMaxChunkSize).validate();

        try {
            TimeInterval tooLongMaxChunkSize = TimeInterval.seconds(allowedMaxChunkSize.getSeconds() + 60);
            new CloudWatchMetricStreamDefinition(streamId, namespace, metricId, statistic, period, RATE_CONVERSION,
                    dimensions, dataSettlingTime, tooLongMaxChunkSize).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            // expected
            assertTrue(e.getMessage().contains("queryChunkSize too long"));
        }
    }

    // TODO: predicates?
}
