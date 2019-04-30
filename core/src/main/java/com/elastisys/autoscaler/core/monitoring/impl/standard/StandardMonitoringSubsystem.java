package com.elastisys.autoscaler.core.monitoring.impl.standard;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamMonitorConfig;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamerConfig;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.StandardMonitoringSubsystemConfig;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.SystemHistorianConfig;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.streammonitor.MetricStreamMonitor;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * The {@link StandardMonitoringSubsystem} is configured via a
 * {@link StandardMonitoringSubsystemConfig}, which provides the implementation
 * class to use for the {@link MetricStreamer} and {@link SystemHistorian}, as
 * well as their (type-specific) configurations.
 * <p/>
 * The {@link MetricStreamer} and {@link SystemHistorian} implementation classes
 * may have certain {@link AutoScaler} dependencies injected on instantiation,
 * for example, by making use of a {@link Inject} annotation on the constructor.
 * For a list of the supported dependencies refer to the javadoc of
 * {@link MetricStreamerCreator} and {@link SystemHistorianCreator}.
 *
 * @see MetricStreamerCreator
 * @see SystemHistorianCreator
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class StandardMonitoringSubsystem implements MonitoringSubsystem<StandardMonitoringSubsystemConfig> {

    private final UUID autoScalerUuid;
    private final String autoScalerId;
    private final Logger logger;
    private final EventBus eventBus;
    private final ScheduledExecutorService executor;
    private final File storageDir;

    /** The currently set configuration. */
    private StandardMonitoringSubsystemConfig config;
    /** <code>true</code> if this subsystem has been started. */
    private boolean started;

    /** Holds the instantiated {@link MetricStreamer}s. */
    private List<MetricStreamer<?>> metricStreamers = new ArrayList<>();
    /** Holds the instantiated {@link SystemHistorian}. */
    private SystemHistorian systemHistorian;
    /** Alerts on suspiciously low metric stream activity. */
    private MetricStreamMonitor metricStreamMonitor;

    @Inject
    public StandardMonitoringSubsystem(@Named("Uuid") UUID autoScalerUuid, @Named("AutoScalerId") String autoScalerId,
            Logger logger, EventBus eventBus, ScheduledExecutorService executor, @Named("StorageDir") File storageDir) {
        this.autoScalerUuid = autoScalerUuid;
        this.autoScalerId = autoScalerId;
        this.logger = logger;
        this.eventBus = eventBus;
        this.executor = executor;
        this.storageDir = storageDir;
    }

    @Override
    public void validate(StandardMonitoringSubsystemConfig configuration) throws IllegalArgumentException {

        checkArgument(configuration != null, "monitoringSubsystem: configuration cannot be null");
        configuration.validate();

        // make sure the specified metric streamer and system historian can
        // be instantiated
        ensureConfigurationCanBeRealized(configuration);
    }

    /**
     * Ensures that the given monitoring subsystem configuration can be
     * realized, by checking that the specified components can be created and
     * configured.
     *
     * @param monitoringConfig
     * @throws ClassNotFoundException
     * @throws ConfigurationException
     */
    private void ensureConfigurationCanBeRealized(StandardMonitoringSubsystemConfig monitoringConfig)
            throws IllegalArgumentException {
        // will throw exceptions on failure to create or apply config
        createAndConfigureMetricStreamers(monitoringConfig.getMetricStreamers());
        createAndConfigureSystemHistorian(monitoringConfig.getSystemHistorian());
        createAndConfigureMetricStreamMonitor(monitoringConfig.getMetricStreamMonitor());
    }

    private List<MetricStreamer<?>> createAndConfigureMetricStreamers(List<MetricStreamerConfig> metricStreamerConfigs)
            throws IllegalArgumentException {
        List<MetricStreamer<?>> createdMetricStreamers = new ArrayList<>();

        for (int i = 0; i < metricStreamerConfigs.size(); i++) {
            List<MetricStreamer<?>> priorDeclaredMetricStreamers = new ArrayList<>(createdMetricStreamers);
            MetricStreamerCreator creator = new MetricStreamerCreator(this.logger, this.eventBus, this.executor,
                    priorDeclaredMetricStreamers);

            MetricStreamerConfig metricStreamerConfig = metricStreamerConfigs.get(i);

            // instantiate
            MetricStreamer metricStreamer;
            try {
                metricStreamer = creator.createMetricStreamer(metricStreamerConfig);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format(
                        "monitoringSubsystem: could not instantiate metricStreamer %d: %s", i, e.getMessage()), e);
            }

            // configure
            try {
                Object config = unmarshal(metricStreamerConfig.getConfig(), metricStreamer.getConfigurationClass());
                metricStreamer.validate(config);
                metricStreamer.configure(config);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format(
                        "monitoringSubsystem: metricStreamer %d: could not apply config: %s", i, e.getMessage()), e);
            }

            // add
            createdMetricStreamers.add(metricStreamer);
        }

        validateMetricStreamIdUniquness(createdMetricStreamers);

        return createdMetricStreamers;
    }

    private void validateMetricStreamIdUniquness(List<MetricStreamer<?>> metricStreamers) {
        Set<String> uniqueIds = new HashSet<>();

        metricStreamers.forEach(metricStreamer -> {
            for (MetricStream stream : metricStreamer.getMetricStreams()) {
                if (uniqueIds.contains(stream.getId())) {
                    throw new IllegalArgumentException(
                            String.format("monitoringSubsystem: duplicate metricStream id: %s", stream.getId()));
                }
                uniqueIds.add(stream.getId());
            }
        });
    }

    private SystemHistorian createAndConfigureSystemHistorian(SystemHistorianConfig systemHistorianSpec)
            throws IllegalArgumentException {
        SystemHistorianCreator creator = new SystemHistorianCreator(this.autoScalerUuid, this.autoScalerId, this.logger,
                this.eventBus, this.executor, this.storageDir);

        // instantiate
        SystemHistorian systemHistorian;
        try {
            systemHistorian = creator.createSystemHistorian(systemHistorianSpec);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "monitoringSubsystem: could not instantiate systemHistorian: " + e.getMessage(), e);
        }

        // configure
        try {
            Object config = unmarshal(systemHistorianSpec.getConfig(), systemHistorian.getConfigurationClass());
            systemHistorian.validate(config);
            systemHistorian.configure(config);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "monitoringSubsystem: systemHistorian config could not be applied: " + e.getMessage(), e);
        }

        return systemHistorian;
    }

    private MetricStreamMonitor createAndConfigureMetricStreamMonitor(MetricStreamMonitorConfig metricStreamMonitorSpec)
            throws IllegalArgumentException {
        try {
            MetricStreamMonitor streamMonitor = new MetricStreamMonitor(this.logger, this.eventBus, this.executor,
                    this);
            streamMonitor.validate(metricStreamMonitorSpec);
            streamMonitor.configure(metricStreamMonitorSpec);
            return streamMonitor;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "monitoringSubsystem: metricStreamMonitor config could not be applied: " + e.getMessage(), e);
        }
    }

    /**
     * Unmarshal a given JSON object to a Java object of a given type.
     *
     * @param json
     * @param javaType
     * @return
     */
    private static <T> T unmarshal(JsonObject json, Class<T> javaType) {
        return JsonUtils.toObject(json, javaType);
    }

    @Override
    public synchronized void configure(StandardMonitoringSubsystemConfig configuration)
            throws IllegalArgumentException {
        validate(configuration);

        boolean needsRestart = isStarted();
        if (needsRestart) {
            stop();
        }

        apply(configuration);

        if (needsRestart) {
            start();
        }

    }

    private void apply(StandardMonitoringSubsystemConfig newConfig) throws IllegalArgumentException {
        try {
            List<MetricStreamer<?>> metricStreamers = createAndConfigureMetricStreamers(newConfig.getMetricStreamers());
            SystemHistorian systemHistorian = createAndConfigureSystemHistorian(newConfig.getSystemHistorian());
            MetricStreamMonitor streamMonitor = createAndConfigureMetricStreamMonitor(
                    newConfig.getMetricStreamMonitor());

            // make sure all can be created and configured before we decide to
            // keep them
            this.metricStreamers = metricStreamers;
            this.systemHistorian = systemHistorian;
            this.metricStreamMonitor = streamMonitor;

            this.config = newConfig;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "monitoringSubsystem: failed to apply monitoringSubsystem configuration: " + e.getMessage(), e);
        }
    }

    @Override
    public void start() throws IllegalStateException {
        checkState(isConfigured(), "cannot start monitoring subsystem without a configuration.");

        if (this.started) {
            // no-op
            return;
        }

        for (MetricStreamer<?> metricStreamer : this.metricStreamers) {
            metricStreamer.start();
        }
        this.systemHistorian.start();
        this.metricStreamMonitor.start();

        this.started = true;
        this.logger.info(getClass().getSimpleName() + " started.");
    }

    @Override
    public void stop() {
        if (!this.started) {
            // no-op
            return;
        }

        this.metricStreamMonitor.stop();
        this.systemHistorian.stop();
        for (MetricStreamer<?> metricStreamer : this.metricStreamers) {
            metricStreamer.stop();
        }

        this.started = false;
        this.logger.info(getClass().getSimpleName() + " stopped.");
    }

    @Override
    public ServiceStatus getStatus() {
        return new ServiceStatus.Builder().started(isStarted()).build();
    }

    @Override
    public StandardMonitoringSubsystemConfig getConfiguration() {
        return this.config;
    }

    @Override
    public Class<StandardMonitoringSubsystemConfig> getConfigurationClass() {
        return StandardMonitoringSubsystemConfig.class;
    }

    @Override
    public List<MetricStreamer<?>> getMetricStreamers() {
        checkState(this.metricStreamers != null, "no metricStreamers have been configured for the monitoringSubsystem");
        return this.metricStreamers;
    }

    @Override
    public SystemHistorian<?> getSystemHistorian() {
        checkState(this.systemHistorian != null, "no systemHistorian has been configured for the monitoringSubsystem");
        return this.systemHistorian;
    }

    public MetricStreamMonitor getMetricStreamMonitor() {
        checkState(this.metricStreamMonitor != null,
                "no metricStreamMonitor has been configured for the monitoringSubsystem");
        return this.metricStreamMonitor;
    }

    private boolean isConfigured() {
        return this.config != null;
    }

    private boolean isStarted() {
        return this.started;
    }
}
