package com.elastisys.autoscaler.core.autoscaler;

import static com.elastisys.autoscaler.core.api.CorePredicates.hasStarted;
import static com.elastisys.autoscaler.core.autoscaler.AutoScalerTestUtils.assertDefaultComponents;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.core.addon.FakeAddon;
import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.Health;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactory;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactoryConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.collection.Maps;
import com.google.gson.JsonObject;

/**
 * Exercises the full service lifecycle of the {@link AutoScaler}.
 */
public class TestAutoScalerOperation {

    /** {@link AutoScaler} blueprint resource file. */
    private static final String BLUEPRINT = "autoscaler/autoscaler-blueprint.json";
    /** {@link AutoScaler} configuration resource file. */
    private static final String CONFIG = "autoscaler/autoscaler-config.json";

    /**
     * Configuration file that contains an configuration for an add-on.
     */
    private static final String CONFIG_WITH_ADDON = "autoscaler/autoscaler-config-with-addon-subsystem.json";

    /** Where autoscaler instance state will be stored. */
    private final static String storageDir = "target/autoscaler/instances";

    /** Object under test. */
    private AutoScaler autoScaler;

    @Before
    public void onSetup() throws Exception {
        // create
        JsonObject autoScalerBlueprint = parseJsonResource(BLUEPRINT);
        this.autoScaler = AutoScalerFactory.launch(new AutoScalerFactoryConfig(storageDir, null))
                .createAutoScaler(autoScalerBlueprint);
        assertDefaultComponents(this.autoScaler);
        // configure
        JsonObject config = parseJsonResource(CONFIG);
        this.autoScaler.validate(config);
        this.autoScaler.configure(config);
    }

    @Test
    public void startAutoScaler() {
        assertThat(this.autoScaler.getStatus().getState(), is(State.STOPPED));
        // start
        this.autoScaler.start();
        // check that all subsystems are go
        assertThat(this.autoScaler.getStatus().getState(), is(State.STARTED));
        assertTrue(this.autoScaler.getSubsystems().stream().allMatch(hasStarted()));
        assertThat(this.autoScaler.getStatus().getHealth(), is(Health.OK));
        assertThat(this.autoScaler.getStatus().getHealthDetail(), is(""));

        // stop
        this.autoScaler.stop();
        assertThat(this.autoScaler.getStatus().getState(), is(State.STOPPED));
        // check that all subsystems are stopped
        assertTrue(this.autoScaler.getSubsystems().stream().allMatch(it -> !hasStarted().test(it)));
        assertThat(this.autoScaler.getStatus().getHealth(), is(Health.OK));
    }

    /**
     * Make sure add-on subsystems get started/stopped when an
     * {@link AutoScaler} is started/stopped.
     */
    @Test
    public void startAutoScalerWithAddons() {
        this.autoScaler = createAutoscalerWithAddon();
        assertTrue(!this.autoScaler.getAddonSubsystems().isEmpty());

        assertThat(this.autoScaler.getStatus().getState(), is(State.STOPPED));
        // start
        this.autoScaler.start();
        // check that add-on subsystem was started
        assertTrue(this.autoScaler.getAddonSubsystems().values().stream().allMatch(hasStarted()));

        // stop
        this.autoScaler.stop();
        assertThat(this.autoScaler.getStatus().getState(), is(State.STOPPED));
        // check that add-on subsystem was stopped
        assertTrue(this.autoScaler.getAddonSubsystems().values().stream().allMatch(it -> !hasStarted().test(it)));
    }

    /**
     * {@link AutoScaler#getSubsystems()} should return all subsystems
     * (including add-ons).
     */
    @Test
    public void getSubsystems() {
        this.autoScaler = createAutoscalerWithAddon();

        Collection<Service> addons = this.autoScaler.getAddonSubsystems().values();
        assertTrue(!addons.isEmpty());

        List<Service> expectedSubsystems = new ArrayList<>(asList(this.autoScaler.getAlerter(),
                this.autoScaler.getCloudPoolProxy(), this.autoScaler.getMetronome(),
                this.autoScaler.getMonitoringSubsystem(), this.autoScaler.getPredictionSubsystem()));
        expectedSubsystems.addAll(addons);

        assertThat(this.autoScaler.getSubsystems().size(), is(expectedSubsystems.size()));
        assertTrue(this.autoScaler.getSubsystems().containsAll(expectedSubsystems));
    }

    @Test
    public void restartAutoScaler() {
        assertThat(this.autoScaler.getStatus().getState(), is(State.STOPPED));
        // start
        this.autoScaler.start();
        assertThat(this.autoScaler.getStatus().getState(), is(State.STARTED));
        assertTrue(this.autoScaler.getSubsystems().stream().allMatch(hasStarted()));

        // stop
        this.autoScaler.stop();
        assertThat(this.autoScaler.getStatus().getState(), is(State.STOPPED));
        assertTrue(this.autoScaler.getSubsystems().stream().allMatch(it -> !hasStarted().test(it)));

        // re-start
        this.autoScaler.start();
        assertThat(this.autoScaler.getStatus().getState(), is(State.STARTED));
        assertTrue(this.autoScaler.getSubsystems().stream().allMatch(hasStarted()));
    }

    @Test(expected = IllegalStateException.class)
    public void startBeforeConfigured() throws Exception {
        // create
        JsonObject autoScalerBlueprint = parseJsonResource(BLUEPRINT);
        AutoScaler autoScaler = AutoScalerFactory.launch(new AutoScalerFactoryConfig(storageDir, null))
                .createAutoScaler(autoScalerBlueprint);
        // start
        assertThat(autoScaler.getStatus().getState(), is(State.STOPPED));
        autoScaler.start();
    }

    private JsonObject parseJsonResource(String resourceName) {
        return JsonUtils.parseJsonResource(resourceName).getAsJsonObject();
    }

    /**
     * Creates and configures an {@link AutoScaler} with an add-on.
     *
     * @return
     */
    private AutoScaler createAutoscalerWithAddon() {
        // create autoscaler with an add-on subsystem
        Map<String, String> addons = Maps.of("fakeAddon", FakeAddon.class.getName());
        AutoScalerFactory factory = AutoScalerFactory.launch(new AutoScalerFactoryConfig(storageDir, addons));
        AutoScaler autoscaler = factory.createAutoScaler(parseJsonResource(BLUEPRINT));
        assertDefaultComponents(autoscaler);
        AutoScalerTestUtils.assertAddonSubsystem(autoscaler, "fakeAddon", FakeAddon.class);

        // configure
        JsonObject config = parseJsonResource(CONFIG_WITH_ADDON);
        autoscaler.validate(config);
        autoscaler.configure(config);
        return autoscaler;

    }
}
