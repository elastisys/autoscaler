package com.elastisys.autoscaler.server.restapi.core;

import static com.elastisys.scale.commons.json.JsonUtils.toObject;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.Charsets;
import org.junit.Test;

import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerBlueprint;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactory;
import com.elastisys.autoscaler.server.restapi.AutoScalerFactoryRestApi;
import com.elastisys.autoscaler.server.restapi.types.UrlsType;
import com.elastisys.scale.commons.util.io.IoUtils;
import com.google.gson.JsonObject;

/**
 * Exercises the factory operations of the {@link AutoScalerFactoryRestApi} REST
 * API.
 */
public class TestRestApiFactoryMethods extends AbstractAutoScalerCoreRestTest {

    @Test
    public void getInstancesOnEmptyFactory() {
        AutoScalerFactory factory = autoScalerFactory;
        assertThat(factory.getAutoScalerIds(), is(set()));
        assertThat(getInstances().getUrls(), is(list()));
    }

    @Test
    public void createInstance() throws Exception {
        AutoScalerFactory factory = autoScalerFactory;
        assertThat(factory.getAutoScalerIds(), is(set()));

        JsonObject blueprint = parseJsonResource("blueprints/autoscaler-blueprint.json");
        Response response = createInstance(blueprint);

        // verify that instance was created
        assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        List<String> instanceUrls = getInstances().getUrls();
        assertThat(instanceUrls, is(list(response.getLocation().toString())));
        String instanceId = instanceId(instanceUrls.get(0));

        // verify that instance is not configured and not started
        AutoScaler instance = factory.getAutoScaler(instanceId);
        assertThat(instance.getId(), is(instanceId));
        assertThat(instance.getStatus().getState(), is(State.STOPPED));
        assertThat(instance.getConfiguration(), is(nullValue()));
        assertThat(factory.getBlueprint(instanceId), is(toObject(blueprint, AutoScalerBlueprint.class)));
    }

    @Test
    public void createInstanceWithDefaults() throws Exception {
        AutoScalerFactory factory = autoScalerFactory;
        assertThat(factory.getAutoScalerIds(), is(set()));

        JsonObject blueprint = parseJsonResource("blueprints/autoscaler-blueprint-relying-on-defaults.json");
        Response response = createInstance(blueprint);

        // verify that instance was created
        assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        List<String> instanceUrls = getInstances().getUrls();
        assertThat(instanceUrls, is(list(response.getLocation().toString())));
        String instanceId = instanceId(instanceUrls.get(0));

        // verify that instance is not configured and not started
        AutoScaler instance = factory.getAutoScaler(instanceId);
        assertThat(instance.getId(), is(instanceId));
        assertThat(instance.getStatus().getState(), is(State.STOPPED));
        assertThat(instance.getConfiguration(), is(nullValue()));
        assertThat(factory.getBlueprint(instanceId), is(toObject(blueprint, AutoScalerBlueprint.class)));
    }

    @Test
    public void createWithDuplicateInstanceId() throws Exception {
        // create instance
        JsonObject blueprint = parseJsonResource("blueprints/autoscaler-blueprint.json");
        Response response = createInstance(blueprint);
        // verify that instance was created
        assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        List<String> instanceUrls = getInstances().getUrls();
        assertThat(instanceUrls, is(list(response.getLocation().toString())));

        // attempt to create a duplicate instance
        response = createInstance(parseJsonResource("blueprints/autoscaler-blueprint.json"));
        // verify that duplicate instance was NOT created
        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(getInstances().getUrls(), is(instanceUrls));
    }

    @Test
    public void createWithIllegalInstanceId() throws Exception {
        // attempt to create a duplicate instance
        String blueprintFile = "blueprints/autoscaler-blueprint-illegal-id.json";
        Response response = createInstance(parseJsonResource(blueprintFile));
        // verify that instance was NOT created
        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(getInstances().getUrls(), is(list()));
    }

    @Test
    public void createWithBadJsonSyntax() throws Exception {
        // attempt to create a instance
        String blueprintFile = "blueprints/autoscaler-blueprint-bad-json-syntax.json";
        String blueprint = IoUtils.toString(blueprintFile, Charsets.UTF_8);
        WebTarget resource = getClient().target(getAutoScalerBaseUrl() + "/autoscaler/instances");
        Response response = resource.request(MediaType.APPLICATION_JSON).post(Entity.text(blueprint));

        // verify that instance was NOT created
        assertThat(response.getStatus(), is(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode()));
        assertThat(getInstances().getUrls(), is(list()));
    }

    @Test
    public void createWithMissingField() throws Exception {
        // attempt to create instance
        String blueprintFile = "blueprints/autoscaler-blueprint-missing-id.json";
        Response response = createInstance(parseJsonResource(blueprintFile));
        // verify that instance was NOT created
        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(getInstances().getUrls(), is(list()));
    }

    @Test
    public void createWithUnknownSubsystem() throws Exception {
        // attempt to create instance
        String blueprintFile = "blueprints/autoscaler-blueprint-unknown-subsystem.json";
        Response response = createInstance(parseJsonResource(blueprintFile));
        // verify that instance was NOT created
        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        assertThat(getInstances().getUrls(), is(list()));
    }

    @Test
    public void getInstanceBlueprint() throws Exception {
        JsonObject blueprint = parseJsonResource("blueprints/autoscaler-blueprint.json");
        Response response = createInstance(blueprint);

        assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        String instanceUrl = getInstances().getUrls().get(0);
        String instanceId = instanceId(instanceUrl);

        assertThat(getInstanceBlueprint(instanceId), is(blueprint));
    }

    @Test
    public void getInstanceBlueprintWithUnknownId() throws Exception {
        String instanceId = "unknownId";
        Response response = get(instanceId, "/blueprint");
        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void deleteInstance() throws Exception {
        JsonObject blueprint = parseJsonResource("blueprints/autoscaler-blueprint.json");
        Response response = createInstance(blueprint);
        assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        String instanceUrl = getInstances().getUrls().get(0);
        String instanceId = instanceId(instanceUrl);

        response = deleteInstance(instanceId);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertThat(getInstances().getUrls(), is(list()));
    }

    @Test
    public void deleteInstanceWithUnknownId() throws Exception {
        String instanceId = "unknownId";
        Response response = deleteInstance(instanceId);
        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        assertThat(getInstances().getUrls(), is(list()));
    }

    private Response createInstance(JsonObject blueprint) {
        return post("", "", blueprint);
    }

    private Response deleteInstance(String instanceId) {
        return delete(instanceId, "");
    }

    private UrlsType getInstances() {
        return get("", "").readEntity(UrlsType.class);
    }

    private JsonObject getInstanceBlueprint(String instanceId) {
        return get(instanceId, "/blueprint").readEntity(JsonObject.class);
    }

    private String instanceId(String fullInstanceUrl) {
        int lastPathSeparator = fullInstanceUrl.lastIndexOf("/");
        return fullInstanceUrl.substring(lastPathSeparator + 1);
    }

    private Set<String> set(String... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }

    private List<String> list(String... elements) {
        return Arrays.asList(elements);
    }
}
