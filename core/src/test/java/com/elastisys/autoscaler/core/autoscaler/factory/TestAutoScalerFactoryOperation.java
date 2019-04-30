package com.elastisys.autoscaler.core.autoscaler.factory;

import static com.elastisys.autoscaler.core.autoscaler.AutoScalerTestUtils.assertDefaultComponents;
import static com.elastisys.autoscaler.core.autoscaler.AutoScalerTestUtils.assertStorageDir;
import static com.elastisys.autoscaler.core.autoscaler.AutoScalerTestUtils.parseBlueprint;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.core.addon.FakeAddon;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Exercises the {@link AutoScalerFactory} logic involved in creating and
 * managing {@link AutoScaler} instances.
 */
public class TestAutoScalerFactoryOperation {
    /** Where autoscaler instance state will be stored. */
    private final static File storageDir = new File("target/autoscaler/instances");

    private AutoScalerFactory factory;
    private File factoryDir;

    @Before
    public void onSetup() throws Exception {
        if (storageDir.isDirectory()) {
            FileUtils.deleteRecursively(storageDir);
        }
        this.factory = AutoScalerFactory.launch(new AutoScalerFactoryConfig(storageDir.getAbsolutePath(), null));
        this.factoryDir = this.factory.getConfiguration().getStorageDir();
    }

    @Test
    public void testEmptyFactory() throws IOException {
        assertThat(this.factory.getAutoScalerIds().isEmpty(), is(true));
    }

    @Test
    public void createWithCompleteBlueprint() throws IOException {
        JsonObject autoScalerBlueprint = parseJsonResource("autoscaler/autoscaler-blueprint.json");
        AutoScaler autoScaler = this.factory.createAutoScaler(autoScalerBlueprint);
        assertTrue(this.factory.getAutoScalerIds().equals(Collections.singleton(autoScaler.getId())));
        // verify that created instance is unconfigured and unstarted
        assertThat(autoScaler.getStatus().getState(), is(State.STOPPED));
        assertThat(autoScaler.getConfiguration(), is(nullValue()));

        assertDefaultComponents(autoScaler);
        assertTrue(autoScaler.getAddonSubsystems().isEmpty());
        assertStorageDir(autoScaler, new File(this.factoryDir, autoScaler.getId()));
    }

    /**
     * Create an {@link AutoScaler} with an add-on subsystem.
     */
    @Test
    public void createWithAddons() {
        Map<String, String> addons = Maps.of("extSubsystem", FakeAddon.class.getName());
        this.factory = AutoScalerFactory.launch(new AutoScalerFactoryConfig(storageDir.getAbsolutePath(), addons));
        JsonObject autoScalerBlueprint = parseJsonResource("autoscaler/autoscaler-blueprint.json");
        AutoScaler autoScaler = this.factory.createAutoScaler(autoScalerBlueprint);

        assertTrue(!autoScaler.getAddonSubsystems().isEmpty());
        assertTrue(autoScaler.getAddonSubsystems().containsKey("extSubsystem"));
        assertThat(autoScaler.getAddonSubsystems().get("extSubsystem").getClass().getName(),
                is(FakeAddon.class.getName()));
    }

    @Test
    public void createWithAliasedBluePrint() throws Exception {
        JsonObject autoScalerBlueprint = parseJsonResource("autoscaler/autoscaler-aliased-blueprint.json");
        AutoScaler autoScaler = this.factory.createAutoScaler(autoScalerBlueprint);
        assertDefaultComponents(autoScaler);
    }

