package com.elastisys.autoscaler.core.prediction.impl.standard.predictor;

import static com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictorTypeAlias.ReactivePredictor;
import static com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictorTypeAlias.RuleBasedPredictor;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.prediction.api.PredictionException;
import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.util.file.FileUtils;

/**
 * Verifies the operation of the {@link PredictorFactory}. In particular, it
 * verifies that dependencies are properly {@link Inject}ed into instantiated
 * {@link Predictor}s.
 */
public class TestPredictorFactory {

    // injectable dependencies
    private final Logger factoryLogger = mock(Logger.class);
    private final EventBus factoryEventBus = mock(EventBus.class);
    private final ScheduledExecutorService factoryExecutorService = mock(ScheduledExecutorService.class);
    private final MonitoringSubsystem<?> factoryMonitoringSubsystem = mock(MonitoringSubsystem.class);

    /** Object under test. */
    private PredictorFactory factory;

    @Before
    public void onSetup() {
        this.factory = new PredictorFactory(this.factoryLogger, this.factoryEventBus, this.factoryExecutorService,
                this.factoryMonitoringSubsystem, FileUtils.cwd());
    }

    /**
     * It should be possible to create a {@link Predictor} without any
     * dependencies.
     */
    @Test
    public void instantiatePredictorWithoutDependencies() throws PredictorInstantiationException {
        Predictor predictor = this.factory.create(PredictorWithoutDependencies.class.getName());
        assertThat(predictor, is(not(nullValue())));
        assertThat(predictor, is(instanceOf(PredictorWithoutDependencies.class)));
    }

    /**
     * Verify that dependencies are properly injected.
     */
    @Test
    public void instantiatePredictorWithDependencies() throws PredictorInstantiationException {
        Predictor predictor = this.factory.create(PredictorWithDependencies.class.getName());
        assertThat(predictor, is(not(nullValue())));
        assertThat(predictor, is(instanceOf(PredictorWithDependencies.class)));
        PredictorWithDependencies predictorInstance = PredictorWithDependencies.class.cast(predictor);

        // verify that all dependencies were properly injected
        assertSame(predictorInstance.eventBus, this.factoryEventBus);
        assertSame(predictorInstance.executorService, this.factoryExecutorService);
        assertSame(predictorInstance.logger, this.factoryLogger);
        assertSame(predictorInstance.monitoringSubsystem, this.factoryMonitoringSubsystem);
    }

    /**
     * It should be possible to specify a short-hand alias (a
     * {@link PredictorTypeAlias}) instead of the full {@link Predictor} class
     * name when specifying the predictor type.
     */
    @Test
    public void predictorTypeAliasTranslation() {
        // verify that factory correctly translates an alias to its full class
        // name
        assertThat(this.factory.fullClassName(RuleBasedPredictor.name()), is(RuleBasedPredictor.getFullClassName()));
        assertThat(this.factory.fullClassName(ReactivePredictor.name()), is(ReactivePredictor.getFullClassName()));

        // verify that an attempt is made to instantiate a predictor from the
        // given alias type. the attempt is expected to fail, since the aliased
        // predictor classes are not on the classpath of the core module.
        try {
            this.factory.create(RuleBasedPredictor.name());
        } catch (Exception e) {
            assertThat(e.getMessage(), is("failed to instantiate predictor type 'RuleBasedPredictor': "
                    + RuleBasedPredictor.getFullClassName()));
        }
    }

    /**
     * {@link Predictor} requesting no dependencies to be {@link Inject}ed.
     *
     *
     */
    private static class PredictorWithoutDependencies extends NoOpPredictorAdapter {
        @Inject
        public PredictorWithoutDependencies() {
            super();
        }
    }

    /**
     * {@link Predictor} requesting several dependencies to be {@link Inject}ed.
     *
     *
     */
    private static class PredictorWithDependencies extends NoOpPredictorAdapter {
        private final Logger logger;
        private final EventBus eventBus;
        private final ScheduledExecutorService executorService;
        private final MonitoringSubsystem monitoringSubsystem;

        @Inject
        public PredictorWithDependencies(Logger logger, EventBus eventBus, ScheduledExecutorService executorService,
                MonitoringSubsystem monitoringSubsystem) {
            super();
            this.logger = logger;
            this.eventBus = eventBus;
            this.executorService = executorService;
            this.monitoringSubsystem = monitoringSubsystem;
        }
    }

    /**
     * Abstract {@link Predictor} class with no-op implementations of most
     * methods. Intended to be extended by concrete {@link Predictor}s in tests.
     *
     *
     */
    public static class NoOpPredictorAdapter implements Predictor {

        protected PredictorConfig config;
        protected boolean started;

        public NoOpPredictorAdapter() {
            this.config = null;
            this.started = false;
        }

        @Override
        public void start() {
            this.started = true;
        }

        @Override
        public void stop() {
            this.started = false;
        }

        @Override
        public ServiceStatus getStatus() {
            return new ServiceStatus.Builder().started(this.started).build();
        }

        @Override
        public void validate(PredictorConfig configuration) throws IllegalArgumentException {
        }

        @Override
        public void configure(PredictorConfig configuration) throws IllegalArgumentException {
            this.config = configuration;
        }

        @Override
        public PredictorConfig getConfiguration() {
            return this.config;
        }

        @Override
        public Class<PredictorConfig> getConfigurationClass() {
            return PredictorConfig.class;
        }

        @Override
        public Optional<Prediction> predict(Optional<PoolSizeSummary> poolSize, DateTime predictionTime)
                throws PredictionException {
            return Optional.empty();
        }
    }

}
