package com.elastisys.autoscaler.core.autoscaler;

import static com.elastisys.autoscaler.core.api.CorePredicates.isConfigured;
import static com.elastisys.autoscaler.core.autoscaler.AutoScalerTestUtils.subsystemConfig;
import static com.elastisys.scale.commons.json.JsonUtils.toJson;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.addon.FakeAddon;
import com.elastisys.autoscaler.core.alerter.impl.standard.StandardAlerter;
import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.autoscaler.builder.AutoScalerBuilder;
import com.elastisys.autoscaler.core.autoscaler.builder.stubs.NoOpAlerterStub;
import com.elastisys.autoscaler.core.autoscaler.builder.stubs.NoOpCloudPoolProxyStub;
import com.elastisys.autoscaler.core.autoscaler.builder.stubs.NoOpMetronomeStub;
import com.elastisys.autoscaler.core.autoscaler.builder.stubs.NoOpMonitoringSubsystemStub;
import com.elastisys.autoscaler.core.autoscaler.builder.stubs.NoOpPredictionSubsystemStub;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactory;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactoryConfig;
import com.elastisys.autoscaler.core.metronome.impl.standard.config.StandardMetronomeConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;
import com.google.gson.JsonObject;

/**
 * Verifies the behavior of the {@link AutoScaler} with respect to
 * configuration.
 */
