package com.elastisys.autoscaler.systemhistorians.influxdb.config;

import static com.elastisys.autoscaler.systemhistorians.influxdb.config.InfluxdbSystemHistorianConfig.DEFAULT_MAX_BATCH_SIZE;
import static com.elastisys.autoscaler.systemhistorians.influxdb.config.InfluxdbSystemHistorianConfig.DEFAULT_REPORTING_INTERVAL;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.autoscaler.systemhistorians.influxdb.config.InfluxdbSecurityConfig;
import com.elastisys.autoscaler.systemhistorians.influxdb.config.InfluxdbSystemHistorianConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;

/**
 * Exercises {@link InfluxdbSystemHistorianConfig}.
 */
public class TestInfluxdbSystemHistorianConfig {

    /**
     * Verifies field access when specifying all fields, both mandatory and
     * optional.
     */
    @Test
    public void basicSanity() {
        TimeInterval pushInterval = new TimeInterval(30L, TimeUnit.SECONDS);
        int maxBatchSize = 2000;
        InfluxdbSystemHistorianConfig config = new InfluxdbSystemHistorianConfig("host", 8086, "db", validSecurity(),
                pushInterval, maxBatchSize);
        config.validate();

        assertThat(config.getHost(), is("host"));
        assertThat(config.getPort(), is(8086));
        assertThat(config.getDatabase(), is("db"));
        assertThat(config.getSecurity().get(), is(validSecurity()));
        assertThat(config.getReportingInterval(), is(pushInterval));
        assertThat(config.getMaxBatchSize(), is(maxBatchSize));
    }

    /**
     * Only host, port and database are required.
     */
    @Test
    public void withoutOptionalFields() {
        InfluxdbSecurityConfig security = null;
        TimeInterval pushInterval = null;
        Integer maxBatchSize = null;
        InfluxdbSystemHistorianConfig config = new InfluxdbSystemHistorianConfig("host", 8086, "db", security,
                pushInterval, maxBatchSize);
        config.validate();

        assertThat(config.getHost(), is("host"));
        assertThat(config.getPort(), is(8086));
        assertThat(config.getDatabase(), is("db"));
        assertThat(config.getSecurity().isPresent(), is(false));
        assertThat(config.getReportingInterval(), is(DEFAULT_REPORTING_INTERVAL));
        assertThat(config.getMaxBatchSize(), is(DEFAULT_MAX_BATCH_SIZE));
    }

    /**
     * {@code host} is a required field.
     */
    @Test
    public void missingHost() {
        try {
            new InfluxdbSystemHistorianConfig(null, 8086, "db", null, null, null).validate();
            fail("expected to fail validation");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("host"));
        }
    }

    /**
     * {@code port} must be within valid port range.
     */
    @Test
    public void illegalPort() {
        try {
            new InfluxdbSystemHistorianConfig("host", 0, "db", null, null, null).validate();
            fail("expected to fail validation");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("port"));
        }
    }

    /**
     * {@code database} is a required field.
     */
    @Test
    public void missingDatabase() {
        try {
            String database = null;
            new InfluxdbSystemHistorianConfig("host", 8086, database, null, null, null).validate();
            fail("expected to fail validation");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("database"));
        }
    }

    /**
     * Verify that config validation also propagates to
     * {@link InfluxdbSecurityConfig}.
     */
    @Test
    public void withIllegalSecurity() {
        try {
            new InfluxdbSystemHistorianConfig("host", 8086, "db", invalidSecurity(), null, null).validate();
            fail("expected to fail validation");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("security"));
        }
    }

    private InfluxdbSecurityConfig validSecurity() {
        return new InfluxdbSecurityConfig(false, null, false, false);
    }

    private InfluxdbSecurityConfig invalidSecurity() {
        BasicCredentials creds = JsonUtils.toObject(JsonUtils.parseJsonString("{\"password\": \"bar\"}"),
                BasicCredentials.class);
        return new InfluxdbSecurityConfig(true, creds, false, false);
    }
}
