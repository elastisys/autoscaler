package com.elastisys.autoscaler.core.autoscaler.factory;

import static com.elastisys.scale.commons.json.JsonUtils.parseJsonFile;
import static com.elastisys.scale.commons.json.JsonUtils.toObject;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;
import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.alerter.api.Alerter;
import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.autoscaler.builder.AutoScalerBuilder;
import com.elastisys.autoscaler.core.autoscaler.builder.AutoScalerBuilder.Defaults;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.metronome.api.Metronome;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.elastisys.scale.commons.util.io.IoUtils;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * An {@link AutoScalerFactory} is a {@link Service} that creates new
 * {@link AutoScaler} instances and maintains a registry of created instances.
 * <p/>
 * An {@link AutoScaler} is created from a <i>blueprint</i>, which specifies
 * what implementations to use for the different subsystems of the
 * {@link AutoScaler}. The {@link AutoScaler} then needs to be passed a JSON
 * configuration, containing the system <i>configuration</i> before it can be
 * started.
 * <p/>
 * In addition to the subsystems specified in the blueprint, an
 * {@link AutoScalerFactory} can be configured to always include add-on
 * subsystems in all {@link AutoScaler} instances it creates. These add-on
 * subsystems are not strictly necessary for the {@link AutoScaler} to operate,
 * but may extend it with additional functionality. Accounting and
 * high-availability are two examples of what such add-on subsystems could
 * achieve.
 * <p/>
 * The state of {@link AutoScaler} instances can be persisted via the
 * {@code save} method. To this end, the {@link AutoScalerFactory} is configured
 * with a storage directory, under which the {@link AutoScalerFactory} persists
 * instance state.
 * <p/>
 * When an {@link AutoScalerFactory} is {@link #start()}ed, it will restore and
 * start any saved {@link AutoScaler} instances to their last known state.
 *
 * @see AutoScaler
 * @see AutoScalerBlueprint
 */
public class AutoScalerFactory implements Service<AutoScalerFactoryConfig> {

    /** {@link Logger} instance. */
    static Logger logger = LoggerFactory.getLogger(AutoScalerFactory.class);

    /** File in instance directory where an autoscaler's UUID is stored. */
    static final String AUTOSCALER_UUID_FILE = "autoscaler.uuid";
    /** File in instance directory where an autoscaler's blueprint is stored. */
    static final String BLUEPRINT_FILE = "blueprint.json";
    /**
     * File in instance directory where an autoscaler's configuration is stored.
     */
    static final String CONFIG_FILE = "config.json";
    /** File in instance directory where an autoscaler's status is stored. */
    static final String STATUS_FILE = "status.json";

    /** Regular expression describing a valid instance id. */
    public static final Pattern VALID_ID_PATTERN = Pattern.compile("[a-zA-Z0-9_\\-]+");

    /**
     * Name of environment variable that can be used to declare the location of
     * a default factory configuration to use when attempts are made to create
     * {@link AutoScalerFactory} without passing an explicit configuration.
     */
    public static final String AUTOSCALER_FACTORY_CONFIG_ENVVAR = "AUTOSCALER_FACTORY_CONFIG";

    /**
     * The {@link AutoScaler} instances known to this {@link AutoScalerFactory}.
     */
    private Map<String, AutoScalerInstance> autoScalerInstances;
    /** Configuration for this {@link AutoScalerFactory} instance. */
    private AutoScalerFactoryConfig config;
    /** <code>true</code> if this {@link AutoScalerFactory} has been started. */
    private boolean started;

    private AutoScalerFactory() {
        this.autoScalerInstances = new ConcurrentHashMap<>();
        this.config = null;
        this.started = false;
    }

    @Override
    public void validate(AutoScalerFactoryConfig configuration) throws IllegalArgumentException {
        checkArgument(configuration != null, "autoScalerFactory: configuration cannot be null");
        configuration.validate();
    }

    @Override
    public void configure(AutoScalerFactoryConfig configuration) throws IllegalArgumentException {
        checkArgument(this.config == null, "autoScalerFactory: re-configuration is not supported");
        validate(configuration);
        logger.debug("AutoScalerFactory created with configuration: {}", configuration);
        prepareStorageDir(configuration.getStorageDir());
        this.config = configuration;
    }

    @Override
    public AutoScalerFactoryConfig getConfiguration() {
        return this.config;
    }

    @Override
    public Class<AutoScalerFactoryConfig> getConfigurationClass() {
        return AutoScalerFactoryConfig.class;
    }

    /**
     * Starts this {@link AutoScalerFactory}, restoring and starting any saved
     * instances to their last known state (according to the last saved
     * configuration for each instance in the storage directory).
     */
    @Override
    public void start() throws IllegalStateException {
        checkState(isConfigured(), "attempt to start factory prior to configuring");
        if (isStarted()) {
            return;
        }
        try {
            restoreInstances();
        } catch (Exception e) {
            throw new IllegalStateException(format("failed to restore auto-scaler instances: %s", e.getMessage()), e);
        }
        this.started = true;
        logger.info("autoscaler factory started.");
    }

    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }
        // save all instance configurations
        for (String autoScalerId : getAutoScalerIds()) {
            this.autoScalerInstances.get(autoScalerId).save();
        }
        // stop all instances
        for (String autoScalerId : getAutoScalerIds()) {
            getAutoScaler(autoScalerId).stop();
        }
        // forget instances
        this.autoScalerInstances.clear();

        this.started = false;
        logger.info("autoscaler factory stopped.");
    }

    @Override
    public ServiceStatus getStatus() {
        return new ServiceStatus.Builder().started(isStarted()).build();
    }

    /**
     * Returns <code>true</code> if this factory is started.
     *
     * @return
     */
    public boolean isStarted() {
        return this.started;
    }

    private boolean isConfigured() {
        return this.config != null;
    }

    /**
     * Creates a new {@link AutoScalerFactory}. The created factory is returned
     * in an unconfigured and {@link #stop()}ed state.
     * <p/>
     * The client needs to configure and {@link #start()} the
     * {@link AutoScalerFactory} before use.
     *
     * @see #launch()
     * @see #launch(File)
     * @see #launch(AutoScalerFactoryConfig)
     *
     * @return An {@link AutoScalerFactory} instance.
     */
    public static AutoScalerFactory create() {
        return new AutoScalerFactory();
    }

    /**
     * Launches a new {@link AutoScalerFactory} from a given configuration. The
     * created factory is returned in a configured and {@link #start()}ed state.
     *
     * @see #create()
     *
     * @param config
     *            The factory configuration.
     * @return An {@link AutoScalerFactory} instance.
     * @throws IllegalArgumentException
     *             If the {@link AutoScalerFactory} could not be configured.
     */
    public static AutoScalerFactory launch(AutoScalerFactoryConfig config) throws IllegalArgumentException {
        logger.info("creating AutoScalerFactory from config: {}", config);
        AutoScalerFactory factory = new AutoScalerFactory();
        factory.validate(config);
        factory.configure(config);
        factory.start();
        return factory;
    }

    /**
     * Creates an {@link AutoScaler} from a {@link AutoScalerBlueprint}, which
     * specifies the implementation classes to use for the various
     * {@link AutoScaler} subsystems.
     * <p/>
     * For cases where the provided blueprint doesn't explicitly provide an
     * implementation class for a certain subsystem, the factory will fall back
     * to using defaults as specified in the {@link AutoScalerBuilder.Defaults}.
     *
     * @param blueprint
     *            An {@link AutoScalerBlueprint} that specifies the
     *            implementation classes to use for the various
     *            {@link AutoScaler} subsystems.
     * @return the created {@link AutoScaler} instance.
     */
    public AutoScaler createAutoScaler(AutoScalerBlueprint blueprint) {
        ensureStarted();
        validateBlueprint(blueprint);
        File storageDir = new File(this.config.getStorageDir(), blueprint.id().get());
        Map<String, String> addonSubsytems = this.config.getAddonSubsytems();
        AutoScaler autoScaler = buildAutoScaler(UUID.randomUUID(), blueprint, addonSubsytems, storageDir);
        String id = autoScaler.getId();

        this.autoScalerInstances.put(id, new AutoScalerInstance(autoScaler, blueprint, storageDir));
        return autoScaler;
    }

    /**
     * Builds an {@link AutoScaler} instance from a blueprint, using
     * implementation {@link Defaults} wherever necessary.
     *
     * @param uuid
     *            The universally unique identifier assigned to the
     *            {@link AutoScaler}.
     * @param blueprint
     *            The {@link AutoScalerBlueprint} used to construct the
     *            instance.
     * @param addonSubsytems
     *            The collection of add-on subsystems to be added to
     *            {@link AutoScaler} instance.
     * @param storageDir
     *            The storage directory to use for the created instance.
     * @return
     */
    private static AutoScaler buildAutoScaler(UUID uuid, AutoScalerBlueprint blueprint,
            Map<String, String> addonSubsytems, File storageDir) {
        logger.debug("Building auto-scaler with id {} and uuid {} from:\nblueprint: {}\nadd-ons: {}", blueprint.id(),
                uuid, blueprint, JsonUtils.toPrettyString(JsonUtils.toJson(addonSubsytems)));

        // get implementation classes from blueprint or use defaults
        String monitoringSubsystemClass = blueprint.monitoringSubsystem()
                .orElse(Defaults.MONITORING_SUBSYSTEM.getName());
        String alerterClass = blueprint.alerter().orElse(Defaults.ALERTER.getName());
        String metronomeClass = blueprint.metronome().orElse(Defaults.METRONOME.getName());
        String predictionSubsystemClass = blueprint.predictionSubsystem()
                .orElse(Defaults.PREDICTION_SUBSYSTEM.getName());
        String cloudPoolProxyClass = blueprint.cloudPool().orElse(Defaults.CLOUD_POOL_PROXY.getName());

        AutoScalerBuilder builder = AutoScalerBuilder.newBuilder();
        builder.withUuid(uuid);
        builder.withId(blueprint.id().get());
        builder.withMonitoringSubsystem(loadClass(monitoringSubsystemClass, MonitoringSubsystem.class));
        builder.withAlerter(loadClass(alerterClass, Alerter.class));
        builder.withMetronome(loadClass(metronomeClass, Metronome.class));
        builder.withPredictionSubsystem(loadClass(predictionSubsystemClass, PredictionSubsystem.class));
        builder.withCloudPoolProxy(loadClass(cloudPoolProxyClass, CloudPoolProxy.class));
        builder.withStorageDir(storageDir);
        // add-on subsystems
        for (Entry<String, String> addon : addonSubsytems.entrySet()) {
            String addonName = addon.getKey();
            String addonClass = addon.getValue();
            builder.withAddonSubsystem(addonName, addonClass);
        }

        AutoScaler autoScaler = builder.build();
        return autoScaler;
    }

    /**
     * Creates an {@link AutoScaler} from a JSON-formatted
     * {@link AutoScalerBlueprint}. The {@link AutoScalerBlueprint} specifies
     * the implementation classes to use for the various {@link AutoScaler}
     * subsystems.
     * <p/>
     * For cases where the provided blueprint doesn't explicitly provide an
     * implementation class for a certain subsystem, the factory will fall back
     * to using defaults as specified in the {@link AutoScaler.Defaults}.
     * <p/>
     * The created {@link AutoScaler} instance is returned in an unconfigured
     * and unstarted state.
     *
     * @param blueprint
     *            A JSON-formatted {@link AutoScalerBlueprint} that specifies
     *            the implementation classes to use for the various
     *            {@link AutoScaler} subsystems.
     * @return the created {@link AutoScaler} instance.
     */
    public AutoScaler createAutoScaler(JsonObject jsonBlueprint) {
        AutoScalerBlueprint bluePrint = JsonUtils.toObject(jsonBlueprint, AutoScalerBlueprint.class);
        return createAutoScaler(bluePrint);
    }

    /**
     * Retrieves an {@link AutoScaler} instance created by this
     * {@link AutoScalerFactory}.
     * <p/>
     * If no instance with the given identifier exists, an
     * {@link IllegalArgumentException} is thrown.
     *
     * @param autoScalerId
     *            The identifier of the {@link AutoScaler} instance.
     * @return The {@link AutoScaler} instance.
     */
    public AutoScaler getAutoScaler(String autoScalerId) throws IllegalArgumentException {
        ensureStarted();
        validateAutoScalerId(autoScalerId);
        return this.autoScalerInstances.get(autoScalerId).getAutoScaler();
    }

    /**
     * Returns all {@link AutoScaler} instance ids known to this
     * {@link AutoScalerFactory}.
     *
     * @return The list of known {@link AutoScaler} instance ids.
     */
    public Set<String> getAutoScalerIds() {
        ensureStarted();
        return Collections.unmodifiableSet(this.autoScalerInstances.keySet());
    }

    /**
     * Retrieves the blueprint from which a certain {@link AutoScaler} instance
     * was created.
     * <p/>
     * If no instance with the given identifier exists, an
     * {@link IllegalArgumentException} is thrown.
     *
     * @param autoScalerId
     *            The identifier of the {@link AutoScaler} instance.
     * @return The blueprint that was used when instantiating the
     *         {@link AutoScaler} instance.
     */
    public AutoScalerBlueprint getBlueprint(String autoScalerId) throws IllegalArgumentException {
        ensureStarted();
        validateAutoScalerId(autoScalerId);
        return this.autoScalerInstances.get(autoScalerId).getBlueprint();
    }

    /**
     * Deletes an {@link AutoScaler} instance created by this
     * {@link AutoScalerFactory}.
     * <p/>
     * The deleted {@link AutoScaler} instance is stopped before being removed,
     * giving all subsystems a chance to clean up. The instance's storage
     * directory is also deleted as part of the operation.
     * <p/>
     * If no instance with the given identifier exists, an
     * {@link IllegalArgumentException} is thrown.
     *
     * @param autoScalerId
     *            The identifier of the {@link AutoScaler} instance.
     */
    public void deleteAutoScaler(String autoScalerId) throws IllegalArgumentException {
        ensureStarted();
        validateAutoScalerId(autoScalerId);

        logger.info("Deleting auto-scaler instance " + autoScalerId);
        AutoScalerInstance instance = this.autoScalerInstances.remove(autoScalerId);
        instance.getAutoScaler().stop();

        try {
            // delete instance's storage directory
            FileUtils.deleteRecursively(instance.getStorageDir());
        } catch (IOException e) {
            throw new RuntimeException(
                    format("failed to delete instance directory '%s': %s", instance.getStorageDir(), e.getMessage()),
                    e);
        }
    }

    /**
     * Saves the (configuration) state of an {@link AutoScaler} instance.
     *
     * @param autoScalerId
     *            The identifier of the {@link AutoScaler} instance to save.
     */
    public void save(String autoScalerId) {
        ensureStarted();
        validateAutoScalerId(autoScalerId);

        logger.info("Saving instance state for auto-scaler {}", autoScalerId);
        AutoScalerInstance instance = this.autoScalerInstances.get(autoScalerId);
        instance.save();
    }

    /**
     * Clears this {@link AutoScalerFactory} by stopping and deleting all
     * registered {@link AutoScaler} instances.
     */
    public void clear() {
        ensureStarted();

        List<AutoScalerInstance> instances = new ArrayList<>(this.autoScalerInstances.values());
        for (AutoScalerInstance instance : instances) {
            deleteAutoScaler(instance.getAutoScaler().getId());
        }
        this.autoScalerInstances.clear();
    }

    private void restoreInstances() throws Exception {
        File storageDir = getConfiguration().getStorageDir();
        logger.debug("Restoring autoscaler instances from {}", storageDir.getAbsolutePath());
        List<File> instanceDirs = FileUtils.listDirectories(storageDir);
        for (File instanceDir : instanceDirs) {
            AutoScalerInstance instance = AutoScalerInstance.restore(instanceDir, this.config.getAddonSubsytems());
            this.autoScalerInstances.put(instance.getAutoScaler().getId(), instance);
        }
    }

    /**
     * Verifies that this factory is in {@link State#STARTED}. If it is not, an
     * {@link IllegalStateException} is thrown.
     *
     * @throws IllegalStateException
     */
    private void ensureStarted() throws IllegalStateException {
        checkState(isStarted(), "operation not permitted when factory is stopped");
    }

    /**
     * Prepares the factory's storage directory for use, creating it if it does
     * not already exist.
     *
     * @param storageDir
     *            The storage directory.
     * @throws IllegalArgumentException
     */
    private void prepareStorageDir(File storageDir) throws IllegalArgumentException {
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                throw new IllegalArgumentException(
                        format("autoScalerFactory: failed to create storageDir '%s'", storageDir));
            }
        }
    }

    private void validateBlueprint(AutoScalerBlueprint blueprint) {
        checkArgument(blueprint.id().isPresent(), "autoscaler blueprint is missing an id");
        String id = blueprint.id().get();

        // verify that instance id is valid
        checkArgument(VALID_ID_PATTERN.matcher(id).matches(),
                format("Invalid auto-scaler instance id '%s': " + "ids must match regular expression '%s'", id,
                        VALID_ID_PATTERN.pattern()));

        // verify that instance id is unique
        checkArgument(!this.autoScalerInstances.containsKey(id),
                format("An auto-scaler instance with id '%s' already exists", id));
    }

    /**
     * Verifies the existence of a certain {@link AutoScaler} instance id. If no
     * such instance exists, an {@link IllegalArgumentException} is thrown.
     *
     * @param autoScalerId
     */
    private void validateAutoScalerId(String autoScalerId) throws IllegalArgumentException {
        checkArgument(this.autoScalerInstances.containsKey(autoScalerId),
                format("The specified auto-scaler id '%s' does not exist.", autoScalerId));
    }

    private static <T> Class<T> loadClass(String className, Class<T> type) {
        try {
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) AutoScalerFactory.class.getClassLoader().loadClass(className);
            return clazz;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Failed to load implementation class '%s' for " + "type %s: %s", className, type,
                            e.getMessage()),
                    e);
        }
    }

    /**
     * Pairs up an {@link AutoScaler} instance with the
     * {@link AutoScalerBlueprint} that was used to create it and provides
     * methods for persisting the {@link AutoScaler} instance.
     */
    static class AutoScalerInstance {
        /** The {@link AutoScaler} instance. */
        private final AutoScaler autoScaler;
        /**
         * The blueprint that was used to create the {@link AutoScaler}
         * instance.
         */
        private final AutoScalerBlueprint blueprint;
        /** The storage directory of this {@link AutoScaler} instance. */
        private final File storageDir;

        public AutoScalerInstance(AutoScaler autoScaler, AutoScalerBlueprint blueprint, File storageDir) {
            this.autoScaler = autoScaler;
            this.blueprint = blueprint;
            this.storageDir = storageDir;
        }

        /**
         * Restores an {@link AutoScalerInstance} from the contents of an
         * instance storage directory. The storage directory must, as a minimum,
         * contain a {@code blueprint.json} file and a {@code status.json} file.
         *
         * @param instanceStorageDir
         *            The storage directory of the {@link AutoScaler} instance.
         * @param addonSubsystems
         *            The addon-subsystems that the restored {@link AutoScaler}
         *            instance will be created with.
         * @throws Exception
         */
        public static AutoScalerInstance restore(File instanceStorageDir, Map<String, String> addonSubsystems)
                throws Exception {
            File uuidFile = new File(instanceStorageDir, AUTOSCALER_UUID_FILE);
            File blueprintFile = new File(instanceStorageDir, BLUEPRINT_FILE);
            File configFile = new File(instanceStorageDir, CONFIG_FILE);
            File statusFile = new File(instanceStorageDir, STATUS_FILE);
            checkArgument(uuidFile.isFile(), "instance directory %s missing %s file", instanceStorageDir,
                    AUTOSCALER_UUID_FILE);
            checkArgument(blueprintFile.isFile(), "instance directory %s missing %s file", instanceStorageDir,
                    BLUEPRINT_FILE);
            checkArgument(statusFile.isFile(), "instance directory %s missing %s file", instanceStorageDir,
                    STATUS_FILE);

            UUID uuid = UUID.fromString(IoUtils.toString(uuidFile, StandardCharsets.UTF_8).trim());
            AutoScalerBlueprint blueprint = toObject(parseJsonFile(blueprintFile), AutoScalerBlueprint.class);
            Type stringMapType = new TypeToken<Map<String, String>>() {
            }.getType();

            AutoScaler autoScaler = buildAutoScaler(uuid, blueprint, addonSubsystems, instanceStorageDir);
            // an autoscaler instance may not have been configured yet, and
            // therefore may not have a config
            if (configFile.isFile()) {
                JsonObject config = parseJsonFile(configFile).getAsJsonObject();
                autoScaler.validate(config);
                autoScaler.configure(config);
            }
            ServiceStatus status = toObject(parseJsonFile(statusFile), ServiceStatus.class);
            if (status.getState() == State.STARTED) {
                autoScaler.start();
            }
            return new AutoScalerInstance(autoScaler, blueprint, instanceStorageDir);
        }

        public AutoScaler getAutoScaler() {
            return this.autoScaler;
        }

        public AutoScalerBlueprint getBlueprint() {
            return this.blueprint;
        }

        public File getStorageDir() {
            return this.storageDir;
        }

        /**
         * Saves the state of this {@link AutoScalerInstance} to its storage
         * directory. More specifically, the instance's blueprint and
         * configuration (if set) are stored under the instance's storage
         * directory.
         */
        public void save() {
            try {
                if (!this.storageDir.exists()) {
                    if (!this.storageDir.mkdirs()) {
                        throw new RuntimeException(format("failed to create instance directory '%s'", this.storageDir));
                    }
                }
                saveUuid();
                saveBlueprint();
                saveConfig();
                saveStatus();
            } catch (Exception e) {
                throw new RuntimeException(
                        format("failed to save AutoScaler instance '%s': %s", this.autoScaler.getId(), e.getMessage()),
                        e);
            }
        }

        /**
         * Saves the {@link UUID} of this {@link AutoScalerInstance}.
         */
        private void saveUuid() throws IOException {
            File uuidFile = new File(this.storageDir, AUTOSCALER_UUID_FILE);
            Files.write(uuidFile.toPath(), this.autoScaler.getUuid().toString().getBytes(StandardCharsets.UTF_8));
        }

        /**
         * Saves the blueprint of this {@link AutoScalerInstance}.
         */
        private void saveBlueprint() throws IOException {
            String blueprintAsJson = JsonUtils.toPrettyString(JsonUtils.toJson(this.blueprint));
            File blueprintFile = new File(this.storageDir, BLUEPRINT_FILE);
            Files.write(blueprintFile.toPath(), blueprintAsJson.getBytes(StandardCharsets.UTF_8));
        }

        /**
         * Saves the configuration of this {@link AutoScalerInstance} (if any).
         *
         * @throws IOException
         */
        private void saveConfig() throws IOException {
            JsonObject config = this.autoScaler.getConfiguration();
            if (config != null) {
                String configAsJson = JsonUtils.toPrettyString(config);
                File configFile = new File(this.storageDir, CONFIG_FILE);
                Files.write(configFile.toPath(), configAsJson.getBytes(StandardCharsets.UTF_8));
            }
        }

        /**
         * Saves the runtime {@link ServiceStatus} of this
         * {@link AutoScalerInstance}.
         *
         * @throws IOException
         */
        private void saveStatus() throws IOException {
            ServiceStatus status = this.autoScaler.getStatus();
            String statusAsJson = JsonUtils.toPrettyString(JsonUtils.toJson(status));
            File statusFile = new File(this.storageDir, STATUS_FILE);
            Files.write(statusFile.toPath(), statusAsJson.getBytes(StandardCharsets.UTF_8));
        }
    }
}
