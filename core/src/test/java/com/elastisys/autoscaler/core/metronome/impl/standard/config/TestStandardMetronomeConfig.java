package com.elastisys.autoscaler.core.metronome.impl.standard.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.autoscaler.core.metronome.impl.standard.config.StandardMetronomeConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Exercises {@link StandardMetronomeConfig}.
 */
public class TestStandardMetronomeConfig {

    private static TimeInterval horizon = new TimeInterval(5L, TimeUnit.MINUTES);
    private static TimeInterval interval = TimeInterval.seconds(15);
    private static Boolean logOnly = false;

    /**
     * should be possible to explicitly specify all values.
     */
    @Test
    public void completeConfig() {
        StandardMetronomeConfig config = new StandardMetronomeConfig(horizon, interval, logOnly);
        config.validate();

        assertThat(config.getHorizon(), is(horizon));
        assertThat(config.getInterval(), is(interval));
        assertThat(config.isLogOnly(), is(logOnly));
    }

    /**
     * only horizon is a required field.
     */
    @Test
    public void defaults() {
        TimeInterval interval = null;
        Boolean logOnly = null;
        StandardMetronomeConfig config = new StandardMetronomeConfig(horizon, interval, logOnly);
        config.validate();

        assertThat(config.getInterval(), is(StandardMetronomeConfig.DEFAULT_METRONOME_INTERVAL));
        assertThat(config.isLogOnly(), is(StandardMetronomeConfig.DEFAULT_LOG_ONLY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void withNegativeHorizon() {
        TimeInterval horizon = JsonUtils.toObject(JsonUtils.parseJsonString("{\"time\": -1, \"unit\": \"seconds\"}"),
                TimeInterval.class);
        new StandardMetronomeConfig(horizon, interval, logOnly).validate();
    }

    /**
     * Should be okay with a zero-length prediction horizon.
     */
    @Test
    public void withZeroHorizon() {
        new StandardMetronomeConfig(horizon, interval, logOnly).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void withNegativeInterval() {
        TimeInterval interval = JsonUtils.toObject(JsonUtils.parseJsonString("{\"time\": -1, \"unit\": \"seconds\"}"),
                TimeInterval.class);
        new StandardMetronomeConfig(horizon, interval, logOnly).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void withZeroInterval() {
        TimeInterval interval = TimeInterval.seconds(0);
        new StandardMetronomeConfig(horizon, interval, logOnly).validate();
    }

}
