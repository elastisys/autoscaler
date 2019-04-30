package com.elastisys.autoscaler.core.prediction.impl.standard.predictor;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;

import java.util.Optional;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.CorePredicates;
import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.reader.MetricStreamReader;
import com.elastisys.autoscaler.core.prediction.api.PredictionException;
import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.util.precond.Preconditions;

/**
 * A {@link Predictor} base class that provides sensible default behavior for
 * most boilerplate code such as the {@link Service} life-cycle. Concrete
 * subclasses must only provide methods for {@link Predictor}-specific behavior.
 * <p/>
 * Subclasses must implement the following abstract methods:
 * <ul>
 * <li>{@link #validateConfig}: validate implementation-specific configuration
 * for the {@link Predictor} subclass.</li>
 * <li>{@link #applyConfig}: apply implementation-specific configuration for the
 * {@link Predictor} subclass.</li>
 * <li>{@link #onStart}: Perform any subclass-specific work that needs to be
 * performed when the {@link Predictor} starts. This could, for example, include
 * querying the {@link MetricStream} for historical data to fill internal data
 * structures before the first prediction is requested.</li>
 * <li>{@link #onStop}: Perform any subclass-specific work that needs to be
 * performed when the {@link Predictor} stops. This could, for example, include
 * stopping to listen for incoming values on the {@link MetricStream}.</li>
 * <li>{@link #doPrediction}: called when the autoscaler requests a
 * prediction.</li>
 * </ul>
 *
 * @see MetricStreamReader
 */
public abstract class AbstractPredictor implements Predictor {

    /** {@link Logger} instance to use. */
    protected final Logger logger;
    /**
     * The {@link EventBus} on which to listen for {@link MetricStreamMessage}s
     * from the configured {@link MetricStream}.
     */
    private final EventBus eventBus;

    /**
     * The {@link MonitoringSubsystem}, from which the {@link MetricStream} that
     * supplies this {@link Predictor} with {@link MetricValue}s can be
     * retrieved.
     */
    private final MonitoringSubsystem<?> monitoringSubsystem;

    /** The configuration set for this {@link Predictor}. */
    private PredictorConfig config;

    /** <code>true</code> if this {@link Predictor} is started. */
    private boolean started;
    /** Holds the latest failure (if any). */
    private Optional<Throwable> lastFailure = Optional.empty();

    /** Used to prevent concurrent updates to predictor state. */
    private Object lock = new Object();

    /**
     * Creates an {@link AbstractPredictor}.
     *
     * @param logger
     *            {@link Logger} instance to use.
     * @param eventBus
     *            The {@link EventBus} on which to listen for
     *            {@link MetricStreamMessage}s from the configured
     *            {@link MetricStream}.
     * @param monitoringSubsystem
     *            The {@link MonitoringSubsystem}, from which the
     *            {@link MetricStream} that supplies this {@link Predictor} with
     *            {@link MetricValue}s can be retrieved.
     */
    @Inject
    public AbstractPredictor(Logger logger, EventBus eventBus, MonitoringSubsystem monitoringSubsystem) {
        this.logger = logger;
        this.eventBus = eventBus;
        this.monitoringSubsystem = monitoringSubsystem;

        this.config = null;
        this.started = false;
    }

    @Override
    public final void validate(PredictorConfig configuration) throws IllegalArgumentException {
        Preconditions.checkArgument(configuration != null, "predictor: configuration cannot be null");
        // validate general part of config
        configuration.validate();
        // sub-class validation of config
        validateConfig(configuration);
    }

    @Override
    public final void configure(PredictorConfig newConfig) throws IllegalArgumentException {
        validate(newConfig);

        synchronized (this.lock) {
            boolean wasStarted = isStarted();
            if (wasStarted) {
                stop();
            }

            applyConfig(newConfig);
            this.config = newConfig;

            if (wasStarted && newConfig.getState() == State.STARTED) {
                start();
            }
        }
    }

    @Override
    public final PredictorConfig getConfiguration() {
        return this.config;
    }

    @Override
    public final Class<PredictorConfig> getConfigurationClass() {
        return PredictorConfig.class;
    }

    @Override
    public final void start() {
        checkState(isConfigured(), "attempt to start predictor without configuration");

        if (isStarted()) {
            this.logger.debug("{} ignored start request, already started.", getId());
            return;
        }

        MetricStreamReader metricReader = new MetricStreamReader(getEventBus(), getMetricStream());
        onStart(metricReader);
        this.started = true;
        this.logger.info(getClass().getSimpleName() + " started.");
    }

    @Override
    public final void stop() {
        if (!isStarted()) {
            this.logger.debug("{} ignored stop request, already stopped.", getId());
            return;
        }
        onStop();
        this.started = false;
        this.logger.info(getClass().getSimpleName() + " stopped.");
    }

    @Override
    public final ServiceStatus getStatus() {
        return new ServiceStatus.Builder().started(isStarted()).lastFault(this.lastFailure).build();
    }

    /**
     * Returns <code>true</code> if this {@link Predictor} is started.
     *
     * @return
     */
    public boolean isStarted() {
        return this.started;
    }

