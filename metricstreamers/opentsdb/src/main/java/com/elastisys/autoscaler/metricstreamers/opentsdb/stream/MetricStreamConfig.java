package com.elastisys.autoscaler.metricstreamers.opentsdb.stream;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.metricstreamers.opentsdb.config.OpenTsdbMetricStreamDefinition;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Settings for a {@link OpenTsdbMetricStream}, which describes what query to
 * run when fetching values.
 */
public class MetricStreamConfig {

    /** OpenTSDB server host name/IP address. Required. */
    private final String host;
    /** OpenTSDB server port. Required. */
    private final int port;
    /**
     * Describes what query to run for this particular {@link MetricStream}.
     */
    private final OpenTsdbMetricStreamDefinition streamDefinition;

    /**
     * @param host
     *            OpenTSDB server host name/IP address. Required.
     * @param port
     *            OpenTSDB server port. Required.
     * @param streamDefinition
     *            Describes what query to run for this particular
     *            {@link MetricStream}.
     */
    public MetricStreamConfig(String host, Integer port, OpenTsdbMetricStreamDefinition streamDefinition) {
        checkArgument(host != null, "no host given");
        checkArgument(port != null, "no port given");
        checkArgument(streamDefinition != null, "no streamDefinition given");

        this.host = host;
        this.port = port;
        this.streamDefinition = streamDefinition;
    }

    /**
     * OpenTSDB server host name/IP address.
     *
     * @return
     */
    public String getHost() {
        return this.host;
    }

    /**
     * OpenTSDB server port.
     *
     * @return
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Describes what query to run for this particular {@link MetricStream}s.
     *
     * @return
     */
    public OpenTsdbMetricStreamDefinition getStreamDefinition() {
        return this.streamDefinition;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.host, this.port, this.streamDefinition);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetricStreamConfig) {
            MetricStreamConfig that = (MetricStreamConfig) obj;
            return Objects.equals(this.host, that.host) && Objects.equals(this.port, that.port)
                    && Objects.equals(this.streamDefinition, that.streamDefinition);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
