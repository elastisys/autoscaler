package com.elastisys.autoscaler.core.monitoring.impl.standard.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * A sub-configuration of a {@link StandardMonitoringSubsystemConfig} that
 * describes how to set up a {@link MetricStreamer}.
 *
 * @see StandardMonitoringSubsystemConfig
 */
public class MetricStreamerConfig {

    /**
     * The type of the {@link MetricStreamer} to create. Either a qualified
     * class name or a {@link MetricStreamerAlias}.
     */
    private final String type;
    /**
     * The (type-specific) configuration to be passed to the
     * {@link MetricStreamer}.
     */
    private final JsonObject config;

    /**
     * Creates a {@link MetricStreamerConfig}.
     *
     * @param type
     *            The type of the {@link MetricStreamer} to create. Either a
     *            qualified class name or a {@link MetricStreamerAlias}.
     * @param config
     *            The (type-specific) configuration to be passed to the
     *            {@link MetricStreamer}.
     */
    public MetricStreamerConfig(String type, JsonObject config) {
        this.type = type;
        this.config = config;
    }

    /**
     * The type of the {@link MetricStreamer} to create. Either a qualified
     * class name or a {@link MetricStreamerAlias}.
     *
     * @return
     */
    public String getType() {
        return this.type;
    }

    /**
     * The (type-specific) configuration to be passed to the
     * {@link MetricStreamer}.
     *
     * @return
     */
    public JsonObject getConfig() {
        return this.config;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.config);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetricStreamerConfig) {
            MetricStreamerConfig that = (MetricStreamerConfig) obj;
            return Objects.equals(this.type, that.type) && Objects.equals(this.config, that.config);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    /**
     * Validates the fields of this configuration. Throws an
     * {@link IllegalArgumentException} if validation fails.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(this.type != null, "metricStreamer: missing type");
        checkArgument(this.config != null, "metricStreamer: missing config");
    }

}
