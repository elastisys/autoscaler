package com.elastisys.autoscaler.core.autoscaler.factory;

import static com.elastisys.autoscaler.core.api.types.ServiceStatus.State.STARTED;
import static com.elastisys.autoscaler.core.api.types.ServiceStatus.State.STOPPED;
import static com.elastisys.autoscaler.core.autoscaler.AutoScalerTestUtils.parseBlueprint;
import static com.elastisys.autoscaler.core.autoscaler.AutoScalerTestUtils.parseStatus;
import static com.elastisys.scale.commons.json.JsonUtils.parseJsonFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.core.addon.FakeAddon;
import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.elastisys.scale.commons.util.io.IoUtils;
import com.google.gson.JsonObject;

/**
 * Tests the {@link AutoScalerFactory} life-cycle and, in particular, the saving
 * and restoring of {@link AutoScaler} instances.
 */
public class TestAutoScalerFactoryPersistence {

    /** Storage directory path to use throughout the tests. */
    private static final String storageDirPath = FileUtils.cwd().getAbsolutePath() + "/target/instances";
    /** Storage directory to use throughout the tests. */
    private static final File storageDir = new File(storageDirPath);

    /**
     * The add-on subsystems that the {@link AutoScalerFactory} is configured to
     * use.
     */
    private static final Map<String, String> addonSubsystems = Maps.of("fakeAddon", FakeAddon.class.getName());

    private static final String BLUEPRINT = "autoscaler/autoscaler-blueprint.json";
    private static final String BLUEPRINT2 = "autoscaler/autoscaler-blueprint2.json";
    /**
     * Configuration file that also contains a configuration for an add-on.
     */
    private static final String CONFIG = "autoscaler/autoscaler-config-with-addon-subsystem.json";
    private static final String CONFIG2 = "autoscaler/autoscaler-config-with-addon-subsystem2.json";

    /** Object under test. */
    private AutoScalerFactory factory;

    @Before
    public void onSetup() throws Exception {
        assertThat(storageDir.exists(), is(false));

        // create and start the factory under test
        this.factory = AutoScalerFactory.launch(new AutoScalerFactoryConfig(storageDirPath, addonSubsystems));
        // sanity check
        assertThat(this.factory.getStatus().getState(), is(State.STARTED));
        assertTrue(this.factory.getAutoScalerIds().isEmpty());
        assertTrue(this.factory.getConfiguration().getStorageDir().exists());
    }

    @After
    public void onTeardown() throws IOException {
        // make sure storage dir is deleted between every test method
        FileUtils.deleteRecursively(storageDir);
    }

    /**
     * Verifies the behavior of {@link AutoScalerFactory#save(String)} on an
     * unconfigured {@link AutoScaler} instance.
     */
    @Test
    public void saveUnconfiguredInstance() throws Exception {
        // create
        JsonObject blueprint = JsonUtils.parseJsonResource(BLUEPRINT).getAsJsonObject();
        AutoScaler autoScaler = this.factory.createAutoScaler(blueprint);

        // save
        this.factory.save(autoScaler.getId());

        // verify saved state
        File instanceDir = new File(storageDir, autoScaler.getId());
        assertTrue(instanceDir.exists());
        File uuidFile = new File(instanceDir, AutoScalerFactory.AUTOSCALER_UUID_FILE);
        File blueprintFile = new File(instanceDir, AutoScalerFactory.BLUEPRINT_FILE);
        File configFile = new File(instanceDir, AutoScalerFactory.CONFIG_FILE);
        File statusFile = new File(instanceDir, AutoScalerFactory.STATUS_FILE);

        // make sure uuid is stored
        assertTrue(uuidFile.exists());
        assertThat(IoUtils.toString(uuidFile, StandardCharsets.UTF_8), is(autoScaler.getUuid().toString()));

        // make sure blueprint is stored
        assertTrue(blueprintFile.exists());
        assertThat(JsonUtils.parseJsonFile(blueprintFile).getAsJsonObject(), is(blueprint));

        // no config set for autoscaler => no file stored
        assertFalse(configFile.exists());

        // make sure status is stored
        assertTrue(statusFile.exists());
        assertThat(parseStatus(statusFile).getState(), is(STOPPED));
    }

