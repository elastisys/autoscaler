package com.elastisys.autoscaler.server.restapi.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.server.restapi.AutoScalerFactoryRestApi;
import com.google.gson.JsonObject;

/**
 * Configures the {@link SystemHistorian} subsystem over the
 * {@link AutoScalerFactoryRestApi} REST API.
 */
public class TestConfigureSystemHistorianOverRestApi extends AbstractAutoScalerCoreRestTest {

    /** {@link AutoScaler} instance under test. */
    private AutoScaler autoScaler;

    @Before
    public void prepare() throws Exception {
        this.autoScaler = autoScalerFactory.createAutoScaler(parseJsonResource("blueprints/autoscaler-blueprint.json"));
    }

    @Test
    public void configure() {
        // configure
        JsonObject config = parseJsonResource("systemHistorian/systemHistorian-config.json");
        Response response = postConfig(this.autoScaler.getId(), config);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(getConfigJson(this.autoScaler.getId()), is(config));
    }

    @Test
    public void reconfigure() {
        // configure
        JsonObject config = parseJsonResource("systemHistorian/systemHistorian-config.json");
        Response response = postConfig(this.autoScaler.getId(), config);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(getConfigJson(this.autoScaler.getId()), is(config));

        // reconfigure
        JsonObject newConfig = parseJsonResource("systemHistorian/systemHistorian-config2.json");
        response = postConfig(this.autoScaler.getId(), newConfig);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(getConfigJson(this.autoScaler.getId()), is(newConfig));
    }

    @Test
    public void configureWithInvalidConfig() {
        JsonObject config = parseJsonResource("systemHistorian/systemHistorian-config-invalid.json");
        Response response = postConfig(this.autoScaler.getId(), config);
        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(getConfig(this.autoScaler.getId()).getStatus(), is(Status.NOT_FOUND.getStatusCode()));
    }
}
