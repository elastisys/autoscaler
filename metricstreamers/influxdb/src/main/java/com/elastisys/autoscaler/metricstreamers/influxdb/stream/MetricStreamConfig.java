package com.elastisys.autoscaler.metricstreamers.influxdb.stream;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.MetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.SecurityConfig;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Settings for a {@link InfluxdbMetricStream}, which describes what query to
 * run when fetching values.
 */
public class MetricStreamConfig {

    /** InfluxDB server host name/IP address. Required. */
    private final String host;
    /** InfluxDB server port. Required. */
    private final int port;
    /**
     * Security settings for connecting with the server. Optional. If left out,
     * the InfluxDB server is assumed to run over HTTP and no client
     * authentication will be used.
     */
    private final Optional<SecurityConfig> security;
    /**
     * Describes what query to run for this particular {@link MetricStream}.
     */
    private final MetricStreamDefinition streamDefinition;

    /**
     * @param host
     * @param port
     * @param security
     *            Security settings for connecting with the server. Optional. If
     *            left out, the InfluxDB server is assumed to run over HTTP and
     *            no client authentication will be used.
     * @param streamDefinition
     *            Describes what query to run for this particular
     *            {@link MetricStream}.
     */
    public MetricStreamConfig(String host, Integer port, Optional<SecurityConfig> security,
            MetricStreamDefinition streamDefinition) {
        checkArgument(host != null, "no host given");
        checkArgument(port != null, "no port given");
        checkArgument(streamDefinition != null, "no streamDefinition given");

        this.host = host;
        this.port = port;
        // handle case where security is null (which really means absent)
        this.security = security != null ? security : Optional.empty();
        this.streamDefinition = streamDefinition;
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
        return this.security;
    }

    /**
     * Describes what query to run for this particular {@link MetricStream}s.
     *
     * @return
     */
    public MetricStreamDefinition getStreamDefinition() {
        return this.streamDefinition;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.host, this.port, this.security, this.streamDefinition);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetricStreamConfig) {
            MetricStreamConfig that = (MetricStreamConfig) obj;
            return Objects.equals(this.host, that.host) && Objects.equals(this.port, that.port)
                    && Objects.equals(this.security, that.security)
                    && Objects.equals(this.streamDefinition, that.streamDefinition);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