    @Test
    public void createWithCompleteBlueprintFromObject() throws JsonSyntaxException, IOException {
        AutoScalerBlueprint blueprint = parseBlueprint("autoscaler/autoscaler-blueprint.json");
        AutoScaler autoScaler = this.factory.createAutoScaler(blueprint);
        assertNotNull(autoScaler);
        assertDefaultComponents(autoScaler);
        assertStorageDir(autoScaler, new File(this.factoryDir, autoScaler.getId()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithClashingIds() throws IOException {
        JsonObject blueprint = parseJsonResource("autoscaler/autoscaler-blueprint.json");
        this.factory.createAutoScaler(blueprint);
        JsonObject sameBlueprint = parseJsonResource("autoscaler/autoscaler-blueprint.json");
        this.factory.createAutoScaler(sameBlueprint);
    }

    @Test
    public void createWithValidIds() throws IOException {
        this.factory.createAutoScaler(new AutoScalerBlueprint("id-1", null, null, null, null, null));
        this.factory.createAutoScaler(new AutoScalerBlueprint("Id_1", null, null, null, null, null));
    }

    @Test
    public void createWithInvalidId1() throws IOException {
        try {
            AutoScalerBlueprint blueprint = new AutoScalerBlueprint("id/1", null, null, null, null, null);
            this.factory.createAutoScaler(blueprint);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid auto-scaler instance id"));
        }
    }

    @Test
    public void createWithInvalidId2() throws IOException {
        try {
            AutoScalerBlueprint blueprint = new AutoScalerBlueprint("id.1", null, null, null, null, null);
            this.factory.createAutoScaler(blueprint);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid auto-scaler instance id"));
        }
    }

    @Test
    public void createWithInvalidId3() throws IOException {
        try {
            AutoScalerBlueprint blueprint = new AutoScalerBlueprint("id+1", null, null, null, null, null);
            this.factory.createAutoScaler(blueprint);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid auto-scaler instance id"));
        }
    }

    @Test
    public void createWithInvalidId4() throws IOException {
        try {
            AutoScalerBlueprint blueprint = new AutoScalerBlueprint("id*1", null, null, null, null, null);
            this.factory.createAutoScaler(blueprint);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid auto-scaler instance id"));
        }
    }

    @Test
    public void createWithBlueprintThatReliesOnDefaults() throws IOException {
        JsonObject autoScalerBlueprint = parseJsonResource("autoscaler/autoscaler-blueprint-relying-on-defaults.json");
        AutoScaler autoScaler = this.factory.createAutoScaler(autoScalerBlueprint);
        assertNotNull(autoScaler);
        assertDefaultComponents(autoScaler);
        assertStorageDir(autoScaler, new File(this.factoryDir, autoScaler.getId()));
    }

    @Test
    public void getInstance() throws IOException {
        AutoScalerBlueprint blueprint = parseBlueprint("autoscaler/autoscaler-blueprint.json");
        AutoScaler autoScaler = this.factory.createAutoScaler(blueprint);
        assertThat(this.factory.getAutoScaler(blueprint.id().get()), is(autoScaler));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getInstanceWithUnrecognizedId() {
        this.factory.getAutoScaler("unrecognized.id");
    }

    @Test
    public void getInstanceBlueprint() throws JsonSyntaxException, IOException {
        AutoScalerBlueprint blueprint = parseBlueprint("autoscaler/autoscaler-blueprint.json");
        this.factory.createAutoScaler(blueprint);
        assertThat(this.factory.getBlueprint(blueprint.id().get()), is(blueprint));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getBlueprintWithUnrecognizedId() {
        this.factory.getBlueprint("unrecognized.id");
    }

    @Test
    public void deleteInstance() throws IOException {
        // create
        JsonObject blueprint = parseJsonResource("autoscaler/autoscaler-blueprint.json");
        AutoScaler autoScaler = this.factory.createAutoScaler(blueprint);
        assertTrue(this.factory.getAutoScalerIds().equals(new HashSet<>(Arrays.asList(autoScaler.getId()))));

        // configure
        JsonObject config = parseJsonResource("autoscaler/autoscaler-config.json");
        // validate config
        autoScaler.validate(config);
        // apply config
        autoScaler.configure(config);

        // start
        assertThat(autoScaler.getStatus().getState(), is(State.STOPPED));
        autoScaler.start();
        assertThat(autoScaler.getStatus().getState(), is(State.STARTED));

        // delete
        String autoScalerId = blueprint.get("id").getAsString();
        this.factory.deleteAutoScaler(autoScalerId);
        // verify that instance was stopped prior to being deleted
        assertThat(autoScaler.getStatus().getState(), is(State.STOPPED));

        try {
            this.factory.getAutoScaler(autoScalerId);
            fail("Auto-scaler instance expected to be deleted.");
        } catch (IllegalArgumentException e) {
            // expected
        }
        assertThat(this.factory.getAutoScalerIds().isEmpty(), is(true));
    }

    @Test
    public void clear() throws IOException {
        // create autoscaler 1
        JsonObject blueprint1 = parseJsonResource("autoscaler/autoscaler-blueprint.json");
        AutoScaler autoScaler1 = this.factory.createAutoScaler(blueprint1);
        assertTrue(this.factory.getAutoScalerIds().equals(Collections.singleton(autoScaler1.getId())));
        JsonObject config = parseJsonResource("autoscaler/autoscaler-config.json");
        autoScaler1.validate(config);
        autoScaler1.configure(config);
        autoScaler1.start();
        assertThat(autoScaler1.getStatus().getState(), is(State.STARTED));

        // create autoscaler 2
        JsonObject blueprint2 = parseJsonResource("autoscaler/autoscaler-blueprint2.json");
        AutoScaler autoScaler2 = this.factory.createAutoScaler(blueprint2);
        assertTrue(this.factory.getAutoScalerIds()
                .equals(new HashSet<>(Arrays.asList(autoScaler1.getId(), autoScaler2.getId()))));
        autoScaler2.validate(config);
        autoScaler2.configure(config);
        autoScaler2.start();
        assertThat(autoScaler2.getStatus().getState(), is(State.STARTED));

        // clear
        this.factory.clear();
        // verify that instances were stopped
        assertThat(autoScaler1.getStatus().getState(), is(State.STOPPED));
        assertThat(autoScaler2.getStatus().getState(), is(State.STOPPED));
        assertThat(this.factory.getAutoScalerIds().isEmpty(), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteInstanceWithUnrecognizedId() {
        this.factory.deleteAutoScaler("unrecognized.id");
    }

    private JsonObject parseJsonResource(String resourceName) {
        return JsonUtils.parseJsonResource(resourceName).getAsJsonObject();
    }
}
