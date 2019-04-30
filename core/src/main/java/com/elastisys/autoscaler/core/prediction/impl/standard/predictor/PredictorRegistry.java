package com.elastisys.autoscaler.core.prediction.impl.standard.predictor;

import static com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictorPredicates.withConfigIn;
import static com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictorPredicates.withId;
import static com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictorPredicates.withIdIn;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.prediction.impl.standard.StandardPredictionSubsystem;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.util.precond.Preconditions;
import com.google.inject.TypeLiteral;

/**
 * The {@link PredictorRegistry} is a sub-component of the
 * {@link StandardPredictionSubsystem} that manages the collection of configured
 * {@link Predictor}s. It makes sure that the set of {@link Predictor}s always
 * is in sync with the most recent configuration received from the
 * {@link StandardPredictionSubsystem}.
 *
 * @see StandardPredictionSubsystem
 *
 */
@SuppressWarnings("rawtypes")
public class PredictorRegistry implements Service<List<PredictorConfig>> {
    private final Logger logger;
    /** Used to set up metric stream subscriptions for {@link Predictor}s. */
    private final MonitoringSubsystem<?> monitoringSubsystem;

    /** Tracks the current set of configured {@link Predictor}s. */
    private final List<Predictor> predictors = new CopyOnWriteArrayList<Predictor>();

    private List<PredictorConfig> config = new CopyOnWriteArrayList<>();

    private PredictorFactory predictorFactory;
    private final AtomicBoolean started;

    @Inject
    public PredictorRegistry(Logger logger, EventBus eventBus, ScheduledExecutorService executorService,
            MonitoringSubsystem monitoringSubsystem, @Named("StorageDir") File storageDir) {
        this.logger = logger;
        this.predictorFactory = new PredictorFactory(logger, eventBus, executorService, monitoringSubsystem,
                storageDir);
        this.monitoringSubsystem = monitoringSubsystem;

        this.started = new AtomicBoolean(false);
    }

    @Override
    public void validate(List<PredictorConfig> configuration) throws IllegalArgumentException {
        Preconditions.checkArgument(configuration != null, "predictors: null configuration not allowed");
        // validate each configuration by verifying that the predictor can be
        // instantiated and that predictor configuration is valid
        for (PredictorConfig predictorConfig : configuration) {
            try {
                predictorConfig.validate();
                Predictor predictorInstance = this.predictorFactory.create(predictorConfig.getType());
                predictorInstance.validate(predictorConfig);

                // note: we do not validate the existence of referenced metric
                // streams at this point, since this check is carried out when
                // the autoscaler performs a "test-apply" of a new configuration
                // on an autoscaler clone (see the configure method)
            } catch (Exception e) {
                throw new IllegalArgumentException(format("predictors: predictor '%s' (type %s): %s",
                        predictorConfig.getId(), predictorConfig.getType(), e.getMessage()), e);
            }
        }
    }

    /**
     * Ensure that all configured {@link Predictor}s refer to
     * {@link MetricStream}s that actually exist and are published by the
     * {@link MetricStreamer} or throw an exception.
     *
     * @param predictorConfigs
     * @throws UnknownMetricStreamIdException
     */
    private void validateMetricStreamExistence(List<PredictorConfig> predictorConfigs)
            throws UnknownMetricStreamIdException {
        for (PredictorConfig config : predictorConfigs) {
            String metricStreamId = config.getMetricStream();
            try {
                findOriginMetricStream(metricStreamId);
            } catch (NoSuchElementException e) {
                throw new UnknownMetricStreamIdException(
                        String.format("metric stream '%s' is not published by the metric streamer", metricStreamId));
            }
        }
    }

    @Override
    public void configure(List<PredictorConfig> predictorConfigs) throws IllegalArgumentException {
        validate(predictorConfigs);
        validateMetricStreamExistence(predictorConfigs);

        boolean needsRestart = isStarted();

        if (needsRestart) {
            stop();
        }

        apply(predictorConfigs);

        if (needsRestart) {
            start();
        }
    }

