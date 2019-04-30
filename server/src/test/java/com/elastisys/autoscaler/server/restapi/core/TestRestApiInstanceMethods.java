package com.elastisys.autoscaler.server.restapi.core;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Test;

import com.elastisys.autoscaler.core.api.types.ServiceStatus.Health;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.server.restapi.AutoScalerFactoryRestApi;
import com.elastisys.autoscaler.server.restapi.types.ServiceStatusType;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Exercises the {@link AutoScaler} instance management operations of the
 * {@link AutoScalerFactoryRestApi} REST API.
 */
public class TestRestApiInstanceMethods extends AbstractAutoScalerCoreRestTest {

    /**
     * Retrieve an {@link AutoScaler} instance's UUID.
     */
    @Test
    public void getInstanceUuid() throws Exception {
        AutoScaler autoScaler = autoScalerFactory
                .createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint.json"));
        Response response = getUuid(autoScaler.getId());
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(response.readEntity(String.class), is(autoScaler.getUuid().toString()));
    }

    @Test
    public void getConfigurationBeforeConfigured() throws Exception {
        AutoScaler autoScaler = autoScalerFactory
                .createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint.json"));
        Response response = getConfig(autoScaler.getId());
        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
    }

    /**
     * Exercises configuration methods for a single {@link AutoScaler} instance.
     *
     * @throws Exception
     */
    @Test
    public void configureInstance() throws Exception {
        AutoScaler autoScaler = autoScalerFactory
                .createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint.json"));
        assertThat(autoScaler.getConfiguration(), is(nullValue()));
        assertThat(getState(autoScaler.getId()), is(State.STOPPED));

        // configure
        JsonObject config = parseJsonResource("autoscaler/autoscaler-config.json");
        Response response = postConfig(autoScaler.getId(), config);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(getState(autoScaler.getId()), is(State.STOPPED));

        // verify that configuration was actually set on instance
        response = getConfig(autoScaler.getId());
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(response.readEntity(JsonObject.class), is(config));
        assertThat(getState(autoScaler.getId()), is(State.STOPPED));
    }

    /**
     * Exercises configuration methods for a single {@link AutoScaler} instance
     * when there are many instances.
     * <p/>
     * Verifies that configuration is applied to the right {@link AutoScaler}
     * instance when there are several registered with the factory.
     *
     * @throws Exception
     */
    @Test
    public void configureWithMoreThanOneInstance() throws Exception {
        AutoScaler autoScaler1 = autoScalerFactory
                .createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint.json"));
        AutoScaler autoScaler2 = autoScalerFactory
                .createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint2.json"));

        // configure autoScaler1
        JsonObject config1 = parseJsonResource("autoscaler/autoscaler-config.json");
        Response response = postConfig(autoScaler1.getId(), config1);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        // verify that configuration was set on autoScaler1 and that autoScaler2
        // was unaffected
        response = getConfig(autoScaler1.getId());
        assertThat(getConfigJson(autoScaler1.getId()), is(config1));
        assertThat(autoScaler2.getConfiguration(), is(nullValue()));

        // configure autoScaler2
        JsonObject config2 = parseJsonResource("autoscaler/autoscaler-config2.json");
        response = postConfig(autoScaler2.getId(), config2);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        // verify that configuration was set on autoScaler2 and that autoScaler1
        // was unaffected
        response = getConfig(autoScaler2.getId());
        assertThat(getConfigJson(autoScaler2.getId()), is(config2));
        assertThat(getConfigJson(autoScaler1.getId()), is(config1));
    }

    @Test
    public void reconfigure() throws Exception {
        AutoScaler autoScaler1 = autoScalerFactory
                .createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint.json"));

        // configure
        JsonObject config1 = parseJsonResource("autoscaler/autoscaler-config.json");
        Response response = postConfig(autoScaler1.getId(), config1);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(getConfigJson(autoScaler1.getId()), is(config1));
        // re-configure
        JsonObject config2 = parseJsonResource("autoscaler/autoscaler-config2.json");
        response = postConfig(autoScaler1.getId(), config2);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(getConfigJson(autoScaler1.getId()), is(config2));
    }

    /**
     * Re-configure an {@link AutoScaler} instance with an invalid configuration
     * and verify that the request fails and that the instance continues to use
     * the prior configuration.
     *
     * @throws Exception
     */
    @Test
    public void reconfigureWithInvalidConfig() throws Exception {
        AutoScaler autoScaler1 = autoScalerFactory
                .createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint.json"));

        // configure
        JsonObject config1 = parseJsonResource("autoscaler/autoscaler-config.json");
        Response response = postConfig(autoScaler1.getId(), config1);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(getConfigJson(autoScaler1.getId()), is(config1));

        // re-configure
        JsonObject invalidConfig = parseJsonResource("autoscaler/autoscaler-config-invalid.json");
        response = postConfig(autoScaler1.getId(), invalidConfig);
        // request should fail
        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        // instance should keep using prior configuration
        assertThat(getConfigJson(autoScaler1.getId()), is(config1));
    }

