package com.elastisys.autoscaler.core.autoscaler.builder;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.alerter.api.Alerter;
import com.elastisys.autoscaler.core.alerter.impl.standard.StandardAlerter;
import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactory;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.cloudpool.impl.StandardCloudPoolProxy;
import com.elastisys.autoscaler.core.metronome.api.Metronome;
import com.elastisys.autoscaler.core.metronome.impl.standard.StandardMetronome;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.impl.standard.StandardMonitoringSubsystem;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;
import com.elastisys.autoscaler.core.prediction.impl.standard.StandardPredictionSubsystem;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Builder for constructing new {@link AutoScaler} instances. The builder
 * requires implementation classes for all subsystems of the {@link AutoScaler}
 * to be specified as well as a storage directory (where instance state will be
 * stored).
 *
 * @see AutoScaler
 */
@SuppressWarnings("rawtypes")
public class AutoScalerBuilder {
    /** Universally unique identifier of the {@link AutoScaler} instance. */
    private UUID uuid;
    /** Id/name of the {@link AutoScaler} instance. */
    private String id;
    /**
     * The {@link Logger} instance to be injected in the {@link AutoScaler}
     * instance. May be <code>null</code>, in which case a default logger will
     * be injected.
     */
    private Logger logger;
    /** {@link MonitoringSubsystem} implementation. */
    private Class<? extends MonitoringSubsystem> monitoringSubsystem;
    /** {@link Alerter} subsystem implementation. */
    private Class<? extends Alerter> alerter;
    /** {@link Metronome} subsystem implementation. */
    private Class<? extends Metronome> metronome;
    /** {@link PredictionSubsystem} subsystem implementation. */
    private Class<? extends PredictionSubsystem> predictionSubsystem;
    /** {@link CloudPoolProxy} subsystem implementation. */
    private Class<? extends CloudPoolProxy> cloudPoolProxy;
    /**
     * The add-on subsystems, which are not strictly necessary for the
     * {@link AutoScaler} to operate, but which may extend it with additional
     * functionality. Accounting and high-availability are two examples of what
     * such add-on subsystems could achieve. Keys are names, such as
     * {@code accountingSubsystem}, and values are classes, such as
     * {@code com.elastisys.AccountingSubsystemImpl}.
     */
    private Map<String, Class<Service>> addonSubsystems;
    /**
     * File system path to the directory where the {@link AutoScaler} instance
     * will persist instance state.
     */
    private File storageDir;

    public AutoScalerBuilder() {
        this.addonSubsystems = new HashMap<>();
    }

    /**
     * Creates a new {@link AutoScalerBuilder}.
     *
     * @return
     */
    public static AutoScalerBuilder newBuilder() {
        return new AutoScalerBuilder();
    }

    /**
     * Creates an {@link AutoScalerBuilder} from a source {@link AutoScaler}
     * instance. The {@link AutoScalerBuilder} will use the source
     * {@link AutoScaler} as blueprint, in that the source's subsystem
     * implementation classes will be used to create new {@link AutoScaler}
     * instances.
     *
     * @param source
     *            A source {@link AutoScaler} that specifies the implementation
     *            classes to use for the various subsystems of new
     *            {@link AutoScaler}s created by the builder.
     * @return The {@link AutoScalerBuilder}.
     */
    public static AutoScalerBuilder newBuilderFromSource(AutoScaler source) {
        checkArgument(source != null, "source autoscaler instance must not be null");

        AutoScalerBuilder copyBuilder = newBuilder().withUuid(source.getUuid()).withId(source.getId())
                .withStorageDir(source.getStorageDir()).withAlerter(source.getAlerter().getClass())
                .withCloudPoolProxy(source.getCloudPoolProxy().getClass())
                .withMonitoringSubsystem(source.getMonitoringSubsystem().getClass())
                .withMetronome(source.getMetronome().getClass())
                .withPredictionSubsystem(source.getPredictionSubsystem().getClass());

        // copy any add-on subsystems
        Map<String, Service> addons = source.getAddonSubsystems();
        if (!addons.isEmpty()) {
            for (String addonName : addons.keySet()) {
                Class<Service> addonClass = (Class<Service>) addons.get(addonName).getClass();
                copyBuilder.withAddonSubsystem(addonName, addonClass);
            }
        }

        return copyBuilder;
    }

    /**
     * Instantiates an {@link AutoScaler} instance from the specified subsystem
     * implementation classes.
     *
     * @return
     */
    public AutoScaler build() {
        validateParameters();
        Injector injector = Guice.createInjector(new AutoScalerModule(this));
        return injector.getInstance(AutoScaler.class);
    }

    private void validateParameters() {
        checkArgument(this.uuid != null, "missing autoscaler UUID");
        checkArgument(this.id != null, "missing autoscaler id");
        checkArgument(this.monitoringSubsystem != null, "missing MonitoringSubsystem implementation class");
        checkArgument(this.alerter != null, "missing Alerter implementation class");
        checkArgument(this.metronome != null, "missing Metronome implementation class");
        checkArgument(this.predictionSubsystem != null, "missing PredictionSubsystem implementation class");
        checkArgument(this.cloudPoolProxy != null, "missing CloudPoolProxy implementation class");
        checkArgument(this.storageDir != null, "missing storage directory");
        checkArgument(this.storageDir.isDirectory() || !this.storageDir.exists(),
                "storage directory '%s' is not a valid directory path", this.storageDir.getAbsolutePath());
    }

