package com.elastisys.autoscaler.core.monitoring.impl.standard.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.elastisys.autoscaler.core.monitoring.streammonitor.MetricStreamMonitor;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;

/**
 * Configuration for a {@link MetricStreamMonitor}.
 */
public class MetricStreamMonitorConfig {
    public static TimeInterval DEFAULT_CHECK_INTERVAL = new TimeInterval(1L, TimeUnit.MINUTES);
    public static TimeInterval DEFAULT_MAX_TOLERABLE_INACTIVITY = new TimeInterval(30L, TimeUnit.MINUTES);

    /**
     * The time between two metric stream activity checks. May be
     * <code>null</code>. Default: {@link #DEFAULT_CHECK_INTERVAL}.
     */
    private final TimeInterval checkInterval;

    /**
     * The longest period of silence (in seconds) that is accepted on a metric
     * stream before an {@link Alert} is posted on the event bus. May be
     * <code>null</code>. Default: {@link #DEFAULT_MAX_TOLERABLE_INACTIVITY}.
     */
    private final TimeInterval maxTolerableInactivity;

    /**
     * Creates a new {@link MetricStreamMonitorConfig}.
     *
     * @param checkInterval
     *            The time between two metric stream activity checks. May be
     *            <code>null</code>. Default: {@link #DEFAULT_CHECK_INTERVAL}.
     * @param maxTolerableInactivity
     *            The longest period of silence (in seconds) that is accepted on
     *            a metric stream before an {@link Alert} is raised on the event
     *            bus. May be <code>null</code>. Default:
     *            {@link #DEFAULT_MAX_TOLERABLE_INACTIVITY}.
     */
    public MetricStreamMonitorConfig(TimeInterval checkInterval, TimeInterval maxTolerableInactivity) {
        this.checkInterval = checkInterval;
        this.maxTolerableInactivity = maxTolerableInactivity;
    }

    /**
     * Returns the time (in seconds) between two metric stream activity checks.
     *
     * @return
     */
    public TimeInterval getCheckInterval() {
        return Optional.ofNullable(this.checkInterval).orElse(DEFAULT_CHECK_INTERVAL);
    }

    /**
     * Returns the longest period of silence (in seconds) that is accepted on a
     * metric stream before an {@link Alert} is raised on the event bus.
     *
     * @return
     */
    public TimeInterval getMaxTolerableInactivity() {
        return Optional.ofNullable(this.maxTolerableInactivity).orElse(DEFAULT_MAX_TOLERABLE_INACTIVITY);
    }

    /**
     * Perorms basic validation of this object. Throws a
     * {@link ConfigurationException} on failure to validate the configuration.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        try {
            getCheckInterval().validate();
            checkArgument(getCheckInterval().getMillis() > 0, "checkInterval must be greater than zero");
        } catch (Exception e) {
            throw new IllegalArgumentException("metricStreamMonitor: checkInterval: " + e.getMessage(), e);
        }

        try {
            getMaxTolerableInactivity().validate();
            checkArgument(getMaxTolerableInactivity().getMillis() > 0,
                    "maxTolerableInactivity must be greater than zero");
        } catch (Exception e) {
            throw new IllegalArgumentException("metricStreamMonitor: maxTolerableInactivity: " + e.getMessage(), e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.checkInterval, this.maxTolerableInactivity);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetricStreamMonitorConfig) {
            MetricStreamMonitorConfig that = (MetricStreamMonitorConfig) obj;

            return Objects.equals(this.checkInterval, that.checkInterval)
                    && Objects.equals(this.maxTolerableInactivity, that.maxTolerableInactivity);
        }
        return super.equals(obj);
    }

    /**
     * Parses and validates a {@link MetricStreamMonitor} JSON configuration
     * into its Java counterpart. An exception is thrown on failure to parse the
     * JSON document or on failure to validate the
     * {@link MetricStreamMonitorConfig}.
     *
     * @param jsonConfig
     *            The configuration as a JSON string.
     * @return The corresponding {@link MetricStreamMonitorConfig}.
     * @throws IllegalArgumentException
     *             If the configuration was invalid.
     */
    public static MetricStreamMonitorConfig parse(String jsonConfig) throws IllegalArgumentException {
        MetricStreamMonitorConfig config = JsonUtils.toObject(JsonUtils.parseJsonString(jsonConfig),
                MetricStreamMonitorConfig.class);
        config.validate();
        return config;
    }
}
