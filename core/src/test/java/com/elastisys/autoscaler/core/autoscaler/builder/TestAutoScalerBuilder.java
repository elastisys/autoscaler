package com.elastisys.autoscaler.core.autoscaler.builder;

import static com.elastisys.autoscaler.core.autoscaler.AutoScalerTestUtils.assertDefaultComponents;
import static com.elastisys.autoscaler.core.autoscaler.AutoScalerTestUtils.assertStorageDir;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.UUID;

import javax.inject.Singleton;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.addon.FakeAddon;
import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.autoscaler.AutoScalerTestUtils;
import com.elastisys.autoscaler.core.autoscaler.builder.AutoScalerBuilder;
import com.elastisys.autoscaler.core.autoscaler.builder.AutoScalerBuilder.Defaults;
import com.elastisys.autoscaler.core.autoscaler.builder.stubs.NoOpAlerterStub;
import com.elastisys.autoscaler.core.autoscaler.builder.stubs.NoOpCloudPoolProxyStub;
import com.elastisys.autoscaler.core.autoscaler.builder.stubs.NoOpMetronomeStub;
import com.elastisys.autoscaler.core.autoscaler.builder.stubs.NoOpMonitoringSubsystemStub;
import com.elastisys.autoscaler.core.autoscaler.builder.stubs.NoOpPredictionSubsystemStub;

/**
 * Verifies the behavior of the {@link AutoScalerBuilder}.
 */
@SuppressWarnings("rawtypes")
public class TestAutoScalerBuilder {

    @Test
    public void buildWithDefaultSubsystems() {
        AutoScaler autoScaler = buildDefaultAutoScaler("autoscaler1");
        assertDefaultComponents(autoScaler);
        assertStorageDir(autoScaler, Defaults.STORAGE_DIR);
        // should not include any add-on subsystems
        assertTrue(autoScaler.getAddonSubsystems().isEmpty());

        // should not yet have been configured
        assertNull(autoScaler.getConfiguration());
    }

    /**
     * Verifies that add-on subsystems can be passed to have the
     * {@link AutoScalerBuilder} include extra subsystems beyond the core ones
     * in created instances.
     */
    @Test
    public void buildWithAddonSubsystems() {
        String subsystemClassName = FakeAddon.class.getName();
        AutoScaler autoScaler = defaultBuilder("autoscaler1")
                .withAddonSubsystem("fakeAddon", subsystemClassName).build();

        Map<String, Service> subsystems = autoScaler.getAddonSubsystems();
        assertFalse("autoscaler expected to be created with add-on subsystems", subsystems.isEmpty());
        assertTrue("autoscaler expected to be created with an fake add-on",
                subsystems.containsKey("fakeAddon"));
        assertThat(subsystems.get("fakeAddon").getClass().getName(), is(subsystemClassName));
    }

    /**
     * Builds an {@link AutoScaler} by specifying subsystem implementation class
     * names (strings) rather than classes.
     */
    @Test
    public void buildBySpecifyingClassNames() {
        AutoScaler autoScaler = AutoScalerBuilder.newBuilder().withUuid(UUID.randomUUID()).withId("autoscaler1")
                .withStorageDir(Defaults.STORAGE_DIR).withAlerter(Defaults.ALERTER.getName())
                .withMonitoringSubsystem(Defaults.MONITORING_SUBSYSTEM.getName())
                .withMetronome(Defaults.METRONOME.getName())
                .withPredictionSubsystem(Defaults.PREDICTION_SUBSYSTEM.getName())
                .withCloudPoolProxy(Defaults.CLOUD_POOL_PROXY.getName()).build();
        assertDefaultComponents(autoScaler);
    }

