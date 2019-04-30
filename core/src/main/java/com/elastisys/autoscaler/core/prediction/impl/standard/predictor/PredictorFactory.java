package com.elastisys.autoscaler.core.prediction.impl.standard.predictor;

import static java.lang.String.format;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Named;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Names;

/**
 * Used to instantiate {@link Predictor}s.
 * <p/>
 * Implementation classes can make use of the {@link javax.inject.Inject}
 * annotation to have certain dependencies from the parent {@link AutoScaler}
 * injected. See
 * <a href="http://code.google.com/p/google-guice/wiki/Injections">this page</a>
 * for a summary of the supported types of dependency injection patterns.
 * <p/>
 * The following dependencies can be injected through the
 * {@link javax.inject.Inject} annotation:
 * <ul>
 * <li>{@link Logger}: the {@link AutoScaler} instance's logger</li>
 * <li>{@link ExecutorService} or {@link ScheduledExecutorService}: the
 * {@link AutoScaler} instance's executor service.</li>
 * <li>{@link EventBus}: the {@link AutoScaler}'s event bus.</li>
 * <li>{@link MonitoringSubsystem}: the {@link AutoScaler}'s
 * {@link MonitoringSubsystem}, which can be used to get hold of configured
 * {@link MetricStream}s.</li>
 * <li>{@code StorageDir}-named {@link File}: the {@link AutoScaler}'s storage
 * directory.</li>
 * </ul>
 *
 * @see PredictorRegistry
 */
class PredictorFactory {

    private final Logger logger;
    private final EventBus eventBus;
    private final ScheduledExecutorService executorService;
    private final MonitoringSubsystem<?> monitoringSubsystem;
    private final File storageDir;

    private Injector guiceInjector;

    /**
     * Constructs a new {@link PredictorFactory} capable of instantiating new
     * {@link Predictor}s. When creating instances, the {@link PredictorFactory}
     * will attempt to inject the instance with the specified dependencies.
     *
     * @param logger
     *            The {@link Logger} to inject in instances (if requested).
     * @param eventBus
     *            The {@link EventBus} to inject in instances (if requested).
     * @param executorService
     *            The {@link ExecutorService} to inject in instances (if
     *            requested).
     * @param monitoringSubsystem
     *            The {@link MonitoringSubsystem} to inject in instances (if
     *            requested).
     * @param storageDir
     *            The storage directory to inject in instances (if requested).
     */
    @Inject
    public PredictorFactory(Logger logger, EventBus eventBus, ScheduledExecutorService executorService,
            MonitoringSubsystem<?> monitoringSubsystem, @Named("StorageDir") File storageDir) {
        this.logger = logger;
        this.eventBus = eventBus;
        this.executorService = executorService;
        this.monitoringSubsystem = monitoringSubsystem;
        this.storageDir = storageDir;

        this.guiceInjector = Guice.createInjector(new PredictorModule());
    }

    /**
     * Instantiates a new {@link Predictor}.
     * <p/>
     * The factory will try to satisfy any injections that the implementation
     * class requires (via {@link javax.inject.Inject} annotations).
     *
     * @param predictorType
     *            The class name of the concrete predictor implementation.
     * @return A {@link Predictor} instance.
     * @throws PredictorInstantiationException
     */
    public Predictor create(String predictorType) throws PredictorInstantiationException {
        try {
            String fullClassName = fullClassName(predictorType);
            Class<? extends Predictor> predictorClass = loadPredictorClass(fullClassName);
            return instantiate(predictorClass);
        } catch (Exception e) {
            throw new PredictorInstantiationException(
                    format("failed to instantiate predictor type '%s': %s", predictorType, e.getMessage()), e);
        }
    }

    /**
     * Returns the full class name of a given predictor type. In case the type
     * refers to a {@link PredictorTypeAlias}, its fully qualified class name is
     * returned. If the type is not an alias, it is assumed to already be a
     * fully qualified class name and is returned as-is.
     *
     * @param predictorType
     * @return
     */
    String fullClassName(String predictorType) {
        try {
            return PredictorTypeAlias.valueOf(predictorType).getFullClassName();
        } catch (IllegalArgumentException e) {
            return predictorType;
        }
    }

    private Predictor instantiate(Class<? extends Predictor> predictorClass) {
        return this.guiceInjector.getInstance(predictorClass);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Predictor> loadPredictorClass(String predictorType) throws ClassNotFoundException {
        return (Class<? extends Predictor>) this.getClass().getClassLoader().loadClass(predictorType);
    }

    /**
     * A Guice module that defines dependency bindings to be injected in
     * instantiated {@link Predictor} instances.
     */
    private class PredictorModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(Logger.class).toInstance(PredictorFactory.this.logger);
            bind(EventBus.class).toInstance(PredictorFactory.this.eventBus);
            bind(ScheduledExecutorService.class).toInstance(PredictorFactory.this.executorService);
            bind(ExecutorService.class).toInstance(PredictorFactory.this.executorService);
            bind(MonitoringSubsystem.class).toInstance(PredictorFactory.this.monitoringSubsystem);
            // Value for File parameter with @Name("StorageDir")
            bind(File.class).annotatedWith(Names.named("StorageDir")).toInstance(PredictorFactory.this.storageDir);
        }
    }
}
