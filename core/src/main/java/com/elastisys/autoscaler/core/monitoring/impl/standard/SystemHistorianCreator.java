package com.elastisys.autoscaler.core.monitoring.impl.standard;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamerAlias;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamerConfig;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.SystemHistorianAlias;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.SystemHistorianConfig;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.impl.noop.NoOpSystemHistorian;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

/**
 * A {@link SystemHistorian} factory used by the
 * {@link StandardMonitoringSubsystem} to instantiate {@link SystemHistorian}s
 * and inject any of a set of supported dependencies.
 * <p/>
 * A {@link SystemHistorian} implementation can have the following objects
 * injected (via a constructor {@link Inject} annotation):
 * <ul>
 * <li>{@code Uuid}-named {@link UUID}: the {@link AutoScaler}'s UUID.</li>
 * <li>{@code Id}-named {@link String}: the {@link AutoScaler}'s ID.</li>
 * <li>{@link Logger}: the {@link AutoScaler} instance's logger</li>
 * <li>{@link ExecutorService} or {@link ScheduledExecutorService}: the
 * {@link AutoScaler} instance's {@link ScheduledExecutorService}.</li>
 * <li>{@link EventBus}: the {@link AutoScaler}'s {@link EventBus}.</li>
 * <li>{@code StorageDir}-named {@link File}: the {@link AutoScaler}'s storage
 * directory.</li>
 * </ul>
 */
class SystemHistorianCreator extends AbstractModule {
    private final UUID autoScalerUuid;
    private final String autoScalerId;
    private final Logger logger;
    private final EventBus eventBus;
    private final ScheduledExecutorService executor;
    private final File storageDir;

    /**
     * Create a {@link SystemHistorianCreator} capable of instantiating objects
     * and injecting dependencies into those instances.
     *
     * @param autoScalerId
     * @param autoScalerUuid
     * @param logger
     * @param eventBus
     * @param executor
     * @param storageDir
     */
    public SystemHistorianCreator(UUID autoScalerUuid, String autoScalerId, Logger logger, EventBus eventBus,
            ScheduledExecutorService executor, File storageDir) {
        this.autoScalerUuid = autoScalerUuid;
        this.autoScalerId = autoScalerId;
        this.logger = logger;
        this.eventBus = eventBus;
        this.executor = executor;
        this.storageDir = storageDir;
    }

    /**
     * Instantiates a {@link MetricStreamer} from a
     * {@link MetricStreamerConfig}. The type can either be specified as a
     * {@link MetricStreamerAlias} or as a qualified class name.
     *
     * @param config
     * @return
     * @throws ClassNotFoundException
     */
    public MetricStreamer<?> createMetricStreamer(MetricStreamerConfig config) throws ClassNotFoundException {
        checkArgument(config != null, "metricStreamer config cannot be null");
        checkArgument(config.getType() != null, "metricStreamer type cannot be null");
        checkArgument(!config.getType().isEmpty(), "metricStreamer type cannot be empty string");

        String type = config.getType();
        if (isMetricStreamerAlias(type)) {
            type = MetricStreamerAlias.valueOf(type).getQualifiedClassName();
        }

        return (MetricStreamer<?>) instantiate(getClass().getClassLoader().loadClass(type));
    }

    /**
     * Returns <code>true</code> if the given type is a
     * {@link MetricStreamerAlias}, <code>false</code> otherwise.
     *
     * @param type
     * @return
     */
    private boolean isMetricStreamerAlias(String type) {
        try {
            MetricStreamerAlias.valueOf(type);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Instantiates a {@link SystemHistorian} from a
     * {@link SystemHistorianConfig}. The type can either be specified as a
     * {@link SystemHistorianAlias} or as a qualified class name.
     *
     * @param config
     *            The {@link SystemHistorianConfig} from which to create a
     *            {@link SystemHistorian}. May be <code>null</code>, which
     *            causes a {@link NoOpSystemHistorian} to be created.
     * @return
     * @throws ClassNotFoundException
     */
    public SystemHistorian<?> createSystemHistorian(SystemHistorianConfig config) throws ClassNotFoundException {
        checkArgument(config != null, "systemHistorian config cannot be null");
        checkArgument(config.getType() != null, "systemHistorian type cannot be null");
        checkArgument(!config.getType().isEmpty(), "systemHistorian type cannot be empty string");

        String type = config.getType();
        if (isSystemHistorianAlias(type)) {
            type = SystemHistorianAlias.valueOf(type).getQualifiedClassName();
        }

        return (SystemHistorian<?>) instantiate(getClass().getClassLoader().loadClass(type));
    }

    /**
     * Returns <code>true</code> if the given type is a
     * {@link SystemHistorianAlias}, <code>false</code> otherwise.
     *
     * @param type
     * @return
     */
    private boolean isSystemHistorianAlias(String type) {
        try {
            SystemHistorianAlias.valueOf(type);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Instantiate an object of a given type and inject any dependencies (as
     * specified by {@link Inject} annotations on the class).
     *
     * @param componentType
     * @return
     */
    private <T> T instantiate(Class<T> componentType) {
        return Guice.createInjector(this).getInstance(componentType);
    }

    @Override
    protected void configure() {
        bind(Logger.class).toInstance(this.logger);
        bind(EventBus.class).toInstance(this.eventBus);
        bind(ScheduledExecutorService.class).toInstance(this.executor);
        bind(ExecutorService.class).toInstance(this.executor);
        // Value for File parameter with @Name("StorageDir")
        bind(File.class).annotatedWith(Names.named("StorageDir")).toInstance(this.storageDir);
        // Value for String parameter with @Name("Uuid")
        bind(UUID.class).annotatedWith(Names.named("Uuid")).toInstance(this.autoScalerUuid);
        // Value for String parameter with @Name("AutoScalerId")
        bind(String.class).annotatedWith(Names.named("AutoScalerId")).toInstance(this.autoScalerId);
    }
}