    /**
     * Sets the universally unique identifier of the {@link AutoScaler} instance
     * being built.
     *
     * @param uuid
     * @return
     */
    public AutoScalerBuilder withUuid(UUID uuid) {
        checkArgument(uuid != null, "uuid cannot be null");
        this.uuid = uuid;
        return this;
    }

    /**
     * Sets the Id/name of the {@link AutoScaler} instance being built.
     *
     * @param id
     * @return
     */
    public AutoScalerBuilder withId(String id) {
        checkArgument(id != null, "id cannot be null");
        this.id = id;
        return this;
    }

    /**
     * Sets the {@link MonitoringSubsystem} implementation class name of the
     * {@link AutoScaler} instance being built.
     *
     * @param monitoringSubsystemClass
     * @return
     */
    public AutoScalerBuilder withMonitoringSubsystem(String monitoringSubsystemClass) {
        return withMonitoringSubsystem(loadClass(monitoringSubsystemClass, MonitoringSubsystem.class));
    }

    /**
     * Sets the {@link MonitoringSubsystem} implementation class of the
     * {@link AutoScaler} instance being built.
     *
     * @param metricStreamer
     * @return
     */
    public AutoScalerBuilder withMonitoringSubsystem(Class<? extends MonitoringSubsystem> monitoringSubsystem) {
        checkArgument(monitoringSubsystem != null, "monitoringSubsystem cannot be null");
        this.monitoringSubsystem = monitoringSubsystem;
        return this;
    }

    /**
     * Sets the {@link Alerter} subsystem implementation class name of the
     * {@link AutoScaler} instance being built.
     *
     * @param alerterClass
     * @return
     */
    public AutoScalerBuilder withAlerter(String alerterClass) {
        return withAlerter(loadClass(alerterClass, Alerter.class));
    }

    /**
     * Sets the {@link Alerter} subsystem implementation class of the
     * {@link AutoScaler} instance being built.
     *
     * @param alerter
     * @return
     */
    public AutoScalerBuilder withAlerter(Class<? extends Alerter> alerter) {
        checkArgument(alerter != null, "alerter cannot be null");
        this.alerter = alerter;
        return this;
    }

    /**
     * Sets the {@link CloudPoolProxy} subsystem implementation class name of
     * the {@link AutoScaler} instance being built.
     *
     * @param cloudPoolProxyClass
     * @return
     */
    public AutoScalerBuilder withCloudPoolProxy(String cloudPoolProxyClass) {
        return withCloudPoolProxy(loadClass(cloudPoolProxyClass, CloudPoolProxy.class));
    }

    /**
     * Sets the {@link CloudPoolProxy} subsystem implementation class of the
     * {@link AutoScaler} instance being built.
     *
     * @param cloudPoolProxy
     * @return
     */
    public AutoScalerBuilder withCloudPoolProxy(Class<? extends CloudPoolProxy> cloudPoolProxy) {
        checkArgument(cloudPoolProxy != null, "cloudPoolProxy cannot be null");
        this.cloudPoolProxy = cloudPoolProxy;
        return this;
    }

    /**
     * Sets the {@link Metronome} subsystem implementation class name of the
     * {@link AutoScaler} instance being built.
     *
     * @param metronomeClass
     * @return
     */
    public AutoScalerBuilder withMetronome(String metronomeClass) {
        return withMetronome(loadClass(metronomeClass, Metronome.class));
    }

    /**
     * Sets the {@link Metronome} subsystem implementation class of the
     * {@link AutoScaler} instance being built.
     *
     * @param metronome
     * @return
     */
    public AutoScalerBuilder withMetronome(Class<? extends Metronome> metronome) {
        checkArgument(metronome != null, "metronome cannot be null");
        this.metronome = metronome;
        return this;
    }

    /**
     * Sets the {@link PredictionSubsystem} implementation class name of the
     * {@link AutoScaler} instance being built.
     *
     * @param predictionSubsystemClass
     * @return
     */
    public AutoScalerBuilder withPredictionSubsystem(String predictionSubsystemClass) {
        return withPredictionSubsystem(loadClass(predictionSubsystemClass, PredictionSubsystem.class));
    }

    /**
     * Sets the {@link PredictionSubsystem} implementation class of the
     * {@link AutoScaler} instance being built.
     *
     * @param predictionSubsystem
     * @return
     */
    public AutoScalerBuilder withPredictionSubsystem(Class<? extends PredictionSubsystem> predictionSubsystem) {
        checkArgument(predictionSubsystem != null, "predictionSubsystem cannot be null");
        this.predictionSubsystem = predictionSubsystem;
        return this;
    }

