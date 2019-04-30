package com.elastisys.autoscaler.core.monitoring.impl.standard;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.autoscaler.builder.stubs.NoOpMetricStreamerStub;
import com.elastisys.autoscaler.core.autoscaler.builder.stubs.NoOpSystemHistorianStub;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamMonitorConfig;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamerConfig;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.StandardMonitoringSubsystemConfig;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.SystemHistorianConfig;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.stubs.MetricStreamerStub;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.stubs.MetricStreamerStubConfig;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.stubs.MetricStreamerStubStreamDefinition;
import com.elastisys.autoscaler.core.monitoring.streammonitor.MetricStreamMonitor;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.impl.noop.NoOpSystemHistorian;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.stubs.SystemHistorianStub;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.stubs.SystemHistorianStubConfig;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.google.gson.JsonObject;

/**
 * Exercises the {@link StandardMonitoringSubsystem}.
 */
public class TestStandardMonitoringSubsystem {

    private static final Logger LOG = LoggerFactory.getLogger(TestStandardMonitoringSubsystem.class);

    private static final String VALID_METRIC_STREAMER_CLASS = MetricStreamerStub.class.getName();
    private static final String INVALID_METRIC_STREAMER_CLASS = "unrecognized.MetricStreamerImpl";
    private static final String VALID_SYSTEM_HISTORIAN_CLASS = SystemHistorianStub.class.getName();
    private static final String INVALID_SYSTEM_HISTORIAN_CLASS = "unrecognized.SystemHistorianImpl";

    private static final UUID autoScalerUuid = UUID.fromString("5b7b9fae-97cb-4283-abe7-39ac6beef58b");
    private static final String autoScalerId = "autoscaler1";
    private static final EventBus eventBus = new SynchronousEventBus(LOG);
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static final File storageDir = new File("target");

    /** Object under test. */
    private StandardMonitoringSubsystem monitoringSubsystem;

    @Before
    public void beforeTestMethod() {
        this.monitoringSubsystem = new StandardMonitoringSubsystem(autoScalerUuid, autoScalerId, LOG, eventBus,
                executor, storageDir);
    }

    /**
     * When applied, a valid {@link StandardMonitoringSubsystemConfig}, should
     * result in the specified {@link MetricStreamer} and
     * {@link SystemHistorian} being wired up and configured.
     */
    @Test
    public void configureWithCompleteConfig() {
        StandardMonitoringSubsystemConfig config = new StandardMonitoringSubsystemConfig(
                Arrays.asList(new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS, validMetricStreamerStubConfig())),
                new SystemHistorianConfig(VALID_SYSTEM_HISTORIAN_CLASS, validSystemHistorianStubConfig()),
                validMetricStreamMonitorConfig());
        this.monitoringSubsystem.validate(config);
        this.monitoringSubsystem.configure(config);

        // verify that the instantiated sub-components are set up as expected
        List<MetricStreamer<?>> metricStreamers = this.monitoringSubsystem.getMetricStreamers();
        SystemHistorian<?> systemHistorian = this.monitoringSubsystem.getSystemHistorian();
        MetricStreamMonitor metricStreamMonitor = this.monitoringSubsystem.getMetricStreamMonitor();

        // make sure correct implementation classes were instantiated for
        // sub-components
        assertThat(metricStreamers.get(0).getClass().getName(), is(VALID_METRIC_STREAMER_CLASS));
        assertThat(systemHistorian.getClass().getName(), is(VALID_SYSTEM_HISTORIAN_CLASS));

        // make sure configs were indeed applied to sub-components
        assertThat(JsonUtils.toJson(metricStreamers.get(0).getConfiguration()), is(validMetricStreamerStubConfig()));
        assertThat(JsonUtils.toJson(systemHistorian.getConfiguration()), is(validSystemHistorianStubConfig()));
        assertThat(metricStreamMonitor.getConfiguration(), is(validMetricStreamMonitorConfig()));

