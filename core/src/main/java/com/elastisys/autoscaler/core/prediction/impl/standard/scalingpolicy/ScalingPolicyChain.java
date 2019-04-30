package com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.prediction.impl.standard.api.ScalingPolicy;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;

/**
 * A composite {@link ScalingPolicy} that contains a chain of
 * {@link ScalingPolicy}s.
 * <p/>
 * The chain of {@link ScalingPolicy}s are applied in sequence. The output of
 * each {@link ScalingPolicy} becomes the input to the next
 * {@link ScalingPolicy} in the chain.
 */
public class ScalingPolicyChain implements ScalingPolicy {
    private final Logger logger;

    /** The chain of {@link ScalingPolicy}s. */
    private final List<ScalingPolicy> policyChain;

    /**
     * Constructs a new {@link ScalingPolicyChain} from a sequence of
     * {@link ScalingPolicy}s.
     *
     * @param logger
     *
     * @param policyChain
     *            The sequence of {@link ScalingPolicy}s.
     */
    public ScalingPolicyChain(Logger logger, List<ScalingPolicy> policyChain) {
        this.logger = logger;
        this.policyChain = policyChain;
    }

    @Override
    public Optional<Double> apply(Optional<PoolSizeSummary> poolSize, Optional<Double> predictedMachineNeed) {
        requireNonNull(this.policyChain, "no policy chain has been set");
        requireNonNull(poolSize, "poolSize is null");
        requireNonNull(predictedMachineNeed, "predicted machine need is null");

        for (ScalingPolicy policy : this.policyChain) {
            String policyName = policy.getClass().getSimpleName();
            this.logger.debug("prediction before applying {}: {}", policyName, predictedMachineNeed);
            predictedMachineNeed = policy.apply(poolSize, predictedMachineNeed);
            this.logger.debug("prediction after applying {}: {}", policyName, predictedMachineNeed);
        }
        return predictedMachineNeed;
    }

    /**
     * Returns a copy of the policy chain for this {@link ScalingPolicyChain}.
     *
     * @return
     */
    public List<ScalingPolicy> getPolicyChain() {
        return new ArrayList<>(this.policyChain);
    }
}
