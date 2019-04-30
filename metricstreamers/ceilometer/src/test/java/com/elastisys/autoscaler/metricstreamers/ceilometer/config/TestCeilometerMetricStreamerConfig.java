package com.elastisys.autoscaler.metricstreamers.ceilometer.config;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;
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

/**
 * Exercises the logic in the {@link CeilometerMetricStreamerConfig} class.
 */
public class TestCeilometerMetricStreamerConfig {

    private final static String region = "RegionOne";
    private final static TimeInterval pollInterval = TimeInterval.seconds(15);

    /**
     * should be possible to pass explicit values for all fields
     */
    @Test
    public void completeConfig() {
        CeilometerMetricStreamerConfig config = new CeilometerMetricStreamerConfig(validAuth(), region, pollInterval,
                asList(validStream()));
        config.validate();

        assertThat(config.getAuth(), is(validAuth()));
        assertThat(config.getRegion(), is(region));
        assertThat(config.getPollInterval(), is(pollInterval));
        assertThat(config.getMetricStreams(), is(asList(validStream())));
    }

    /**
     * pollInterval and metricStreams are optional fields
     */
    @Test
    public void defaults() {
        List<CeilometerMetricStreamDefinition> streams = null;
        TimeInterval pollinterval = null;
        CeilometerMetricStreamerConfig config = new CeilometerMetricStreamerConfig(validAuth(), region, pollinterval,
                streams);
        config.validate();

        assertThat(config.getPollInterval(), is(CeilometerMetricStreamerConfig.DEFAULT_POLL_INTERVAL));
        assertThat(config.getMetricStreams(), is(Collections.emptyList()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingRegion() {
        String region = null;
        new CeilometerMetricStreamerConfig(validAuth(), region, pollInterval, asList(validStream())).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativePollInterval() {
        TimeInterval pollInterval = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": -1, \"unit\": \"seconds\"}"), TimeInterval.class);
        new CeilometerMetricStreamerConfig(validAuth(), region, pollInterval, asList(validStream())).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroPollInterval() {
        TimeInterval pollinterval = TimeInterval.seconds(0);
        new CeilometerMetricStreamerConfig(validAuth(), region, pollinterval, asList(validStream())).validate();
    }

    /**
     * Validation should propagate to sub-fields.
     */
    @Test(expected = IllegalArgumentException.class)
    public void illegalAuth() {
        new CeilometerMetricStreamerConfig(invalidAuth(), region, pollInterval, asList(validStream())).validate();
    }

    /**
     * Validation should propagate to sub-fields.
     */
    @Test(expected = IllegalArgumentException.class)
    public void illegalMetricStream() {
        new CeilometerMetricStreamerConfig(validAuth(), region, pollInterval, asList(invalidStream())).validate();
    }

    private static AuthConfig invalidAuth() {
        // auth missing username
        String username = null;
        return new AuthConfig("http://keystone.example.com:5000/v2",
                new AuthV2Credentials("tenantName", username, "password"), null);
    }

    private static CeilometerMetricStreamDefinition invalidStream() {
        String nullId = null;
        String meter = "meter";
        String resourceId = "resource";
        Downsampling downsampling = new Downsampling(CeilometerFunction.Average, TimeInterval.seconds(60));
        boolean rateConversion = false;
        TimeInterval dataSettlingTime = TimeInterval.seconds(60);
        TimeInterval queryChunkSize = new TimeInterval(14L, TimeUnit.DAYS);
        return new CeilometerMetricStreamDefinition(nullId, meter, resourceId, downsampling, rateConversion,
                dataSettlingTime, queryChunkSize);
    }

    private static AuthConfig validAuth() {
        return new AuthConfig("http://keystone.example.com:5000/v2",
                new AuthV2Credentials("tenantName", "userName", "password"), null);
    }

    private static CeilometerMetricStreamDefinition validStream() {
        String id = "id";
        String meter = "meter";
        String resourceId = "resource";
        Downsampling downsampling = new Downsampling(CeilometerFunction.Average, TimeInterval.seconds(60));
        boolean rateConversion = false;
        TimeInterval dataSettlingTime = TimeInterval.seconds(60);
        TimeInterval queryChunkSize = new TimeInterval(14L, TimeUnit.DAYS);
        return new CeilometerMetricStreamDefinition(id, meter, resourceId, downsampling, rateConversion,
                dataSettlingTime, queryChunkSize);
    }
}