        // should be in a stopped state (until monitoring subsystem is started)
        assertThat(metricStreamers.get(0).getStatus().getState(), is(ServiceStatus.State.STOPPED));
        assertThat(systemHistorian.getStatus().getState(), is(ServiceStatus.State.STOPPED));
        assertThat(metricStreamMonitor.getStatus().getState(), is(ServiceStatus.State.STOPPED));
    }

    /**
     * Only {@link MetricStreamer} is a mandatory configuration parameters.
     * Defaults are provided for {@link SystemHistorian} (a
     * {@link NoOpSystemHistorian}) and the {@link MetricStreamMonitor}
     * configuration.
     */
    @Test
    public void configureWithDefaults() {
        StandardMonitoringSubsystemConfig config = new StandardMonitoringSubsystemConfig(
                Arrays.asList(new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS, validMetricStreamerStubConfig())),
                null, null);
        this.monitoringSubsystem.validate(config);
        this.monitoringSubsystem.configure(config);

        // verify that the instantiated sub-components are set up as expected
        SystemHistorian<?> systemHistorian = this.monitoringSubsystem.getSystemHistorian();
        MetricStreamMonitor metricStreamMonitor = this.monitoringSubsystem.getMetricStreamMonitor();

        // the default system historian is a no-op implementation
        assertThat(systemHistorian.getClass().getName(),
                is(StandardMonitoringSubsystemConfig.DEFAULT_SYSTEM_HISTORIAN.getType()));

        // make sure default configuration was applied to metric stream monitor
        assertThat(metricStreamMonitor.getConfiguration(),
                is(StandardMonitoringSubsystemConfig.DEFAULT_METRIC_STREAM_MONITOR_CONFIG));
    }

    /**
     * A {@link MonitoringSubsystem} should support several
     * {@link MetricStreamer}s.
     */
    @Test
    public void configureWithMultipleMetricStreamers() {
        MetricStreamerConfig metricStreamerConfig1 = new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS,
                validMetricStreamerStubConfig());
        MetricStreamerConfig metricStreamerConfig2 = new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS,
                validMetricStreamerStub2Config());
        StandardMonitoringSubsystemConfig config = new StandardMonitoringSubsystemConfig(
                Arrays.asList(metricStreamerConfig1, metricStreamerConfig2), null, null);
        this.monitoringSubsystem.validate(config);
        this.monitoringSubsystem.configure(config);

        List<MetricStreamer<?>> metricStreamers = this.monitoringSubsystem.getMetricStreamers();
        assertThat(metricStreamers.size(), is(2));
        assertThat(metricStreamers.get(0).getClass().getName(), is(VALID_METRIC_STREAMER_CLASS));
        assertThat(metricStreamers.get(1).getClass().getName(), is(VALID_METRIC_STREAMER_CLASS));

        assertThat(JsonUtils.toJson(metricStreamers.get(0).getConfiguration()), is(validMetricStreamerStubConfig()));
        assertThat(JsonUtils.toJson(metricStreamers.get(1).getConfiguration()), is(validMetricStreamerStub2Config()));
    }

    /**
     * To allow a {@link MetricStreamer} to consume values from another
     * {@link MetricStreamer}, a {@link MetricStreamer} may choose to have the
     * prior declared {@link MetricStreamer}s injected on creation. The reason
     * that only prior declared {@link MetricStreamer}s can be referenced is to
     * prevent cyclic dependencies.
     */
    @Test
    public void allowMetricStreamerReferences() {
        MetricStreamerConfig metricStreamerConfig1 = new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS,
                validMetricStreamerStubConfig());
        MetricStreamerConfig metricStreamerConfig2 = new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS,
                validMetricStreamerStub2Config());
        MetricStreamerConfig metricStreamerConfig3 = new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS,
                validMetricStreamerStub3Config());
        StandardMonitoringSubsystemConfig config = new StandardMonitoringSubsystemConfig(
                Arrays.asList(metricStreamerConfig1, metricStreamerConfig2, metricStreamerConfig3), null, null);
        this.monitoringSubsystem.validate(config);
        this.monitoringSubsystem.configure(config);

        List<MetricStreamer<?>> metricStreamers = this.monitoringSubsystem.getMetricStreamers();
        assertThat(metricStreamers.size(), is(3));

        MetricStreamerStub firstMetricStreamer = MetricStreamerStub.class.cast(metricStreamers.get(0));
        MetricStreamerStub secondMetricStreamer = MetricStreamerStub.class.cast(metricStreamers.get(1));
        MetricStreamerStub thirdMetricStreamer = MetricStreamerStub.class.cast(metricStreamers.get(2));

        assertThat(firstMetricStreamer.getPriorDeclaredMetricStreamers(), is(Collections.emptyList()));
        assertThat(secondMetricStreamer.getPriorDeclaredMetricStreamers(), is(Arrays.asList(firstMetricStreamer)));
        assertThat(thirdMetricStreamer.getPriorDeclaredMetricStreamers(),
                is(Arrays.asList(firstMetricStreamer, secondMetricStreamer)));
    }

    /**
     * {@link MetricStream} ids must be unique across all
     * {@link MetricStreamer}s.
     */
    @Test
    public void configureWithDuplicateStreamIds() {
        // note: both metric streamers define a stream with the same name
        // (cpu.stream)
        MetricStreamerStubConfig config1 = new MetricStreamerStubConfig("localhost", 12345, 60,
                Arrays.asList(new MetricStreamerStubStreamDefinition("cpu.stream", "cpu.percent", "SUM")));
        MetricStreamerConfig metricStreamerConfig1 = new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS,
                JsonUtils.toJson(config1).getAsJsonObject());

        MetricStreamerStubConfig config2 = new MetricStreamerStubConfig("otherhost", 12345, 60,
                Arrays.asList(new MetricStreamerStubStreamDefinition("cpu.stream", "cpu.total", "SUM")));
        MetricStreamerConfig metricStreamerConfig2 = new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS,
                JsonUtils.toJson(config2).getAsJsonObject());

        StandardMonitoringSubsystemConfig config = new StandardMonitoringSubsystemConfig(
                Arrays.asList(metricStreamerConfig1, metricStreamerConfig2), null, null);
        try {
            this.monitoringSubsystem.validate(config);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("duplicate metricStream id: cpu.stream"));
        }
    }

    /**
     * Validation should fail when the {@link MetricStreamer} implementation
     * class specified in the config cannot be instantiated.
     */
    @Test
    public void validationOnIllegalMetricStreamerClass() {
        StandardMonitoringSubsystemConfig config = new StandardMonitoringSubsystemConfig(
                Arrays.asList(new MetricStreamerConfig(INVALID_METRIC_STREAMER_CLASS, validMetricStreamerStubConfig())),
                new SystemHistorianConfig(VALID_SYSTEM_HISTORIAN_CLASS, validSystemHistorianStubConfig()),
                validMetricStreamMonitorConfig());
        try {
            this.monitoringSubsystem.validate(config);
            fail("unexpectedly succeeded to validate illegal config ");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("could not instantiate metricStreamer"));
        }
    }

    /**
     * Validation should fail when the {@link SystemHistorian} implementation
     * class specified in the config cannot be instantiated.
     */
    @Test
    public void validationOnIllegalSystemHistorianClass() {
        StandardMonitoringSubsystemConfig config = new StandardMonitoringSubsystemConfig(
                Arrays.asList(new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS, validMetricStreamerStubConfig())),
                new SystemHistorianConfig(INVALID_SYSTEM_HISTORIAN_CLASS, validSystemHistorianStubConfig()),
                validMetricStreamMonitorConfig());
        try {
            this.monitoringSubsystem.validate(config);
            fail("unexpectedly succeeded to validate illegal config ");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("could not instantiate systemHistorian"));
        }
    }

    /**
     * Validation should fail when the {@link MetricStreamer} configuration
     * document fails to be validated by the instantiated
     * {@link MetricStreamer}.
     */
    @Test
    public void validationOnIllegalMetricStreamerConfig() {
        StandardMonitoringSubsystemConfig config = new StandardMonitoringSubsystemConfig(
                Arrays.asList(new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS, invalidMetricStreamerStubConfig())),
                new SystemHistorianConfig(VALID_SYSTEM_HISTORIAN_CLASS, validSystemHistorianStubConfig()),
                validMetricStreamMonitorConfig());
        try {
            this.monitoringSubsystem.validate(config);
            fail("unexpectedly succeeded to validate illegal config ");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("could not apply config: metricStreamer: missing host"));
        }
    }

    /**
     * Validation should fail when the {@link SystemHistorian} configuration
     * document fails to be validated by the instantiated
     * {@link SystemHistorian}.
     */
    @Test
    public void validationOnIllegalSystemHistorianConfig() {
        StandardMonitoringSubsystemConfig config = new StandardMonitoringSubsystemConfig(
                Arrays.asList(new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS, validMetricStreamerStubConfig())),
                new SystemHistorianConfig(VALID_SYSTEM_HISTORIAN_CLASS, invalidSystemHistorianStubConfig()),
                validMetricStreamMonitorConfig());
        try {
            this.monitoringSubsystem.validate(config);
            fail("unexpectedly succeeded to validate illegal config ");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("systemHistorian config could not be applied"));
        }
    }

    /**
     * Validation should fail when the {@link MetricStreamMonitor} configuration
     * is invalid.
     */
    @Test
    public void validationOnIllegalMetricStreamMonitorConfig() {
        StandardMonitoringSubsystemConfig config = new StandardMonitoringSubsystemConfig(
                Arrays.asList(new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS, validMetricStreamerStubConfig())),
                new SystemHistorianConfig(VALID_SYSTEM_HISTORIAN_CLASS, validSystemHistorianStubConfig()),
                invalidMetricStreamMonitorConfig());
        try {
            this.monitoringSubsystem.validate(config);
            fail("unexpectedly succeeded to validate illegal config ");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("metricStreamMonitor"));
        }
    }

    /**
     * Make sure that sub-components get replaced and re-configured when a new
     * configuration document is applied to the
     * {@link StandardMonitoringSubsystem}.
     */
    @Test
    public void reconfigure() {
        StandardMonitoringSubsystemConfig config = new StandardMonitoringSubsystemConfig(
                Arrays.asList(new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS, validMetricStreamerStubConfig())),
                new SystemHistorianConfig(VALID_SYSTEM_HISTORIAN_CLASS, validSystemHistorianStubConfig()),
                validMetricStreamMonitorConfig());
        this.monitoringSubsystem.validate(config);
        this.monitoringSubsystem.configure(config);

        List<MetricStreamer<?>> metricStreamers = this.monitoringSubsystem.getMetricStreamers();
        SystemHistorian<?> systemHistorian = this.monitoringSubsystem.getSystemHistorian();
        MetricStreamMonitor metricStreamMonitor = this.monitoringSubsystem.getMetricStreamMonitor();
        // verify implementation classes for sub-components
        assertThat(metricStreamers.get(0).getClass().getName(), is(VALID_METRIC_STREAMER_CLASS));
        assertThat(systemHistorian.getClass().getName(), is(VALID_SYSTEM_HISTORIAN_CLASS));
        // verify configurations for sub-components
        assertThat(JsonUtils.toJson(metricStreamers.get(0).getConfiguration()), is(validMetricStreamerStubConfig()));
        assertThat(JsonUtils.toJson(systemHistorian.getConfiguration()), is(validSystemHistorianStubConfig()));
        assertThat(metricStreamMonitor.getConfiguration(), is(validMetricStreamMonitorConfig()));

        // re-configure
        JsonObject newMetricStreamerConfig = JsonUtils.parseJsonString("{\"a\": 1}").getAsJsonObject();
        JsonObject newSystemHistorianConfig = JsonUtils.parseJsonString("{\"b\": 2}").getAsJsonObject();
        MetricStreamMonitorConfig newMetricStreamMonitorConfig = new MetricStreamMonitorConfig(
                new TimeInterval(60L, TimeUnit.SECONDS), new TimeInterval(900L, TimeUnit.SECONDS));
        StandardMonitoringSubsystemConfig newConfig = new StandardMonitoringSubsystemConfig(
                Arrays.asList(
                        new MetricStreamerConfig(NoOpMetricStreamerStub.class.getName(), newMetricStreamerConfig)),
                new SystemHistorianConfig(NoOpSystemHistorianStub.class.getName(), newSystemHistorianConfig),
                newMetricStreamMonitorConfig);
        this.monitoringSubsystem.validate(newConfig);
        this.monitoringSubsystem.configure(newConfig);

        // verify that the new configuration took effect
        metricStreamers = this.monitoringSubsystem.getMetricStreamers();
        systemHistorian = this.monitoringSubsystem.getSystemHistorian();
        metricStreamMonitor = this.monitoringSubsystem.getMetricStreamMonitor();
        // verify implementation classes for sub-components
        assertThat(metricStreamers.get(0).getClass().getName(), is(NoOpMetricStreamerStub.class.getName()));
        assertThat(systemHistorian.getClass().getName(), is(NoOpSystemHistorianStub.class.getName()));
        // verify configurations for sub-components
        assertThat(JsonUtils.toJson(metricStreamers.get(0).getConfiguration()), is(newMetricStreamerConfig));
        assertThat(JsonUtils.toJson(systemHistorian.getConfiguration()), is(newSystemHistorianConfig));
        assertThat(metricStreamMonitor.getConfiguration(), is(newMetricStreamMonitorConfig));
    }

    /**
     * A configuration should either be entirely applied or not applied at all.
     * If either sub-configuration fails to be validated/applied, the prior
     * {@link MonitoringSubsystem} state should be kept.
     */
    @Test
    public void configurationIsAtomic() {
        // set up pre-test monitoring subsystem
        StandardMonitoringSubsystemConfig config = new StandardMonitoringSubsystemConfig(
                Arrays.asList(new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS, validMetricStreamerStubConfig())),
                new SystemHistorianConfig(VALID_SYSTEM_HISTORIAN_CLASS, validSystemHistorianStubConfig()),
                validMetricStreamMonitorConfig());
        this.monitoringSubsystem.validate(config);
        this.monitoringSubsystem.configure(config);
        assertThat(this.monitoringSubsystem.getMetricStreamers().get(0).getClass().getName(),
                is(VALID_METRIC_STREAMER_CLASS));
        assertThat(this.monitoringSubsystem.getSystemHistorian().getClass().getName(),
                is(VALID_SYSTEM_HISTORIAN_CLASS));
        assertThat(JsonUtils.toJson(this.monitoringSubsystem.getMetricStreamers().get(0).getConfiguration()),
                is(validMetricStreamerStubConfig()));
        assertThat(JsonUtils.toJson(this.monitoringSubsystem.getSystemHistorian().getConfiguration()),
                is(validSystemHistorianStubConfig()));
        assertThat(this.monitoringSubsystem.getMetricStreamMonitor().getConfiguration(),
                is(validMetricStreamMonitorConfig()));

        // try to apply a (partly) illegal configuration (bad metric streamer
        // config

        StandardMonitoringSubsystemConfig illegalConfig = new StandardMonitoringSubsystemConfig(
                Arrays.asList(new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS, invalidMetricStreamerStubConfig())),
                new SystemHistorianConfig(VALID_SYSTEM_HISTORIAN_CLASS, validSystemHistorianStubConfig()),
                validMetricStreamMonitorConfig());
        try {
            this.monitoringSubsystem.configure(illegalConfig);
            fail("unexpectedly succeeded to apply illegal config ");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // verify that none of the configuration was applied, i.e. the system
        // looks exactly like it did prior to applying the illegal config
        assertThat(this.monitoringSubsystem.getMetricStreamers().get(0).getClass().getName(),
                is(VALID_METRIC_STREAMER_CLASS));
        assertThat(this.monitoringSubsystem.getSystemHistorian().getClass().getName(),
                is(VALID_SYSTEM_HISTORIAN_CLASS));
        assertThat(JsonUtils.toJson(this.monitoringSubsystem.getMetricStreamers().get(0).getConfiguration()),
                is(validMetricStreamerStubConfig()));
        assertThat(JsonUtils.toJson(this.monitoringSubsystem.getSystemHistorian().getConfiguration()),
                is(validSystemHistorianStubConfig()));
        assertThat(this.monitoringSubsystem.getMetricStreamMonitor().getConfiguration(),
                is(validMetricStreamMonitorConfig()));
    }

    /**
     * Starting and stopping the {@link MonitoringSubsystem} should propagate to
     * the sub-components.
     */
    @Test
    public void startAndStop() {
        StandardMonitoringSubsystemConfig config = new StandardMonitoringSubsystemConfig(
                Arrays.asList(new MetricStreamerConfig(VALID_METRIC_STREAMER_CLASS, validMetricStreamerStubConfig())),
                new SystemHistorianConfig(VALID_SYSTEM_HISTORIAN_CLASS, validSystemHistorianStubConfig()),
                validMetricStreamMonitorConfig());
        this.monitoringSubsystem.validate(config);
        this.monitoringSubsystem.configure(config);

        List<MetricStreamer<?>> metricStreamers = this.monitoringSubsystem.getMetricStreamers();
        SystemHistorian<?> systemHistorian = this.monitoringSubsystem.getSystemHistorian();
        MetricStreamMonitor metricStreamMonitor = this.monitoringSubsystem.getMetricStreamMonitor();

        // should be in a stopped state (until monitoring subsystem is started)
        assertThat(metricStreamers.get(0).getStatus().getState(), is(ServiceStatus.State.STOPPED));
        assertThat(systemHistorian.getStatus().getState(), is(ServiceStatus.State.STOPPED));
        assertThat(metricStreamMonitor.getStatus().getState(), is(ServiceStatus.State.STOPPED));

        // start
        this.monitoringSubsystem.start();

        assertThat(metricStreamers.get(0).getStatus().getState(), is(ServiceStatus.State.STARTED));
        assertThat(systemHistorian.getStatus().getState(), is(ServiceStatus.State.STARTED));
        assertThat(metricStreamMonitor.getStatus().getState(), is(ServiceStatus.State.STARTED));

        // stop
        this.monitoringSubsystem.stop();

        assertThat(metricStreamers.get(0).getStatus().getState(), is(ServiceStatus.State.STOPPED));
        assertThat(systemHistorian.getStatus().getState(), is(ServiceStatus.State.STOPPED));
        assertThat(metricStreamMonitor.getStatus().getState(), is(ServiceStatus.State.STOPPED));

        // restart
        this.monitoringSubsystem.start();

        assertThat(metricStreamers.get(0).getStatus().getState(), is(ServiceStatus.State.STARTED));
        assertThat(systemHistorian.getStatus().getState(), is(ServiceStatus.State.STARTED));
        assertThat(metricStreamMonitor.getStatus().getState(), is(ServiceStatus.State.STARTED));
    }

    /**
     * It should not be possible to start a {@link MonitoringSubsystem} before
     * it has been configured.
     */
    @Test(expected = IllegalStateException.class)
    public void startBeforeConfigured() {
        this.monitoringSubsystem.start();
    }

    private static JsonObject validMetricStreamerStubConfig() {
        MetricStreamerStubConfig config = new MetricStreamerStubConfig("some.host", 12345, 60,
                Arrays.asList(new MetricStreamerStubStreamDefinition("cpu.stream", "cpu.percent", "AVG")));
        return JsonUtils.toJson(config).getAsJsonObject();
    }

    private static JsonObject validMetricStreamerStub2Config() {
        MetricStreamerStubConfig config = new MetricStreamerStubConfig("other.host", 12345, 60,
                Arrays.asList(new MetricStreamerStubStreamDefinition("mem.stream", "mem.usage", "SUM")));
        return JsonUtils.toJson(config).getAsJsonObject();
    }

    private static JsonObject validMetricStreamerStub3Config() {
        MetricStreamerStubConfig config = new MetricStreamerStubConfig("third.host", 12345, 60,
                Arrays.asList(new MetricStreamerStubStreamDefinition("disk.stream", "disk.usage", "SUM")));
        return JsonUtils.toJson(config).getAsJsonObject();
    }

    private static JsonObject invalidMetricStreamerStubConfig() {
        // missing host
        MetricStreamerStubConfig config = new MetricStreamerStubConfig(null, 12345, 60,
                Arrays.asList(new MetricStreamerStubStreamDefinition("cpu.stream", "cpu.percent", "AVG")));
        return JsonUtils.toJson(config).getAsJsonObject();
    }

    private static JsonObject validSystemHistorianStubConfig() {
        return JsonUtils.toJson(new SystemHistorianStubConfig("some.host", 12345)).getAsJsonObject();
    }

    private static JsonObject invalidSystemHistorianStubConfig() {
        // missing host
        return JsonUtils.toJson(new SystemHistorianStubConfig(null, 12345)).getAsJsonObject();
    }

    private static MetricStreamMonitorConfig validMetricStreamMonitorConfig() {
        return new MetricStreamMonitorConfig(new TimeInterval(300L, TimeUnit.SECONDS),
                new TimeInterval(30L, TimeUnit.MINUTES));
    }

    private static MetricStreamMonitorConfig invalidMetricStreamMonitorConfig() {
        TimeInterval legalInterval = new TimeInterval(300L, TimeUnit.SECONDS);
        TimeInterval illegalInterval = JsonUtils.toObject(JsonUtils.parseJsonString("{\"time\": 1}"),
                TimeInterval.class);
        return new MetricStreamMonitorConfig(legalInterval, illegalInterval);
    }

}
