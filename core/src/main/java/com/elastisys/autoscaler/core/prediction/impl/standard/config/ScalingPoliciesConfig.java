package com.elastisys.autoscaler.core.prediction.impl.standard.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.autoscaler.core.prediction.impl.standard.StandardPredictionSubsystem;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.ScalingPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * A {@link ScalingPoliciesConfig} configures the {@link ScalingPolicy}s to
 * apply to the aggregate predictions produced by the
 * {@link StandardPredictionSubsystem}.
 * <p/>
 * Scaling policies are applied, for example, with the intent of reducing
 * oscillations (prematurely scaling up/down the machine pool), preventing
 * excessive over-shooting, introducing damping/delay to scaling decisions, etc.
 */
public class ScalingPoliciesConfig {

    /** Default value for {@link #machineDeltaTolerance}. */
    public static final double DEFAULT_MACHINE_DELTA_TOLERANCE = 0.0;
    /** Default value for {@link #overprovisioningGracePeriod}. */
    public static final TimeInterval DEFAULT_OVERPROVISIONING_GRACE_PERIOD = TimeInterval.seconds(0);

    /**
     * The smallest machine instance delta fraction to act upon.
     * <p/>
     * If the difference between the current machine pool size and a predicted
     * machine need is smaller than this, the prediction is ignored (and the
     * current pool size is kept).
     * <p/>
     * Purpose: reduce oscillations by not reacting too quickly to small changes
     * in predicted machine demand.
     * <p/>
     * May be <code>null</code>. Default:
     * {@link #DEFAULT_MACHINE_DELTA_TOLERANCE}.
     */
    private final Double machineDeltaTolerance;

    /**
     * The time period that a scale-down prediction (suggesting that the machine
     * pool is over-provisioned) needs to be observed before being acted upon. A
     * scale-down prediction will not be passed through unless a longer period
     * than this of constant over-provisioning has been observed.
     * <p/>
     * Purpose: prevent premature scale-down (which can lead to oscillating
     * behavior) on temporary decreases in load.
     * <p/>
     * May be <code>null</code>. Default:
     * {@link #DEFAULT_OVERPROVISIONING_GRACE_PERIOD}.
     */
    private final TimeInterval overprovisioningGracePeriod;

    /**
     * Constructs a new {@link ScalingPoliciesConfig}.
     *
     * @param machineDeltaTolerance
     *            The smallest machine instance delta fraction to act upon.
     *            <p/>
     *            If the difference between the current machine pool size and a
     *            predicted machine need is smaller than this, the prediction is
     *            ignored (and the current pool size is kept).
     *            <p/>
     *            Purpose: reduce oscillations by not reacting too quickly to
     *            small changes in predicted machine demand.
     *            <p/>
     *            May be <code>null</code>. Default:
     *            {@link #DEFAULT_MACHINE_DELTA_TOLERANCE}.
     * @param overprovisioningGracePeriod
     *            The time period that a scale-down prediction (suggesting that
     *            the machine pool is over-provisioned) needs to be observed
     *            before being acted upon. A scale-down prediction will not be
     *            passed through unless a longer period than this of constant
     *            over-provisioning has been observed.
     *            <p/>
     *            Purpose: prevent premature scale-down (which can lead to
     *            oscillating behavior) on temporary decreases in load.
     *            <p/>
     *            May be <code>null</code>. Default:
     *            {@link #DEFAULT_OVERPROVISIONING_GRACE_PERIOD}.
     */
    public ScalingPoliciesConfig(Double machineDeltaTolerance, TimeInterval overprovisioningGracePeriod) {
        this.machineDeltaTolerance = machineDeltaTolerance;
        this.overprovisioningGracePeriod = overprovisioningGracePeriod;
    }

    /**
     * The smallest machine instance delta fraction to act upon.
     * <p/>
     * If the difference between the current machine pool size and a predicted
     * machine need is smaller than this, the prediction is ignored (and the
     * current pool size is kept).
     * <p/>
     * Purpose: reduce oscillations by not reacting too quickly to small changes
     * in predicted machine demand.
     *
     * @return
     */
    public double getMachineDeltaTolerance() {
        return Optional.ofNullable(this.machineDeltaTolerance).orElse(DEFAULT_MACHINE_DELTA_TOLERANCE);
    }

    /**
     * The time period that a scale-down prediction (suggesting that the machine
     * pool is over-provisioned) needs to be observed before being acted upon. A
     * scale-down prediction will not be passed through unless a longer period
     * than this of constant over-provisioning has been observed.
     * <p/>
     * Purpose: prevent premature scale-down (which can lead to oscillating
     * behavior) on temporary decreases in load.
     *
     * @return
     */
    public TimeInterval getOverprovisioningGracePeriod() {
        return Optional.ofNullable(this.overprovisioningGracePeriod).orElse(DEFAULT_OVERPROVISIONING_GRACE_PERIOD);
    }

    /**
     * Validates this {@link ScalingPoliciesConfig}. Returns to the called on
     * success. Throws a {@link IllegalArgumentException} on validation failure.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(getMachineDeltaTolerance() >= 0.0, "scalingPolicies: machineDeltaTolerance must be >= 0.0");
        checkArgument(getOverprovisioningGracePeriod().getSeconds() >= 0,
                "scalingPolicies: overprovisioningGracePeriod must be a positive duration");
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.machineDeltaTolerance, this.overprovisioningGracePeriod);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScalingPoliciesConfig) {
            ScalingPoliciesConfig that = (ScalingPoliciesConfig) obj;
            return Objects.equals(this.machineDeltaTolerance, that.machineDeltaTolerance)
                    && Objects.equals(this.overprovisioningGracePeriod, that.overprovisioningGracePeriod);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
