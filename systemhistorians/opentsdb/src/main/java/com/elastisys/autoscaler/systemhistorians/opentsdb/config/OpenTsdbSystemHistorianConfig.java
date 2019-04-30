package com.elastisys.autoscaler.systemhistorians.opentsdb.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Configuration class for the OpenTSDB {@link SystemHistorian}.
 */
public class OpenTsdbSystemHistorianConfig {
    /** Default {@link #pushInterval}. */
    public static final TimeInterval DEFAULT_PUSH_INTERVAL = TimeInterval.seconds(30);
    /** Default for {@link #openTsdbPort}. */
    public static final int DEFAULT_OPENTSDB_PORT = 4242;

    /** The hostname or IP address of the OpenTSDB server to use. */
    private final String openTsdbHost;
    /**
     * The IP port number of the OpenTSDB server to use. May be
     * <code>null</code>. Default: {@link #DEFAULT_OPENTSDB_PORT}.
     */
    private final Integer openTsdbPort;
    /**
     * The time interval between two successive report attempts. May be
     * <code>null</code>. Default: {@link #DEFAULT_PUSH_INTERVAL}.
     */
    private final TimeInterval pushInterval;

    /**
     * Creates a new instance with the specified configuration values
     *
     * @param openTsdbHost
     *            The hostname or IP address of the OpenTSDB server to use.
     * @param openTsdbPort
     *            The IP port number of the OpenTSDB server to use. May be
     *            <code>null</code>. Default: {@link #DEFAULT_OPENTSDB_PORT}.
     * @param pushInterval
     *            The time interval between two successive report attempts. May
     *            be <code>null</code>. Default: {@link #DEFAULT_PUSH_INTERVAL}.
     */
    public OpenTsdbSystemHistorianConfig(String openTsdbHost, Integer openTsdbPort, TimeInterval pushInterval) {
        this.openTsdbHost = openTsdbHost;
        this.openTsdbPort = openTsdbPort;
        this.pushInterval = pushInterval;
    }

    /**
     * Returns the hostname or IP address of the OpenTSDB server to use.
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
     * Returns the time interval between two successive report attempts.
     *
     * @return
     */
    public TimeInterval getPushInterval() {
        return Optional.ofNullable(this.pushInterval).orElse(DEFAULT_PUSH_INTERVAL);
    }

    /**
     * Validates the configuration (checks if there are any obvious errors).
     *
     * @throws IllegalArgumentException
     *             Thrown if there are obvious errors, such as missing values
     *             the values are out of permissible ranges.
     */
    public void validate() throws IllegalArgumentException {
        try {
            Objects.requireNonNull(this.openTsdbHost, "openTsdbHost missing");
            checkArgument(1 <= getOpenTsdbPort() && getOpenTsdbPort() <= 65353,
                    "openTsdbPort not in allowed range [1,65353]");
            checkArgument(getPushInterval().getSeconds() >= 1, "pushInterval must be a positive duration");
        } catch (Exception e) {
            throw new IllegalArgumentException("systemHistorian: " + e.getMessage(), e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.openTsdbHost, this.openTsdbPort, this.pushInterval);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OpenTsdbSystemHistorianConfig) {
            OpenTsdbSystemHistorianConfig that = (OpenTsdbSystemHistorianConfig) obj;
            return Objects.equals(this.openTsdbHost, that.openTsdbHost)
                    && Objects.equals(this.openTsdbPort, that.openTsdbPort)
                    && Objects.equals(this.pushInterval, that.pushInterval);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