    /**
     * Appends an add-on subsystem to the collection of add-on subsystems set
     * for this builder. These add-on subsystems are not strictly necessary for
     * the {@link AutoScaler} to operate, but may extend it with additional
     * functionality. Accounting and high-availability are two examples of what
     * such add-on subsystems could achieve. Keys are , and values are classes,
     * such as {@code com.elastisys.AccountingSubsystemImpl}.
     *
     * @param subsystemName
     *            A name, such as names, {@code accountingSubsystem}.
     * @param subsystemClassName
     * @return
     */
    public AutoScalerBuilder withAddonSubsystem(String subsystemName, String subsystemClassName) {
        checkArgument(subsystemName != null, "add-on subsystem: subsystemName cannot be null");
        checkArgument(subsystemClassName != null, "add-on subsystem: subsystemClassName cannot be null");
        try {
            return withAddonSubsystem(subsystemName, loadClass(subsystemClassName, Service.class));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("failed to add add-on subsystem '%s': %s", subsystemName, e.getMessage()), e);
        }
    }

    /**
     * Appends an add-on subsystem to the collection of add-on subsystems set
     * for this builder. These add-on subsystems are not strictly necessary for
     * the {@link AutoScaler} to operate, but may extend it with additional
     * functionality. Accounting and high-availability are two examples of what
     * such add-on subsystems could achieve. Keys are , and values are classes,
     * such as {@code com.elastisys.AccountingSubsystemImpl}.
     *
     * @param subsystemName
     *            A name, such as names, {@code accountingSubsystem}.
     * @param subsystemClass
     * @return
     */
    public AutoScalerBuilder withAddonSubsystem(String subsystemName, Class<? extends Service> subsystemClass) {
        checkArgument(subsystemName != null, "subsystemName cannot be null");
        checkArgument(subsystemClass != null, "subsystemClass cannot be null");

        Class<Service> serviceClass = (Class<Service>) subsystemClass;
        this.addonSubsystems.put(subsystemName, serviceClass);
        return this;
    }

    /**
     * Sets the storage directory to use for the {@link AutoScaler} instance
     * being built.
     *
     * @param storageDir
     * @return
     */
    public AutoScalerBuilder withStorageDir(File storageDir) {
        checkArgument(storageDir != null, "storageDir cannot be null");
        // defensive copy
        this.storageDir = new File(storageDir.getAbsolutePath());
        return this;
    }

    /**
     * Sets the {@link Logger} to use for the {@link AutoScaler} instance being
     * built.
     *
     * @param logger
     * @return
     */
    public AutoScalerBuilder withLogger(Logger logger) {
        checkArgument(logger != null, "logger cannot be null");
        this.logger = logger;
        return this;
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

    public Class<? extends MonitoringSubsystem> getMonitoringSubsystem() {
        return this.monitoringSubsystem;
    }

    public Class<? extends Alerter> getAlerter() {
        return this.alerter;
    }

    public Class<? extends Metronome> getMetronome() {
        return this.metronome;
    }

    public Class<? extends CloudPoolProxy> getCloudPoolProxy() {
        return this.cloudPoolProxy;
    }

    public Class<? extends PredictionSubsystem> getPredictionSubsystem() {
        return this.predictionSubsystem;
    }

    /**
     * The add-on subsystems, which are not strictly necessary for the
     * {@link AutoScaler} to operate, but which may extend it with additional
     * functionality. Accounting and high-availability are two examples of what
     * such add-on subsystems could achieve. Keys are names, such as
     * {@code accountingSubsystem}, and values are classes, such as
     * {@code com.elastisys.AccountingSubsystemImpl}.
     *
     * @return
     */
    public Map<String, Class<Service>> getAddonSubsystems() {
        return this.addonSubsystems;
    }

    public File getStorageDir() {
        return this.storageDir;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    /**
     * Loads a given class of a given type. On failure, an
     * {@link IllegalArgumentException} is thrown.
     *
     * @param className
     *            The name of the class to load.
     * @param type
     *            The expected (super) type of the class.
     * @return The loaded class.
     * @throws IllegalArgumentException
     */
    public static <T> Class<T> loadClass(String className, Class<T> type) throws IllegalArgumentException {
        try {
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) AutoScalerFactory.class.getClassLoader().loadClass(className);
            return clazz;
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("failed to load implementation class '%s' for type %s: %s",
                    className, type, e.getMessage()), e);
        }
    }

    /**
     * Default {@link AutoScalerBuilder} parameters, including default subsystem
     * implementations (for subsystems that have a default implementation) and
     * default storage directory.
     *
     * @see AutoScalerBuilder
     */
    public static class Defaults {
        public static final Class<? extends MonitoringSubsystem<?>> MONITORING_SUBSYSTEM = StandardMonitoringSubsystem.class;
        public static final Class<? extends Alerter<?>> ALERTER = StandardAlerter.class;
        public static final Class<? extends Metronome<?>> METRONOME = StandardMetronome.class;
        public static final Class<? extends PredictionSubsystem<?>> PREDICTION_SUBSYSTEM = StandardPredictionSubsystem.class;
        public static final Class<? extends CloudPoolProxy<?>> CLOUD_POOL_PROXY = StandardCloudPoolProxy.class;
        public static File STORAGE_DIR = FileUtils.cwd();
    }

}