package com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy;

import java.util.Optional;

import com.elastisys.autoscaler.core.prediction.impl.standard.api.ScalingPolicy;

/**
 * Utility methods for tests relating to {@link ScalingPolicy} management.
 */
public class ScalingPolicyTestUtils {

    /**
     * Wraps a predicted machine need value inside an {@link Optional}.
     *
     * @param predictedMachineNeed
     * @return
     */
    public static Optional<Double> prediction(double predictedMachineNeed) {
        return Optional.of(predictedMachineNeed);
    }

}
