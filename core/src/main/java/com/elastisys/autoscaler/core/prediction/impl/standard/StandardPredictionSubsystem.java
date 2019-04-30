package com.elastisys.autoscaler.core.prediction.impl.standard;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.alerter.api.types.AlertTopics;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.prediction.api.PredictionException;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;
import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.impl.standard.aggregator.Aggregator;
import com.elastisys.autoscaler.core.prediction.impl.standard.aggregator.AggregatorException;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.ScalingPolicy;
import com.elastisys.autoscaler.core.prediction.impl.standard.capacitylimit.CapacityLimitRegistry;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.StandardPredictionSubsystemConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.mapper.CapacityMapper;
import com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictorRegistry;
import com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy.ScalingPolicyEnforcer;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.util.exception.Stacktrace;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.name.Names;

/**
 * Standard {@link PredictionSubsystem} implementation.
 * <p/>
 * Internally, it makes use of a prediction pipeline that starts with a
 * collection of {@link Predictor}s, each of which predicts future capacity need
 * for a given metric (or directly in terms of compute units). The capacity
 * predictions are translated to compute units by a {@link CapacityMapper}. An
 * {@link Aggregator} takes care of producing a single <i>aggregate
 * prediction</i> from the set of compute unit predictions. After this, the set
 * of configured {@link ScalingPolicy}s are applied to the prediction with the
 * intent of reducing oscillations (prematurely scaling up/down the machine
 * pool), preventing excessive over-shooting, introducing damping/delay to
 * scaling decisions, etc. Finally, the currently active capacity limit (if any)
 * is applied to the aggregate prediction to produce a <i>bounded capacity</i>
 * (which is capped by the min and max limit of the capacity limit).
 *
 * @see Predictor
 */
@SuppressWarnings("rawtypes")
public class StandardPredictionSubsystem implements PredictionSubsystem<StandardPredictionSubsystemConfig> {

    private final Logger logger;
    private final EventBus eventBus;
    private final ScheduledExecutorService executorService;
    /**
     * {@link MonitoringSubsystem} used to set up metric stream subscriptions on
     * behalf of {@link Predictor}s.
     */
    private final MonitoringSubsystem monitoringSubsystem;
    private final File storageDir;

    private final PredictorRegistry predictorRegistry;
    private final CapacityMapper capacityMapper;
    private final Aggregator aggregator;
    private final ScalingPolicyEnforcer scalingPolicyEnforcer;
    private final CapacityLimitRegistry capacityLimitRegistry;

    /** Posts system metrics time-series values on the {@link EventBus}. */
    private final SystemMetricPoster systemMetricPoster;

    private final AtomicBoolean started;
    private Optional<StandardPredictionSubsystemConfig> config = Optional.empty();
    /** Contains fault details of the latest prediction run failure. */
    private Optional<Throwable> lastFailure = Optional.empty();

    @Inject
    public StandardPredictionSubsystem(Logger logger, EventBus bus, ScheduledExecutorService executorService,
            MonitoringSubsystem monitoringSubsystem, @Named("StorageDir") File storageDir) {
        this.logger = logger;
        this.eventBus = bus;
        this.executorService = executorService;
        this.monitoringSubsystem = monitoringSubsystem;
        this.storageDir = storageDir;

        this.predictorRegistry = instantiate(PredictorRegistry.class);
        this.capacityMapper = instantiate(CapacityMapper.class);
        this.aggregator = instantiate(Aggregator.class);
        this.scalingPolicyEnforcer = instantiate(ScalingPolicyEnforcer.class);
        this.capacityLimitRegistry = instantiate(CapacityLimitRegistry.class);
        this.systemMetricPoster = instantiate(SystemMetricPoster.class);

        this.started = new AtomicBoolean(false);
    }

