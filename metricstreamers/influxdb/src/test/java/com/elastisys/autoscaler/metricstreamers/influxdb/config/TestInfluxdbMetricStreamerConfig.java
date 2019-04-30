package com.elastisys.autoscaler.metricstreamers.influxdb.config;

import static com.elastisys.autoscaler.metricstreamers.influxdb.config.InfluxdbMetricStreamerConfig.DEFAULT_POLL_INTERVAL;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;

/**
 * Exercises {@link InfluxdbMetricStreamerConfig}.
 */
public class TestInfluxdbMetricStreamerConfig {

    /**
     * Verifies field access when specifying all fields, both mandatory and
     * optional.
     */
    @Test
    public void basicSanity() {
        TimeInterval pollInterval = new TimeInterval(30L, TimeUnit.SECONDS);
        InfluxdbMetricStreamerConfig config = new InfluxdbMetricStreamerConfig("host", 8086, validSecurity(),
                pollInterval, validStreams());
        config.validate();

        assertThat(config.getHost(), is("host"));
        assertThat(config.getPort(), is(8086));
        assertThat(config.getSecurity().get(), is(validSecurity()));
        assertThat(config.getPollInterval(), is(new TimeInterval(30L, TimeUnit.SECONDS)));
        assertThat(config.getMetricStreams(), is(validStreams()));
    }

    /**
     * Only host and port are required.
     */
    @Test
    public void withoutOptionalFields() {
        SecurityConfig security = null;
        TimeInterval pollInterval = null;
        List<MetricStreamDefinition> streams = null;
        InfluxdbMetricStreamerConfig config = new InfluxdbMetricStreamerConfig("host", 8086, security, pollInterval,
                streams);
        config.validate();

        assertThat(config.getHost(), is("host"));
        assertThat(config.getPort(), is(8086));
        assertThat(config.getSecurity().isPresent(), is(false));
        assertThat(config.getPollInterval(), is(DEFAULT_POLL_INTERVAL));
        assertThat(config.getMetricStreams(), is(Collections.emptyList()));
    }

    /**
     * {@code host} is a required field.
     */
    @Test
    public void missingHost() {
        try {
            new InfluxdbMetricStreamerConfig(null, 8086, null, null, null).validate();
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
            new InfluxdbMetricStreamerConfig("host", 0, null, null, null).validate();
            fail("expected to fail validation");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("port"));
        }
    }

    /**
     * Verify that config validation also propagates to {@link SecurityConfig}.
     */
    @Test
    public void withIllegalSecurity() {
        try {
            new InfluxdbMetricStreamerConfig("host", 8086, invalidSecurity(), null, null).validate();
            fail("expected to fail validation");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("security"));
        }
    }

    /**
     * Verify that config validation also propagates to
     * {@link MetricStreamDefinition}.
     *
     * @return
     */
    @Test
    public void withIllegalMetricStreams() {
        try {
            new InfluxdbMetricStreamerConfig("host", 8086, null, null, invalidStreams()).validate();
            fail("expected to fail validation");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("metricStream"));
        }
    }

    private SecurityConfig validSecurity() {
        return new SecurityConfig(false, null, false, false);
    }

    private SecurityConfig invalidSecurity() {
        BasicCredentials creds = JsonUtils.toObject(JsonUtils.parseJsonString("{\"password\": \"bar\"}"),
                BasicCredentials.class);
        return new SecurityConfig(true, creds, false, false);
    }

    private List<MetricStreamDefinition> validStreams() {
        TimeInterval dataSettlingTime = new TimeInterval(2L, TimeUnit.MINUTES);
        TimeInterval maxQueryChunkSize = new TimeInterval(14L, TimeUnit.DAYS);
        Query query = Query.builder().select("non_negative_derivative(max(requests),1s)").from("nginx")
                .where("region = 'us-east-1' AND machineState = 'RUNNING'").groupBy("time(1m) fill(none)").build();
        MetricStreamDefinition stream = new MetricStreamDefinition("id", "request_rate", "mydb", query,
                dataSettlingTime, maxQueryChunkSize);
        return Arrays.asList(stream);
    }

    private List<MetricStreamDefinition> invalidStreams() {
        // stream specifies a query without FROM clause
        Query query = Query.builder().select("non_negative_derivative(max(requests),1s)")
                .where("region = 'us-east-1' AND machineState = 'RUNNING'").groupBy("time(1m) fill(none)").build();
        TimeInterval dataSettlingTime = new TimeInterval(2L, TimeUnit.MINUTES);
        TimeInterval maxQueryChunkSize = new TimeInterval(14L, TimeUnit.DAYS);
        MetricStreamDefinition stream = new MetricStreamDefinition("id", "request_rate", "mydb", query,
                dataSettlingTime, maxQueryChunkSize);
        return Arrays.asList(stream);
    }

}
