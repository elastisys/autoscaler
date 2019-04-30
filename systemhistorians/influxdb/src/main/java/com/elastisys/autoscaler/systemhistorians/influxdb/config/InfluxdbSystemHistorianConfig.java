package com.elastisys.autoscaler.systemhistorians.influxdb.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.elastisys.autoscaler.systemhistorians.influxdb.InfluxdbSystemHistorian;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Represents a configuration for an {@link InfluxdbSystemHistorian}
 *
 * @see InfluxdbSystemHistorian
 *
 */
public class InfluxdbSystemHistorianConfig {
    /** Default reporting interval when none is specified in configuration. */
    static final TimeInterval DEFAULT_REPORTING_INTERVAL = new TimeInterval(30L, TimeUnit.SECONDS);
    /** Default maximum batch size to send to InfluxDB in a single call. */
    static final int DEFAULT_MAX_BATCH_SIZE = 1000;

    /** InfluxDB server host name/IP address. Required. */
    private final String host;
    /** InfluxDB server port. Required. */
    private final int port;
    /**
     * The InfluxDB database to write to. An InfluxDB database acts as a
     * container for time series data. For example, {@code mydb}. Required.
     */
    private final String database;
    /**
     * Security settings for connecting with the server. Optional. If left out,
     * the InfluxDB server is assumed to run over HTTP and no client
     * authentication will be used.
     */
    private final InfluxdbSecurityConfig security;

    /**
     * The time interval between two successive reporting attempts to InfluxDB.
     * Defaults to: {@link #DEFAULT_REPORTING_INTERVAL}.
     */
    private final TimeInterval reportingInterval;
    /**
     * The maximum number of datapoints to send in a single call to InfluxDB. As
     * noted in the <a href=
     * "https://docs.influxdata.com/influxdb/v1.0/guides/writing_data/">InfluxDB
     * docs</a/> it may be necessary to split datapoints into smaller batches
     * once they exceed a few thousand points to avoid request time outs.
     * Defaults to: {@link #DEFAULT_MAX_BATCH_SIZE}.
     */
    private final Integer maxBatchSize;

    /**
     * Creates an {@link InfluxdbSystemHistorianConfig}.
     *
     * @param host
     *            InfluxDB server host name/IP address. Required.
     * @param port
     *            InfluxDB server port. Required.
     * @param database
     *            The InfluxDB database to write to. An InfluxDB database acts
     *            as a container for time series data. For example,
     *            {@code mydb}. Required.
     * @param security
     *            Security settings for connecting with the server. Optional. If
     *            left out, the InfluxDB server is assumed to run over HTTP and
     *            no client authentication will be used.
     * @param reportingInterval
     *            The time interval between two successive reporting attempts to
     *            InfluxDB. Defaults to: {@link #DEFAULT_REPORTING_INTERVAL}.
     * @param maxBatchSize
     *            The maximum number of datapoints to send in a single call to
     *            InfluxDB. As noted in the <a href=
     *            "https://docs.influxdata.com/influxdb/v1.0/guides/writing_data/">InfluxDB
     *            docs</a/> it may be necessary to split datapoints into smaller
     *            batches once they exceed a few thousand points to avoid
     *            request time outs. Defaults to:
     *            {@link #DEFAULT_MAX_BATCH_SIZE}.
     */
    public InfluxdbSystemHistorianConfig(String host, int port, String database, InfluxdbSecurityConfig security,
            TimeInterval reportingInterval, Integer maxBatchSize) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.security = security;
        this.reportingInterval = reportingInterval;
        this.maxBatchSize = maxBatchSize;
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
     * The InfluxDB database to write to. An InfluxDB database acts as a
     * container for time series data. For example, {@code mydb}. Required.
     *
     * @return
     */
    public String getDatabase() {
        return this.database;
    }

    /**
     * Security settings for connecting with the server. Optional. If left out,
     * the InfluxDB server is assumed to run over HTTP and no client
     * authentication will be used.
     *
     * @return
     */
    public Optional<InfluxdbSecurityConfig> getSecurity() {
        return Optional.ofNullable(this.security);
    }

    /**
     * The time interval between two successive reporting attempts to InfluxDB.
     *
     * @return
     */
    public TimeInterval getReportingInterval() {
        return Optional.ofNullable(this.reportingInterval).orElse(DEFAULT_REPORTING_INTERVAL);
    }

    /**
     * The maximum number of datapoints to send in a single call to InfluxDB. As
     * noted in the <a href=
     * "https://docs.influxdata.com/influxdb/v1.0/guides/writing_data/">InfluxDB
     * docs</a/> it may be necessary to split datapoints into smaller batches
     * once they exceed a few thousand points to avoid request time outs.
     *
     * @return
     */
    public Integer getMaxBatchSize() {
        return Optional.ofNullable(this.maxBatchSize).orElse(DEFAULT_MAX_BATCH_SIZE);
    }

    public void validate() throws IllegalArgumentException {
        try {
            checkArgument(this.host != null, "no host given");
            checkArgument(1 <= this.port && this.port <= 65535, "port must be in range [1,65535]");
            checkArgument(this.database != null, "no database given");
            if (this.security != null) {
                this.security.validate();
            }
            getReportingInterval().validate();
            checkArgument(getMaxBatchSize() > 0, "maxBatchSize must be a positive number");
        } catch (Exception e) {
            throw new IllegalArgumentException("systemHistorian: " + e.getMessage(), e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.host, this.port, this.database, getSecurity(), getReportingInterval(),
                getMaxBatchSize());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InfluxdbSystemHistorianConfig) {
            InfluxdbSystemHistorianConfig that = (InfluxdbSystemHistorianConfig) obj;
            return Objects.equals(this.host, that.host) //
                    && Objects.equals(this.port, that.port) //
                    && Objects.equals(this.database, that.database) //
                    && Objects.equals(getSecurity(), that.getSecurity()) //
                    && Objects.equals(getReportingInterval(), that.getReportingInterval()) //
                    && Objects.equals(getMaxBatchSize(), that.getMaxBatchSize());
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
