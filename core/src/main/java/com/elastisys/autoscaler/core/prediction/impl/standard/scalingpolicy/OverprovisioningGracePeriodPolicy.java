package com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Optional;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.prediction.impl.standard.api.ScalingPolicy;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * A {@link ScalingPolicy} that disproves any scale-down suggestion unless the
 * prediction has been observed for a sufficiently long time.
 * <p/>
 * Purpose: prevent premature scale-down (which can lead to oscillating
 * behavior) on temporary decreases in load.
 */
public class OverprovisioningGracePeriodPolicy implements ScalingPolicy {

    private final Logger logger;
    /**
     * The time period that a scale-down prediction (suggesting that the machine
     * pool is over-provisioned) needs to be observed before being acted upon. A
     * scale-down prediction will not be passed through unless a longer period
     * than this of constant over-provisioning has been observed.
     * <p/>
     * Purpose: prevent premature scale-down (which can lead to oscillating
     * behavior) on temporary decreases in load.
     */
    private final TimeInterval overprovisioningGracePeriod;

    /**
     * The point in time on which the first scale-down suggestion was received.
     * This field will only be set when an uninterrupted sequence of scale-down
     * predictions have been received. Whenever a scale-up prediction is
     * received, this time stamp is reset.
     */
    private Optional<DateTime> firstScaleDownPrediction;

    public OverprovisioningGracePeriodPolicy(Logger logger, TimeInterval overprovisioningGracePeriod) {
        this.logger = logger;
        this.overprovisioningGracePeriod = overprovisioningGracePeriod;
        this.firstScaleDownPrediction = Optional.empty();
    }

    @Override
    public Optional<Double> apply(Optional<PoolSizeSummary> poolSize, Optional<Double> prediction) {
        checkArgument(poolSize != null, "poolSize is null");
        checkArgument(prediction != null, "predictedMachineNeed is null");

        if (!prediction.isPresent()) {
            resetFirstScaleDownTime();
            return prediction;
        }

        if (!poolSize.isPresent()) {
            this.logger
                    .warn("cannot enforce scaling policy: current pool size " + "is unknown. accepting prediction ...");
            return prediction;
        }

        if (!isScaleDown(poolSize.get(), prediction.get())) {
            resetFirstScaleDownTime();
            return prediction;
        }

        DateTime currentTime = UtcTime.now();
        if (!this.firstScaleDownPrediction.isPresent()) {
            this.firstScaleDownPrediction = Optional.of(currentTime);
            this.logger.debug("noting first scale-down prediction at {}", currentTime);
        }

        long overprovisioningPeriod = new Duration(this.firstScaleDownPrediction.get(), currentTime)
                .getStandardSeconds();
        if (overprovisioningPeriod < this.overprovisioningGracePeriod.getSeconds()) {
            // not overprovisioned sufficiently long: return current desired
            // pool size
            this.logger.debug(
                    "consistently overprovisioned for {} seconds, won't allow scale-down until after {} seconds",
                    overprovisioningPeriod, this.overprovisioningGracePeriod);
            double currentDesiredSize = poolSize.get().getDesiredSize();
            return Optional.of(currentDesiredSize);
        }
        // allow scale-down decision to pass
        return prediction;
    }

    private void resetFirstScaleDownTime() {
        this.firstScaleDownPrediction = Optional.empty();
    }

    /**
     * Returns <code>true</code> if the prediction is a scale-down suggestion
     * (that is, the ceiling of the prediction is smaller than the current
     * desired pool size).
     *
     * @param poolSize
     * @param poolSizePrediction
     * @return
     */
    private boolean isScaleDown(PoolSizeSummary poolSize, double poolSizePrediction) {
        int predictedMachineNeed = (int) Math.ceil(poolSizePrediction);
        int desiredSize = poolSize.getDesiredSize();
        int delta = predictedMachineNeed - desiredSize;
        return delta < 0;
    }

    public Optional<DateTime> getFirstScaleDownPrediction() {
        return this.firstScaleDownPrediction;
    }
}