    private void apply(List<PredictorConfig> newConfiguration) throws IllegalArgumentException {
        PredictorRegistryConfigDiff configDiff = new PredictorRegistryConfigDiff(this.config, newConfiguration);
        if (!configDiff.isDifferent()) {
            this.logger.info("Ignoring received configuration, " + "since it is equal to the existing one.");
            return;
        }

        try {
            addPredictors(configDiff.added());
            deletePredictors(configDiff.deleted());
            modifyPredictors(configDiff.modified());
        } catch (Exception e) {
            throw new IllegalArgumentException("predictors: " + e.getMessage(), e);
        }
        this.config = new CopyOnWriteArrayList<>(newConfiguration);

    }

    @Override
    public void start() {
        requireNonNull(this.config, "attempt to start without configuration");

        if (isStarted()) {
            return;
        }

        // start any predictors configured to be in started state
        for (Predictor predictor : this.predictors) {
            PredictorConfig predictorConf = predictor.getConfiguration();
            if (predictorConf.getState() == State.STARTED) {
                this.logger.debug("starting predictor {}", predictorConf.getId());
                predictor.start();
            }
        }
        this.started.set(true);
    }

    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }

        // stop all predictors
        for (Predictor predictor : this.predictors) {
            String id = predictor.getConfiguration().getId();
            this.logger.debug("stopping predictor {}", id);
            predictor.stop();
        }
        this.started.set(false);
    }

    @Override
    public ServiceStatus getStatus() {
        return new ServiceStatus.Builder().started(isStarted()).build();
    }

    private boolean isStarted() {
        return this.started.get();
    }

    private void addPredictors(List<PredictorConfig> addedConfigs) throws IllegalArgumentException {
        for (PredictorConfig predictorConfig : addedConfigs) {
            addPredictor(predictorConfig);
        }
    }

    private void addPredictor(PredictorConfig predictorConfig) throws IllegalArgumentException {
        this.logger.debug("adding predictor: {}", predictorConfig);

        Predictor predictorInstance = this.predictorFactory.create(predictorConfig.getType());
        predictorInstance.configure(predictorConfig);
        this.predictors.add(predictorInstance);
    }

    private void deletePredictors(List<PredictorConfig> deletedConfigs) {
        this.predictors.stream().filter(withConfigIn(deletedConfigs)).forEach(p -> {
            deletePredictor(p);
        });
    }

    private void deletePredictor(Predictor predictor) {
        this.logger.debug("deleting predictor: {}", predictor.getConfiguration().getId());
        if (!this.predictors.remove(predictor)) {
            throw new IllegalStateException(format("failed to delete predictor: %s", predictor.getConfiguration()));
        }
        predictor.stop();
    }

    private void modifyPredictors(List<PredictorConfig> modifiedConfigs) throws IllegalArgumentException {
        for (PredictorConfig modifiedConfig : modifiedConfigs) {
            String predictorId = modifiedConfig.getId();

            Optional<Predictor> predictor = this.predictors.stream().filter(withId(predictorId)).findAny();
            if (!predictor.isPresent()) {
                throw new IllegalStateException("could not find predictor with id " + predictorId);
            }
            this.logger.debug("updating configuration for predictor {} to {}", predictorId, modifiedConfig);
            predictor.get().configure(modifiedConfig);
        }
    }

    @Override
    public List<PredictorConfig> getConfiguration() {
        return this.config;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<List<PredictorConfig>> getConfigurationClass() {
        TypeLiteral<List<PredictorConfig>> configType = new TypeLiteral<List<PredictorConfig>>() {
        };
        return (Class<List<PredictorConfig>>) configType.getRawType();
    }

    /**
     * Returns all configured {@link Predictor} instances.
     *
     * @return
     */
    public List<Predictor> getPredictors() {
        return new ArrayList<>(this.predictors);
    }

    /**
     * Only returns {@link Predictor} instances that are in
     * {@link State#STARTED} state.
     *
     * @return
     */
    public List<Predictor> getStartedPredictors() {
        return this.predictors.stream().filter(PredictorPredicates.isStarted()).collect(Collectors.toList());
    }

    /**
     * Determines differences between two {@link PredictorRegistry}
     * configurations. More specifically, it determines what
     * {@link PredictorConfig}s have been added, deleted and modified,
     * respectively.
     *
     *
     *
     */
    static class PredictorRegistryConfigDiff {

        private final List<PredictorConfig> oldConfigs;
        private final List<PredictorConfig> newConfigs;

        /**
         * Constructs a new {@link PredictorRegistryConfigDiff}.
         *
         * @param oldConfig
         *            The old {@link PredictorRegistry} configuration.
         * @param newConfig
         *            The new {@link PredictorRegistry} configuration.
         */
        public PredictorRegistryConfigDiff(List<PredictorConfig> oldConfig, List<PredictorConfig> newConfig) {
            this.oldConfigs = oldConfig == null ? new ArrayList<PredictorConfig>() : oldConfig;
            this.newConfigs = newConfig == null ? new ArrayList<PredictorConfig>() : newConfig;
        }

        /**
         * Returns <code>true</code> if there are differences between the old
         * and new configuration.
         *
         * @return
         */
        public boolean isDifferent() {
            return !Objects.equals(this.oldConfigs, this.newConfigs);
        }

        /**
         * Returns the {@link PredictorConfig}s that have been added in the new
         * configuration compared to the old configuration.
         *
         * @return
         */
        public List<PredictorConfig> added() {
            Set<String> oldIds = idSet(this.oldConfigs);
            Set<String> newIds = idSet(this.newConfigs);
            newIds.removeAll(oldIds);
            Set<String> addedIds = newIds;
            return this.newConfigs.stream().filter(withIdIn(addedIds)).collect(Collectors.toList());
        }

        /**
         * Returns the {@link PredictorConfig}s that have been deleted in the
         * new configuration compared to the old configuration.
         *
         * @return
         */
        public List<PredictorConfig> deleted() {
            Set<String> oldIds = idSet(this.oldConfigs);
            Set<String> newIds = idSet(this.newConfigs);
            oldIds.removeAll(newIds);
            Set<String> deletedIds = oldIds;
            return this.oldConfigs.stream().filter(withIdIn(deletedIds)).collect(Collectors.toList());
        }

        /**
         * Returns the {@link PredictorConfig}s that have been modified in the
         * new configuration compared to the old configuration.
         *
         * @return
         */
        public List<PredictorConfig> modified() {
            Set<String> oldIds = idSet(this.oldConfigs);
            Set<String> newIds = idSet(this.newConfigs);
            newIds.retainAll(oldIds);
            Set<String> updatedIds = newIds;
            // get all configs that are in both old and new sets
            List<PredictorConfig> updatedConfigs = this.newConfigs.stream().filter(withIdIn(updatedIds))
                    .collect(Collectors.toList());
            // filter so that only configurations whose content has actually
            // changed when compared to the old version remain.
            List<PredictorConfig> withModifiedContent = updatedConfigs.stream()
                    .filter(it -> !this.oldConfigs.contains(it)).collect(Collectors.toList());
            return withModifiedContent;
        }
    }

    /**
     * Returns a copy of the set of identifiers for a list of
     * {@link PredictorConfig}s.
     *
     * @param predictorConfigs
     * @return
     */
    private static Set<String> idSet(List<PredictorConfig> predictorConfigs) {
        List<String> identifiers = predictorConfigs.stream().map(PredictorConfig::getId).collect(Collectors.toList());
        return new HashSet<>(identifiers);
    }

    private MetricStream findOriginMetricStream(String streamId) {
        for (MetricStreamer<?> metricStreamer : this.monitoringSubsystem.getMetricStreamers()) {
            try {
                return metricStreamer.getMetricStream(streamId);
            } catch (IllegalArgumentException e) {
                // not found, try next
            }
        }
        throw new NoSuchElementException(String.format("no metric stream with id %s was found", streamId));
    }

}