    @Test
    public void buildWithMissingAlerter() {
        try {
            AutoScalerBuilder.newBuilder().withUuid(UUID.randomUUID()).withId("autoscaler1")
                    .withStorageDir(Defaults.STORAGE_DIR).withMonitoringSubsystem(Defaults.MONITORING_SUBSYSTEM)
                    .withMetronome(Defaults.METRONOME).withPredictionSubsystem(Defaults.PREDICTION_SUBSYSTEM)
                    .withCloudPoolProxy(Defaults.CLOUD_POOL_PROXY).build();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("missing Alerter implementation"));
        }
    }

    @Test
    public void buildWithMissingMonitoringSubsystem() {
        try {
            AutoScalerBuilder.newBuilder().withUuid(UUID.randomUUID()).withId("autoscaler1")
                    .withStorageDir(Defaults.STORAGE_DIR).withAlerter(Defaults.ALERTER)
                    .withMetronome(Defaults.METRONOME).withPredictionSubsystem(Defaults.PREDICTION_SUBSYSTEM)
                    .withCloudPoolProxy(Defaults.CLOUD_POOL_PROXY).build();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("missing MonitoringSubsystem implementation"));
        }
    }

    @Test
    public void buildWithMissingMetronome() {
        try {
            AutoScalerBuilder.newBuilder().withUuid(UUID.randomUUID()).withId("autoscaler1")
                    .withStorageDir(Defaults.STORAGE_DIR).withMonitoringSubsystem(Defaults.MONITORING_SUBSYSTEM)
                    .withAlerter(Defaults.ALERTER).withPredictionSubsystem(Defaults.PREDICTION_SUBSYSTEM)
                    .withCloudPoolProxy(Defaults.CLOUD_POOL_PROXY).build();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("missing Metronome implementation"));
        }

    }

    @Test
    public void buildWithMissingCloudPoolProxy() {
        try {
            AutoScalerBuilder.newBuilder().withUuid(UUID.randomUUID()).withId("autoscaler1")
                    .withStorageDir(Defaults.STORAGE_DIR).withAlerter(Defaults.ALERTER)
                    .withMonitoringSubsystem(Defaults.MONITORING_SUBSYSTEM).withMetronome(Defaults.METRONOME)
                    .withPredictionSubsystem(Defaults.PREDICTION_SUBSYSTEM).build();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("missing CloudPoolProxy implementation"));
        }

    }

    @Test
    public void buildWithMissingId() {
        try {
            AutoScalerBuilder.newBuilder().withUuid(UUID.randomUUID()).withStorageDir(Defaults.STORAGE_DIR)
                    .withAlerter(Defaults.ALERTER).withMonitoringSubsystem(Defaults.MONITORING_SUBSYSTEM)
                    .withMetronome(Defaults.METRONOME).withPredictionSubsystem(Defaults.PREDICTION_SUBSYSTEM)
                    .withCloudPoolProxy(Defaults.CLOUD_POOL_PROXY).build();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("missing autoscaler id"));
        }
    }

    @Test
    public void buildWithMissingUuid() {
        try {
            AutoScalerBuilder.newBuilder().withId("autoScalerId").withStorageDir(Defaults.STORAGE_DIR)
                    .withAlerter(Defaults.ALERTER).withMonitoringSubsystem(Defaults.MONITORING_SUBSYSTEM)
                    .withMetronome(Defaults.METRONOME).withPredictionSubsystem(Defaults.PREDICTION_SUBSYSTEM)
                    .withCloudPoolProxy(Defaults.CLOUD_POOL_PROXY).build();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("missing autoscaler UUID"));
        }

    }

    @Test
    public void buildWithMissingPredictionSubsystem() {
        try {
            AutoScalerBuilder.newBuilder().withUuid(UUID.randomUUID()).withId("autoscaler1")
                    .withStorageDir(Defaults.STORAGE_DIR).withAlerter(Defaults.ALERTER)
                    .withMonitoringSubsystem(Defaults.MONITORING_SUBSYSTEM).withMetronome(Defaults.METRONOME)
                    .withCloudPoolProxy(Defaults.CLOUD_POOL_PROXY).build();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("missing PredictionSubsystem implementation"));
        }
    }

    @Test
    public void buildWithMissingStorageDir() {
        try {
            AutoScalerBuilder.newBuilder().withUuid(UUID.randomUUID()).withId("autoScaler1")
                    .withAlerter(Defaults.ALERTER).withMonitoringSubsystem(Defaults.MONITORING_SUBSYSTEM)
                    .withMetronome(Defaults.METRONOME).withPredictionSubsystem(Defaults.PREDICTION_SUBSYSTEM)
                    .withCloudPoolProxy(Defaults.CLOUD_POOL_PROXY).build();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("missing storage directory"));
        }
    }

    @Test
    public void buildWithNullUuid() {
        try {
            AutoScalerBuilder.newBuilder().withUuid(null);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("uuid"));
        }
    }

    @Test
    public void buildWithNullId() {
        try {
            AutoScalerBuilder.newBuilder().withId(null);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("id"));
        }
    }

    @Test
    public void buildWithNullStorageDir() {
        try {
            AutoScalerBuilder.newBuilder().withStorageDir(null);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("storageDir"));
        }

    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithIllegalAlerterClass() {
        AutoScalerBuilder.newBuilder().withAlerter("bad.ImplClass");
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithIllegalMonitoringSubsystemClass() {
        AutoScalerBuilder.newBuilder().withMonitoringSubsystem("bad.ImplClass");
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithIllegalMetronomeClass() {
        AutoScalerBuilder.newBuilder().withMetronome("bad.ImplClass");
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithIllegalPredictionSubsystemClass() {
        AutoScalerBuilder.newBuilder().withPredictionSubsystem("bad.ImplClass");
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithIllegalCloudPoolProxyClass() {
        AutoScalerBuilder.newBuilder().withCloudPoolProxy("bad.ImplClass");
    }

    /**
     * A name must be specified for an add-on subsystem.
     */
    @Test(expected = IllegalArgumentException.class)
    public void buildWithNullAddOnSubsystemName() {
        AutoScalerBuilder.newBuilder().withAddonSubsystem(null, FakeAddon.class.getName());
    }

    /**
     * A class must be specified for an add-on subsystem.
     */
    @Test(expected = IllegalArgumentException.class)
    public void buildWithNullAddOnClass() {
        AutoScalerBuilder.newBuilder().withAddonSubsystem("extSubsystem", (String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithIllegalAddOnClass() {
        AutoScalerBuilder.newBuilder().withAddonSubsystem("extSubsystem", "bad.ImplClass");
    }

    /**
     * Instantiates multiple auto-scalers and verifies that they have been wired
     * with different objects in their object graphs.
     * <p/>
     * It thereby ensures that components created for one auto-scaler instance
     * don't leak over to other instances (that is, that the {@link Singleton}
     * scope only applies within an auto-scaler instance).
     */
    @Test
    public void autoScalerInstanceIsolation() {
        AutoScaler autoScaler1 = buildDefaultAutoScaler("autoscaler1");
        AutoScaler autoScaler2 = buildDefaultAutoScaler("autoscaler2");

        AutoScalerTestUtils.assertNotSameAutoScalers(autoScaler1, autoScaler2);
    }

    /**
     * It should be possible to pass a custom {@link Logger} instance to use by
     * the {@link AutoScaler}.
     */
    @Test
    public void buildWithCustomLogger() {
        Logger customLogger = LoggerFactory.getLogger("my.own.logger");
        AutoScaler autoScaler = AutoScalerBuilder.newBuilder().withUuid(UUID.randomUUID()).withId("autoScalerId")
                .withLogger(customLogger).withStorageDir(Defaults.STORAGE_DIR).withAlerter(Defaults.ALERTER)
                .withMonitoringSubsystem(Defaults.MONITORING_SUBSYSTEM).withMetronome(Defaults.METRONOME)
                .withPredictionSubsystem(Defaults.PREDICTION_SUBSYSTEM).withCloudPoolProxy(Defaults.CLOUD_POOL_PROXY)
                .build();
        assertThat(autoScaler.getLogger(), is(customLogger));
    }

    /**
     * Verify that {@link AutoScalerBuilder#newBuilderFromSource(AutoScaler)}
     * creates instances that blueprint-copies of an {@link AutoScaler}. That
     * is, they have the same subsystem implementation classes.
     */
    @Test
    public void testBuildFromSourceAutoScaler() {
        AutoScaler source = AutoScalerBuilder.newBuilder().withUuid(UUID.randomUUID()).withId("autoScalerId")
                .withStorageDir(Defaults.STORAGE_DIR).withAlerter(NoOpAlerterStub.class)
                .withMonitoringSubsystem(NoOpMonitoringSubsystemStub.class).withMetronome(NoOpMetronomeStub.class)
                .withPredictionSubsystem(NoOpPredictionSubsystemStub.class)
                .withCloudPoolProxy(NoOpCloudPoolProxyStub.class)
                .withAddonSubsystem("fakeAddon", FakeAddon.class).build();

        AutoScalerBuilder builder = AutoScalerBuilder.newBuilderFromSource(source);
        // verify that built autoscaler is a component-wise copy of original
        AutoScaler copy = builder.build();
        assertEquals(copy.getUuid(), source.getUuid());
        assertEquals(copy.getId(), source.getId());
        assertEquals(copy.getStorageDir(), source.getStorageDir());
        assertEquals(copy.getAlerter().getClass(), source.getAlerter().getClass());
        assertEquals(copy.getMonitoringSubsystem().getClass(), source.getMonitoringSubsystem().getClass());
        assertEquals(copy.getMetronome().getClass(), source.getMetronome().getClass());
        assertEquals(copy.getPredictionSubsystem().getClass(), source.getPredictionSubsystem().getClass());
        assertEquals(copy.getCloudPoolProxy().getClass(), source.getCloudPoolProxy().getClass());
        assertEquals(copy.getAlerter().getClass(), source.getAlerter().getClass());
        // verify same add-on subsystems were used
        Map<String, Service> copyAddons = copy.getAddonSubsystems();
        Map<String, Service> sourceAddons = source.getAddonSubsystems();
        assertThat(copyAddons.size(), is(sourceAddons.size()));
        copyAddons.forEach((name, subsys) -> assertThat(sourceAddons.get(name).getClass().getName(),
                is(subsys.getClass().getName())));
    }

    /**
     * Builds an {@link AutoScaler} instance out of default subsystem
     * implementations and without add-on subsystems. The exception is the
     * metric streamer, since its implementation class is in a different module.
     *
     * @param autoScalerId
     * @return
     */
    private AutoScaler buildDefaultAutoScaler(String autoScalerId) {
        AutoScalerBuilder builder = defaultBuilder(autoScalerId);
        return builder.build();
    }

    /**
     * Sets up a {@link AutoScalerBuilder} with defaults implementations for all
     * subsystems.
     *
     * @param autoScalerId
     * @return
     */
    private AutoScalerBuilder defaultBuilder(String autoScalerId) {
        return AutoScalerBuilder.newBuilder().withUuid(UUID.randomUUID()).withId(autoScalerId)
                .withStorageDir(Defaults.STORAGE_DIR).withAlerter(Defaults.ALERTER)
                .withMonitoringSubsystem(Defaults.MONITORING_SUBSYSTEM).withMetronome(Defaults.METRONOME)
                .withPredictionSubsystem(Defaults.PREDICTION_SUBSYSTEM).withCloudPoolProxy(Defaults.CLOUD_POOL_PROXY);
    }
}
