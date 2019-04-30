package com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.Configurable;
import com.elastisys.autoscaler.core.api.CorePredicates;
import com.elastisys.autoscaler.core.prediction.impl.standard.StandardPredictionSubsystem;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.ScalingPolicy;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.ScalingPoliciesConfig;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;

/**
 * A step in the {@link StandardPredictionSubsystem} pipeline that applies
 * {@link ScalingPolicy}s to the machine need predictions produced by the
 * {@link Predictor}s, with the intent of reducing oscillations (prematurely
 * scaling up/down the machine pool), preventing excessive over-shooting,
 * introducing damping/delay to scaling decisions, etc.
 *
 * @see StandardPredictionSubsystem
 *
 */
public class ScalingPolicyEnforcer implements Configurable<ScalingPoliciesConfig>, ScalingPolicy {

    private final Logger logger;

    private ScalingPolicyChain policyChain;
    private ScalingPoliciesConfig config;

    @Inject
    public ScalingPolicyEnforcer(Logger logger) {
        this.logger = logger;

        this.policyChain = null;
        this.config = null;
    }

    @Override
    public void validate(ScalingPoliciesConfig configuration) throws IllegalArgumentException {
        checkArgument(configuration != null, "scalingPolicies: configuration cannot be null");
        configuration.validate();
    }

    @Override
    public void configure(ScalingPoliciesConfig configuration) throws IllegalArgumentException {
        validate(configuration);

        this.config = configuration;
        this.policyChain = createPolicyChain(configuration);
    }

    @Override
    public ScalingPoliciesConfig getConfiguration() {
        return this.config;
    }

    @Override
    public Class<ScalingPoliciesConfig> getConfigurationClass() {
        return ScalingPoliciesConfig.class;
    }

    @Override
    public Optional<Double> apply(Optional<PoolSizeSummary> poolSize, Optional<Double> predictedMachineNeed) {
        checkArgument(isConfigured(), "attempt to apply scaling policies before being configured");
        requireNonNull(poolSize, "poolSize is null");
        requireNonNull(predictedMachineNeed, "predicted machine need is null");

        return this.policyChain.apply(poolSize, predictedMachineNeed);
    }

    /**
     * Returns a copy of the {@link ScalingPolicy}s for this
     * {@link ScalingPolicyChain}.
     *
     * @return
     */
    public List<ScalingPolicy> getScalingPolicies() {
        return new ArrayList<>(this.policyChain.getPolicyChain());
    }

    private boolean isConfigured() {
        return CorePredicates.isConfigured().test(this);
    }

    /**
     * Creates a {@link ScalingPolicyChain} that embodies the configuration
     * contained in a {@link ScalingPoliciesConfig}.
     *
     * @param configuration
     *            The {@link ScalingPoliciesConfig}.
     * @return A {@link ScalingPolicyChain} that captures all
     *         {@link ScalingPolicy}s in the configuration.
     */
    private ScalingPolicyChain createPolicyChain(ScalingPoliciesConfig configuration) {
        List<ScalingPolicy> policyChain = new LinkedList<>();
        if (configuration.getMachineDeltaTolerance() > 0) {
            policyChain.add(new MachineDeltaTolerancePolicy(this.logger, configuration.getMachineDeltaTolerance()));
        }
        if (configuration.getOverprovisioningGracePeriod().getSeconds() > 0) {
            policyChain.add(
                    new OverprovisioningGracePeriodPolicy(this.logger, configuration.getOverprovisioningGracePeriod()));
        }
        return new ScalingPolicyChain(this.logger, policyChain);
    }
}
