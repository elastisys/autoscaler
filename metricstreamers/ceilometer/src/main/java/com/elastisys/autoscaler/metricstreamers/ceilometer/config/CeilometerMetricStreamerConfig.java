package com.elastisys.autoscaler.metricstreamers.ceilometer.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.metricstreamers.ceilometer.CeilometerMetricStreamer;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.openstack.AuthConfig;

/**
 * Carries configuration parameters for the {@link CeilometerMetricStreamer}.
 *
 * @see CeilometerMetricStreamer
 *
 */
public class CeilometerMetricStreamerConfig {
    /** Default poll interval when none is specified in configuration. */
    static final TimeInterval DEFAULT_POLL_INTERVAL = new TimeInterval(30L, TimeUnit.SECONDS);

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
     * The polling interval for metric streams. Defaults to:
     * {@link #DEFAULT_POLL_INTERVAL}.
     */
    private final TimeInterval pollInterval;

    /**
     * The collection of published {@link MetricStream}s. May be
     * <code>null</code>.
     */
    private final List<CeilometerMetricStreamDefinition> metricStreams;

    /**
     * Creates a new {@link CeilometerMetricStreamerConfig}.
     *
     * @param auth
     *            The authentication block, either in version 2 or version 3
     *            format. Required.
     * @param region
     *            The particular OpenStack region (out of the ones available in
     *            Keystone's service catalog) to connect to. For example,
     *            {@code RegionOne}. Required.
     * @param pollInterval
     *            The polling interval for metric streams.
     * @param metricStreams
     *            The collection of published {@link MetricStream}s. May be
     *            <code>null</code>.
     */
    public CeilometerMetricStreamerConfig(AuthConfig auth, String region, TimeInterval pollInterval,
            List<CeilometerMetricStreamDefinition> metricStreams) {
        this.auth = auth;
        this.region = region;
        this.pollInterval = pollInterval;
        this.metricStreams = metricStreams;
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
     * Returns the polling interval (given in seconds) for metric streams.
     *
     * @return
     */
    public TimeInterval getPollInterval() {
        return Optional.ofNullable(this.pollInterval).orElse(DEFAULT_POLL_INTERVAL);
    }

    /**
     * The collection of published {@link MetricStream}s.
     *
     * @return
     */
    public List<CeilometerMetricStreamDefinition> getMetricStreams() {
        return Optional.ofNullable(this.metricStreams).orElse(Collections.emptyList());
    }

    /**
     * Validates the configuration. On validation error, an
     * {@link IllegalArgumentException} is thrown.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(this.auth != null, "metricStreamer: missing auth");
        checkArgument(this.region != null, "metricStreamer: missing region");

        try {
            getPollInterval().validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("metricStreamer: pollInterval: " + e.getMessage(), e);
        }
        checkArgument(getPollInterval().getSeconds() > 0, "metricStreamer: pollInterval must be a positive duration");

        try {
            getMetricStreams().forEach(stream -> stream.validate());
        } catch (Exception e) {
            throw new IllegalArgumentException("metricStreamer: metricStreams: " + e.getMessage(), e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.auth, this.region, this.pollInterval, this.metricStreams);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CeilometerMetricStreamerConfig) {
            CeilometerMetricStreamerConfig that = (CeilometerMetricStreamerConfig) obj;
            return Objects.equals(this.auth, that.auth) && Objects.equals(this.region, that.region)
                    && Objects.equals(this.pollInterval, that.pollInterval)
                    && Objects.equals(this.metricStreams, that.metricStreams);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    /**
     * Returns a copy of this {@link CeilometerMetricStreamerConfig} with an
     * additional {@link CeilometerMetricStreamDefinition}. Note that the
     * instance that this method is invoked on remains unchanged.
     *
     * @param stream
     *            The {@link CeilometerMetricStreamDefinition} that will be
     *            added in the returned copy.
     * @return A copy of this {@link CeilometerMetricStreamerConfig} with the
     *         stream definition added.
     */
    public CeilometerMetricStreamerConfig withMetricStream(CeilometerMetricStreamDefinition stream) {
        // create copy of streams
        List<CeilometerMetricStreamDefinition> streams = new ArrayList<>(getMetricStreams());
        streams.add(stream);
        return new CeilometerMetricStreamerConfig(this.auth, this.region, this.pollInterval, streams);
    }

}
