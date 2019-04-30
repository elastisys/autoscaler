package com.elastisys.autoscaler.core.monitoring.impl.standard.config;

import static com.elastisys.autoscaler.core.monitoring.impl.standard.config.MonitoringSystemTestUtils.illegalMetricStreamMonitorConfig;
import static com.elastisys.autoscaler.core.monitoring.impl.standard.config.MonitoringSystemTestUtils.illegalMetricStreamerConfig;
import static com.elastisys.autoscaler.core.monitoring.impl.standard.config.MonitoringSystemTestUtils.illegalSystemHistorianConfig;
import static com.elastisys.autoscaler.core.monitoring.impl.standard.config.MonitoringSystemTestUtils.metricStreamMonitorConfig;
import static com.elastisys.autoscaler.core.monitoring.impl.standard.config.MonitoringSystemTestUtils.metricStreamerConfig;
import static com.elastisys.autoscaler.core.monitoring.impl.standard.config.MonitoringSystemTestUtils.metricStreamerConfig2;
import static com.elastisys.autoscaler.core.monitoring.impl.standard.config.MonitoringSystemTestUtils.systemHistorianConfig;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.junit.Test;

import com.elastisys.autoscaler.core.monitoring.impl.standard.config.StandardMonitoringSubsystemConfig;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;

/**
 * Verifies the behavior of {@link StandardMonitoringSubsystemConfig}.
 *
 */
public class TestStandardMonitoringSubsystemConfig {

    /**
     * Build a valid configuration and make sure it passes validation and all
     * fields are properly stored.
     */
    @Test
    public void basicSanity() {
        StandardMonitoringSubsystemConfig config = new StandardMonitoringSubsystemConfig(asList(metricStreamerConfig()),
                systemHistorianConfig(), metricStreamMonitorConfig());
        config.validate();

        assertThat(config.getMetricStreamers().size(), is(1));
        assertThat(config.getMetricStreamers().get(0), is(metricStreamerConfig()));
        assertThat(config.getSystemHistorian(), is(systemHistorianConfig()));
        assertThat(config.getMetricStreamMonitor(), is(metricStreamMonitorConfig()));
    }

    /**
     * Default values are provided for the system historian and metric stream
     * monitor configuration fields.
     */
    @Test
    public void withDefaults() {
        StandardMonitoringSubsystemConfig config = new StandardMonitoringSubsystemConfig(asList(metricStreamerConfig()),
                null, null);

        assertThat(config.getSystemHistorian(), is(StandardMonitoringSubsystemConfig.DEFAULT_SYSTEM_HISTORIAN));
        assertThat(config.getMetricStreamMonitor(),
                is(StandardMonitoringSubsystemConfig.DEFAULT_METRIC_STREAM_MONITOR_CONFIG));
    }

    /**
     * Multiple {@link MetricStreamer}s are to be supported.
     */
    @Test
    public void withMultipleMetricStreamers() {
        StandardMonitoringSubsystemConfig config = new StandardMonitoringSubsystemConfig(
                asList(metricStreamerConfig(), metricStreamerConfig2()), null, null);

        assertThat(config.getMetricStreamers().size(), is(2));
        assertThat(config.getMetricStreamers().get(0), is(metricStreamerConfig()));
        assertThat(config.getMetricStreamers().get(1), is(metricStreamerConfig2()));
    }

    /**
     * Metric streamer config is mandatory.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withNullMetricStreamer() {
        new StandardMonitoringSubsystemConfig(null, systemHistorianConfig(), metricStreamMonitorConfig()).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void withEmptyListOfMetricStreamers() {
        new StandardMonitoringSubsystemConfig(Collections.emptyList(), systemHistorianConfig(),
                metricStreamMonitorConfig()).validate();
    }

    /**
     * System historian config is optional.
     */
    @Test
    public void withNullSystemHistorian() {
        new StandardMonitoringSubsystemConfig(asList(metricStreamerConfig()), null, metricStreamMonitorConfig())
                .validate();
    }

    /**
     * Metric stream monitor config is optional.
     */
    @Test
    public void withNullMetricStreamMonitor() {
        new StandardMonitoringSubsystemConfig(asList(metricStreamerConfig()), systemHistorianConfig(), null).validate();
    }

    /**
     * Validation should be recursive and also validate sub-configurations.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withIllegalMetricStreamerConfig() {
        new StandardMonitoringSubsystemConfig(asList(illegalMetricStreamerConfig()), systemHistorianConfig(),
                metricStreamMonitorConfig()).validate();
    }

    /**
     * Validation should be recursive and also validate sub-configurations.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withIllegalSystemHistorianConfig() {
        new StandardMonitoringSubsystemConfig(asList(metricStreamerConfig()), illegalSystemHistorianConfig(),
                metricStreamMonitorConfig()).validate();
    }

    /**
     * Validation should be recursive and also validate sub-configurations.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withIllegalMetricStreamMonitorConfig() {
        new StandardMonitoringSubsystemConfig(asList(metricStreamerConfig()), systemHistorianConfig(),
                illegalMetricStreamMonitorConfig()).validate();
    }

}
