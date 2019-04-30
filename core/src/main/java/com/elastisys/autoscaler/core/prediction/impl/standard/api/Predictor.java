package com.elastisys.autoscaler.core.prediction.impl.standard.api;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.prediction.api.PredictionException;
import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.impl.standard.StandardPredictionSubsystem;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;

/**
 * A {@link Predictor} is a component of the {@link StandardPredictionSubsystem}
 * that predicts the future capacity need with respect to a given metric, based
 * on the historical values observed for that metric.
 * <p/>
 * In the {@link AutoScaler}, a {@link MetricStreamer} is responsible for
 * collecting metric values from metric sources. Each such collected metric
 * forms a separate {@link MetricStream}, whose values are broadcast (with a
 * unique identifier) onto the {@link AutoScaler} {@link EventBus}. A
 * {@link Predictor} may choose to consume a given {@link MetricStream} by
 * listening to the {@link EventBus} for {@link MetricStreamMessage}s that
 * originate from the given {@link MetricStream}.
 * <p/>
 * If the {@link Predictor} needs a long tail of historical metric data to build
 * its internal data structures, it can query the {@link MetricStream} of
 * interest on startup.
 * <p/>
 * On a regular basis the {@link Predictor} will be asked to make a capacity
 * need prediction based on the monitored metric for a time in the (near-time)
 * future.
 * <p/>
 * It is recommended that a {@link Predictor} does not use its own logger,
 * thread pool, etc. Instead, {@link Predictor} implementation classes can make
 * use of the {@link javax.inject.Inject} annotation to have certain
 * dependencies injected. See
 * <a href="http://code.google.com/p/google-guice/wiki/Injections">this page</a>
 * for a summary of the supported types of dependency injection patterns.
 * <p/>
 * The following dependencies can be injected through the
 * {@link javax.inject.Inject} annotation:
 * <ul>
 * <li>{@link Logger}: the {@link AutoScaler} instance's logger</li>
 * <li>{@link ExecutorService} or {@link ScheduledExecutorService}: the
 * {@link AutoScaler} instance's executor service.</li>
 * <li>{@link EventBus}: the {@link AutoScaler}'s event bus, which can be used
 * to subscribe to a certain {@link MetricStream}.</li>
 * </ul>
 *
 * @see Prediction
 * @see StandardPredictionSubsystem
 */
public interface Predictor extends Service<PredictorConfig> {

    /**
     * Predicts the capacity need with respect to the monitored metric at a
     * point in the (near-time) future.
     *
     * @param poolSize
     *            The present size (both desired and actual size) of the cloud
     *            pool. Note that this value may be absent, for example in case
     *            the cloud pool failed to respond. The {@link Predictor} may
     *            choose to not produce a prediction if it needs to know the
     *            current pool size.
     * @param predictionTime
     *            The point in time for which to predict the capacity need.
     * @return The capacity need with respect to the monitored metric at time
     *         {@code predictionTime}. If the prediction cannot be performed the
     *         return value may be absent.
     * @throws PredictionException
     */
    public Optional<Prediction> predict(Optional<PoolSizeSummary> poolSize, DateTime predictionTime)
            throws PredictionException;
}