    /**
     * Verifies the behavior of {@link AutoScalerFactory#save(String)} on a
     * configured {@link AutoScaler} instance.
     */
    @Test
    public void saveConfiguredInstance() throws Exception {
        // create
        JsonObject blueprint = parseJsonResource(BLUEPRINT);
        AutoScaler autoScaler = this.factory.createAutoScaler(blueprint);
        // configure
        JsonObject config = parseJsonResource(CONFIG);
        autoScaler.validate(config);
        autoScaler.configure(config);

        // save
        this.factory.save(autoScaler.getId());

        // verify saved state
        File instanceDir = new File(storageDir, autoScaler.getId());
        assertTrue(instanceDir.exists());
        File uuidFile = new File(instanceDir, AutoScalerFactory.AUTOSCALER_UUID_FILE);
        File blueprintFile = new File(instanceDir, AutoScalerFactory.BLUEPRINT_FILE);
        File configFile = new File(instanceDir, AutoScalerFactory.CONFIG_FILE);
        File statusFile = new File(instanceDir, AutoScalerFactory.STATUS_FILE);

        // make sure uuid is stored
        assertTrue(uuidFile.exists());
        assertThat(IoUtils.toString(uuidFile, StandardCharsets.UTF_8), is(autoScaler.getUuid().toString()));

        // make sure blueprint is stored
        assertTrue(blueprintFile.exists());
        assertThat(JsonUtils.parseJsonFile(blueprintFile).getAsJsonObject(), is(blueprint));

        // make sure autoscaler config was saved
        assertTrue(configFile.exists());
        assertThat(parseJsonFile(configFile).getAsJsonObject(), is(autoScaler.getConfiguration()));

        // make sure status is stored
        assertTrue(statusFile.exists());
        assertThat(parseStatus(statusFile).getState(), is(STOPPED));
    }

    /**
     * Verifies the behavior of {@link AutoScalerFactory#save(String)} on a
     * configured and started {@link AutoScaler} instance.
     */
    @Test
    public void saveConfiguredStartedInstance() throws Exception {
        // create
        JsonObject blueprint = parseJsonResource(BLUEPRINT);
        AutoScaler autoScaler = this.factory.createAutoScaler(blueprint);
        // configure
        JsonObject config = parseJsonResource(CONFIG);
        autoScaler.validate(config);
        autoScaler.configure(config);
        // start
        autoScaler.start();

        // save
        this.factory.save(autoScaler.getId());

        // verify saved state
        File instanceDir = new File(storageDir, autoScaler.getId());
        assertTrue(instanceDir.exists());
        File uuidFile = new File(instanceDir, AutoScalerFactory.AUTOSCALER_UUID_FILE);
        File blueprintFile = new File(instanceDir, AutoScalerFactory.BLUEPRINT_FILE);
        File configFile = new File(instanceDir, AutoScalerFactory.CONFIG_FILE);
        File statusFile = new File(instanceDir, AutoScalerFactory.STATUS_FILE);

        // make sure uuid is stored
        assertTrue(uuidFile.exists());
        assertThat(IoUtils.toString(uuidFile, StandardCharsets.UTF_8), is(autoScaler.getUuid().toString()));

        // make sure blueprint is stored
        assertTrue(blueprintFile.exists());
        assertThat(JsonUtils.parseJsonFile(blueprintFile).getAsJsonObject(), is(blueprint));

        // make sure autoscaler config was saved
        assertTrue(configFile.exists());
        assertThat(parseJsonFile(configFile).getAsJsonObject(), is(autoScaler.getConfiguration()));

        // make sure status is stored
        assertTrue(statusFile.exists());
        assertThat(parseStatus(statusFile).getState(), is(STARTED));
    }

