package com.elastisys.autoscaler.server.restapi.core;

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactory;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactoryConfig;
import com.elastisys.autoscaler.server.AutoScalerFactoryServer;
import com.elastisys.autoscaler.server.AutoScalerFactoryServerOptions;
import com.elastisys.autoscaler.server.restapi.AutoScalerFactoryRestApi;
import com.elastisys.autoscaler.server.restapi.types.ServiceStatusType;
import com.elastisys.autoscaler.server.testutils.RestTestUtils;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.google.gson.JsonObject;

/**
 * Base class that can be used to unit test the functionality of the
 * {@link AutoScalerFactoryRestApi} when deployed in an embedded Jetty web
 * server.
 * <p/>
 * This class takes care of setting up (and tearing down) an embedded
 * {@link JettyServer} onto which the {@link AutoScalerFactoryRestApi} web
 * application has been deployed. It also takes care of clearing the
 * {@link AutoScalerFactory} between each test method.
 */
public abstract class AbstractAutoScalerCoreRestTest {
    static final Logger LOG = LoggerFactory.getLogger(AbstractAutoScalerCoreRestTest.class);

    private static final String SERVER_KEYSTORE = "src/test/resources/security/server/server_keystore.p12";
    private static final String SERVER_KEYSTORE_PASSWORD = "serverpass";

    private static final String SECURITY_REALM_FILE = "src/test/resources/security/server/security-realm.properties";

    /** Where autoscaler instance state will be stored. */
    private final static String storageDir = "target/autoscaler/instances";

    /** Web server to use throughout the tests. */
    private static Server server;
    /** Server port to use for HTTPS. */
    private static int httpsPort;

    /** The autoscaler factory under test. */
    protected static AutoScalerFactory autoScalerFactory;

    @BeforeClass
    public static final void beforeTest() throws Exception {
        List<Integer> freePorts = HostUtils.findFreePorts(1);
        httpsPort = freePorts.get(0);

        autoScalerFactory = AutoScalerFactory.launch(new AutoScalerFactoryConfig(storageDir, null));
        AutoScalerFactoryServerOptions options = new AutoScalerFactoryServerOptions();
        options.httpsPort = httpsPort;
        options.sslKeyStore = SERVER_KEYSTORE;
        options.sslKeyStorePassword = SERVER_KEYSTORE_PASSWORD;
        options.requireClientCert = false;
        options.requireBasicAuth = true;
        options.realmFile = SECURITY_REALM_FILE;
        options.requireRole = "USER";

        LOG.info("server options: {}", options);
        server = AutoScalerFactoryServer.createServer(autoScalerFactory, options);
        server.start();
    }

    @Before
    public final void beforeTestMethod() {
        // reset auto-scaler factory before each unit test method (prevent any
        // saved autoscaler instances from earlier test runs/executions from
        // interfering with tests).
        autoScalerFactory.clear();
        LOG.debug("Test method can now run");
    }

    @After
    public final void afterTestMethod() {
        // reset auto-scaler factory after each unit test method
        LOG.debug("Test method has run, cleaning up");
        autoScalerFactory.clear();
    }

    @AfterClass
    public static final void afterTest() throws Exception {
        server.stop();
        server.join();
    }

    /**
     * Returns the base URL to the ElastiScale REST service endpoint.
     *
     * @return
     */
    public static String getAutoScalerBaseUrl() {
        return "https://localhost:" + httpsPort;
    }

    /**
     * Creates a secure Jersey REST {@link Client} used to connect over SSL to
     * the {@link AutoScalerFactoryRestApi}.
     *
     * @return
     */
    public static Client getClient() {
        return RestTestUtils.basicAuthClient("admin", "adminpassword");
    }

