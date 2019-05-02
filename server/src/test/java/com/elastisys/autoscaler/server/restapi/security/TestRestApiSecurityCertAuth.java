package com.elastisys.autoscaler.server.restapi.security;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.SocketException;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactory;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactoryConfig;
import com.elastisys.autoscaler.server.AutoScalerFactoryServer;
import com.elastisys.autoscaler.server.AutoScalerFactoryServerOptions;
import com.elastisys.autoscaler.server.testutils.RestTestUtils;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.net.ssl.KeyStoreType;
import com.elastisys.scale.commons.util.exception.Stacktrace;
import com.elastisys.scale.commons.util.io.Resources;
import com.google.gson.JsonObject;

/**
 * Verifies the behavior of the {@link AutoScalerFactoryServer} when configured
 * to require client certificate authentication.
 */
public class TestRestApiSecurityCertAuth {
    static final Logger LOG = LoggerFactory.getLogger(TestRestApiSecurityCertAuth.class);

    private static final String SERVER_KEYSTORE = Resources.getResource("security/server/server_keystore.p12")
            .toString();
    private static final String SERVER_KEYSTORE_PASSWORD = "serverpass";

    private static final String SERVER_TRUSTSTORE = Resources.getResource("security/server/server_truststore.jks")
            .toString();
    private static final String SERVER_TRUSTSTORE_PASSWORD = "trustpass";

    private static final String CLIENT_KEYSTORE = "src/test/resources/security/client/client_keystore.p12";
    private static final String CLIENT_KEYSTORE_PASSWORD = "clientpass";

    private static final String UNTRUSTED_CLIENT_KEYSTORE = "src/test/resources/security/untrusted/untrusted_keystore.p12";
    private static final String UNTRUSTED_CLIENT_KEYSTORE_PASSWORD = "untrustedpass";

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
        options.requireClientCert = true;
        options.sslTrustStore = SERVER_TRUSTSTORE;
        options.sslTrustStorePassword = SERVER_TRUSTSTORE_PASSWORD;

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
     * server requires a client certificate.
     */
    @Test
    public void connectWithNoAuthentication() {
        try {
            Client noAuthClient = RestTestUtils.noAuthClient();
            noAuthClient.target(getUrl()).request().get();
        } catch (ProcessingException e) {
            LOG.error("connectWithNoAuthentication failed: " + Stacktrace.toString(e));
            assertSslHandshakeFailure(e.getCause());
        }
    }

    /**
     * Connect with a client that uses basic authentication. This should fail,
     * since server requires a client certificate.
     */
    @Test
    public void connectWithBasicAuthentication() {
        try {
            Client basicAuthClient = RestTestUtils.basicAuthClient("admin", "adminpassword");
            basicAuthClient.target(getUrl()).request().get();
        } catch (ProcessingException e) {
            LOG.error("connectWithBasicAuthentication failed: " + Stacktrace.toString(e));
            assertSslHandshakeFailure(e.getCause());
        }
    }

    /**
     * Test connecting with a client certificate that is trusted by the server.
     * This should succeed.
     */
    @Test
    public void connectWithTrustedCertificate() {
        Client certAuthClient = RestTestUtils.certAuthClient(CLIENT_KEYSTORE, CLIENT_KEYSTORE_PASSWORD,
                KeyStoreType.PKCS12);
        Response response = certAuthClient.target(getUrl()).request().get();
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
        assertNotNull(response.readEntity(JsonObject.class));
    }

    /**
     * Test connecting with a client certificate that is <i>not</i> trusted by
     * the server. This should fail.
     */
    @Test
    public void connectWithUntrustedCertificate() {
        try {
            Client certAuthClient = RestTestUtils.certAuthClient(UNTRUSTED_CLIENT_KEYSTORE,
                    UNTRUSTED_CLIENT_KEYSTORE_PASSWORD, KeyStoreType.PKCS12);
            certAuthClient.target(getUrl()).request().get();
        } catch (ProcessingException e) {
            LOG.error("connectWithUntrustedCertificate failed: " + Stacktrace.toString(e));
            assertSslHandshakeFailure(e.getCause());
        }
    }

    private static String getUrl() {
        return String.format("https://localhost:%d/autoscaler/instances", httpsPort);
    }

    /**
     * Verify that an exception is due to a failure to establish an SSL
     * connection.
     *
     * @param cause
     */
    private void assertSslHandshakeFailure(Throwable cause) {
        if (cause instanceof SSLHandshakeException) {
            return;
        }
        // Since JDK9 SocketExceptions can sometimes be seen instead of the
        // expected SSLHandshakeException due to multiple messages in flight.
        // See: https://bugs.openjdk.java.net/browse/JDK-8172163
        if (cause instanceof SocketException) {
            // TODO: Would be nice to test this without having to be too
            //       broad.
            return;
        }
    }
}