    /**
     * Verifies that all {@link AutoScaler} instances are saved when the
     * {@link AutoScalerFactory} is stopped.
     */
    @Test
    public void verifyThatInstanceIsSavedWhenFactoryIsStopped() throws Exception {
        // create
        JsonObject blueprint = parseJsonResource(BLUEPRINT);
        AutoScaler autoScaler = this.factory.createAutoScaler(blueprint);
        // configure
        JsonObject config = parseJsonResource(CONFIG);
        autoScaler.validate(config);
        autoScaler.configure(config);
        // start
        autoScaler.start();

        // verify: nothing saved yet
        File instanceDir = new File(storageDir, autoScaler.getId());
        assertFalse(instanceDir.exists());

        // stop factory: saves all instances
        this.factory.stop();

        // verify saved state
        File uuidFile = new File(instanceDir, AutoScalerFactory.AUTOSCALER_UUID_FILE);
        File blueprintFile = new File(instanceDir, AutoScalerFactory.BLUEPRINT_FILE);
        File configFile = new File(instanceDir, AutoScalerFactory.CONFIG_FILE);
        File statusFile = new File(instanceDir, AutoScalerFactory.STATUS_FILE);
        // make sure uuid is stored
        assertTrue(uuidFile.exists());
        assertThat(IoUtils.toString(uuidFile, StandardCharsets.UTF_8), is(autoScaler.getUuid().toString()));
        // make sure blueprint is stored
        assertTrue(blueprintFile.exists());
        assertThat(JsonUtils.parseJsonFile(blueprintFile).getAsJsonObject(), is(blueprint));
        // make sure autoscaler config was saved
        assertTrue(configFile.exists());
        assertThat(parseJsonFile(configFile).getAsJsonObject(), is(autoScaler.getConfiguration()));
        // make sure status is stored
        assertTrue(statusFile.exists());
        assertThat(parseStatus(statusFile).getState(), is(STARTED));

        // verify that factory and all instances are stopped
        assertThat(this.factory.getStatus().getState(), is(STOPPED));
        assertThat(autoScaler.getStatus().getState(), is(STOPPED));
    }

    /**
     * Verifies that a saved {@link AutoScaler} instance is restored on restart
     * of the {@link AutoScalerFactory}.
     */
    @Test
    public void verifyThatInstanceIsRestoredOnFactoryRestart() throws Exception {
        // create
        AutoScalerBlueprint blueprint = parseBlueprint(BLUEPRINT);
        AutoScaler autoScaler = this.factory.createAutoScaler(blueprint);
        String id = autoScaler.getId();
        File instanceDir = new File(storageDir, id);
        assertThat(autoScaler.getStorageDir(), is(instanceDir));
        // configure
        JsonObject config = parseJsonResource(CONFIG);
        autoScaler.validate(config);
        autoScaler.configure(config);
        // start
        autoScaler.start();

        // stop factory: saves all instances
        this.factory.stop();

        // verify that factory and all instances are stopped
        assertThat(this.factory.getStatus().getState(), is(STOPPED));
        assertThat(autoScaler.getStatus().getState(), is(STOPPED));

        // restart factory: restores all instances
        this.factory.start();
        assertThat(this.factory.getStatus().getState(), is(STARTED));

        // verify that blueprint, configuration and state was properly restored
        AutoScaler restoredAutoScaler = this.factory.getAutoScaler(id);
        assertThat(this.factory.getBlueprint(id), is(blueprint));
        assertThat(restoredAutoScaler.getUuid(), is(autoScaler.getUuid()));
        // verify that add-on subsystems were restored
        Map<String, Service> restoredAddons = restoredAutoScaler.getAddonSubsystems();
        assertThat(restoredAddons.size(), is(addonSubsystems.size()));
        addonSubsystems.forEach(
                (name, addonClass) -> assertThat(restoredAddons.get(name).getClass().getName(), is(addonClass)));

        assertThat(restoredAutoScaler.getConfiguration(), is(config));
        assertThat(restoredAutoScaler.getStatus().getState(), is(STARTED));
        assertThat(restoredAutoScaler.getStorageDir(), is(instanceDir));
    }

