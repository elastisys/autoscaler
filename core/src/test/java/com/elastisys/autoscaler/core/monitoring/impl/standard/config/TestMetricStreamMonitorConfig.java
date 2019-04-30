package com.elastisys.autoscaler.core.monitoring.impl.standard.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamMonitorConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Exercises the {@link MetricStreamMonitorConfig} class.
 */
public class TestMetricStreamMonitorConfig {

    @Test
    public void create() {
        TimeInterval checkInterval = new TimeInterval(10L, TimeUnit.SECONDS);
        TimeInterval maxTolerableInactivity = new TimeInterval(180L, TimeUnit.SECONDS);

        MetricStreamMonitorConfig config = new MetricStreamMonitorConfig(checkInterval, maxTolerableInactivity);
        assertThat(config.getCheckInterval(), is(checkInterval));
        assertThat(config.getMaxTolerableInactivity(), is(maxTolerableInactivity));
    }

    @Test
    public void defaults() {
        TimeInterval checkInterval = null;
        TimeInterval maxTolerableInactivity = null;

        MetricStreamMonitorConfig config = new MetricStreamMonitorConfig(checkInterval, maxTolerableInactivity);
        assertThat(config.getCheckInterval(), is(MetricStreamMonitorConfig.DEFAULT_CHECK_INTERVAL));
        assertThat(config.getMaxTolerableInactivity(), is(MetricStreamMonitorConfig.DEFAULT_MAX_TOLERABLE_INACTIVITY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithZeroCheckInterval() {
        TimeInterval checkInterval = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": 0, \"unit\": \"seconds\"}"), TimeInterval.class);
        TimeInterval maxTolerableInactivity = new TimeInterval(180L, TimeUnit.SECONDS);

        MetricStreamMonitorConfig config = new MetricStreamMonitorConfig(checkInterval, maxTolerableInactivity);
        config.validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithNegativeCheckInterval() {
        TimeInterval checkInterval = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": -1, \"unit\": \"seconds\"}"), TimeInterval.class);
        TimeInterval maxTolerableInactivity = new TimeInterval(180L, TimeUnit.SECONDS);

        MetricStreamMonitorConfig config = new MetricStreamMonitorConfig(checkInterval, maxTolerableInactivity);
        config.validate();

    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithZeroMaxTolerableInactivity() {
        TimeInterval checkInterval = new TimeInterval(10L, TimeUnit.SECONDS);
        TimeInterval maxTolerableInactivity = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": 0, \"unit\": \"seconds\"}"), TimeInterval.class);

        MetricStreamMonitorConfig config = new MetricStreamMonitorConfig(checkInterval, maxTolerableInactivity);
        config.validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithNegativeMaxTolerableInactivity() {
        TimeInterval checkInterval = new TimeInterval(10L, TimeUnit.SECONDS);
        TimeInterval maxTolerableInactivity = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": -1, \"unit\": \"seconds\"}"), TimeInterval.class);

        MetricStreamMonitorConfig config = new MetricStreamMonitorConfig(checkInterval, maxTolerableInactivity);
        config.validate();
    }
}
