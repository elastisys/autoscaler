package com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;
import com.elastisys.autoscaler.core.prediction.api.types.PredictionUnit;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.commons.eventbus.EventBus;

/**
 * An enumeration containing all {@link AutoScaler} system metrics that are
 * being tracked in time-series by the {@link SystemHistorian}.
 * <p/>
 * These values are intended for use as metric in {@link MetricValue}s reported
 * to the {@link SystemHistorian} by posting {@link SystemMetricEvent}s on the
 * {@link AutoScaler}'s {@link EventBus}.
 *
 * @see SystemMetricEvent
 */
public enum SystemMetric {
    /**
     * A metric used to report the current load as approximated by a certain
     * {@link Predictor}.
     * <p/>
     * Note: tags should be used on the {@link SystemMetricEvent} to
     * differentiate values reported by different {@link AutoScaler} instances,
     * as well as to differentiate the reporting {@link Predictor}, to specify
     * which metric the load observation concerns, etc.
     */
    CURRENT_LOAD("autoscaler.current.load"),
    /**
     * A metric used to report capacity predictions made by {@link Predictor}s.
     * <p/>
     * Note: tags can be used on the {@link SystemMetricEvent} to differentiate
     * values reported by different {@link AutoScaler} instances and
     * {@link Predictor}s and to specify the predicted metric, the
     * {@link PredictionUnit}, etc.
     */
    PREDICTION("autoscaler.prediction"),
    /**
     * A metric used to report <i>aggregate predictions</i> produced by the
     * {@link PredictionSubsystem}.
     * <p/>
     * Note: tags can be used on the {@link SystemMetricEvent} to differentiate
     * values reported by different {@link AutoScaler} instances.
     */
    AGGREGATE_PREDICTION("autoscaler.prediction.aggregate"),
    /**
     * A metric used to report <i>bounded predictions</i> produced by the
     * {@link PredictionSubsystem}.
     * <p/>
     * Note: tags can be used on the {@link SystemMetricEvent} to differentiate
     * values reported by different {@link AutoScaler} instances.
     */
    BOUNDED_PREDICTION("autoscaler.prediction.bounded"),
    /**
     * A metric used to report a compute-unit prediction made during a resize
     * iteration.
     * <p/>
     * Note: tags can be used on the {@link SystemMetricEvent} to differentiate
     * values reported by different {@link AutoScaler} instances.
     */
    COMPUTE_UNIT_PREDICTION("autoscaler.prediction.computeunit"),
    /**
     * A metric used to report the minimum capacity limit active at a certain
     * point in time.
     * <p/>
     * Note: tags can be used on the {@link SystemMetricEvent} to differentiate
     * values set by different capacity limits.
     */
    MIN_CAPACITY_LIMIT("autoscaler.capacity.limit.min"),
    /**
     * A metric used to report the maximum capacity limit active at a certain
     * point in time.
     * <p/>
     * Note: tags can be used on the {@link SystemMetricEvent} to differentiate
     * values set by different capacity limits.
     */
    MAX_CAPACITY_LIMIT("autoscaler.capacity.limit.max"),
    /**
     * A metric used to report the current size of the managed {@link CloudPool}
     * . A separate value will be reported for every valid {@link MachineState}
     * with the number of current pool members in that particular state. The
     * particular {@link MachineState} to which a given reading belongs can be
     * read from the {@code machineState} tag.
     * <p/>
     * Note: tags should be used on the {@link SystemMetricEvent} to
     * differentiate values reported by different {@link AutoScaler} instances,
     * as well as to differentiate the reporting {@link Predictor}, to specify
     * which metric the load observation concerns, etc.
     */
    CLOUDPOOL_SIZE("autoscaler.cloudpool.size"),
    /**
     * A metric used to report that the size of the managed {@link CloudPool}
     * has changed.
     * <p/>
     * Note: tags can be used on the {@link SystemMetricEvent} to differentiate
     * values reported by different {@link AutoScaler} instances.
     */
    CLOUDPOOL_SIZE_CHANGED("autoscaler.cloudpool.size.changed");

    private final String metricName;

    private SystemMetric(String metricName) {
        this.metricName = metricName;
    }

    /**
     * Returns the textual representation of this {@link SystemMetric}.
     *
     * @return
     */
    public String getMetricName() {
        return this.metricName;
    }
}
