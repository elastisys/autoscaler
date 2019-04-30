package com.elastisys.autoscaler.core.monitoring.impl.standard;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamerAlias;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamerConfig;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.TypeLiteral;

/**
 * A {@link MetricStreamer} factory used by the
 * {@link StandardMonitoringSubsystem} to instantiate {@link MetricStreamer}s
 * and inject any of a set of supported dependencies.
 * <p/>
 * A {@link MetricStreamer} implementation can have the following objects
 * injected (via a constructor {@link Inject} annotation):
 * <ul>
 * <li>{@link Logger}: the {@link AutoScaler} instance's logger</li>
 * <li>{@link ExecutorService} or {@link ScheduledExecutorService}: the
 * {@link AutoScaler} instance's {@link ScheduledExecutorService}.</li>
 * <li>{@link EventBus}: the {@link AutoScaler}'s {@link EventBus}.</li>
 * <li>{@code List<MetricStreamer<?>>}: a list of {@link MetricStreamer}s that
 * were declared (and instantiated) prior to the {@link MetricStreamer} being
 * created. This can be useful for a {@link MetricStreamer} that needs to
 * consume metrics from (and hence reference) other {@link MetricStreamer}s. To
 * prevent cyclic dependencies, a {@link MetricStreamer} is only allowed to
 * reference {@link MetricStreamer}s declared before itself.</li>
 * </ul>
 */
class MetricStreamerCreator extends AbstractModule {
    private final Logger logger;
    private final EventBus eventBus;
    private final ScheduledExecutorService executor;
    private final List<MetricStreamer<?>> priorDeclaredMetricStreamers;

    /**
     * Create a {@link MetricStreamerCreator} capable of instantiating objects
     * and injecting dependencies into those instances.
     *
     * @param logger
     * @param eventBus
     * @param executor
     * @param priorDeclaredMetricStreamers
     */
    public MetricStreamerCreator(Logger logger, EventBus eventBus, ScheduledExecutorService executor,
            List<MetricStreamer<?>> priorDeclaredMetricStreamers) {
        this.logger = requireNonNull(logger, "logger cannot be null");
        this.eventBus = requireNonNull(eventBus, "eventBus cannot be null");
        this.executor = requireNonNull(executor, "executor cannot be null");
        this.priorDeclaredMetricStreamers = requireNonNull(priorDeclaredMetricStreamers,
                "priorDeclaredMetricStreamers cannot be null");
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
        bind(new TypeLiteral<List<MetricStreamer<?>>>() {
        }).toInstance(this.priorDeclaredMetricStreamers);
    }
}