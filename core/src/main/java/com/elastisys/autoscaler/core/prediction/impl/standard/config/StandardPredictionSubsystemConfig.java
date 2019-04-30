package com.elastisys.autoscaler.core.prediction.impl.standard.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.prediction.impl.standard.StandardPredictionSubsystem;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.ScalingPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.google.gson.JsonObject;

/**
 * Specifies the configuration of the {@link StandardPredictionSubsystem}.
 *
 * @see StandardPredictionSubsystem#configure(StandardPredictionSubsystemConfig)
 *
 */
public class StandardPredictionSubsystemConfig {
    /** Default for {@link #predictors}. */
    public static final List<PredictorConfig> DEFAULT_PREDICTORS = Collections.emptyList();
    /** Default for {@link #capacityMappings}. */
    public static final List<CapacityMappingConfig> DEFAULT_CAPACITY_MAPPINGS = Collections.emptyList();
    /**
     * Default value for {@link #aggregator}. Takes the maximum of all
     * predictions.
     */
    public static final AggregatorConfig DEFAULT_AGGREGATOR = new AggregatorConfig(
            "Math.max.apply(Math, input.predictions.map( function(p){return p.prediction;} ))");
    /** Default value for {@link #scalingPolicies}. */
    public static final ScalingPoliciesConfig DEFAULT_SCALING_POLICIES = new ScalingPoliciesConfig(0.0,
            TimeInterval.seconds(0));
    /** Default vlaue for {@link #capacityLimits}. */
    public static final List<CapacityLimitConfig> DEFAULT_CAPACITY_LIMITS = Collections.emptyList();

    /**
     * A list of configurations, one for each {@link Predictor}. May be
     * <code>null</code>. Default: {@link #DEFAULT_PREDICTORS}.
     */
    private final List<PredictorConfig> predictors;
    /**
     * A translation table for the metrics used by the {@link Predictor}s, used
     * to convert raw metric capacity into a number of compute units. May be
     * <code>null</code>. Default: {@link #DEFAULT_CAPACITY_MAPPINGS}.
     */
    private final List<CapacityMappingConfig> capacityMappings;
    /**
     * The JavaScript expression to use as aggregation function. May be
     * <code>null</code>. Default: {@link #DEFAULT_AGGREGATOR}.
     */
    private final AggregatorConfig aggregator;
    /**
     * The {@link ScalingPolicy}s to use. May be <code>null</code>. Default:
     * {@link #DEFAULT_SCALING_POLICIES}.
     */
    private final ScalingPoliciesConfig scalingPolicies;
    /**
     * The list of scheduled capacity limit rules used to bound predictions. May
     * be <code>null</code>. Default: no capacity limits set.
     */
    private final List<CapacityLimitConfig> capacityLimits;

    /**
     * Creates a {@link StandardPredictionSubsystemConfig}.
     *
     * @param predictors
     *            A list of configurations, one for each {@link Predictor}. May
     *            be <code>null</code>. Default: {@link #DEFAULT_PREDICTORS}.
     * @param capacityMappings
     *            A translation table for the metrics used by the
     *            {@link Predictor}s, used to convert raw metric capacity into a
     *            number of compute units. May be <code>null</code>. Default:
     *            {@link #DEFAULT_CAPACITY_MAPPINGS}.
     * @param aggregator
     *            The JavaScript expression to use as aggregation function. May
     *            be <code>null</code>. Default: {@link #DEFAULT_AGGREGATOR}.
     * @param scalingPolicies
     *            The {@link ScalingPolicy}s to use. May be <code>null</code>.
     *            Default: {@link #DEFAULT_SCALING_POLICIES}.
     * @param capacityLimits
     *            The list of scheduled capacity limit rules used to bound
     *            predictions. May be <code>null</code>. Default: no capacity
     *            limits set.
     */
    public StandardPredictionSubsystemConfig(List<PredictorConfig> predictors,
            List<CapacityMappingConfig> capacityMappings, AggregatorConfig aggregator,
            ScalingPoliciesConfig scalingPolicies, List<CapacityLimitConfig> capacityLimits) {
        this.predictors = predictors;
        this.capacityMappings = capacityMappings;
        this.aggregator = aggregator;
        this.scalingPolicies = scalingPolicies;
        this.capacityLimits = capacityLimits;
    }

    /**
     * Returns the list of {@link Predictor} configurations.
     *
     * @return
     */
    public List<PredictorConfig> getPredictors() {
        return Optional.ofNullable(this.predictors).orElse(DEFAULT_PREDICTORS);
    }

    /**
     * Returns the translation table for the metrics used by the
     * {@link Predictor}s, used to convert raw metric capacity into a number of
     * compute units.
     *
     * @return
     */
    public List<CapacityMappingConfig> getCapacityMappings() {
        return Optional.ofNullable(this.capacityMappings).orElse(DEFAULT_CAPACITY_MAPPINGS);
    }

