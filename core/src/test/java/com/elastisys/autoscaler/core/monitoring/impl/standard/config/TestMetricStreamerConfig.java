package com.elastisys.autoscaler.core.monitoring.impl.standard.config;

import static com.elastisys.autoscaler.core.monitoring.impl.standard.config.MonitoringSystemTestUtils.metricStreamerConfigDocument;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamerConfig;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;

/**
 * Verfies the behavior of the {@link MetricStreamerConfig}.
 */
public class TestMetricStreamerConfig {
    @Test
    public void basicSanity() {
        MetricStreamerConfig config = new MetricStreamerConfig("some.MetricStreamerImpl",
                metricStreamerConfigDocument());
        config.validate();

        assertThat(config.getType(), is("some.MetricStreamerImpl"));
        assertThat(config.getConfig(), is(metricStreamerConfigDocument()));
    }

    /**
     * {@link MetricStreamer} type is a mandatory field.
     */
    @Test(expected = IllegalArgumentException.class)
    public void missingType() {
        new MetricStreamerConfig(null, metricStreamerConfigDocument()).validate();
    }

    /**
     * {@link MetricStreamer} configuration is a mandatory field.
     */
    @Test(expected = IllegalArgumentException.class)
    public void missingConfig() {
        new MetricStreamerConfig("MetricStreamerImpl", null).validate();
    }

}
