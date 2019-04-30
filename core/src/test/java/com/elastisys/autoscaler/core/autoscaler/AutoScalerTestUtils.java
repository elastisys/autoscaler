package com.elastisys.autoscaler.core.autoscaler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import com.elastisys.autoscaler.core.api.Configurable;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.autoscaler.builder.AutoScalerBuilder.Defaults;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerBlueprint;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactoryConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

public class AutoScalerTestUtils {
    private AutoScalerTestUtils() {
        throw new RuntimeException(AutoScalerTestUtils.class.getName() + " is not instantiable.");
    }

    public static AutoScalerFactoryConfig parseFactoryConfig(String resourceFile)
            throws JsonSyntaxException, IOException {
        return JsonUtils.toObject(JsonUtils.parseJsonResource(resourceFile), AutoScalerFactoryConfig.class);
    }

    public static AutoScalerBlueprint parseBlueprint(String resourceFile) throws JsonSyntaxException, IOException {
        return JsonUtils.toObject(JsonUtils.parseJsonResource(resourceFile), AutoScalerBlueprint.class);
    }

    public static ServiceStatus parseStatus(File statusFile) throws JsonSyntaxException, IOException {
        return JsonUtils.toObject(JsonUtils.parseJsonFile(statusFile), ServiceStatus.class);
    }

    /**
     * Verifies that an {@link AutoScaler} instance is complete (component-wise)
     * and made up of default components.
     *
     * @see Defaults
     *
     * @param autoScaler
     */
    public static void assertDefaultComponents(AutoScaler autoScaler) {
        assertNotNull(autoScaler);
        assertThat(autoScaler.getId(), is("autoscaler1"));

        assertNotNull(autoScaler.getBus());
        assertNotNull(autoScaler.getExecutorService());
        assertNotNull(autoScaler.getLogger());

        // Verify that default implementations have been set up for subsystems
        assertEquals(Defaults.MONITORING_SUBSYSTEM, autoScaler.getMonitoringSubsystem().getClass());
        assertEquals(Defaults.ALERTER, autoScaler.getAlerter().getClass());
        assertEquals(Defaults.METRONOME, autoScaler.getMetronome().getClass());
        assertEquals(Defaults.PREDICTION_SUBSYSTEM, autoScaler.getPredictionSubsystem().getClass());
        assertEquals(Defaults.CLOUD_POOL_PROXY, autoScaler.getCloudPoolProxy().getClass());
    }

    /**
     * Verifies that a given add-on subsystem has been configured for an
     * {@link AutoScaler}.
     *
     * @param subsystemName
     * @param subsystemClass
     */
    public static void assertAddonSubsystem(AutoScaler autoscaler, String subsystemName, Class subsystemClass) {
        assertTrue("expected add-on subsystem " + subsystemName + " to be configured for autoscaler",
                autoscaler.getAddonSubsystems().containsKey(subsystemName));
        assertThat(autoscaler.getAddonSubsystems().get(subsystemName).getClass().getName(),
                is(subsystemClass.getName()));
    }

    /**
     * Verifies that a particular {@link AutoScaler} instance has a given
     * storage directory.
     *
     * @param autoScaler
     * @param expectedStorageDir
     */
    public static void assertStorageDir(AutoScaler autoScaler, File expectedStorageDir) {
        assertEquals(expectedStorageDir.getAbsolutePath(), autoScaler.getStorageDir().getAbsolutePath());
    }

    /**
     * Verifies that two {@link AutoScaler}s do not share any components. That
     * is, it tests that two auto-scalers have been wired with different objects
     * in their object graphs.
     *
     * @param autoScaler1
     * @param autoScaler2
     */
    public static void assertNotSameAutoScalers(AutoScaler autoScaler1, AutoScaler autoScaler2) {
        assertNotSame(autoScaler1, autoScaler2);
        assertNotSame(autoScaler1.getUuid(), autoScaler2.getUuid());
        assertNotSame(autoScaler1.getId(), autoScaler2.getId());
        assertNotSame(autoScaler1.getAlerter(), autoScaler2.getAlerter());
        assertNotSame(autoScaler1.getBus(), autoScaler2.getBus());
        assertNotSame(autoScaler1.getExecutorService(), autoScaler2.getExecutorService());
        assertNotSame(autoScaler1.getCloudPoolProxy(), autoScaler2.getCloudPoolProxy());
        assertNotSame(autoScaler1.getLogger(), autoScaler2.getLogger());
        assertNotSame(autoScaler1.getMonitoringSubsystem(), autoScaler2.getMonitoringSubsystem());
        assertNotSame(autoScaler1.getMetronome(), autoScaler2.getMetronome());
        assertNotSame(autoScaler1.getPredictionSubsystem(), autoScaler2.getPredictionSubsystem());
        assertNotSame(autoScaler1.getStorageDir(), autoScaler2.getStorageDir());
    }

    /**
     * Returns the configuration of a certain {@link Configurable}
     * {@link AutoScaler} subsystem as a JSON object.
     *
     * @param configurable
     *            An {@link Configurable} {@link AutoScaler} subsystem.
     * @return The configuration as a {@link JsonElement}.
     */
    public static JsonElement subsystemConfig(Configurable<?> configurable) {
        return JsonUtils.toJson(configurable.getConfiguration());
    }
}
