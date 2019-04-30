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
import com.google.gson.JsonObject;

/**
 * Verifies the behavior of the {@link AutoScalerFactoryServer} when configured
 * to require basic (username/password) client authentication.
 *
 *
 *
 */
public class TestRestApiSecurityBasicAuth {

    private static final String SERVER_KEYSTORE = "src/test/resources/security/server/server_keystore.p12";
    private static final String SERVER_KEYSTORE_PASSWORD = "serverpass";

    private static final String SECURITY_REALM_FILE = "src/test/resources/security/server/security-realm.properties";
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
        options.requireBasicAuth = true;
        options.requireClientCert = false;
        options.realmFile = SECURITY_REALM_FILE;
        options.requireRole = "USER";

        server = AutoScalerFactoryServer.createServer(autoScalerFactory, options);
        server.start();
    }

    @AfterClass
    public static void onTeardown() throws Exception {
        server.stop();
        server.join();
    }

    /**
     * Connect with a client that doesn't authenticate. This should fail, since
     * server requires a basic authentication.
     */
    @Test
    public void connectWithNoAuthentication() {
        Client noAuthClient = RestTestUtils.noAuthClient();
        noAuthClient.target(getUrl()).request().get();
    }

    /**
     * Connect with a client that uses basic authentication with valid
     * credentials. This should succeed.
     */
    @Test
    public void connectWithBasicAuthentication() {
        Client basicAuthClient = RestTestUtils.basicAuthClient("admin", "adminpassword");
        Response response = basicAuthClient.target(getUrl()).request().get();
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertNotNull(response.readEntity(JsonObject.class));
    }

    /**
     * Connect with a client that uses basic authentication but specifies an
     * unknown user name. This should fail.
     */
    @Test
    public void connectWithUnrecognizedUser() {
        Client basicAuthClient = RestTestUtils.basicAuthClient("unknown_user", "adminpassword");
        Response response = basicAuthClient.target(getUrl()).request().get();
        assertThat(response.getStatus(), is(Status.UNAUTHORIZED.getStatusCode()));
    }

    /**
     * Connect with a client that uses basic authentication but enters the wrong
     * password. This should fail.
     */
    @Test
    public void connectWithWrongPassword() {
        Client basicAuthClient = RestTestUtils.basicAuthClient("admin", "wrongpassword");
        Response response = basicAuthClient.target(getUrl()).request().get();
        assertThat(response.getStatus(), is(Status.UNAUTHORIZED.getStatusCode()));
    }

    /**
     * Connect with a client that uses basic authentication with valid
     * credentials but with a role that is not granted access. This should fail.
     */
    @Test
    public void connectWithBasicAuthenticationWrongRole() {
        Client basicAuthClient = RestTestUtils.basicAuthClient("guest", "guestpassword");
        Response response = basicAuthClient.target(getUrl()).request().get();
        assertThat(response.getStatus(), is(Status.FORBIDDEN.getStatusCode()));
    }

    /**
     * Test connecting with a client certificate. This should fail since the the
     * server expects basic authentication.
     */
    @Test
    public void connectWithCertificateAuthentication() {
        Client certAuthClient = RestTestUtils.certAuthClient("src/test/resources/security/client/client_keystore.p12",
                "clientpass", KeyStoreType.PKCS12);
        Response response = certAuthClient.target(getUrl()).request().get();
        assertThat(response.getStatus(), is(Status.UNAUTHORIZED.getStatusCode()));
    }

    private static String getUrl() {
        return String.format("https://localhost:%d/autoscaler/instances", httpsPort);
    }

}
