package com.elastisys.autoscaler.systemhistorians.opentsdb.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.systemhistorians.opentsdb.config.OpenTsdbSystemHistorianConfig;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Exercises {@link OpenTsdbSystemHistorianConfig}.
 */
public class TestOpenTsdbSystemHistorianConfig {

    private static final String openTsdbHost = "opentsdb";
    private static final Integer openTsdbPort = 4243;
    private static final TimeInterval pushInterval = TimeInterval.seconds(15);

    /**
     * should be possible to explicitly set all fields
     */
    @Test
    public void completeConfig() {
        OpenTsdbSystemHistorianConfig config = new OpenTsdbSystemHistorianConfig(openTsdbHost, openTsdbPort,
                pushInterval);
        config.validate();

        assertThat(config.getOpenTsdbHost(), is(openTsdbHost));
        assertThat(config.getOpenTsdbPort(), is(openTsdbPort));
        assertThat(config.getPushInterval(), is(pushInterval));
    }

    /**
     * only openTsdbHost is required.
     */
    @Test
    public void defaults() {
        Integer opentsdbport = null;
        TimeInterval pushinterval = null;
        OpenTsdbSystemHistorianConfig config = new OpenTsdbSystemHistorianConfig(openTsdbHost, opentsdbport,
                pushinterval);
        config.validate();

        assertThat(config.getOpenTsdbPort(), is(OpenTsdbSystemHistorianConfig.DEFAULT_OPENTSDB_PORT));
        assertThat(config.getPushInterval(), is(OpenTsdbSystemHistorianConfig.DEFAULT_PUSH_INTERVAL));
    }

}
