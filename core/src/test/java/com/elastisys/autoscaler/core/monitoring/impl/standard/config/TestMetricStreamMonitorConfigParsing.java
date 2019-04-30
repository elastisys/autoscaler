package com.elastisys.autoscaler.core.monitoring.impl.standard.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.autoscaler.core.monitoring.streammonitor.MetricStreamMonitor;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.io.IoUtils;
import com.google.gson.JsonSyntaxException;

/**
 * Verifies that parsing of a {@link MetricStreamMonitor} JSON configuration to
 * its {@link MetricStreamMonitorConfig} object counterpart works as expected.
 */
public class TestMetricStreamMonitorConfigParsing {

    @Test
    public void parseValidConfig() {
        MetricStreamMonitorConfig javaConfig = MetricStreamMonitorConfig
                .parse(IoUtils.toString("streammonitor/valid-config.json", StandardCharsets.UTF_8));

        assertThat(javaConfig.getCheckInterval(), is(new TimeInterval(30L, TimeUnit.SECONDS)));
        assertThat(javaConfig.getMaxTolerableInactivity(), is(new TimeInterval(30L, TimeUnit.MINUTES)));
    }

    @Test(expected = JsonSyntaxException.class)
    public void parseConfigWithIllegalValue() {
        MetricStreamMonitorConfig.parse(
                IoUtils.toString("streammonitor/invalid-config-with-illegal-value.json", StandardCharsets.UTF_8));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseConfigWithInvalidValue() {
        MetricStreamMonitorConfig.parse(
                IoUtils.toString("streammonitor/invalid-config-with-invalid-value.json", StandardCharsets.UTF_8));
    }

}
