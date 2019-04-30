package com.elastisys.autoscaler.core.prediction.impl.standard.api;

import java.util.Optional;

import com.elastisys.autoscaler.core.prediction.impl.standard.StandardPredictionSubsystem;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;

/**
 * A {@link ScalingPolicy} decides whether or not to accept machine need
 * predictions. When applied to a prediction, it may choose either to accept the
 * given prediction or to suppress/overrule the prediction.
 * <p/>
 * In a little more detail, a {@link ScalingPolicy} is applied to each machine
 * need <i>aggregate prediction</i> produced by the
 * {@link StandardPredictionSubsystem}. The {@link ScalingPolicy} may choose to
 * overrule the suggested machine need prediction, for example, with the intent
 * of reducing oscillations (prematurely scaling up/down the machine pool),
 * preventing excessive over-shooting, introducing damping/delay to scaling
 * decisions, etc.
 *
 * @see StandardPredictionSubsystem
 * @see Predictor
 */
public interface ScalingPolicy {
    /**
     * Applies this {@link ScalingPolicy} to a machine need prediction. The
     * {@link ScalingPolicy} may choose to accept the prediction as-is, or it
     * may decide to overrule the {@link Predictor}-suggested machine need
     * prediction, for example, with the intent of reducing oscillations
     * (prematurely scaling up/down the machine pool).
     *
     * @param poolSize
     *            The present size (both desired and actual size) of the cloud
     *            pool. May be absent if the cloud pool size could not be
     *            determined. The <i>effective size</i> of the machine pool
     *            should be interpreted as the number of active machines.
     * @param poolSizePrediction
     *            The machine need prediction to act on.
     * @return The machine need prediction after applying this policy.
     */
    public Optional<Double> apply(Optional<PoolSizeSummary> poolSize, Optional<Double> poolSizePrediction);
}
