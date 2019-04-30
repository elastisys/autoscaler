package com.elastisys.autoscaler.core.autoscaler.builder;

import java.io.File;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.inject.Singleton;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.elastisys.autoscaler.core.alerter.api.Alerter;
import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.metronome.api.Metronome;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.AsynchronousEventBus;
import com.elastisys.scale.commons.util.concurrent.RestartableScheduledExecutorService;
import com.elastisys.scale.commons.util.concurrent.StandardRestartableScheduledExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

/**
 * Guice module that given the implementation classes specified in a
 * {@link AutoScalerBuilder} takes care of wiring up an {@link AutoScaler} and
 * each of its subsystems.
 * <p/>
 * The {@link AutoScalerBuilder} makes use of a {@link AutoScalerModule}
 * internally to create an {@link AutoScaler} instance.
 * <p/>
 * The {@link AutoScalerModule} can be passed to
 * {@link Guice#createInjector(com.google.inject.Module...)} in order to get an
 * {@link Injector} capable of instantiating and wiring up an {@link AutoScaler}
 * instance:
 *
 * <pre>
 * AutoScaler.Builder builder = ...
 * Injector injector = Guice.createInjector(new AutoScalerModule(builder));
 * AutoScaler instance = injector.getInstance(AutoScaler.class);
 * </pre>
 *
 * @see AutoScalerBuilder
 */
@SuppressWarnings("rawtypes")
class AutoScalerModule extends AbstractModule {

    /**
     * The size of the thread pool allocated to each {@link AutoScaler}
     * instance. Note that the thread pool is shared by all {@link AutoScaler}
     * components that make use of the {@link ExecutorService} and
     * {@link EventBus}.
     */
    private static final int MAX_THREAD_POOL_SIZE = 15;
    private final AutoScalerBuilder builder;

    /**
     * Constructs an {@link AutoScalerModule} from the subsystem implementations
     * specified by an {@link AutoScalerBuilder}.
     *
     * @param builder
     *            The {@link AutoScalerBuilder} that specifies which subsystem
     *            implementation classes to use for the {@link AutoScaler}
     *            instance.
     */
    public AutoScalerModule(AutoScalerBuilder builder) {
        this.builder = builder;
    }

    @Override
    protected void configure() {
        // logger instance which can, optionally, be specified in builder
        Logger logger = Optional.ofNullable(this.builder.getLogger()).orElse(loggerInstance());
        bind(Logger.class).toInstance(logger);

        // hardwired components for the AutoScaler instance
        RestartableScheduledExecutorService executorService = executorService();
        bind(EventBus.class).toInstance(new AsynchronousEventBus(executorService, logger));
        bind(RestartableScheduledExecutorService.class).toInstance(executorService);
        bind(ScheduledExecutorService.class).toInstance(executorService);
        bind(ExecutorService.class).toInstance(executorService);

        // core subsystems (Services) of the AutoScaler instance
        bind(MonitoringSubsystem.class).to(this.builder.getMonitoringSubsystem()).in(Singleton.class);
        bind(Alerter.class).to(this.builder.getAlerter()).in(Singleton.class);
        bind(Metronome.class).to(this.builder.getMetronome()).in(Singleton.class);
        bind(PredictionSubsystem.class).to(this.builder.getPredictionSubsystem()).in(Singleton.class);
        bind(CloudPoolProxy.class).to(this.builder.getCloudPoolProxy()).in(Singleton.class);

        // also bind the entire add-on subsystems map to a Map<String, Service>
        MapBinder<String, Service> mapBinder = MapBinder.newMapBinder(binder(), String.class, Service.class);
        for (Entry<String, Class<Service>> addon : this.builder.getAddonSubsystems().entrySet()) {
            String addonName = addon.getKey();
            Class<Service> addonClass = addon.getValue();
            mapBinder.addBinding(addonName).to(addonClass).in(Singleton.class);
        }

        // Value for String parameter with @Name("Uuid")
        bind(UUID.class).annotatedWith(Names.named("Uuid")).toInstance(this.builder.getUuid());
        // Value for String parameter with @Name("AutoScalerId")
        bind(String.class).annotatedWith(Names.named("AutoScalerId")).toInstance(this.builder.getId());
        // Value for File parameter with @Name("StorageDir")
        bind(File.class).annotatedWith(Names.named("StorageDir")).toInstance(this.builder.getStorageDir());
    }

    private Logger loggerInstance() {
        return LoggerFactory.getLogger(AutoScaler.class.getPackage().getName() + "-" + this.builder.getId());
    }

    /**
     * Returns an executor service that is intended to be used to run sub-tasks
     * (threads) in this {@link AutoScaler} instance.
     * <p/>
     * The returned {@link RestartableScheduledExecutorService} will be in a
     * started state (that is, ready for use).
     * <p/>
     * The returned executor service will use an underlying
     * {@link ThreadFactory} that injects a logging context particular to the
     * {@link AutoScaler} instance being built into all {@link Thread}s it
     * creates. This logging context will contain a
     * <a href="http://logback.qos.ch/manual/mdc.html">Mapped Diagnostics
     * Context</a> with properties for the {@code autoScalerId} and
     * {@code autoScalerDir}, which, for example, can be used with a
     * <a href="http://logback.qos.ch/manual/appenders.html#SiftingAppender" >
     * sifting appender</a> to generate instance-specific log files.
     *
     * @return
     */
    private RestartableScheduledExecutorService executorService() {
        String autoScalerId = this.builder.getId();

        ThreadFactory threadFactory = new BasicThreadFactory.Builder().namingPattern(autoScalerId + "-thread-%d")
                .wrappedFactory(new LogContextInjectingThreadFactory(autoScalerId)).build();
        // note: the thread pool won't spawn new threads above the pool size
        RestartableScheduledExecutorService executorService = new StandardRestartableScheduledExecutorService(
                MAX_THREAD_POOL_SIZE, threadFactory);
        executorService.start();
        return executorService;
    }

    /**
     * A {@link ThreadFactory} that injects a logging context particular to the
     * {@link AutoScaler} instance being built into all {@link Thread}s it
     * creates.
     * <p/>
     * The injected logging context will contain a
     * <a href="http://logback.qos.ch/manual/mdc.html">Mapped Diagnostics
     * Context</a> with properties for the {@code autoScalerId} and
     * {@code autoScalerDir}, which, for example, can be used with a
     * <a href="http://logback.qos.ch/manual/appenders.html#SiftingAppender" >
     * sifting appender</a> to produce an instance-specific log file.
     */
    private static class LogContextInjectingThreadFactory implements ThreadFactory {

        /** Id of the {@link AutoScaler} instance. */
        private final String autoScalerId;

        public LogContextInjectingThreadFactory(String autoScalerId) {
            this.autoScalerId = autoScalerId;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r) {
                @Override
                public void run() {
                    // inserts autoscaler-instance specific MDC properties prior
                    // to starting the execution of each thread.
                    MDC.put("autoScalerId", LogContextInjectingThreadFactory.this.autoScalerId);
                    super.run();
                }
            };
        }
    }
}
