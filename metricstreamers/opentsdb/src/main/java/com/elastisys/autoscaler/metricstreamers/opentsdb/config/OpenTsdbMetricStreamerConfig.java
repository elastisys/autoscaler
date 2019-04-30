package com.elastisys.autoscaler.metricstreamers.opentsdb.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.metricstreamers.opentsdb.OpenTsdbMetricStreamer;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Carries configuration parameters for the {@link OpenTsdbMetricStreamer}.
 *
 * @see OpenTsdbMetricStreamer
 *
 */
public class OpenTsdbMetricStreamerConfig {
    /** Default {@link MetricStream} polling interval. */
    public static final TimeInterval DEFAULT_POLL_INTERVAL = new TimeInterval(30L, TimeUnit.SECONDS);
    /** Default for {@link #openTsdbPort}. */
    public static final int DEFAULT_OPENTSDB_PORT = 4242;

    /** The host name or IP address of the OpenTSDB server to use. */
    private final String openTsdbHost;
    /**
     * The IP port number of the OpenTSDB server to use. May be
     * <code>null</code>. Default: {@link #DEFAULT_OPENTSDB_PORT}.
     */
    private final Integer openTsdbPort;
    /**
     * The polling interval for {@link MetricStream}s. May be <code>null</code>.
     * Default is {@value #DEFAULT_POLL_INTERVAL}.
     */
    private final TimeInterval pollInterval;
    /**
     * The collection of published {@link MetricStream}s. May be
     * <code>null</code>.
     */
    private final List<OpenTsdbMetricStreamDefinition> metricStreams;

    /**
     * Creates a new {@link OpenTsdbMetricStreamerConfig} with the specified
     * configuration values.
     *
     * @param openTsdbHost
     *            The host name or IP address of the OpenTSDB server to use.
     * @param openTsdbPort
     *            The IP port number of the OpenTSDB server to use. May be
     *            <code>null</code>. Default: {@link #DEFAULT_OPENTSDB_PORT}.
     * @param pollInterval
     *            The polling interval for metric streams. May be
     *            <code>null</code>. Default is {@value #DEFAULT_POLL_INTERVAL}.
     * @param metricStreams
     *            The collection of published {@link MetricStream}s. May be
     *            <code>null</code>.
     */
    public OpenTsdbMetricStreamerConfig(String openTsdbHost, Integer openTsdbPort, TimeInterval pollInterval,
            List<OpenTsdbMetricStreamDefinition> metricStreams) {
        this.openTsdbHost = openTsdbHost;
        this.openTsdbPort = openTsdbPort;
        this.pollInterval = pollInterval;
        this.metricStreams = metricStreams;
    }

    /**
     * Returns the host name or IP address of the OpenTSDB server to use.
     *
     * @return
     */
    public String getOpenTsdbHost() {
        return this.openTsdbHost;
    }

    /**
     * Returns the IP port number of the OpenTSDB server to use.
     *
     * @return
     */
    public int getOpenTsdbPort() {
        return Optional.ofNullable(this.openTsdbPort).orElse(DEFAULT_OPENTSDB_PORT);
    }

    /**
     * Returns the polling interval for metric streams.
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
    public List<OpenTsdbMetricStreamDefinition> getMetricStreams() {
        return Optional.ofNullable(this.metricStreams).orElse(Collections.emptyList());
    }

    /**
     * Returns a copy of this {@link OpenTsdbMetricStreamerConfig} with an
     * additional {@link OpenTsdbMetricStreamDefinition}. Note that the instance
     * that this method is invoked on remains unchanged.
     *
     * @param streamDefinition
     *            The {@link OpenTsdbMetricStreamDefinition} that will be added
     *            in the returned copy.
     * @return A copy of this {@link OpenTsdbMetricStreamerConfig} with the
     *         stream definition added.
     */
    public OpenTsdbMetricStreamerConfig withMetricStream(OpenTsdbMetricStreamDefinition streamDefinition) {
        List<OpenTsdbMetricStreamDefinition> newDefinitions = new ArrayList<>(this.metricStreams);
        newDefinitions.add(streamDefinition);
        return new OpenTsdbMetricStreamerConfig(this.openTsdbHost, this.openTsdbPort, this.pollInterval,
                newDefinitions);
    }

    /**
     * Validates the configuration (checks if there are any obvious errors).
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        try {
            checkArgument(this.openTsdbHost != null, "missing openTsdbHost");
            checkArgument(getOpenTsdbPort() >= 1 && getOpenTsdbPort() <= 65353,
                    "port number not in allowed range [1,65353]");

            checkArgument(getPollInterval().getMillis() >= 1, "pollInterval must be a positive duration");
            for (OpenTsdbMetricStreamDefinition stream : getMetricStreams()) {
                stream.validate();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("metricStreamer: opentsdb: " + e.getMessage(), e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.openTsdbHost, this.openTsdbPort, this.pollInterval, this.metricStreams);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OpenTsdbMetricStreamerConfig) {
            OpenTsdbMetricStreamerConfig that = (OpenTsdbMetricStreamerConfig) obj;
            return Objects.equals(this.openTsdbHost, that.openTsdbHost)
                    && Objects.equals(this.openTsdbPort, that.openTsdbPort)
                    && Objects.equals(this.pollInterval, that.pollInterval)
                    && Objects.equals(this.metricStreams, that.metricStreams);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
