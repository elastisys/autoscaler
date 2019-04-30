package com.elastisys.autoscaler.core.monitoring.impl.standard;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.autoscaler.builder.stubs.NoOpMetricStreamerStub;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamerAlias;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamerConfig;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.google.gson.JsonObject;

/**
 * Verifies the behavior of the {@link MetricStreamerCreator} - that it can
 * instantiate and wire up {@link MetricStreamer} implementations.
 */
public class TestMetricStreamerCreator {
    private static final Logger logger = LoggerFactory.getLogger(TestMetricStreamerCreator.class);
    private static final EventBus eventBus = new SynchronousEventBus(logger);
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static final List<MetricStreamer<?>> priorDeclaredMetricStreamers = Arrays
            .asList(mock(MetricStreamer.class), mock(MetricStreamer.class));

    /** Object under test. */
    private MetricStreamerCreator creator;

    @Before
    public void beforeTestMethod() {
        this.creator = new MetricStreamerCreator(logger, eventBus, executor, priorDeclaredMetricStreamers);
    }

    /**
     * Instantiate and wire up a {@link MetricStreamer} by its full class name.
     */
    @Test
    public void createMetricStreamerFromFullyQualifiedClass() throws ClassNotFoundException {
        // create
        Class<DummyMetricStreamer> implClass = DummyMetricStreamer.class;
        MetricStreamer<?> metricStreamer = this.creator
                .createMetricStreamer(new MetricStreamerConfig(implClass.getName(), new JsonObject()));

        assertThat(metricStreamer.getClass().getName(), is(implClass.getName()));
        DummyMetricStreamer instance = DummyMetricStreamer.class.cast(metricStreamer);
        assertThat(instance.logger, is(logger));
        assertThat(instance.eventBus, is(eventBus));
        assertThat(instance.executor, is(executor));
        assertThat(instance.priorDeclaredMetricStreamers, is(priorDeclaredMetricStreamers));
    }

    /**
     * It should be possible to specify one of the supported
     * {@link MetricStreamerAlias}es, when building a {@link MetricStreamer}.
     */
    @Test
    public void createMetricStreamerFromAlias() {
        // The creation is expected to fail since OpenTsdbMetricStreamer class
        // is not on the classpath in autoscaler.core. However, if an attempt
        // has been made to load the class by its full name, that is evidence
        // enough that the alias worked.
        try {
            this.creator.createMetricStreamer(new MetricStreamerConfig("OpenTsdbMetricStreamer", new JsonObject()));
            fail("expected to fail");
        } catch (ClassNotFoundException e) {
            assertTrue(e.getMessage().contains(MetricStreamerAlias.OpenTsdbMetricStreamer.getQualifiedClassName()));
        }
    }

    /**
     * Creation from a null config should fail.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createMetricStreamerFromNullConfig() throws ClassNotFoundException {
        this.creator.createMetricStreamer(null);
    }

    /**
     * Creation from a null type name should fail.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createMetricStreamerFromNullType() throws ClassNotFoundException {
        this.creator.createMetricStreamer(new MetricStreamerConfig(null, new JsonObject()));
    }

    /**
     * Creation from an empty type name should fail.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createMetricStreamerFromEmptyType() throws ClassNotFoundException {
        this.creator.createMetricStreamer(new MetricStreamerConfig("", new JsonObject()));
    }

    /**
     * Creation from an unknown type should fail.
     */
    @Test(expected = ClassNotFoundException.class)
    public void createMetricStreamerFromUnknownType() throws ClassNotFoundException {
        this.creator.createMetricStreamer(new MetricStreamerConfig("unknown.ClassName", new JsonObject()));
    }

    /**
     * Dummy implementation that will have its dependencies wired by the
     * {@link MonitoringComponentCreator}.
     */
    private static class DummyMetricStreamer extends NoOpMetricStreamerStub {
        final Logger logger;
        final EventBus eventBus;
        final ScheduledExecutorService executor;
        final List<MetricStreamer<?>> priorDeclaredMetricStreamers;

        @Inject
        public DummyMetricStreamer(Logger logger, EventBus eventBus, ScheduledExecutorService executor,
                List<MetricStreamer<?>> priorDeclaredMetricStreamers) {
            super();
            this.logger = logger;
            this.eventBus = eventBus;
            this.executor = executor;
            this.priorDeclaredMetricStreamers = priorDeclaredMetricStreamers;
        }
    }
}