    /**
     * Returns <code>true</code> if this {@link Predictor} is configured.
     *
     * @return
     */
    public boolean isConfigured() {
        return CorePredicates.isConfigured().test(this);
    }

    private String getId() {
        return getConfiguration().getId();
    }

    @Override
    public final Optional<Prediction> predict(Optional<PoolSizeSummary> poolSize, DateTime predictionTime)
            throws PredictionException {
        checkState(isConfigured(), "cannot predict before being configured");
        checkState(isStarted(), "cannot predict before being started");

        this.logger.debug("{} asked to predict for time {} for machine pool of size {}", getId(), predictionTime,
                poolSize);
        try {
            // delay prediction while predictor is being updated/restarted
            synchronized (this.lock) {
                Optional<Prediction> prediction = doPrediction(poolSize, predictionTime);
                this.lastFailure = Optional.empty();
                return prediction;
            }
        } catch (Throwable e) {
            this.lastFailure = Optional.of(e);
            throw new PredictionException(e);
        }
    }

    /**
     * Returns the {@link MetricStream} that this {@link Predictor} is
     * configured to read metrics from.
     *
     * @return
     */
    private MetricStream getMetricStream() {
        checkState(isConfigured(), "attempt to get metric stream before being configured");

        String metricStreamId = this.config.getMetricStream();
        for (MetricStreamer<?> metricStreamer : this.monitoringSubsystem.getMetricStreamers()) {
            try {
                return metricStreamer.getMetricStream(metricStreamId);
            } catch (IllegalArgumentException e) {
                // not found, try next
            }
        }
        throw new IllegalArgumentException(String.format("no metric stream with id %s was found", metricStreamId));
    }

    /**
     * Returns the {@link AutoScaler} {@link EventBus} from which
     * {@link MetricStream} values can consumed (by listening for
     * {@link MetricStreamMessage}s with the right {@link MetricStream} id).
     *
     * @return
     */
    public EventBus getEventBus() {
        return this.eventBus;
    }

    /**
     * Called to let the subclass perform any additional validation of the
     * configuration.
     * <p/>
     * All subclasses of {@link AbstractPredictor} should provide an
     * implementation of this method.
     *
     * @param configuration
     * @throws IllegalArgumentException
     *             Thrown if validation fails.
     */
    public abstract void validateConfig(PredictorConfig configuration) throws IllegalArgumentException;

    /**
     * Called to let the subclass perform any additional work involved in
     * applying the configuration.
     * <p/>
     * All subclasses of {@link AbstractPredictor} should provide an
     * implementation of this method.
     *
     * @param newConfig
     *            The new {@link PredictorConfig}.
     * @throws IllegalArgumentException
     *             Thrown if the configuration cannot be applied.
     */
    public abstract void applyConfig(PredictorConfig newConfig) throws IllegalArgumentException;

    /**
     * Performs any subclass-specific work that needs to be performed when the
     * {@link Predictor} starts. This method must complete and return to its
     * caller in a timely manner. If this {@link Predictor} needs to perform any
     * long-running tasks, these should be started in a separate thread of
     * execution.
     * <p/>
     * This typically includes starting to consume values from the
     * {@link MetricStream} that the {@link Predictor} has been configured to
     * use. For this purpose, a {@link MetricStreamReader} is passed as part of
     * the call. It is set up with the right {@link MetricStream} but needs to
     * be started before use.
     * <p/>
     * This could also include querying the {@link MetricStream} for historical
     * data to fill internal data structures before the first prediction is
     * requested.
     *
     * @param metricReader
     *            A {@link MetricStreamReader} set up to listen to the
     *            {@link MetricStream} that the {@link Predictor} is configured
     *            to use.<i>Note: the {@link MetricStreamReader} is in a stopped
     *            state. The sub-class is responsible for starting it.</i>
     */
    public abstract void onStart(MetricStreamReader metricReader);

    /**
     * Performs any subclass-specific work that needs to be performed when the
     * {@link Predictor} stops.
     * <p/>
     * This could includes stopping to consume values from the
     * {@link MetricStreamReader}.
     *
     * @param metricReader
     *            A {@link MetricStreamReader} set up to listen to the
     *            {@link MetricStream} that the {@link Predictor} is configured
     *            to use.<i>Note: the {@link MetricStreamReader} is in a stopped
     *            state. The sub-class is responsible for starting it.</i>
     */
    public abstract void onStop();

    /**
     * Predicts the capacity need with respect to the monitored metric at a
     * point in the (near-time) future.
     *
     * @param poolSize
     *            The present pool size (both desired and actual) of the
     *            auto-scaled machine pool. May be absent if the current pool
     *            size is unknown. The <i>effective size</i> of the machine pool
     *            should be interpreted as the number of active machines.
     * @param predictionTime
     *            The point in time for which to predict the capacity need.
     * @return The capacity need with respect to the monitored metric at time
     *         {@code predictionTime}. If the prediction cannot be performed the
     *         return value may be absent.
     * @throws PredictionException
     */
    public abstract Optional<Prediction> doPrediction(Optional<PoolSizeSummary> poolSize, DateTime predictionTime)
            throws PredictionException;
}
