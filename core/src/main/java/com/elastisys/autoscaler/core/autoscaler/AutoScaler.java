package com.elastisys.autoscaler.core.autoscaler;

import static com.elastisys.autoscaler.core.api.CorePredicates.hasStarted;
import static com.elastisys.autoscaler.core.api.CorePredicates.isUnhealthy;
import static com.elastisys.scale.commons.json.JsonUtils.toJson;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import com.elastisys.autoscaler.core.alerter.api.Alerter;
import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.Health;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.autoscaler.builder.AutoScalerBuilder;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerBlueprint;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactory;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.metronome.api.Metronome;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.util.concurrent.RestartableScheduledExecutorService;
import com.google.gson.JsonObject;

/**
 * An {@link AutoScaler} monitors the load on a cloud service, predicts the
 * future machine need and resizes the machine pool accordingly.
 * <p/>
 * For creating a single {@link AutoScaler} instance, an
 * {@link AutoScalerBuilder} can be used. For creating and managing several
 * {@link AutoScaler} instance, an {@link AutoScalerFactory} is more
 * appropriate.
 * <p/>
 * After creation, the {@link AutoScaler} needs to be configured (
 * {@link #configure(JsonObject)}) with a <i>system configuration</i>, which
 * parameterizes each of the {@link AutoScaler}'s subsystems. The system
 * configuration is a JSON document, whose subsections address the configuration
 * of each of the subsystems that comprise the {@link AutoScaler}.
 * <p/>
 * Once configured, the {@link AutoScaler}'s {@link #start()} method can be used
 * to start auto-scaling.
 * <p/>
 * The {@link AutoScaler} and its subsystems shares an {@link EventBus} which
 * can be used to communicate different objects (such as {@link Alert}s and
 * {@link SystemMetricEvent}s). The {@link AutoScaler} listens to the
 * {@link EventBus} for {@link AutoScalerEvent}s, which can be posted onto the
 * {@link EventBus} by other subsystems to trigger different {@link AutoScaler}
 * actions.
 *
 * @see AutoScalerFactory
 * @see AutoScalerBuilder
 */
@SuppressWarnings("rawtypes")
public class AutoScaler implements Service<JsonObject> {
    /**
     * The grace period given to running tasks (to complete) when the
     * {@link AutoScaler} is stopped.
     */
    private static final TimeInterval TASK_TERMINATION_GRACE_PERIOD = new TimeInterval(100L, TimeUnit.MILLISECONDS);

    /** Config element name for {@link MonitoringSubsystem}. */
    private static final String MONITORING_SUBSYSTEM_CONFIG = "monitoringSubsystem";
    /** Config element name for {@link Alerter} subsystem. */
    private static final String ALERTER_CONFIG = "alerter";
    /** Config element name for {@link PredictionSubsystem} subsystem. */
    private static final String PREDICTION_SUBSYSTEM_CONFIG = "predictionSubsystem";
    /** Config element name for {@link Metronome} subsystem. */
    private static final String METRONOME_CONFIG = "metronome";
    /** Config element name for {@link CloudPoolProxy subsystem. */
    private static final String CLOUDPOOL_PROXY_CONFIG = "cloudPool";

    /**
     * A universally unique id assigned to the {@link AutoScaler} on creation.
     */
    private final UUID uuid;
    /**
     * The id/name of this {@link AutoScaler} instance, as assigned from the
     * {@link AutoScalerBlueprint}.
     */
    private final String id;
    private final Logger logger;
    private final EventBus bus;
    private final RestartableScheduledExecutorService executorService;

    private final MonitoringSubsystem monitoringSubsystem;
    private final Alerter alerter;
    private final PredictionSubsystem predictionSubsystem;
    private final Metronome metronome;
    private final CloudPoolProxy cloudPoolProxy;

    /**
     * The add-on subsystems that are part of this {@link AutoScaler}. Add-on
     * subsystems are not strictly necessary for the {@link AutoScaler} to
     * operate, but may extend it with additional functionality. Accounting and
     * high-availability are two examples of what such add-on subsystems could
     * achieve. Keys are names, such as {@code accountingSubsystem}, and values
     * are {@link Service} implementatilastFaultons.
     */
    private final Map<String, Service> addonSubsystems;

    private final File storageDir;

    /**
     * <code>true</code> if this {@link AutoScaler} instance has been
     * configured.
     */
    private final AtomicBoolean configured;

