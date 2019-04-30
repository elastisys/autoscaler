package com.elastisys.autoscaler.core.monitoring.impl.standard.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * A sub-configuration of a {@link StandardMonitoringSubsystemConfig} that
 * describes how to set up a {@link SystemHistorian}.
 *
 * @see StandardMonitoringSubsystemConfig
 */
public class SystemHistorianConfig {

    /**
     * The type of the {@link SystemHistorian} to create. Either a qualified
     * class name or a {@link SystemHistorianAlias}.
     */
    private final String type;
    /**
     * The (type-specific) configuration to be passed to the
     * {@link SystemHistorian}.
     */
    private final JsonObject config;

    /**
     * Creates a {@link SystemHistorianConfig}.
     *
     * @param type
     *            The type of the {@link SystemHistorian} to create. Either a
     *            qualified class name or a {@link SystemHistorianAlias}.
     * @param config
     *            The (type-specific) configuration to be passed to the
     *            {@link SystemHistorian}.
     */
    public SystemHistorianConfig(String type, JsonObject config) {
        this.type = type;
        this.config = config;
    }

    /**
     * The type of the {@link SystemHistorian} to create. Either a qualified
     * class name or a {@link SystemHistorianAlias}.
     *
     * @return
     */
    public String getType() {
        return this.type;
    }

    /**
     * The (type-specific) configuration to be passed to the
     * {@link SystemHistorian}.
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
        if (obj instanceof SystemHistorianConfig) {
            SystemHistorianConfig that = (SystemHistorianConfig) obj;
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
        checkArgument(this.type != null, "systemHistorian: missing type");
        checkArgument(this.config != null, "systemHistorian: missing config");
    }
}
