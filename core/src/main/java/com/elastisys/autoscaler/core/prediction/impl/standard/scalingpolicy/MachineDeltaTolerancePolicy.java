package com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Optional;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.prediction.impl.standard.api.ScalingPolicy;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;

/**
 * Scaling policy that strives to reduce oscillations by introducing a smallest
 * machine instance delta fraction to act upon.
 * <p/>
 * If the difference between the current desired machine pool size and a
 * predicted machine need is smaller than this, the prediction is ignored (and
 * the current pool size is kept).
 */
public class MachineDeltaTolerancePolicy implements ScalingPolicy {

    private final Logger logger;
    /**
     * The smallest machine instance delta fraction to act upon.
     * <p/>
     * If the difference between the current machine pool size and a predicted
     * machine need is smaller than this, the prediction is ignored (and the
     * current pool size is kept).
     * <p/>
     * Purpose: reduce oscillations by not reacting too quickly to small changes
     * in predicted machine demand.
     */
    private final double machineDeltaTolerance;

    /**
     * Constructs a new {@link MachineDeltaTolerancePolicy} with a given
     * smallest machine instance delta fraction to act upon.
     *
     * @param logger
     * @param machineDeltaTolerance
     *            The smallest machine instance delta fraction to act upon.
     */
    public MachineDeltaTolerancePolicy(Logger logger, double machineDeltaTolerance) {
        this.logger = logger;
        this.machineDeltaTolerance = machineDeltaTolerance;
    }

    @Override
    public Optional<Double> apply(Optional<PoolSizeSummary> poolSize, Optional<Double> prediction) {
        checkArgument(poolSize != null, "poolSize is null");
        checkArgument(prediction != null, "prediction is null");

        if (!prediction.isPresent()) {
            return Optional.empty();
        }

        if (!poolSize.isPresent()) {
            this.logger
                    .warn("cannot enforce scaling policy: current pool size " + "is unknown. accepting prediction ...");
            return prediction;
        }

        double currentDesiredSize = poolSize.get().getDesiredSize();
        double delta = currentDesiredSize - prediction.get();

        if (Math.abs(delta) <= this.machineDeltaTolerance) {
            return Optional.of(currentDesiredSize);
        }
        return prediction;
    }
}
