package com.elastisys.autoscaler.metricstreamers.ceilometer.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerFunction;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerMetricStreamerConfig;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.Downsampling;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.openstack.AuthConfig;
import com.elastisys.scale.commons.openstack.AuthV2Credentials;
import com.google.gson.JsonObject;

/**
 * Verifies that parsing of {@link CeilometerMetricStreamerConfig}s from JSON
 * works as expected.
 */
public class TestCeilometerMetricStreamerConfigParsing {

    @Test
    public void parseMinimalConfig() throws Exception {
        CeilometerMetricStreamerConfig config = parseConfig("metricstreamer/minimal.json");
        config.validate();

        AuthConfig expectedAuth = new AuthConfig("http://keystone.example.com:5000/v2.0",
                new AuthV2Credentials("tenant", "clouduser", "cloudpass"), null);

        assertThat(config.getAuth(), is(expectedAuth));
        assertThat(config.getRegion(), is("RegionOne"));
        assertThat(config.getPollInterval(), is(CeilometerMetricStreamerConfig.DEFAULT_POLL_INTERVAL));
        assertThat(config.getMetricStreams(), is(Collections.emptyList()));
    }

    @Test
    public void parseComplete() throws Exception {
        CeilometerMetricStreamerConfig config = parseConfig("metricstreamer/complete.json");
        config.validate();

        AuthConfig expectedAuth = new AuthConfig("http://keystone.example.com:5000/v2.0",
                new AuthV2Credentials("tenant", "clouduser", "cloudpass"), null);
        CeilometerMetricStreamDefinition expectedStream = new CeilometerMetricStreamDefinition("connrate.stream",
                "network.services.lb.total.connections.rate", null,
                new Downsampling(CeilometerFunction.Sum, TimeInterval.seconds(60)), false, TimeInterval.seconds(20),
                new TimeInterval(7L, TimeUnit.DAYS));

        assertThat(config.getAuth(), is(expectedAuth));
        assertThat(config.getRegion(), is("RegionOne"));
        assertThat(config.getPollInterval(), is(TimeInterval.seconds(30)));

        assertThat(config.getMetricStreams(), is(Arrays.asList(expectedStream)));
    }

    private CeilometerMetricStreamerConfig parseConfig(String resourceFile) throws IOException {
        JsonObject jsonConfig = JsonUtils.parseJsonResource(resourceFile).getAsJsonObject();
        CeilometerMetricStreamerConfig config = JsonUtils.toObject(jsonConfig.get("metricStreamer"),
                CeilometerMetricStreamerConfig.class);
        return config;
    }

}
