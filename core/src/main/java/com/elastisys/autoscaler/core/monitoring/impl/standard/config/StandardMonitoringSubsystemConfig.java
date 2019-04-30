package com.elastisys.autoscaler.core.monitoring.impl.standard.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.elastisys.autoscaler.core.monitoring.impl.standard.StandardMonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.streammonitor.MetricStreamMonitor;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.impl.noop.NoOpSystemHistorian;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Represents a configuration for a {@link StandardMonitoringSubsystem}, which
 * controls the behavior of the {@link MetricStreamer}, the
 * {@link SystemHistorian} and the {@link MetricStreamMonitor}.
 *
 * @see StandardMonitoringSubsystem
 *
 */
public class StandardMonitoringSubsystemConfig {
    /** The default system historian to use if none is specified. */
    public static final SystemHistorianConfig DEFAULT_SYSTEM_HISTORIAN = new SystemHistorianConfig(
            NoOpSystemHistorian.class.getName(), new JsonObject());

    /**
     * Default settings to use for {@link MetricStreamMonitor} unless specified
     * in configuration.
     */
    public static final MetricStreamMonitorConfig DEFAULT_METRIC_STREAM_MONITOR_CONFIG = new MetricStreamMonitorConfig(
            MetricStreamMonitorConfig.DEFAULT_CHECK_INTERVAL,
            MetricStreamMonitorConfig.DEFAULT_MAX_TOLERABLE_INACTIVITY);

    /** Configuration that describes the {@link MetricStreamer}s to use. */
    private final List<MetricStreamerConfig> metricStreamers;
    /**
     * Configuration that describes the {@link SystemHistorian} to use. May be
     * <code>null</code>, in which case a no-op {@link SystemHistorian} should
     * be used.
     */
    private final SystemHistorianConfig systemHistorian;

    /**
     * Configuration controlling the behavior of the
     * {@link MetricStreamMonitor}. May be <code>null</code>, in which case
     * default settings will be used. See
     * {@link #DEFAULT_METRIC_STREAM_MONITOR_CONFIG}.
     **/
    private final MetricStreamMonitorConfig metricStreamMonitor;

    /**
     * Creates a {@link StandardMonitoringSubsystemConfig}.
     *
     * @param metricStreamers
     *            Configuration that describes the {@link MetricStreamer}s to
     *            use.
     * @param systemHistorian
     *            Configuration that describes the {@link SystemHistorian} to
     *            use. May be <code>null</code>, in which case a no-op
     *            {@link SystemHistorian} should be used.
     * @param metricStreamMonitor
     *            Configuration controlling the behavior of the
     *            {@link MetricStreamMonitor}. May be <code>null</code>, in
     *            which case default settings will be used. See
     *            {@link #DEFAULT_METRIC_STREAM_MONITOR_CONFIG}.
     */
    public StandardMonitoringSubsystemConfig(List<MetricStreamerConfig> metricStreamers,
            SystemHistorianConfig systemHistorian, MetricStreamMonitorConfig metricStreamMonitor) {
        this.metricStreamers = metricStreamers;
        this.systemHistorian = systemHistorian;
        this.metricStreamMonitor = metricStreamMonitor;
    }

    /**
     * Configuration that describes the {@link MetricStreamer}s to use.
     *
     * @return
     */
    public List<MetricStreamerConfig> getMetricStreamers() {
        return this.metricStreamers;
    }

    /**
     * Configuration that describes the {@link SystemHistorian} to use. If none
     * was explicitly specified in the configuraion,a
     * {@link NoOpSystemHistorian} will be used.
     *
     * @return
     */
    public SystemHistorianConfig getSystemHistorian() {
        return Optional.ofNullable(this.systemHistorian).orElse(DEFAULT_SYSTEM_HISTORIAN);
    }

    /**
     * Configuration controlling the behavior of the
     * {@link MetricStreamMonitor}.
     *
     * @return
     */
    public MetricStreamMonitorConfig getMetricStreamMonitor() {
        return Optional.ofNullable(this.metricStreamMonitor).orElse(DEFAULT_METRIC_STREAM_MONITOR_CONFIG);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.metricStreamers, this.systemHistorian, this.metricStreamMonitor);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StandardMonitoringSubsystemConfig) {
            StandardMonitoringSubsystemConfig that = (StandardMonitoringSubsystemConfig) obj;
            return Objects.equals(this.metricStreamers, that.metricStreamers)
                    && Objects.equals(getSystemHistorian(), that.getSystemHistorian())
                    && Objects.equals(this.metricStreamMonitor, that.metricStreamMonitor);
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
        try {
            checkArgument(this.metricStreamers != null, "missing metricStreamers");
            checkArgument(!this.metricStreamers.isEmpty(),
                    "metricStreamers: at least one metricStreamer must be specified");
            this.metricStreamers.forEach(m -> m.validate());

            getSystemHistorian().validate();
            getMetricStreamMonitor().validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("monitoringSubsystem: " + e.getMessage(), e);
        }
    }
}