    /**
     * Verifies that several saved {@link AutoScaler} instance are restored on
     * restart of the {@link AutoScalerFactory}.
     */
    @Test
    public void verifyRestoreOnFactoryRestartWithMultipleInstances() throws Exception {
        // create first instance
        AutoScalerBlueprint blueprint1 = parseBlueprint(BLUEPRINT);
        AutoScaler instance1 = this.factory.createAutoScaler(blueprint1);
        File instance1Dir = new File(storageDir, instance1.getId());
        String id1 = instance1.getId();
        JsonObject config1 = parseJsonResource(CONFIG);
        instance1.validate(config1);
        instance1.configure(config1);
        instance1.start();

        // create second instance
        AutoScalerBlueprint blueprint2 = parseBlueprint(BLUEPRINT2);
        AutoScaler instance2 = this.factory.createAutoScaler(blueprint2);
        File instance2Dir = new File(storageDir, instance2.getId());
        String id2 = instance2.getId();
        JsonObject config2 = parseJsonResource(CONFIG2);
        instance2.validate(config2);
        instance2.configure(config2);
        instance2.start();

        // stop factory: saves all instances
        this.factory.stop();

        // verify that factory and all instances are stopped
        assertThat(this.factory.getStatus().getState(), is(STOPPED));
        assertThat(instance1.getStatus().getState(), is(STOPPED));
        assertThat(instance2.getStatus().getState(), is(STOPPED));

        // restart factory: restores all instances
        this.factory.start();
        assertThat(this.factory.getStatus().getState(), is(STARTED));

        // verify that instance1 was properly restored
        AutoScaler restoredInstance1 = this.factory.getAutoScaler(id1);
        assertThat(restoredInstance1.getUuid(), is(instance1.getUuid()));
        assertThat(this.factory.getBlueprint(id1), is(blueprint1));
        assertThat(restoredInstance1.getConfiguration(), is(config1));
        assertThat(restoredInstance1.getStatus().getState(), is(STARTED));
        assertThat(restoredInstance1.getStorageDir(), is(instance1Dir));
        // verify that all addon subsystems were restored for instance1
        restoredInstance1.getAddonSubsystems()
                .forEach((name, addon) -> assertThat(addon.getClass().getName(), is(addonSubsystems.get(name))));

        // verify that instance2 was properly restored
        AutoScaler restoredInstance2 = this.factory.getAutoScaler(id2);
        assertThat(this.factory.getBlueprint(id2), is(blueprint2));
        assertThat(restoredInstance2.getUuid(), is(instance2.getUuid()));
        assertThat(restoredInstance2.getConfiguration(), is(config2));
        assertThat(restoredInstance2.getStatus().getState(), is(STARTED));
        assertThat(restoredInstance2.getStorageDir(), is(instance2Dir));
        // verify that all addon subsystems were restored for instance2
        restoredInstance2.getAddonSubsystems()
                .forEach((name, addon) -> assertThat(addon.getClass().getName(), is(addonSubsystems.get(name))));

    }

