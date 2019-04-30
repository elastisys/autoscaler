package com.elastisys.autoscaler.core.metronome.impl.standard;

import static com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetric.COMPUTE_UNIT_PREDICTION;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;

import java.util.Optional;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.alerter.api.types.AlertTopics;
import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxyException;
import com.elastisys.autoscaler.core.metronome.impl.standard.config.StandardMetronomeConfig;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.autoscaler.core.prediction.api.PredictionException;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.util.exception.Stacktrace;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * A {@link Runnable} task used within the {@link StandardMetronome} that, for
 * every invocation of its {@link #run()} method, executes a single resize
 * iteration.
 * <p/>
 * A resize iteration is performed in the following steps:
 * <ol>
 * <li>An attempt is made to retrieve the size of the cloud pool.</li>
 * <li>Next, the service load at a specified point in the future (the horizon)
 * is predicted by the {@link PredictionSubsystem}. If the pool size was
 * successfully retrieved in the previous step, it will get passed as an
 * {@link Optional} parameter to the {@link PredictionSubsystem} (since some
 * {@link Predictor}s, such as reactive predictors, may need to make pool size
 * adjustments relative to the current size).</li>
 * <li>The prediction gets passed on to the {@link CloudPoolProxy} which sets
 * the new desired cloud pool size.</li>
 * </ol>
 *
 * @see StandardMetronome
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ResizeLoop implements Runnable {

    private final Logger logger;
    private final EventBus eventBus;

    /** Predicts future machine need. */
    private final PredictionSubsystem predictionSubsystem;
    /** Carries out resize actions. */
    private final CloudPoolProxy cloudPool;

    /** The configuration, which may be modified at runtime. */
    private StandardMetronomeConfig config;

    /** Contains fault details if the latest resize iteration failed. */
    private Optional<Throwable> lastFailure = Optional.empty();

    /**
     * Constructs a new {@link ResizeLoop}.
     *
     * @param logger
     * @param eventBus
     * @param timeSource
     * @param cloudPool
     * @param predictionSubsystem
     */
    public ResizeLoop(Logger logger, EventBus eventBus, CloudPoolProxy cloudPool,
            PredictionSubsystem predictionSubsystem) {
        this.logger = logger;
        this.eventBus = eventBus;
        this.cloudPool = cloudPool;
        this.predictionSubsystem = predictionSubsystem;
    }

    @Override
    public void run() {
        try {
            checkState(getConfig() != null, "cannot execute resize loop without configuration.");

            this.logger.debug("new resize iteration started");
            executeIteration();
        } catch (Throwable e) {
            String message = String.format("failed to execute resize iteration");
            String detail = String.format("%s: %s\n%s", message, e.getMessage(), Stacktrace.toString(e));
            this.eventBus.post(new Alert(AlertTopics.RESIZE_ITERATION_FAILURE.getTopicPath(), AlertSeverity.ERROR,
                    UtcTime.now(), message, detail));
            this.logger.error(detail);
            this.lastFailure = Optional.of(e);
        }
    }

    /**
     * Executes a resize operation and returns the compute-unit prediction (if
     * one could be produced).
     *
     * @return
     */
    private Optional<Integer> executeIteration() throws CloudPoolProxyException, PredictionException {
        // attempt to get cloud pool size
        Optional<PoolSizeSummary> poolSize = getPoolSizeOptionally();
        // predict

        DateTime predictionTime = UtcTime.now().plus(this.config.getHorizon().getMillis());
        Optional<Integer> computeUnitPrediction = this.predictionSubsystem.predict(poolSize, predictionTime);

        if (!computeUnitPrediction.isPresent()) {
            this.logger.warn("no pool size prediction produced by prediction subsystem");
        } else {
            this.logger.debug("prediction subsystem produced machine need prediction: {}", computeUnitPrediction);
            this.eventBus.post(new SystemMetricEvent(new MetricValue(COMPUTE_UNIT_PREDICTION.getMetricName(),
                    computeUnitPrediction.get(), UtcTime.now())));
        }

        // resize (unless we are running in log-only mode)
        if (computeUnitPrediction.isPresent() && !this.config.isLogOnly()) {
            this.cloudPool.setDesiredSize(computeUnitPrediction.get());
        }
        return computeUnitPrediction;
    }

    /**
     * Attempts to retrieve the current cloud pool size. In case of failure,
     * {@link Optional#empty()} is returned.
     *
     * @return
     */
    private Optional<PoolSizeSummary> getPoolSizeOptionally() {
        Optional<PoolSizeSummary> poolSize = Optional.empty();
        try {
            poolSize = Optional.ofNullable(this.cloudPool.getPoolSize());
        } catch (Exception e) {
            this.logger.warn("continuing with unknown cloud pool size:" + " could not be retrieved: {}", e.getMessage(),
                    e);
        }
        return poolSize;
    }

    public Optional<Throwable> getLastFailure() {
        return this.lastFailure;
    }

    public synchronized StandardMetronomeConfig getConfig() {
        return this.config;
    }

    public synchronized void setConfig(StandardMetronomeConfig config) {
        checkArgument(config != null, "resizeLoop: null configuration not allowed");
        this.config = config;
    }

}