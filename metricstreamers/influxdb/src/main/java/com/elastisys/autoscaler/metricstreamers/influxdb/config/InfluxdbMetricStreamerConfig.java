package com.elastisys.autoscaler.metricstreamers.influxdb.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.metricstreamers.influxdb.InfluxdbMetricStreamer;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Represents a configuration for the {@link InfluxdbMetricStreamer}.
 *
 * @see InfluxdbMetricStreamer
 */
public class InfluxdbMetricStreamerConfig {
    /** Default poll interval when none is specified in configuration. */
    static final TimeInterval DEFAULT_POLL_INTERVAL = new TimeInterval(30L, TimeUnit.SECONDS);

    /** InfluxDB server host name/IP address. Required. */
    private final String host;
    /** InfluxDB server port. Required. */
    private final int port;
    /**
     * Security settings for connecting with the server. Optional. If left out,
     * the InfluxDB server is assumed to run over HTTP and no client
     * authentication will be used.
     */
    private final SecurityConfig security;

    /**
     * The polling interval for metric streams. Defaults to:
     * {@link #DEFAULT_POLL_INTERVAL}.
     */
    private final TimeInterval pollInterval;

    /**
     * The collection of published {@link MetricStream}s. May be
     * <code>null</code>.
     */
    private final List<MetricStreamDefinition> metricStreams;

    /**
     * Creates an {@link InfluxdbMetricStreamerConfig}.
     *
     * @param host
     *            InfluxDB server host name/IP address. Required.
     * @param port
     *            InfluxDB server port. Required.
     * @param security
     *            Security settings for connecting with the server. Optional. If
     *            left out, the InfluxDB server is assumed to run over HTTP and
     *            no client authentication will be used.
     * @param pollInterval
     *            The polling interval for metric streams.
     * @param metricStreams
     *            The collection of published {@link MetricStream}s. May be
     *            <code>null</code>.
     */
    public InfluxdbMetricStreamerConfig(String host, int port, SecurityConfig security, TimeInterval pollInterval,
            List<MetricStreamDefinition> metricStreams) {
        this.host = host;
        this.port = port;
        this.security = security;
        this.pollInterval = pollInterval;
        this.metricStreams = metricStreams;
    }

    /**
     * InfluxDB server host name/IP address.
     *
     * @return
     */
    public String getHost() {
        return this.host;
    }

    /**
     * InfluxDB server port.
     *
     * @return
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Security settings for connecting with the server. Optional. If left out,
     * the InfluxDB server is assumed to run over HTTP and no client
     * authentication will be used.
     *
     * @return
     */
    public Optional<SecurityConfig> getSecurity() {
        return Optional.ofNullable(this.security);
    }

    /**
     * The polling interval for metric streams.
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
    public List<MetricStreamDefinition> getMetricStreams() {
        return Optional.ofNullable(this.metricStreams).orElse(Collections.emptyList());
    }

    public void validate() throws IllegalArgumentException {
        try {
            checkArgument(this.host != null, "no host given");
            checkArgument(1 <= this.port && this.port <= 65535, "port must be in range [1,65535]");
            if (this.security != null) {
                this.security.validate();
            }
            getPollInterval().validate();
            getMetricStreams().forEach(stream -> stream.validate());
        } catch (Exception e) {
            throw new IllegalArgumentException("metricStreamer: influxdb: " + e.getMessage(), e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.host, this.port, getSecurity(), getPollInterval(), this.metricStreams);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InfluxdbMetricStreamerConfig) {
            InfluxdbMetricStreamerConfig that = (InfluxdbMetricStreamerConfig) obj;
            return Objects.equals(this.host, that.host) && Objects.equals(this.port, that.port)
                    && Objects.equals(getSecurity(), that.getSecurity())
                    && Objects.equals(getPollInterval(), that.getPollInterval())
                    && Objects.equals(this.metricStreams, that.metricStreams);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
