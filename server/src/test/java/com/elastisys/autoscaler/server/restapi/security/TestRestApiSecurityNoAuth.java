package com.elastisys.autoscaler.server.restapi.security;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactory;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactoryConfig;
import com.elastisys.autoscaler.server.AutoScalerFactoryServer;
import com.elastisys.autoscaler.server.AutoScalerFactoryServerOptions;
import com.elastisys.autoscaler.server.testutils.RestTestUtils;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.net.ssl.KeyStoreType;
import com.elastisys.scale.commons.util.io.Resources;
import com.google.gson.JsonObject;

/**
 * Verifies the behavior of the {@link AutoScalerFactoryServer} when executed
 * without requiring any client authentication at all.
 */
public class TestRestApiSecurityNoAuth {

    private static final String SERVER_KEYSTORE = Resources.getResource("security/server/server_keystore.p12")
            .toString();
    private static final String SERVER_KEYSTORE_PASSWORD = "serverpass";

    /** Where autoscaler instance state will be stored. */
    private final static String storageDir = "target/autoscaler/instances";

    /** The {@link AutoScalerFactory} under test. */
    private static AutoScalerFactory autoScalerFactory;
    /** Web server to use throughout the tests. */
    private static Server server;
    /** Server port to use for HTTPS. */
    private static int httpsPort;

    @BeforeClass
    public static void onSetup() throws Exception {
        List<Integer> freePorts = HostUtils.findFreePorts(1);
        httpsPort = freePorts.get(0);

        autoScalerFactory = AutoScalerFactory.launch(new AutoScalerFactoryConfig(storageDir, null));
        autoScalerFactory.clear();
        AutoScalerFactoryServerOptions options = new AutoScalerFactoryServerOptions();
        options.httpsPort = httpsPort;
        options.sslKeyStore = SERVER_KEYSTORE;
        options.sslKeyStorePassword = SERVER_KEYSTORE_PASSWORD;
        options.requireBasicAuth = false;
        options.requireClientCert = false;

        server = AutoScalerFactoryServer.createServer(autoScalerFactory, options);
        server.start();
    }

    @AfterClass
    public static void onTeardown() throws Exception {
        server.stop();
        server.join();
    }

    /**
     * Test connecting with clients using different authentication mechanisms.
     * All should succeed, since the server doesn't care about client
     * authentication.
     */
    @Test
    public void testConnect() {
        Client noAuthClient = RestTestUtils.noAuthClient();
        Response response = noAuthClient.target(getUrl()).request().get();
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertNotNull(response.readEntity(JsonObject.class));

        Client basicAuthClient = RestTestUtils.basicAuthClient("admin", "adminpassword");
        response = basicAuthClient.target(getUrl()).request().get();
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertNotNull(response.readEntity(JsonObject.class));

        Client certAuthClient = RestTestUtils.certAuthClient("src/test/resources/security/client/client_keystore.p12",
                "clientpass", KeyStoreType.PKCS12);
        response = certAuthClient.target(getUrl()).request().get();
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertNotNull(response.readEntity(JsonObject.class));
    }

    private static String getUrl() {
        return String.format("https://localhost:%d/autoscaler/instances", httpsPort);
    }

}
