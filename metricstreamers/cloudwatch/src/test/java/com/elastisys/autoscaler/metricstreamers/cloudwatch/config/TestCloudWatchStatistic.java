package com.elastisys.autoscaler.metricstreamers.cloudwatch.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.amazonaws.services.cloudwatch.model.Statistic;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchStatistic;

/**
 * Exercises the {@link CloudWatchStatistic} class.
 */
public class TestCloudWatchStatistic {

    @Test
    public void testTranslate() {
        assertThat(CloudWatchStatistic.Average.toStatistic(), is(Statistic.Average));
        assertThat(CloudWatchStatistic.Sum.toStatistic(), is(Statistic.Sum));
        assertThat(CloudWatchStatistic.SampleCount.toStatistic(), is(Statistic.SampleCount));
        assertThat(CloudWatchStatistic.Minimum.toStatistic(), is(Statistic.Minimum));
        assertThat(CloudWatchStatistic.Maximum.toStatistic(), is(Statistic.Maximum));
    }
}