    @Override
    public void validate(StandardPredictionSubsystemConfig config) throws IllegalArgumentException {
        checkArgument(config != null, "predictionSubsystem: config cannot be null");
        config.validate();
        try {
            this.predictorRegistry.validate(config.getPredictors());
            this.capacityMapper.validate(config.getCapacityMappings());
            this.aggregator.validate(config.getAggregator());
            this.scalingPolicyEnforcer.validate(config.getScalingPolicies());
            this.capacityLimitRegistry.validate(config.getCapacityLimits());
        } catch (Throwable e) {
            throw new IllegalArgumentException("predictionSubsystem: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void configure(StandardPredictionSubsystemConfig config) throws IllegalArgumentException {
        this.logger.debug("applying configuration ...");

        boolean needsRestart = isStarted();
        if (needsRestart) {
            stop();
        }

        apply(config);

        if (needsRestart) {
            start();
        }
    }

    @Override
    public synchronized void start() {
        checkState(this.config.isPresent(), "cannot start prediction subsystem without a configuration.");

        if (isStarted()) {
            return;
        }

        this.predictorRegistry.start();
        this.started.set(true);
        this.logger.info(getClass().getSimpleName() + " started.");
    }

    @Override
    public synchronized void stop() {
        if (!isStarted()) {
            return;
        }

        this.predictorRegistry.stop();
        this.started.set(false);
        this.logger.info(getClass().getSimpleName() + " stopped.");
    }

    private boolean isStarted() {
        return this.started.get();
    }

    @Override
    public ServiceStatus getStatus() {
        return new ServiceStatus.Builder().started(isStarted()).lastFault(this.lastFailure).build();
    }

    @Override
    public StandardPredictionSubsystemConfig getConfiguration() {
        return this.config.orElse(null);
    }

    @Override
    public Class<StandardPredictionSubsystemConfig> getConfigurationClass() {
        return StandardPredictionSubsystemConfig.class;
    }

    public PredictorRegistry getPredictorRegistry() {
        return this.predictorRegistry;
    }

    public CapacityMapper getCapacityMapper() {
        return this.capacityMapper;
    }

    public Aggregator getAggregator() {
        return this.aggregator;
    }

    public ScalingPolicyEnforcer getScalingPolicyEnforcer() {
        return this.scalingPolicyEnforcer;
    }

    public CapacityLimitRegistry getCapacityLimitRegistry() {
        return this.capacityLimitRegistry;
    }

    /**
     * Executes the prediction pipeline.
     *
     * @see PredictionSubsystem#predict(PoolSizeSummary, DateTime)
     */
    @Override
    public Optional<Integer> predict(Optional<PoolSizeSummary> poolSize, DateTime predictionTime)
            throws PredictionException {
        checkArgument(poolSize != null, "poolSizeSummary is null");
        checkArgument(predictionTime != null, "prediction time is null");
        checkState(isStarted(), "won't answer predictions when stopped");

        try {
            List<Predictor> predictors = this.predictorRegistry.getStartedPredictors();
            Map<Predictor, Future<Optional<Prediction>>> predictions = startPredictions(predictors, poolSize,
                    predictionTime);
            Map<Predictor, Optional<Prediction>> capacityPredictions = await(predictions);
            Map<Predictor, Optional<Prediction>> computeUnitPredictions = map(capacityPredictions);
            Optional<Double> aggregatePrediction = aggregate(computeUnitPredictions, predictionTime);
            this.logger.debug("Aggregate prediction (CU): {}", aggregatePrediction);

            if (!aggregatePrediction.isPresent() && !poolSize.isPresent()) {
                // if we don't have a prediction and pool size is unknown, we
                // keep pool at whatever size it is in
                this.logger.warn("no prediction was produced and pool size is "
                        + "unknown. pool size prediction set to absent.");
                return Optional.empty();
            }

            Optional<Double> policyBoundedPrediction = this.scalingPolicyEnforcer.apply(poolSize, aggregatePrediction);
            this.logger.debug("Policy-bounded prediction (CU): {}", policyBoundedPrediction);
            Optional<Integer> boundedPrediction = limit(policyBoundedPrediction, predictionTime, poolSize);
            this.logger.debug("Limit-bounded prediction (CU): {}", boundedPrediction);
            this.lastFailure = Optional.empty();
            return boundedPrediction;
        } catch (Throwable e) {
            String message = String.format("prediction pipeline failure");
            String detail = String.format("%s: %s\n%s", message, e.getMessage(), Stacktrace.toString(e));
            this.eventBus.post(new Alert(AlertTopics.PREDICTION_FAILURE.getTopicPath(), AlertSeverity.ERROR,
                    UtcTime.now(), message, detail));
            this.lastFailure = Optional.of(e);
            throw new PredictionException(detail, e);
        }
    }

    /**
     * Starts capacity prediction tasks for a collection of {@link Predictor}s.
     * Each prediction is executed as a separate sub-task (in a separate
     * {@link Thread}).
     *
     * @param predictors
     *            The {@link Predictor}s to execute.
     * @param poolSize
     *            The cloud pool size (both desired and actual). May be absent
     *            if the cloud pool size could not be determined.
     * @param predictionTime
     *            The time for which to predict capacity.
     * @return The prediction task execution handles (as {@link Future}
     *         objects).
     */
    private Map<Predictor, Future<Optional<Prediction>>> startPredictions(List<Predictor> predictors,
            Optional<PoolSizeSummary> poolSize, DateTime predictionTime) {
        Map<Predictor, Future<Optional<Prediction>>> predictions = new HashMap<>();
        for (Predictor predictor : predictors) {
            this.logger.debug("Launching predictor " + predictor.getConfiguration().getId());
            PredictionTask task = new PredictionTask(predictor, poolSize, predictionTime);
            predictions.put(predictor, this.executorService.submit(task));
        }
        return predictions;
    }

    /**
     * Awaits completion of a collection of capacity prediction tasks and
     * returns their results.
     *
     * @param predictionTasks
     *            The capacity prediction tasks to await.
     * @return The result of the capacity predictions.
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private Map<Predictor, Optional<Prediction>> await(Map<Predictor, Future<Optional<Prediction>>> predictionTasks)
            throws InterruptedException, ExecutionException {
        Map<Predictor, Optional<Prediction>> predictionResults = new HashMap<>();
        for (Entry<Predictor, Future<Optional<Prediction>>> task : predictionTasks.entrySet()) {
            Predictor predictor = task.getKey();
            // Note: any predictor error will be re-raised, effectively causing
            // the current prediction pipeline execution to be aborted. It is
            // better that we fail early/hard than risking more subtle errors
            // downstream.
            Optional<Prediction> result = task.getValue().get();
            predictionResults.put(predictor, result);
        }
        logPredictionResults(predictionResults);
        this.systemMetricPoster.postPredictionResults(predictionResults);
        return predictionResults;
    }

    private void logPredictionResults(Map<Predictor, Optional<Prediction>> predictionResults) {
        List<String> results = new ArrayList<>();
        for (Entry<Predictor, Optional<Prediction>> predictionResult : predictionResults.entrySet()) {
            Predictor predictor = predictionResult.getKey();
            Optional<Prediction> value = predictionResult.getValue();
            results.add(String.format("%s: %s", predictor.getConfiguration().getId(), value));
        }
        this.logger.debug("prediction results: [{}]", String.join(",", results));
    }

    /**
     * Performs capacity mapping on all capacity predictions, converting
     * predictions expressed in "raw" (metric) capacity to capacity in compute
     * units.
     * <p/>
     * Any predictions already expressed in compute unit are returned as-is.
     *
     * @param capacityPredictions
     *            A set of predictions.
     * @return The set of predictions, now mapped from raw metric unit to
     *         compute units.
     */
    private Map<Predictor, Optional<Prediction>> map(Map<Predictor, Optional<Prediction>> capacityPredictions) {
        Map<Predictor, Optional<Prediction>> computeUnitPredictions = new HashMap<>();
        for (Entry<Predictor, Optional<Prediction>> capacityPrediction : capacityPredictions.entrySet()) {
            Predictor predictor = capacityPrediction.getKey();
            Optional<Prediction> prediction = capacityPrediction.getValue();
            Optional<Prediction> computeUnits = this.capacityMapper.toComputeUnits(prediction);
            this.logger.debug("prediction (CU): " + computeUnits);
            computeUnitPredictions.put(predictor, computeUnits);
        }
        this.systemMetricPoster.postPredictionResults(computeUnitPredictions);
        return computeUnitPredictions;
    }

    /**
     * Aggregates a set of predictions into a single <i>aggregate prediction</i>
     * by applying an aggregation function. The aggregation function is applied
     * by the {@link Aggregator} configured with this
     * {@link StandardPredictionSubsystem}.
     *
     * @param computeUnitPredictions
     *            A set of capacity predictions expressed in compute units.
     * @param predictionTime
     *            The time for which the prediction was made.
     * @return The <i>aggregate prediction</i>.
     * @throws AggregatorException
     */
    private Optional<Double> aggregate(Map<Predictor, Optional<Prediction>> computeUnitPredictions,
            DateTime predictionTime) throws AggregatorException {
        Optional<Double> aggregatePrediction = this.aggregator.aggregate(computeUnitPredictions, predictionTime);
        this.systemMetricPoster.postAggregatePrediction(aggregatePrediction);
        return aggregatePrediction;
    }

    /**
     * Applies the currently active capacity limit (if any) to a prediction in
     * order to produce a <i>bounded prediction</i>.The capacity limit is
     * applied by the {@link CapacityLimitRegistry} configured with the
     * {@link StandardPredictionSubsystem}.
     * <p/>
     * If no aggregate prediction was received, the currently active capacity
     * limit (if any) is applied to bound the current pool size.
     *
     * @param poolSizePrediction
     *            The <i>aggregate prediction</i> that is to be bounded.
     * @param predictionTime
     *            The time for which capacity prediction is made.
     * @param currentPoolSize
     *            The size of the current machine pool (both desired and
     *            actual). May be absent if the cloud pool size could not be
     *            determined.
     * @return
     */
    private Optional<Integer> limit(Optional<Double> poolSizePrediction, DateTime predictionTime,
            Optional<PoolSizeSummary> currentPoolSize) {
        if (!poolSizePrediction.isPresent() && currentPoolSize.isPresent()) {
            // for the case where we don't have any prediction but we have a
            // known pool size, we use the current desired pool size as our
            // "prediction" and apply the active capacity limit (if any) to
            // bound it.
            int desiredSize = currentPoolSize.get().getDesiredSize();
            this.logger.info("prediction absent, applying capacity limit " + "to current desired pool size: {}",
                    desiredSize);
            return this.capacityLimitRegistry.limit(Optional.of(new Double(desiredSize)), predictionTime);
        }

        Optional<Integer> boundedPrediction = this.capacityLimitRegistry.limit(poolSizePrediction, predictionTime);
        this.systemMetricPoster.postBoundedPrediction(boundedPrediction);
        return boundedPrediction;
    }

    /**
     * Applies a configuration by dispatching sub-configurations to the
     * sub-components.
     *
     * @param config
     * @throws IllegalArgumenException
     */
    private void apply(StandardPredictionSubsystemConfig config) throws IllegalArgumentException {
        try {
            this.predictorRegistry.configure(config.getPredictors());
            this.capacityMapper.configure(config.getCapacityMappings());
            this.aggregator.configure(config.getAggregator());
            this.scalingPolicyEnforcer.configure(config.getScalingPolicies());
            this.capacityLimitRegistry.configure(config.getCapacityLimits());
            this.config = Optional.of(config);
        } catch (Exception e) {
            throw new IllegalArgumentException("predictionSubsystem: " + e.getMessage(), e);
        }
    }

    /**
     * A {@link Callable} task that drives the execution of a single
     * {@link Predictor}.
     */
    private static class PredictionTask implements Callable<Optional<Prediction>> {

        private final Predictor predictor;
        private final Optional<PoolSizeSummary> poolSize;
        private final DateTime predictionTime;

        public PredictionTask(Predictor predictor, Optional<PoolSizeSummary> poolSize, DateTime predictionTime) {
            this.predictor = predictor;
            this.poolSize = poolSize;
            this.predictionTime = predictionTime;
        }

        @Override
        public Optional<Prediction> call() throws Exception {
            return this.predictor.predict(this.poolSize, this.predictionTime);
        }
    }

    /**
     * Wires up an instance of the given class according to the Guice bindings
     * specified in the {@link PredictionSubsystemModule}.
     *
     * @param componentClass
     *            The class to be instantiated.
     * @return An instance of the class, wired up with collaborators as
     *         specified in the {@link PredictionSubsystemModule}.
     */
    private <T> T instantiate(Class<T> componentClass) {
        return Guice.createInjector(new PredictionSubsystemModule()).getInstance(componentClass);
    }

    /**
     * Guice {@link Module} that specifies dependency bindings for the
     * {@link StandardPredictionSubsystem} and any components it instantiates.
     */
    private class PredictionSubsystemModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(Logger.class).toInstance(StandardPredictionSubsystem.this.logger);
            bind(EventBus.class).toInstance(StandardPredictionSubsystem.this.eventBus);
            bind(ScheduledExecutorService.class).toInstance(StandardPredictionSubsystem.this.executorService);
            bind(ExecutorService.class).toInstance(StandardPredictionSubsystem.this.executorService);
            bind(MonitoringSubsystem.class).toInstance(StandardPredictionSubsystem.this.monitoringSubsystem);
            // Value for File parameter with @Name("StorageDir")
            bind(File.class).annotatedWith(Names.named("StorageDir"))
                    .toInstance(StandardPredictionSubsystem.this.storageDir);
        }
    }
}
