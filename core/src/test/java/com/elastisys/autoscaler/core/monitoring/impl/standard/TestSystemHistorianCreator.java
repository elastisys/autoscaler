package com.elastisys.autoscaler.core.monitoring.impl.standard;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.monitoring.impl.standard.config.SystemHistorianAlias;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.SystemHistorianConfig;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.impl.noop.NoOpSystemHistorian;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.google.gson.JsonObject;

/**
 * Verifies the behavior of the {@link MonitoringComponentCreator} - that it can
 * instantiate and wire up {@link MetricStreamer} and {@link SystemHistorian}
 * implementations.
 */
public class TestSystemHistorianCreator {
    private static final UUID autoScalerUuid = UUID.randomUUID();
    private static final String autoScalerId = "autoscalerId";
    private static final Logger logger = LoggerFactory.getLogger(TestSystemHistorianCreator.class);
    private static final EventBus eventBus = new SynchronousEventBus(logger);
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static final File storageDir = new File("target/");

    /** Object under test. */
    private SystemHistorianCreator creator;

    @Before
    public void beforeTestMethod() {
        this.creator = new SystemHistorianCreator(autoScalerUuid, autoScalerId, logger, eventBus, executor, storageDir);
    }

    /**
     * Instantiate and wire up a {@link SystemHistorian} by its full class name.
     */
    @Test
    public void createSystemHistorianFromFullyQualifiedClass() throws ClassNotFoundException {
        // create
        Class<DummySystemHistorian> implClass = DummySystemHistorian.class;
        SystemHistorian<?> systemHistorian = this.creator
                .createSystemHistorian(new SystemHistorianConfig(implClass.getName(), new JsonObject()));

        assertThat(systemHistorian.getClass().getName(), is(implClass.getName()));
        DummySystemHistorian instance = DummySystemHistorian.class.cast(systemHistorian);
        assertThat(instance.autoScalerUuid, is(autoScalerUuid));
        assertThat(instance.autoScalerId, is(autoScalerId));
        assertThat(instance.logger, is(logger));
        assertThat(instance.eventBus, is(eventBus));
        assertThat(instance.executor, is(executor));
        assertThat(instance.storageDir, is(storageDir));
    }

    /**
     * It should be possible to specify one of the supported
     * {@link SystemHistorianAlias}es, when building a {@link SystemHistorian}.
     */
    @Test
    public void createSystemHistorianFromAlias() {
        // The creation is expected to fail since OpenTsdbSystemHistorian class
        // is not on the classpath in autoscaler.core. However, if an attempt
        // has been made to load the class by its full name, that is evidence
        // enough that the alias worked.
        try {
            this.creator.createSystemHistorian(new SystemHistorianConfig("OpenTsdbSystemHistorian", new JsonObject()));
            fail("expected to fail");
        } catch (ClassNotFoundException e) {
            assertTrue(e.getMessage().contains(SystemHistorianAlias.OpenTsdbSystemHistorian.getQualifiedClassName()));
        }
    }

    /**
     * Creation from a null config should fail.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createSystemHistorianFromNullConfig() throws ClassNotFoundException {
        this.creator.createSystemHistorian(null);
    }

    /**
     * Creation from a null type name should fail.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createSystemHistorianFromNullType() throws ClassNotFoundException {
        this.creator.createSystemHistorian(new SystemHistorianConfig(null, new JsonObject()));
    }

    /**
     * Creation from an empty type name should fail.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createSystemHistorianFromEmptyType() throws ClassNotFoundException {
        this.creator.createSystemHistorian(new SystemHistorianConfig("", new JsonObject()));
    }

    /**
     * Creation from an unknown type should fail.
     */
    @Test(expected = ClassNotFoundException.class)
    public void createSystemHistorianFromUnknownType() throws ClassNotFoundException {
        this.creator.createSystemHistorian(new SystemHistorianConfig("unknown.ClassName", new JsonObject()));
    }

    /**
     * Dummy implementation that will have its dependencies wired by the
     * {@link MonitoringComponentCreator}.
     */
    private static class DummySystemHistorian extends NoOpSystemHistorian {
        final UUID autoScalerUuid;
        final String autoScalerId;
        final Logger logger;
        final EventBus eventBus;
        final ScheduledExecutorService executor;
        final File storageDir;

        @Inject
        public DummySystemHistorian(Logger logger, @Named("Uuid") UUID autoScalerUuid,
                @Named("AutoScalerId") String autoScalerId, EventBus eventBus, ScheduledExecutorService executor,
                @Named("StorageDir") File storageDir) {
            super(logger);
            this.autoScalerUuid = autoScalerUuid;
            this.autoScalerId = autoScalerId;
            this.logger = logger;
            this.eventBus = eventBus;
            this.executor = executor;
            this.storageDir = storageDir;
        }
    }
}