    /**
     * An invalid configuration that doesn't follow the expected json schema (in
     * this case, the metric streamer config specifies tags as key-value pairs,
     * whereas it's expected to be key-list pairs), a 400 (Bad Request) response
     * should be given.
     */
    @Test
    public void configureWithInvalidConfig() throws Exception {
        AutoScaler autoScaler1 = autoScalerFactory
                .createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint.json"));
        // configure
        JsonObject config1 = parseJsonResource("autoscaler/invalid-metricstreamer-config.json");
        Response response = postConfig(autoScaler1.getId(), config1);
        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(autoScaler1.getConfiguration(), is(nullValue()));
    }

    @Test
    public void configureWithoutArgument() throws Exception {
        AutoScaler autoScaler1 = autoScalerFactory
                .createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint.json"));
        Response response = postConfig(autoScaler1.getId(), JsonUtils.parseJsonString("{}").getAsJsonObject());
        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
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
        AutoScaler autoScaler1 = autoScalerFactory
                .createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint.json"));

        // configure: has metric stream 'lbaas.connection.rate.stream'
        JsonObject preConfig = parseJsonResource("autoscaler/config1.json");
        Response response = postConfig(autoScaler1.getId(), preConfig);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(autoScaler1.getConfiguration(), is(preConfig));

        // reconfigure: metric stream was replaced with a new one
        // ('lbaas.active.connections.stream') but predictor still references
        // the old stream ''lbaas.connection.rate.stream'. Therefore, the
        // configuration should fail to be applied and rolled back to its prior
        // state.
        JsonObject postConfig = parseJsonResource("autoscaler/config1-with-illegal-metricstream-reference.json");
        response = postConfig(autoScaler1.getId(), postConfig);
        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        // verify that configuration was rolled back
        assertThat(autoScaler1.getConfiguration(), is(preConfig));
    }

    @Test
    public void start() throws Exception {
        AutoScaler autoScaler1 = autoScalerFactory
                .createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint.json"));
        assertThat(getStatus(autoScaler1.getId()), is(new ServiceStatusType(State.STOPPED, Health.OK, "")));

        // configure
        JsonObject config = parseJsonResource("autoscaler/autoscaler-config.json");
        Response response = postConfig(autoScaler1.getId(), config);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(getConfigJson(autoScaler1.getId()), is(config));
        assertThat(getState(autoScaler1.getId()), is(State.STOPPED));
        assertThat(getStatus(autoScaler1.getId()), is(new ServiceStatusType(State.STOPPED, Health.OK, "")));

        // start
        response = start(autoScaler1.getId());
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(getState(autoScaler1.getId()), is(State.STARTED));
        assertThat(getStatus(autoScaler1.getId()), is(new ServiceStatusType(State.STARTED, Health.OK, "")));
    }

    @Test
    public void startBeforeConfigured() throws Exception {
        AutoScaler autoScaler = autoScalerFactory
                .createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint.json"));
        assertThat(autoScaler.getConfiguration(), is(nullValue()));

        Response response = start(autoScaler.getId());
        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void stop() throws Exception {
        AutoScaler autoScaler1 = autoScalerFactory
                .createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint.json"));
        // configure
        JsonObject config = parseJsonResource("autoscaler/autoscaler-config.json");
        Response response = postConfig(autoScaler1.getId(), config);
        // start
        response = start(autoScaler1.getId());
        assertThat(getState(autoScaler1.getId()), is(State.STARTED));
        // stop
        response = stop(autoScaler1.getId());
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(getState(autoScaler1.getId()), is(State.STOPPED));
    }

    @Test
    public void stopBeforeStarted() throws Exception {
        AutoScaler autoScaler1 = autoScalerFactory
                .createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint.json"));
        assertThat(getState(autoScaler1.getId()), is(State.STOPPED));
        // stop
        Response response = stop(autoScaler1.getId());
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(getState(autoScaler1.getId()), is(State.STOPPED));
    }

    @Test
    public void restart() throws Exception {
        AutoScaler autoScaler1 = autoScalerFactory
                .createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint.json"));
        // configure
        JsonObject config = parseJsonResource("autoscaler/autoscaler-config.json");
        postConfig(autoScaler1.getId(), config);
        // start
        Response response = start(autoScaler1.getId());
        assertThat(getState(autoScaler1.getId()), is(State.STARTED));
        // stop
        response = stop(autoScaler1.getId());
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(getState(autoScaler1.getId()), is(State.STOPPED));
        // re-start
        response = start(autoScaler1.getId());
        assertThat(getState(autoScaler1.getId()), is(State.STARTED));
    }

}
