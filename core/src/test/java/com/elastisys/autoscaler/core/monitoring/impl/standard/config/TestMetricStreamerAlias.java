package com.elastisys.autoscaler.core.monitoring.impl.standard.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamerAlias;

/**
 * Verify that {@link MetricStreamerAlias} refer to the correct implementation
 * classes.
 */
public class TestMetricStreamerAlias {

    @Test
    public void testAliases() {
        assertThat(MetricStreamerAlias.OpenTsdbMetricStreamer.getQualifiedClassName(),
                is("com.elastisys.autoscaler.metricstreamers.opentsdb.OpenTsdbMetricStreamer"));
        assertThat(MetricStreamerAlias.CloudWatchMetricStreamer.getQualifiedClassName(),
                is("com.elastisys.autoscaler.metricstreamers.cloudwatch.CloudWatchMetricStreamer"));
        assertThat(MetricStreamerAlias.CeilometerMetricStreamer.getQualifiedClassName(),
                is("com.elastisys.autoscaler.metricstreamers.ceilometer.CeilometerMetricStreamer"));
        assertThat(MetricStreamerAlias.InfluxdbMetricStreamer.getQualifiedClassName(),
                is("com.elastisys.autoscaler.metricstreamers.influxdb.InfluxdbMetricStreamer"));
        assertThat(MetricStreamerAlias.MetricStreamJoiner.getQualifiedClassName(),
                is("com.elastisys.autoscaler.metricstreamers.streamjoiner.MetricStreamJoiner"));

    }
}
