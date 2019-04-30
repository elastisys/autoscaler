package com.elastisys.autoscaler.core.prediction.api;

import java.util.Optional;

import org.joda.time.DateTime;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;

/**
 * An {@link AutoScaler} subsystem responsible for carrying out pool size
 * predictions, which determine the pool size needed to accommodate projected
 * future load on the service.
 *
 * @param <T>
 *            The configuration type.
 */
public interface PredictionSubsystem<T> extends Service<T> {
    /**
     * Predicts the pool size at a point in the (near-time) future.
     *
     * @param poolSize
     *            The present size (both desired and actual size) of the cloud
     *            pool. May be absent if the cloud pool size could not be
     *            determined. The {@link PredictionSubsystem} may choose to not
     *            produce a prediction if it needs to know the current pool
     *            size.
     * @param predictionTime
     *            The point in time for which to predict machine need.
     * @return The desired machine pool size at time {@code predictionTime}. If
     *         the prediction cannot be performed the return value may be
     *         absent.
     * @throws PredictionException
     *             Thrown if an unexpected error prevented a prediction from
     *             being produced.
     */
    public Optional<Integer> predict(Optional<PoolSizeSummary> poolSize, DateTime predictionTime)
            throws PredictionException;
}