    @Inject
    private AutoScaler(@Named("Uuid") UUID uuid, @Named("AutoScalerId") String id, @Named("StorageDir") File storageDir,
            Logger logger, EventBus bus, RestartableScheduledExecutorService executorService,
            MonitoringSubsystem monitoringSubsystem, Alerter alerter, Metronome metronome, CloudPoolProxy cloudPool,
            PredictionSubsystem predictionSubsystem, Map<String, Service> addonSubsystems) {
        this.uuid = uuid;
        this.id = id;
        this.logger = logger;
        this.bus = bus;
        this.executorService = executorService;
        this.bus.register(this);

        this.monitoringSubsystem = monitoringSubsystem;
        this.alerter = alerter;
        this.metronome = metronome;
        this.cloudPoolProxy = cloudPool;
        this.predictionSubsystem = predictionSubsystem;
        this.addonSubsystems = addonSubsystems;

        this.storageDir = storageDir;

        this.configured = new AtomicBoolean(false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void validate(JsonObject configuration) throws IllegalArgumentException {
        checkArgument(configuration != null, "autoScaler: null configuration not permitted");

        // monitoring subsystem
        Object config = extractSubsystemConfig(configuration, MONITORING_SUBSYSTEM_CONFIG,
                this.monitoringSubsystem.getConfigurationClass());
        this.monitoringSubsystem.validate(config);

        // alerter
        config = extractSubsystemConfig(configuration, ALERTER_CONFIG, this.alerter.getConfigurationClass());
        this.alerter.validate(config);

        // metronome
        config = extractSubsystemConfig(configuration, METRONOME_CONFIG, this.metronome.getConfigurationClass());
        this.metronome.validate(config);

        // cloud pool
        config = extractSubsystemConfig(configuration, CLOUDPOOL_PROXY_CONFIG,
                this.cloudPoolProxy.getConfigurationClass());
        this.cloudPoolProxy.validate(config);

        // prediction subsystem
        config = extractSubsystemConfig(configuration, PREDICTION_SUBSYSTEM_CONFIG,
                this.predictionSubsystem.getConfigurationClass());
        this.predictionSubsystem.validate(config);

        // add-on subsystems
        for (Entry<String, Service> addon : this.addonSubsystems.entrySet()) {
            String addonName = addon.getKey();
            Service addonSubsystem = addon.getValue();
            config = extractSubsystemConfig(configuration, addonName, addonSubsystem.getConfigurationClass());
            addonSubsystem.validate(config);
        }
    }

    /**
     * Applies a new {@link AutoScaler} configuration in an atomic
     * all-or-nothing manner. That is, if any subsystem fails to apply the new
     * configuration, the old configuration is kept.
     *
     * @see com.elastisys.autoscaler.core.api.Configurable#configure(java.lang.Object)
     */
    @Override
    public synchronized void configure(JsonObject configuration) throws IllegalArgumentException {
        validate(configuration);
        try {
            ensureApplicableOrFail(configuration);
        } catch (Exception e) {
            String message = String.format("autoScaler: rejecting config: %s", e.getMessage());
            this.logger.warn(message);
            throw new IllegalArgumentException(message, e);
        }

        applyConfig(configuration);
        this.configured.set(true);
    }

    /**
     * Applies the configuration to this {@link AutoScaler} without first
     * checking if it is applicable.
     *
     * @param configuration
     */
    @SuppressWarnings("unchecked")
    private void applyConfig(JsonObject configuration) throws IllegalArgumentException {
        // receives full configuration, pushes the different sub-configurations
        // to their respective subsystem
        this.logger.info("applying configuration ...");

        // monitoring subsystem. note: should be configured prior to predictors,
        // since these may reference metric stream ids from the new
        // configuration.
        Object config = extractSubsystemConfig(configuration, MONITORING_SUBSYSTEM_CONFIG,
                this.monitoringSubsystem.getConfigurationClass());
        this.monitoringSubsystem.configure(config);

        // alerter
        config = extractSubsystemConfig(configuration, ALERTER_CONFIG, this.alerter.getConfigurationClass());
        this.alerter.configure(config);

        // metronome
        config = extractSubsystemConfig(configuration, METRONOME_CONFIG, this.metronome.getConfigurationClass());
        this.metronome.configure(config);

        // cloud pool
        config = extractSubsystemConfig(configuration, CLOUDPOOL_PROXY_CONFIG,
                this.cloudPoolProxy.getConfigurationClass());
        this.cloudPoolProxy.configure(config);

        // prediction subsystem
        config = extractSubsystemConfig(configuration, PREDICTION_SUBSYSTEM_CONFIG,
                this.predictionSubsystem.getConfigurationClass());
        this.predictionSubsystem.configure(config);

        // add-on subsystems
        for (Entry<String, Service> addon : this.addonSubsystems.entrySet()) {
            String addonName = addon.getKey();
            Service addonSubsystem = addon.getValue();
            config = extractSubsystemConfig(configuration, addonName, addonSubsystem.getConfigurationClass());
            addonSubsystem.configure(config);
        }
    }

    /**
     * Ensures that a given {@link AutoScaler} configuration can be applied to
     * this {@link AutoScaler} instance. If any part of the configuration would
     * not be possible to apply, the method raises a
     * {@link ConfigurationException}.
     *
     * @param autoScalerConfig
     */
    private void ensureApplicableOrFail(JsonObject autoScalerConfig) throws IllegalArgumentException {
        // Try to apply the configuration to a blueprint copy of this
        // AutoScaler. If no subsystem complains, the configuration is assumed
        // to be possible to apply.
        this.logger.debug("creating a copy autoscaler to validate config ...");
        AutoScaler silentCopy = AutoScalerBuilder.newBuilderFromSource(this).withLogger(NOPLogger.NOP_LOGGER).build();
        this.logger.debug("trying out configuration on copy autoscaler ...");
        silentCopy.applyConfig(autoScalerConfig);
        this.logger.debug("configuration successfully applied to copy autoscaler.");
    }

    /**
     * Extracts and parses the configuration for a given subsystem to an object
     * of the expected configuration class for that subsystem. Returns a
     * <code>null</code> object if the configuration does not contain the
     * expected element. If the JSON object is present, an attempt is made to
     * convert it to an object of the given type.
     *
     * @param configuration
     *            Full {@link AutoScaler} configuration document.
     * @param subsystemConfigName
     *            The name of the configuration object for the subsystem of
     *            interest. For example, {@code alerter}.
     * @param configClass
     *            The expected class of the configuration object.
     * @return A configuration object of type {@code configClass}, or
     *         <code>null</code> if no {@code subsystemConfigName} field exists.
     */
    private Object extractSubsystemConfig(JsonObject configuration, String subsystemConfigName, Class<?> configClass)
            throws IllegalArgumentException {
        if (!configuration.has(subsystemConfigName)) {
            return null;
        }
        JsonObject subsystemConfig = configuration.getAsJsonObject(subsystemConfigName);
        return JsonUtils.toObject(subsystemConfig, configClass);
    }

    @Override
    public synchronized void start() {
        checkState(this.configured.get(), "cannot start auto scaler before being configured");
        this.logger.info("starting autoscaler {} ...", this.id);

        this.executorService.start();

        this.monitoringSubsystem.start();
        this.alerter.start();
        this.metronome.start();
        this.predictionSubsystem.start();
        this.cloudPoolProxy.start();
        // add-on subsystems
        for (Service addonSubsystem : this.addonSubsystems.values()) {
            addonSubsystem.start();
        }
    }

    @Override
    public synchronized void stop() {
        this.logger.info("stopping autoscaler {} ...", this.id);

        // add-on subsystems
        for (Service addonSubsystem : this.addonSubsystems.values()) {
            addonSubsystem.stop();
        }
        this.cloudPoolProxy.stop();
        this.predictionSubsystem.stop();
        this.metronome.stop();
        this.alerter.stop();
        this.monitoringSubsystem.stop();

        try {
            this.executorService.stop(TASK_TERMINATION_GRACE_PERIOD.getTime().intValue(),
                    TASK_TERMINATION_GRACE_PERIOD.getUnit());
        } catch (InterruptedException e) {
            this.logger.warn(
                    "Stopping of AutoScaler was interrupted " + "while waiting for spawned sub-tasks to complete.");
        }
    }

    @Override
    public ServiceStatus getStatus() {
        List<Service> systemServices = getSubsystems();

        State state = systemServices.stream().allMatch(hasStarted()) ? State.STARTED : State.STOPPED;
        Health health = systemServices.stream().anyMatch(isUnhealthy()) ? Health.NOT_OK : Health.OK;

        Stream<Service> unhealthyServices = systemServices.stream().filter(isUnhealthy());
        // health detail: concatenate health details of all subsystems
        // whose health is NOT_OK
        StringBuilder healthDetails = new StringBuilder();
        unhealthyServices.forEach(s -> {
            healthDetails.append(s.getStatus().getHealthDetail() + "\n");
        });
        Optional<String> detail = Optional.empty();
        if (!healthDetails.toString().isEmpty()) {
            detail = Optional.of(healthDetails.toString());
        }
        return new ServiceStatus(state, health, detail);
    }

    @Override
    public JsonObject getConfiguration() {
        if (!this.configured.get()) {
            return null;
        }

        // fetch configurations from all sub-systems and return their values in
        // JSON format.
        JsonObject config = new JsonObject();
        if (this.monitoringSubsystem.getConfiguration() != null) {
            config.add(MONITORING_SUBSYSTEM_CONFIG, toJson(this.monitoringSubsystem.getConfiguration(), false));
        }
        if (this.alerter.getConfiguration() != null) {
            config.add(ALERTER_CONFIG, toJson(this.alerter.getConfiguration(), false));
        }
        if (this.metronome.getConfiguration() != null) {
            config.add(METRONOME_CONFIG, toJson(this.metronome.getConfiguration(), false));
        }
        if (this.cloudPoolProxy.getConfiguration() != null) {
            config.add(CLOUDPOOL_PROXY_CONFIG, toJson(this.cloudPoolProxy.getConfiguration(), false));
        }
        if (this.predictionSubsystem.getConfiguration() != null) {
            config.add(PREDICTION_SUBSYSTEM_CONFIG, toJson(this.predictionSubsystem.getConfiguration(), false));
        }
        // add-on subsystems
        for (Entry<String, Service> addon : this.addonSubsystems.entrySet()) {
            String addonName = addon.getKey();
            Service addonSubsystem = addon.getValue();
            Object addonConfig = addonSubsystem.getConfiguration();
            if (addonConfig != null) {
                config.add(addonName, toJson(addonConfig, false));
            }
        }

        return config;
    }

    @Override
    public Class<JsonObject> getConfigurationClass() {
        return JsonObject.class;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getId() {
        return this.id;
    }

    public Logger getLogger() {
        return this.logger;
    }

    public EventBus getBus() {
        return this.bus;
    }

    public MonitoringSubsystem getMonitoringSubsystem() {
        return this.monitoringSubsystem;
    }

    public Alerter getAlerter() {
        return this.alerter;
    }

    public Metronome getMetronome() {
        return this.metronome;
    }

    public CloudPoolProxy getCloudPoolProxy() {
        return this.cloudPoolProxy;
    }

    public PredictionSubsystem getPredictionSubsystem() {
        return this.predictionSubsystem;
    }

    public ScheduledExecutorService getExecutorService() {
        return this.executorService;
    }

    /**
     * The add-on subsystems that are part of this {@link AutoScaler}. Add-on
     * subsystems are not strictly necessary for the {@link AutoScaler} to
     * operate, but may extend it with additional functionality. Accounting and
     * high-availability are two examples of what such add-on subsystems could
     * achieve. Keys are names, such as {@code accountingSubsystem}, and values
     * are {@link Service} implementations.
     *
     * @return
     */
    public Map<String, Service> getAddonSubsystems() {
        return this.addonSubsystems;
    }

    /**
     * Returns the directory where the {@link AutoScaler} instance persists its
     * state.
     *
     * @return
     */
    public File getStorageDir() {
        return this.storageDir;
    }

    /**
     * Called whenever an {@link AutoScalerEvent} has been received on the
     * {@link AutoScaler}'s {@link EventBus}.
     *
     * @param event
     *            The received event.
     */
    @Subscriber
    public void onAutoScalerEvent(AutoScalerEvent event) {
        this.logger.info("received autoscaler {} event", event);
        switch (event) {
        case STOP:
            // execute stop action in a separate thread to not shut down
            // executor before this event has been processed
            String shutdownThreadName = this.id + "-shutdown-thread";
            new Thread(() -> {
                stop();
            }, shutdownThreadName).start();
            break;
        default:
            this.logger.info("ignoring unrecognized autoscaler event: {}", event);
            break;
        }
    }

    /**
     * Returns all subsystems of this {@link AutoScaler}.
     *
     * @return All subsystem {@link Service}s.
     */
    List<Service> getSubsystems() {
        List<Service> services = new ArrayList<>();
        services.addAll(Arrays.asList(this.alerter, this.cloudPoolProxy, this.monitoringSubsystem, this.metronome,
                this.predictionSubsystem));
        services.addAll(this.addonSubsystems.values());
        return services;
    }
}