    /**
     * Returns the JavaScript expression to use as aggregation function.
     *
     * @return
     */
    public AggregatorConfig getAggregator() {
        return Optional.ofNullable(this.aggregator).orElse(DEFAULT_AGGREGATOR);
    }

    /**
     * Returns the {@link ScalingPolicy}s to use.
     *
     * @return
     */
    public ScalingPoliciesConfig getScalingPolicies() {
        return Optional.ofNullable(this.scalingPolicies).orElse(DEFAULT_SCALING_POLICIES);
    }

    /**
     * Returns the list of scheduled capacity limit rules used to bound
     * predictions.
     *
     * @return
     */
    public List<CapacityLimitConfig> getCapacityLimits() {
        return Optional.ofNullable(this.capacityLimits).orElse(DEFAULT_CAPACITY_LIMITS);
    }

    /**
     * Validates that all expected configuration fields are present. Throws an
     * {@link IllegalArgumenException} if any field is missing.
     *
     * @param config
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        try {
            getPredictors().forEach(predictor -> predictor.validate());
            getCapacityMappings().forEach(mapping -> mapping.validate());
            getAggregator().validate();
            getCapacityLimits().forEach(limit -> limit.validate());
            getScalingPolicies().validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("predictionSubsystem: " + e.getMessage(), e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.aggregator, this.capacityLimits, this.capacityMappings, this.scalingPolicies,
                this.predictors);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StandardPredictionSubsystemConfig) {
            StandardPredictionSubsystemConfig that = (StandardPredictionSubsystemConfig) obj;
            return Objects.equals(this.aggregator, that.aggregator)
                    && Objects.equals(this.capacityLimits, that.capacityLimits)
                    && Objects.equals(this.capacityMappings, that.capacityMappings)
                    && Objects.equals(this.scalingPolicies, that.scalingPolicies)
                    && Objects.equals(this.predictors, that.predictors);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    /**
     * Builder class used to produce {@link StandardPredictionSubsystemConfig}
     * instances.
     *
     *
     */
    public static class Builder {
        private List<PredictorConfig> predictors;
        private List<CapacityMappingConfig> capacityMappings;
        private AggregatorConfig aggregator;
        private ScalingPoliciesConfig scalingPolicies;
        private List<CapacityLimitConfig> capacityLimits;

        private Builder() {
            this.predictors = new ArrayList<>();
            this.capacityMappings = new ArrayList<>();
            this.aggregator = null;
            this.scalingPolicies = new ScalingPoliciesConfig(ScalingPoliciesConfig.DEFAULT_MACHINE_DELTA_TOLERANCE,
                    ScalingPoliciesConfig.DEFAULT_OVERPROVISIONING_GRACE_PERIOD);
            this.capacityLimits = new ArrayList<>();
        }

        public static Builder create() {
            return new Builder();
        }

        public StandardPredictionSubsystemConfig build() {
            return new StandardPredictionSubsystemConfig(this.predictors, this.capacityMappings, this.aggregator,
                    this.scalingPolicies, this.capacityLimits);
        }

        public Builder withPredictor(String id, Class<? extends Predictor> predictorType, State state, String metric,
                JsonObject config) {
            this.predictors.add(new PredictorConfig(id, predictorType.getName(), state, metric, config));
            return this;
        }

        public Builder withPredictor(String id, Class<? extends Predictor> predictorType, State state, String metric) {
            this.predictors.add(new PredictorConfig(id, predictorType.getName(), state, metric, new JsonObject()));
            return this;
        }

        public Builder withPredictors(List<PredictorConfig> predictors) {
            this.predictors = predictors;
            return this;
        }

        public Builder withCapacityMapping(String metric, double amountPerComputeUnit) {
            this.capacityMappings.add(new CapacityMappingConfig(metric, amountPerComputeUnit));
            return this;
        }

        public Builder withCapacityMappings(List<CapacityMappingConfig> capacityMappings) {
            this.capacityMappings = capacityMappings;
            return this;
        }

        public Builder withAggregator(String expression) {
            this.aggregator = new AggregatorConfig(expression);
            return this;
        }

        public Builder withScalingPolicies(ScalingPoliciesConfig scalingPolicies) {
            this.scalingPolicies = scalingPolicies;
            return this;
        }

        public Builder withCapacityLimit(String id, long rank, String schedule, int min, int max) {
            this.capacityLimits.add(new CapacityLimitConfig(id, rank, schedule, min, max));
            return this;
        }

        public Builder withCapacityLimits(List<CapacityLimitConfig> capacityLimits) {
            this.capacityLimits = capacityLimits;
            return this;
        }
    }
}