public class TestAutoScalerConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(TestAutoScalerConfiguration.class);

    /** {@link AutoScaler} blueprint resource file. */
    private static final String BLUEPRINT_RESOURCE = "autoscaler/autoscaler-blueprint-relying-on-defaults.json";
    /** {@link AutoScaler} configuration resource file. */
    private static final String CONFIG = "autoscaler/autoscaler-config.json";

    /**
     * Configuration file that contains an configuration for an add-on.
     */
    private static final String CONFIG_WITH_ADDON = "autoscaler/autoscaler-config-with-addon-subsystem.json";

    /** Where autoscaler instance state will be stored. */
    private final static String storageDir = "target/autoscaler/instances";

    /** The {@link AutoScaler} instance under test. */
    private AutoScaler autoScaler;
    private AutoScalerFactory factory;

    @Before
    public void onSetup() throws Exception {
        this.factory = AutoScalerFactory.launch(new AutoScalerFactoryConfig(storageDir, null));
        this.autoScaler = this.factory.createAutoScaler(parseJsonResource(BLUEPRINT_RESOURCE));
        AutoScalerTestUtils.assertDefaultComponents(this.autoScaler);
    }

    @After
    public void onTeardown() {
        this.factory.deleteAutoScaler(this.autoScaler.getId());
    }

    /**
     * Configures an {@link AutoScaler} and verifies that it properly forwards
     * configurations for each of the subsystems.
     *
     * @throws Exception
     */
    @Test
    public void configureWithValidConfig() throws Exception {
        JsonObject config = parseJsonResource(CONFIG);
        this.autoScaler.validate(config);
        this.autoScaler.configure(config);

        assertTrue(this.autoScaler.getSubsystems().stream().allMatch(isConfigured()));

        assertThat(subsystemConfig(this.autoScaler.getMonitoringSubsystem()), is(config.get("monitoringSubsystem")));
        assertThat(subsystemConfig(this.autoScaler.getAlerter()), is(config.get("alerter")));
        assertThat(subsystemConfig(this.autoScaler.getMetronome()), is(config.get("metronome")));
        assertThat(subsystemConfig(this.autoScaler.getPredictionSubsystem()), is(config.get("predictionSubsystem")));
        assertThat(subsystemConfig(this.autoScaler.getCloudPoolProxy()), is(config.get("cloudPool")));
    }

    /**
     * Make sure that any add-on subsystems also get configured.
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void configureWithAddOnSubsystem() {
        this.autoScaler = createAutoscalerWithAddon();

        // make sure the add-on is not yet configured
        Service addonSubsys = this.autoScaler.getAddonSubsystems().get("fakeAddon");
        assertThat(isConfigured().test(addonSubsys), is(false));

        // configure
        JsonObject config = parseJsonResource(CONFIG_WITH_ADDON);
        this.autoScaler.validate(config);
        this.autoScaler.configure(config);

        // make sure the add-on subsystem also received a configuration
        assertThat(isConfigured().test(addonSubsys), is(true));
        assertThat(subsystemConfig(addonSubsys), is(config.get("fakeAddon")));
    }

    /**
     * Verifies that an {@link AutoScaler} can be re-configured.
     */
    @Test
    public void reconfigure() {
        // configure
        JsonObject preConfig = parseJsonResource("autoscaler/config1.json");
        this.autoScaler.validate(preConfig);
        this.autoScaler.configure(preConfig);
        assertThat(this.autoScaler.getConfiguration(), is(preConfig));

        // reconfigure with a configuration updating a metric stream definition
        JsonObject postConfig = parseJsonResource("autoscaler/config1-with-updated-metricstream-def.json");
        this.autoScaler.validate(postConfig);
        this.autoScaler.configure(postConfig);
        assertThat(this.autoScaler.getConfiguration(), is(postConfig));
        assertThat(preConfig, is(not(postConfig)));
    }

    /**
     * Verifies that {@link AutoScaler} configuration has atomic semantics. That
     * is, an {@link AutoScaler} configuration is either fully applied or not
     * applied at-all. If applying a subsystem configuration fails part-way
     * through the update, the action should be rolled back, recovering each
     * subsystem that was updated to its prior state.
     */
    @Test
    public void configureShouldBeAtomic() throws Exception {
        // configure: has metric stream 'lbaas.connection.rate.stream'
        JsonObject preConfig = parseJsonResource("autoscaler/config1.json");
        this.autoScaler.validate(preConfig);
        this.autoScaler.configure(preConfig);
        assertThat(this.autoScaler.getConfiguration(), is(preConfig));

        // reconfigure: metric stream was replaced with a new one
        // ('lbaas.active.connections.stream') but predictor still references
        // the old stream ''lbaas.connection.rate.stream'. Therefore, the
        // configuration should fail to be applied and rolled back to its prior
        // state.
        JsonObject postConfig = parseJsonResource("autoscaler/config1-with-illegal-metricstream-reference.json");
        this.autoScaler.validate(postConfig);
        try {
            this.autoScaler.configure(postConfig);
            fail("should fail to apply config where a predictor references " + "a stale metric stream id");
        } catch (IllegalArgumentException e) {
            // expected
            LOG.warn("configuration could not be applied: {}", e.getMessage(), e);
        }
        // verify that configuration was rolled back
        assertThat(this.autoScaler.getConfiguration(), is(preConfig));
    }

    /**
     * Verifies that the {@link AutoScaler#getConfiguration()} returns an
     * up-to-date view of all subsystem configurations (that is, it checks that
     * the {@link AutoScaler} properly collects configurations from each
     * subsystem).
     */
    @SuppressWarnings("unchecked")
    @Test
    public void verifyThatAutoScalerReturnsAnUpToDateViewOfSubsystemConfigurations() throws Exception {
        this.autoScaler = createAutoscalerWithAddon();

        // configure entire AutoScaler
        JsonObject originalConfig = parseJsonResource(CONFIG_WITH_ADDON);
        this.autoScaler.validate(originalConfig);
        this.autoScaler.configure(originalConfig);

        // retrieve full configuration (from subsystems)
        assertThat(this.autoScaler.getConfiguration(), is(originalConfig));

        // update one subsystem config
        TimeInterval horizon = TimeInterval.seconds(600);
        TimeInterval interval = TimeInterval.seconds(15);
        StandardMetronomeConfig newMetronomeConfig = new StandardMetronomeConfig(horizon, interval, false);
        this.autoScaler.getMetronome().configure(newMetronomeConfig);

        // retrieve full configuration again and make it is up-to-date
        // (includes the latest subsystem configuration changes)
        assertThat(this.autoScaler.getConfiguration(), is(not(originalConfig)));
        JsonObject expectedConfig = parseJsonResource(CONFIG_WITH_ADDON);
        expectedConfig.add("metronome", toJson(newMetronomeConfig));
        assertThat(this.autoScaler.getConfiguration(), is(expectedConfig));
    }

    /**
     * Subsystems with null configurations must not be included in the effective
     * autoscaler configuration returned by
     * {@link AutoScaler#getConfiguration()}.
     */
    @Test
    public void excludeNullConfigsFromAutoScalerConfig() {
        // alerter configuration is empty/null
        JsonObject preConfig = parseJsonResource("autoscaler/config-no-alerter.json");
        this.autoScaler.validate(preConfig);
        this.autoScaler.configure(preConfig);

        assertThat(this.autoScaler.getAlerter().getConfiguration(), is(nullValue()));
        assertThat(this.autoScaler.getConfiguration(), is(preConfig));

    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithMissingMonitoringSubsystemConfig() throws Exception {
        JsonObject config = parseJsonResource(CONFIG);
        config.remove("monitoringSubsystem");
        this.autoScaler.validate(config);
    }

    /**
     * {@link StandardAlerter} configuration is optional. If none is given, a
     * default (empty) one is used by the alerter.
     */
    @Test
    public void configureWithMissingAlerterConfig() throws Exception {
        JsonObject config = parseJsonResource(CONFIG);
        config.remove("alerter");
        this.autoScaler.validate(config);
        this.autoScaler.configure(config);
        assertThat(this.autoScaler.getAlerter().getConfiguration(), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithMissingMetronomeConfig() throws Exception {
        JsonObject config = parseJsonResource(CONFIG);
        config.remove("metronome");
        this.autoScaler.validate(config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithMissingPredictionSubsystemConfig() throws Exception {
        JsonObject config = parseJsonResource(CONFIG);
        config.remove("predictionSubsystem");
        this.autoScaler.validate(config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithMissingCloudPoolConfig() throws Exception {
        JsonObject config = parseJsonResource(CONFIG);
        config.remove("cloudPool");
        this.autoScaler.validate(config);
    }

    /**
     * It should be allowed to leave out subsystem configurations (or set them
     * explicitly to null) if the particular implementations can live without
     * configurations. It is up to each subsystem to validate the configuration
     * (or lack thereof).
     */
    @Test
    public void nullSubsystemConfigurationsAreAllowed() {
        // create an autoscaler whose subsystems require no configuration
        AutoScaler tolerantAutoScaler = AutoScalerBuilder.newBuilder().withAlerter(NoOpAlerterStub.class)
                .withCloudPoolProxy(NoOpCloudPoolProxyStub.class)
                .withMonitoringSubsystem(NoOpMonitoringSubsystemStub.class)
                .withPredictionSubsystem(NoOpPredictionSubsystemStub.class).withMetronome(NoOpMetronomeStub.class)
                .withId("nullAcceptingAutoScaler").withUuid(UUID.randomUUID()).withStorageDir(new File(storageDir))
                .build();

        // set configuration missing all subsystem configurations
        JsonObject emptyConfig = JsonUtils.parseJsonString("{}").getAsJsonObject();
        tolerantAutoScaler.validate(emptyConfig);
        tolerantAutoScaler.configure(emptyConfig);
    }

    private JsonObject parseJsonResource(String resourceName) {
        return JsonUtils.parseJsonResource(resourceName).getAsJsonObject();
    }

    /**
     * Creates an {@link AutoScaler} with an add-on subsystem.
     *
     * @return
     */
    private AutoScaler createAutoscalerWithAddon() {
        // create autoscaler with an add-on subsystem
        Map<String, String> addons = Maps.of("fakeAddon", FakeAddon.class.getName());
        AutoScalerFactory factory = AutoScalerFactory.launch(new AutoScalerFactoryConfig(storageDir, addons));
        return factory.createAutoScaler(parseJsonResource(BLUEPRINT_RESOURCE));
    }
}
