package com.elastisys.autoscaler.core.prediction.impl.standard.capacitylimit;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static java.util.Collections.reverseOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.Configurable;
import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetric;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.autoscaler.core.prediction.impl.standard.StandardPredictionSubsystem;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.CapacityLimitConfig;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.util.collection.Maps;
import com.google.inject.TypeLiteral;

/**
 * The {@link CapacityLimitRegistry} manages a collection of <i>capacity limit
 * rules</i>, which are essentially scheduled min-max boundaries for the
 * predicted compute unit need output by the {@link StandardPredictionSubsystem}
 * . These limits express min-max rules for capacity in order to place budget
 * ceilings to prevent over-spending and/or guarantee minimum elastic computer
 * system capacity levels in order to handle expected peaks.
 * <p/>
 * At any time, at most one capacity limit can be active. When applied to a
 * prediction, a capacity limit rule, produces a <i>bounded prediction</i> that
 * restricts the prediction to make sure it stays within the min and max limit
 * of the currently active capacity limit rule.
 *
 * @see CapacityLimitConfig
 * @see StandardPredictionSubsystem
 */
public class CapacityLimitRegistry implements Configurable<List<CapacityLimitConfig>> {
    private final Logger logger;
    private final EventBus eventBus;

    private List<CapacityLimitConfig> capacityLimits = new CopyOnWriteArrayList<>();

    @Inject
    public CapacityLimitRegistry(Logger logger, EventBus eventBus) {
        this.logger = logger;
        this.eventBus = eventBus;
    }

    @Override
    public void validate(List<CapacityLimitConfig> configuration) throws IllegalArgumentException {
        checkArgument(configuration != null, "capacityLimits: configuration cannot be null");
        try {
            for (CapacityLimitConfig capacityLimitConfig : configuration) {
                capacityLimitConfig.validate();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("capacityLimits: " + e.getMessage(), e);
        }
    }

    @Override
    public void configure(List<CapacityLimitConfig> configuration) throws IllegalArgumentException {
        validate(configuration);
        this.capacityLimits = new CopyOnWriteArrayList<CapacityLimitConfig>(configuration);
    }

    @Override
    public List<CapacityLimitConfig> getConfiguration() {
        return this.capacityLimits;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<List<CapacityLimitConfig>> getConfigurationClass() {
        TypeLiteral<List<CapacityLimitConfig>> configType = new TypeLiteral<List<CapacityLimitConfig>>() {
        };
        return (Class<List<CapacityLimitConfig>>) configType.getRawType();
    }

    /**
     * Applies the capacity limit active at a given time (if any) to a
     * prediction, to produce a <i>bounded prediction</i> -- a prediction that
     * falls within the permissible range of the active capacity limit.
     *
     * @param prediction
     *            The prediction to be bounded.
     * @param timestamp
     *            The time stamp used to determine what capacity limit to
     *            activate.
     * @return The (bounded) prediction after applying the capacity limit that
     *         is active at the given time.
     */
    public Optional<Integer> limit(Optional<Double> prediction, DateTime timestamp) {
        if (!prediction.isPresent()) {
            return Optional.empty();
        }
        // apply currently active limit (if any)
        Optional<CapacityLimitConfig> activeLimit = getActiveLimit(timestamp);
        if (activeLimit.isPresent()) {
            postActiveLimits(activeLimit.get(), timestamp);
            return apply(activeLimit.get(), prediction.get());
        }
        Double roundedUp = Math.ceil(prediction.get());
        return Optional.of(roundedUp.intValue());
    }

    /**
     * Returns the capacity limit (if any) that will be active at a certain
     * point in time.
     *
     * @param timestamp
     *            The time instant of interest.
     * @return The active capacity limit ({@link CapacityLimitConfig}) at the
     *         specified point in time.
     */
    private Optional<CapacityLimitConfig> getActiveLimit(DateTime timestamp) {
        List<CapacityLimitConfig> limits = new ArrayList<>(this.capacityLimits);
        Collections.sort(limits, reverseOrder());
        for (CapacityLimitConfig limit : limits) {
            if (limit.inEffectAt(timestamp)) {
                return Optional.of(limit);
            }
        }
        return Optional.empty();
    }

    private Optional<Integer> apply(CapacityLimitConfig limit, double prediction) {
        prediction = Math.ceil(prediction);
        Double boundedValue = Math.max(prediction, limit.getMin());
        boundedValue = Math.min(boundedValue, limit.getMax());
        return Optional.of(boundedValue.intValue());
    }

    /**
     * Posts {@link SystemMetricEvent}s on the {@link AutoScaler} event bus for
     * the min and max bounds of the capacity limit active at a given point in
     * time.
     *
     * @param activeLimit
     *            The capacity limit.
     * @param atTime
     *            The point in time when capacity limit was/is active.
     */
    private void postActiveLimits(CapacityLimitConfig activeLimit, DateTime atTime) {
        Map<String, String> tags = Maps.of("limit", activeLimit.getId());

        MetricValue value = new MetricValue(SystemMetric.MIN_CAPACITY_LIMIT.getMetricName(), activeLimit.getMin(),
                atTime, tags);
        this.eventBus.post(new SystemMetricEvent(value));

        value = new MetricValue(SystemMetric.MAX_CAPACITY_LIMIT.getMetricName(), activeLimit.getMax(), atTime, tags);
        this.eventBus.post(new SystemMetricEvent(value));
    }

}
