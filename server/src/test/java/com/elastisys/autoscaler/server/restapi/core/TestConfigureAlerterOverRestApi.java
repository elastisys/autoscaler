package com.elastisys.autoscaler.server.restapi.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.core.alerter.api.Alerter;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.server.restapi.AutoScalerFactoryRestApi;
import com.google.gson.JsonObject;

/**
 * Configures the {@link Alerter} subsystem over the
 * {@link AutoScalerFactoryRestApi} REST API.
 */
public class TestConfigureAlerterOverRestApi extends AbstractAutoScalerCoreRestTest {
    /** {@link AutoScaler} instance under test. */
    private AutoScaler autoScaler;

    @Before
    public void prepare() {
        this.autoScaler = AbstractAutoScalerCoreRestTest.autoScalerFactory
                .createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint.json"));
    }

    @Test
    public void configure() {
        // configure
        JsonObject config = parseJsonResource("alerter/alerter_smtp.json");
        Response response = postConfig(this.autoScaler.getId(), config);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(getConfigJson(this.autoScaler.getId()), is(config));
    }

    @Test
    public void reconfigure() {
        // configure
        JsonObject config = parseJsonResource("alerter/alerter_smtp.json");
        Response response = postConfig(this.autoScaler.getId(), config);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(getConfigJson(this.autoScaler.getId()), is(config));

        // reconfigure with http and smtp alerter
        JsonObject newConfig = parseJsonResource("alerter/alerter_http_and_smtp.json");
        response = postConfig(this.autoScaler.getId(), newConfig);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(getConfigJson(this.autoScaler.getId()), is(newConfig));
    }

    @Test
    public void configureWithInvalidConfig() {
        JsonObject config = parseJsonResource("alerter/alerter_http_invalid.json");
        Response response = postConfig(this.autoScaler.getId(), config);
        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(getConfig(this.autoScaler.getId()).getStatus(), is(Status.NOT_FOUND.getStatusCode()));
    }
}
