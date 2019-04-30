package com.elastisys.autoscaler.core.prediction.impl.standard.capacitylimit;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetric;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.CapacityLimitConfig;
import com.elastisys.scale.commons.util.collection.Maps;

/**
 * Utilities for testing capacity limits and the {@link CapacityLimitRegistry}.
 */
public class CapacityLimitTestUtils {

    private CapacityLimitTestUtils() {
        throw new IllegalStateException("not instantiable");
    }

    /**
     * Returns <code>true</code> if the given capacity limit is in effect at the
     * given time.
     *
     * @param limit
     *            A capacity limit.
     * @param time
     *            A string representation of a time stamp. Format:
     *            <code>yyyy-MM-dd'T'HH:mm:ss</code>.
     * @return
     * @throws ParseException
     */
    public static boolean inEffectAt(CapacityLimitConfig capacityLimit, String time) {
        return inEffectAt(capacityLimit, new DateTime(time));
    }

    /**
     * Returns <code>true</code> if the given capacity limit is in effect at the
     * given time.
     *
     * @param limit
     *            A capacity limit.
     * @param time
     *            A time stamp.
     * @return
     * @throws ParseException
     */
    public static boolean inEffectAt(CapacityLimitConfig capacityLimit, DateTime time) {
        return capacityLimit.inEffectAt(time);
    }

    public static SystemMetricEvent minLimitEvent(long value, DateTime timestamp, String capacityLimitName) {
        return new SystemMetricEvent(new MetricValue(SystemMetric.MIN_CAPACITY_LIMIT.getMetricName(), value, timestamp,
                Maps.of("limit", capacityLimitName)));
    }

    public static SystemMetricEvent maxLimitEvent(long value, DateTime timestamp, String capacityLimitName) {
        return new SystemMetricEvent(new MetricValue(SystemMetric.MAX_CAPACITY_LIMIT.getMetricName(), value, timestamp,
                Maps.of("limit", capacityLimitName)));
    }

    public static List<CapacityLimitConfig> configs(CapacityLimitConfig... configs) {
        return Arrays.asList(configs);
    }

    public static CapacityLimitConfig config(String id, long rank, String schedule, int min, int max) {
        return new CapacityLimitConfig(id, rank, schedule, min, max);
    }

}
