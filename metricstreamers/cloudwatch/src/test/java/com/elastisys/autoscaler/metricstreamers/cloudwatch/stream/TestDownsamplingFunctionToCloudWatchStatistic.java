package com.elastisys.autoscaler.metricstreamers.cloudwatch.stream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.DownsampleFunction;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchStatistic;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.stream.DownsamplingFunctionToCloudWatchStatistic;

/**
 * Exercise {@link DownsamplingFunctionToCloudWatchStatistic}.
 */
public class TestDownsamplingFunctionToCloudWatchStatistic {

    @Test
    public void convert() {
        assertThat(new DownsamplingFunctionToCloudWatchStatistic().apply(DownsampleFunction.MAX),
                is(CloudWatchStatistic.Maximum));
        assertThat(new DownsamplingFunctionToCloudWatchStatistic().apply(DownsampleFunction.MIN),
                is(CloudWatchStatistic.Minimum));
        assertThat(new DownsamplingFunctionToCloudWatchStatistic().apply(DownsampleFunction.SUM),
                is(CloudWatchStatistic.Sum));
        assertThat(new DownsamplingFunctionToCloudWatchStatistic().apply(DownsampleFunction.MEAN),
                is(CloudWatchStatistic.Average));
    }
}
