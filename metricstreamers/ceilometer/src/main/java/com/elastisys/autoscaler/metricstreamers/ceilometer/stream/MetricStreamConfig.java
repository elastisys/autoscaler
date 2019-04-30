package com.elastisys.autoscaler.metricstreamers.ceilometer.stream;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerMetricStreamDefinition;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.openstack.AuthConfig;

/**
 * Settings for a {@link CeilometerMetricStream}.
 */
public class MetricStreamConfig {

    /**
     * Declares how to authenticate with the OpenStack identity service
     * (Keystone). Required.
     */
    private final AuthConfig auth;

    /**
     * The particular OpenStack region (out of the ones available in Keystone's
     * service catalog) to connect to. For example, {@code RegionOne}. Required.
     */
    private final String region;
    /**
     * Describes what query to run for this particular {@link MetricStream}.
     * Required.
     */
    private final CeilometerMetricStreamDefinition streamDefinition;

    /**
     * @param auth
     *            Declares how to authenticate with the OpenStack identity
     *            service (Keystone). Required.
     * @param region
     *            The particular OpenStack region (out of the ones available in
     *            Keystone's service catalog) to connect to. For example,
     *            {@code RegionOne}. Required.
     * @param streamDefinition
     *            Describes what query to run for this particular
     *            {@link MetricStream}. Required.
     */
    public MetricStreamConfig(AuthConfig auth, String region, CeilometerMetricStreamDefinition streamDefinition) {
        checkArgument(auth != null, "no auth given");
        checkArgument(region != null, "no region given");
        checkArgument(streamDefinition != null, "no streamDefinition given");

        this.auth = auth;
        this.region = region;
        this.streamDefinition = streamDefinition;
    }

    /**
     * Declares how to authenticate with the OpenStack identity service
     * (Keystone).
     *
     * @return
     */
    public AuthConfig getAuth() {
        return this.auth;
    }

    /**
     * The particular OpenStack region (out of the ones available in Keystone's
     * service catalog) to connect to. For example, {@code RegionOne}.
     *
     * @return
     */
    public String getRegion() {
        return this.region;
    }

    /**
     * Describes what query to run for this particular {@link MetricStream}.
     *
     * @return
     */
    public CeilometerMetricStreamDefinition getStreamDefinition() {
        return this.streamDefinition;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.auth, this.region, this.streamDefinition);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetricStreamConfig) {
            MetricStreamConfig that = (MetricStreamConfig) obj;
            return Objects.equals(this.auth, that.auth) && Objects.equals(this.region, that.region)
                    && Objects.equals(this.streamDefinition, that.streamDefinition);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