    /**
     * {@code GET}s a resource for a certain {@link AutoScaler} instance. That
     * is, it {@code GET}s: <br/>
     * {@code http://host:port/contextpath/autoscaler/instances/
     * <instanceId><path>}
     *
     * @param instanceId
     *            The identifier of the {@link AutoScaler} instance of interest.
     * @param path
     *            Path relative to {@link AutoScaler} instance's base URL.
     * @return A {@link Response} resulting from the {@code GET} call.
     */
    public static Response get(String instanceId, String path) {
        return getClient().target(getAutoScalerBaseUrl() + "/autoscaler/instances/" + instanceId + path).request()
                .get();
    }

    /**
     * {@code DELETE}s a resource for a certain {@link AutoScaler} instance.
     * That is, it {@code DELETE}s: <br/>
     * {@code http://host:port/contextpath/autoscaler/instances/
     * <instanceId><path>}
     *
     * @param instanceId
     *            The identifier of the {@link AutoScaler} instance of interest.
     * @param path
     *            Path relative to {@link AutoScaler} instance's base URL.
     * @return A {@link Response} resulting from the {@code DELETE} call.
     */
    public static Response delete(String instanceId, String path) {
        return getClient().target(getAutoScalerBaseUrl() + "/autoscaler/instances/" + instanceId + path).request()
                .delete();
    }

    /**
     * {@code POST}s a JSON entity to a path relative to a certain
     * {@link AutoScaler} instance. That is, it {@code POST}s the provided
     * entity to the following address: <br/>
     * {@code http://host:port/contextpath/autoscaler/instances/
     * <instanceId><path>}
     *
     * @param instanceId
     *            The identifier of the {@link AutoScaler} instance of interest.
     * @param path
     *            Path relative to {@link AutoScaler} instance's base URL.
     * @param entity
     *            The JSON entity to {@code POST}.
     * @return A {@link Response} resulting from the {@code POST} call.
     */
    public static Response post(String instanceId, String path, Object entity) {
        String url = getAutoScalerBaseUrl() + "/autoscaler/instances/" + instanceId + path;
        return getClient().target(url).request(MediaType.APPLICATION_JSON).post(Entity.json(entity));
    }

    protected JsonObject parseJsonResource(String resourceName) {
        return JsonUtils.parseJsonResource(resourceName).getAsJsonObject();
    }

    /**
     * Retrieves the {@link AutoScaler} status over REST.
     *
     * @param autoScalerId
     * @return
     */
    protected ServiceStatusType getStatus(String autoScalerId) {
        return get(autoScalerId, "/status").readEntity(ServiceStatusType.class);
    }

    /**
     * Retrieves the {@link AutoScaler} execution state over REST.
     *
     * @param autoScalerId
     * @return
     */
    protected State getState(String autoScalerId) {
        return getStatus(autoScalerId).getState();
    }

    /**
     * Starts the {@link AutoScaler} over REST.
     *
     * @param autoScalerId
     * @return
     */
    protected Response start(String autoScalerId) {
        return post(autoScalerId, "/start", null);
    }

    /**
     * Stops the {@link AutoScaler} over REST.
     *
     * @param autoScalerId
     * @return
     */
    protected Response stop(String autoScalerId) {
        return post(autoScalerId, "/stop", null);
    }

    /**
     * Retrieves the {@link AutoScaler}'s UUID over REST.
     *
     * @param autoScalerId
     * @return
     */
    protected Response getUuid(String autoScalerId) {
        return get(autoScalerId, "/uuid");
    }

    /**
     * Retrieves the {@link AutoScaler} configuration over REST.
     *
     * @param autoScalerId
     * @return
     */
    protected Response getConfig(String autoScalerId) {
        return get(autoScalerId, "/config");
    }

    /**
     * Retrieves the {@link AutoScaler} configuration over REST as a
     * {@link JsonObject}.
     *
     * @param autoScalerId
     * @return
     */
    protected JsonObject getConfigJson(String autoScalerId) {
        return get(autoScalerId, "/config").readEntity(JsonObject.class);
    }

    /**
     * Sets the {@link AutoScaler} config over REST.
     *
     * @param autoScalerId
     * @param config
     * @return
     */
    protected Response postConfig(String autoScalerId, JsonObject config) {
        return post(autoScalerId, "/config", config);
    }
}