    /**
     * Verifies that saved {@link AutoScaler} instances are restored on
     * re-creation of the {@link AutoScalerFactory}. This is slightly different
     * than just restarting a factory, since it exercises the behavior on a full
     * JVM restart when the factory instance needs to be reinstantiated.
     */
    @Test
    public void verifyThatInstancesAreRestoredOnRecreationOfFactory() throws Exception {
        // create first instance
        AutoScalerBlueprint blueprint1 = parseBlueprint(BLUEPRINT);
        AutoScaler instance1 = this.factory.createAutoScaler(blueprint1);
        File instance1Dir = new File(storageDir, instance1.getId());
        String id1 = instance1.getId();
        JsonObject config1 = parseJsonResource(CONFIG);
        instance1.validate(config1);
        instance1.configure(config1);
        instance1.start();

        // create second instance
        AutoScalerBlueprint blueprint2 = parseBlueprint(BLUEPRINT2);
        AutoScaler instance2 = this.factory.createAutoScaler(blueprint2);
        File instance2Dir = new File(storageDir, instance2.getId());
        String id2 = instance2.getId();
        JsonObject config2 = parseJsonResource(CONFIG2);
        instance2.validate(config2);
        instance2.configure(config2);
        instance2.start();

        // stop factory: saves all instances
        this.factory.stop();
        // verify that factory and all instances are stopped
        assertThat(this.factory.getStatus().getState(), is(STOPPED));
        assertThat(instance1.getStatus().getState(), is(STOPPED));
        assertThat(instance2.getStatus().getState(), is(STOPPED));
        this.factory = null;

        // recreate factory: all instances should be restored on start
        AutoScalerFactory newFactory = AutoScalerFactory
                .launch(new AutoScalerFactoryConfig(storageDirPath, addonSubsystems));
        newFactory.start();
        assertThat(newFactory.getStatus().getState(), is(STARTED));

        // verify that instance1 was properly restored
        AutoScaler restoredInstance1 = newFactory.getAutoScaler(id1);
        assertThat(newFactory.getBlueprint(id1), is(blueprint1));
        assertThat(restoredInstance1.getUuid(), is(instance1.getUuid()));
        assertThat(restoredInstance1.getConfiguration(), is(config1));
        assertThat(restoredInstance1.getStatus().getState(), is(STARTED));
        assertThat(restoredInstance1.getStorageDir(), is(instance1Dir));
        // verify that all addon subsystems were restored for instance1
        restoredInstance1.getAddonSubsystems()
                .forEach((name, addon) -> assertThat(addon.getClass().getName(), is(addonSubsystems.get(name))));

        // verify that instance2 was properly restored
        AutoScaler restoredInstance2 = newFactory.getAutoScaler(id2);
        assertThat(restoredInstance2.getUuid(), is(instance2.getUuid()));
        assertThat(newFactory.getBlueprint(id2), is(blueprint2));
        assertThat(restoredInstance2.getConfiguration(), is(config2));
        assertThat(restoredInstance2.getStatus().getState(), is(STARTED));
        assertThat(restoredInstance2.getStorageDir(), is(instance2Dir));
        // verify that all addon subsystems were restored for instance2
        restoredInstance2.getAddonSubsystems()
                .forEach((name, addon) -> assertThat(addon.getClass().getName(), is(addonSubsystems.get(name))));
    }

    /**
     * Verifies that an instance's storage directory is properly cleaned up on
     * deletion.
     */
    @Test
    public void verifyThatDeletingInstanceRemovesItsStorageDir() throws Exception {
        // create and save
        JsonObject blueprint = parseJsonResource(BLUEPRINT);
        AutoScaler autoScaler = this.factory.createAutoScaler(blueprint);
        JsonObject config = parseJsonResource(CONFIG);
        autoScaler.validate(config);
        autoScaler.configure(config);
        autoScaler.start();
        String id = autoScaler.getId();
        File instanceDir = new File(storageDir, id);
        this.factory.save(id);
        // verify that storage directory was created when instance was saved
        assertTrue(instanceDir.exists());

        // delete instance
        this.factory.deleteAutoScaler(id);
        // verify that instance was stopped, removed from factory, and its
        // storage directory was deleted
        assertThat(autoScaler.getStatus().getState(), is(STOPPED));
        assertFalse(this.factory.getAutoScalerIds().contains(id));
        assertFalse(instanceDir.exists());
    }

    private JsonObject parseJsonResource(String resourceName) {
        return JsonUtils.parseJsonResource(resourceName).getAsJsonObject();
    }

    /**
     * Tries to load the add-on subsystem map found in an {@code addons.json}
     * file.
     *
     * @param addonsFile
     * @return
     */
    private Map<String, String> loadAddonsFile(File addonsFile) {
        Type type = new TypeLiteral<Map<String, String>>() {
        }.getType();
        return JsonUtils.toObject(JsonUtils.parseJsonFile(addonsFile), type);
    }

}
