package com.elastisys.autoscaler.systemhistorians.influxdb.inserter.impl;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

import org.eclipse.jetty.server.Server;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.systemhistorians.influxdb.config.InfluxdbSecurityConfig;
import com.elastisys.autoscaler.systemhistorians.influxdb.config.InfluxdbSystemHistorianConfig;
import com.elastisys.autoscaler.systemhistorians.influxdb.inserter.InfluxdbInserterException;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.server.ServletDefinition;
import com.elastisys.scale.commons.server.ServletServerBuilder;
import com.elastisys.scale.commons.server.SslKeyStoreType;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.exception.Stacktrace;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises {@link HttpInfluxdbInserter}.
 */
public class TestHttpInfluxdbInserter {

    private static Logger LOG = LoggerFactory.getLogger(TestHttpInfluxdbInserter.class);

    /** Directory where fake server security settings are stored. */
    private static final String SERVER_DIR = "src/test/resources/fakeserver";
    /**
     * Sample response document that can be used to set up faked responses for
     * the fake server.
     */
    private static final File RESPONSE = new File(SERVER_DIR + "/response.json");

    /** The current system time set in the test. */
    private static final DateTime NOW = UtcTime.parse("2016-01-01T12:00:00.000Z");

    /** The local port where the server is set up to listen. */
    private int port = HostUtils.findFreePorts(1).get(0);
    /** The fake InfluxDB server set up under the test. */
    private Server server;

    @Before
    public void beforeTestMethod() {
        FrozenTime.setFixed(NOW);

        // server instances are created by each individual test method
        this.server = null;
    }

    /**
     * Tears down the {@link Server} instance (if any) created by the test.
     */
    @After
    public void onTeardown() throws Exception {
        if (this.server != null) {
            this.server.stop();
            this.server.join();
        }
    }

    /**
     * Verifies that the {@link HttpInfluxdbInserter} posts correctly formatted
     * data points (that adhere to the line protocol) to the InfluxDB server
     * when asked to insert {@link MetricValue}s.
     */
    @Test
    public void datapointInsert() throws Exception {
        FakeInfluxdbEndpoint influxdbFake = new FakeInfluxdbEndpoint(200);
        this.server = startNoAuthHttpServer(influxdbFake, this.port);

        boolean https = false;
        BasicCredentials auth = null;
        boolean verifyCert = false;
        boolean verifyHost = false;
        InfluxdbSecurityConfig security = new InfluxdbSecurityConfig(https, auth, verifyCert, verifyHost);
        InfluxdbSystemHistorianConfig config = new InfluxdbSystemHistorianConfig("localhost", this.port, "mydb",
                security, null, null);

        HttpInfluxdbInserter inserter = new HttpInfluxdbInserter(LOG, config);

        // no call has reached the server yet
        assertThat(influxdbFake.getCallUrlHistory().isEmpty(), is(true));

        inserter.insert(metricValues());

        // verify that expected calls were made to the server and that
        // correctly formatted data was posted

        // first call should be a "CREATE DATABASE" call
        assertThat(influxdbFake.getCallParametersHistory().get(0).get("q").get(0), is("CREATE DATABASE \"mydb\""));
        // second call should post the datapoints in the line protocol format
        String expectedReport = String.join("\n", asList("metric1 value=1.0 1451649600000000000", //
                "metric.2 value=2.0 1451649661100000000", //
                "metric3,region=us-east-1,host=srv1 value=1.0 1451649722222000000"));
        assertThat(influxdbFake.getCallBodyHistory().get(1), is(expectedReport));
    }

    /**
     * On failure to create the database (prior to inserting metric values) the
     * entire operation must fail, with an {@link InfluxdbInserterException}.
     */
    @Test
    public void datapointInsertOnServerFailure() throws Exception {
        // influxdb will fail
        FakeInfluxdbEndpoint influxdbFake = new FakeInfluxdbEndpoint(500);
        this.server = startNoAuthHttpServer(influxdbFake, this.port);

        boolean https = false;
        BasicCredentials auth = null;
        boolean verifyCert = false;
        boolean verifyHost = false;
        InfluxdbSecurityConfig security = new InfluxdbSecurityConfig(https, auth, verifyCert, verifyHost);
        InfluxdbSystemHistorianConfig config = new InfluxdbSystemHistorianConfig("localhost", this.port, "mydb",
                security, null, null);

        HttpInfluxdbInserter inserter = new HttpInfluxdbInserter(LOG, config);

        // no call has reached the server yet
        assertThat(influxdbFake.getCallUrlHistory().isEmpty(), is(true));

        try {
            inserter.insert(metricValues());
            fail("insert call expected to fail");
        } catch (InfluxdbInserterException e) {
            // expected to fail
            assertTrue(e.getMessage().contains("influxdb server responded with 500 status code"));
        }

        // verify that only database creation call came through
        assertThat(influxdbFake.getCallUrlHistory().size(), is(1));
        assertThat(influxdbFake.getCallParametersHistory().get(0).get("q").get(0), is("CREATE DATABASE \"mydb\""));

    }

    /**
     * Verify that the {@link HttpInfluxdbInserter} can be configured to connect
     * over plain HTTP.
     */
    @Test
    public void connectOverHttp() throws Exception {
        FakeInfluxdbEndpoint influxdbFake = new FakeInfluxdbEndpoint(200);
        this.server = startNoAuthHttpServer(influxdbFake, this.port);

        boolean https = false;
        BasicCredentials auth = null;
        boolean verifyCert = false;
        boolean verifyHost = false;
        InfluxdbSecurityConfig security = new InfluxdbSecurityConfig(https, auth, verifyCert, verifyHost);
        InfluxdbSystemHistorianConfig config = new InfluxdbSystemHistorianConfig("localhost", this.port, "mydb",
                security, null, null);

        HttpInfluxdbInserter inserter = new HttpInfluxdbInserter(LOG, config);

        // no call has reached the server yet
        assertThat(influxdbFake.getCallUrlHistory().isEmpty(), is(true));

        inserter.insert(metricValues());

        // verify that the call reached the server
        assertThat(influxdbFake.getCallUrlHistory().isEmpty(), is(false));
        assertTrue(influxdbFake.getCallUrlHistory().get(0).startsWith("http"));
    }

    /**
     * Verify that the {@link HttpInfluxdbInserter} can be configured to connect
     * over HTTPS.
     */
    @Test
    public void connectOverHttps() throws Exception {
        FakeInfluxdbEndpoint influxdbFake = new FakeInfluxdbEndpoint(200);
        this.server = startNoAuthHttpsServer(influxdbFake, this.port);

        boolean https = true;
        BasicCredentials auth = null;
        boolean verifyCert = false;
        boolean verifyHost = false;
        InfluxdbSecurityConfig security = new InfluxdbSecurityConfig(https, auth, verifyCert, verifyHost);
        InfluxdbSystemHistorianConfig config = new InfluxdbSystemHistorianConfig("localhost", this.port, "mydb",
                security, null, null);

        HttpInfluxdbInserter inserter = new HttpInfluxdbInserter(LOG, config);

        // no call has reached the server yet
        assertThat(influxdbFake.getCallUrlHistory().isEmpty(), is(true));

        inserter.insert(metricValues());

        // verify that the call reached the server
        assertThat(influxdbFake.getCallUrlHistory().isEmpty(), is(false));
        assertTrue(influxdbFake.getCallUrlHistory().get(0).startsWith("http"));
    }

    /**
     * Verify that the {@link HttpInfluxdbInserter} can be configured to connect
     * with basic authentication.
     */
    @Test
    public void connectWithBasicAuth() throws Exception {
        FakeInfluxdbEndpoint influxdbFake = new FakeInfluxdbEndpoint(200);
        this.server = startAuthHttpsServer(influxdbFake, this.port);

        boolean https = true;
        BasicCredentials auth = new BasicCredentials("user", "password");
        boolean verifyCert = false;
        boolean verifyHost = false;
        InfluxdbSecurityConfig security = new InfluxdbSecurityConfig(https, auth, verifyCert, verifyHost);
        InfluxdbSystemHistorianConfig config = new InfluxdbSystemHistorianConfig("localhost", this.port, "mydb",
                security, null, null);

        HttpInfluxdbInserter inserter = new HttpInfluxdbInserter(LOG, config);

        // no call has reached the server yet
        assertThat(influxdbFake.getCallUrlHistory().isEmpty(), is(true));

        inserter.insert(metricValues());

        // verify that the call reached the server
        assertThat(influxdbFake.getCallUrlHistory().isEmpty(), is(false));
        assertTrue(influxdbFake.getCallUrlHistory().get(0).startsWith("https"));
        // verify that basic auth header was included
        String expectedAuthHeader = String.format("Basic %s", Base64Utils.toBase64("user:password"));
        assertThat(influxdbFake.getCallHeadersHistory().get(0).get("Authorization"), is(expectedAuthHeader));
    }

    /**
     * A 401 (Unauthorized) server response should result in an
     * {@link InfluxdbInserterException}.
     */
    @Test
    public void connectOnAuthFailure() throws Exception {
        FakeInfluxdbEndpoint influxdbFake = new FakeInfluxdbEndpoint(200);
        this.server = startAuthHttpsServer(influxdbFake, this.port);

        boolean https = true;
        BasicCredentials auth = new BasicCredentials("user", "wrong-password");
        boolean verifyCert = false;
        boolean verifyHost = false;
        InfluxdbSecurityConfig security = new InfluxdbSecurityConfig(https, auth, verifyCert, verifyHost);
        InfluxdbSystemHistorianConfig config = new InfluxdbSystemHistorianConfig("localhost", this.port, "mydb",
                security, null, null);

        HttpInfluxdbInserter inserter = new HttpInfluxdbInserter(LOG, config);

        try {
            inserter.insert(metricValues());
            fail("expected to fail authentication");
        } catch (InfluxdbInserterException e) {
            assertThat(e.getMessage().contains("401"), is(true));
        }
    }

    /**
     * When asked to verify the server's certificate, the insert must fail if
     * the server certificate is not up to standards.
     */
    @Test
    public void connectWithCertVerification() throws Exception {
        FakeInfluxdbEndpoint influxdbFake = new FakeInfluxdbEndpoint(200);
        this.server = startAuthHttpsServer(influxdbFake, this.port);

        boolean https = true;
        BasicCredentials auth = new BasicCredentials("user", "password");
        boolean verifyCert = true;
        boolean verifyHost = false;
        InfluxdbSecurityConfig security = new InfluxdbSecurityConfig(https, auth, verifyCert, verifyHost);
        InfluxdbSystemHistorianConfig config = new InfluxdbSystemHistorianConfig("localhost", this.port, "mydb",
                security, null, null);

        HttpInfluxdbInserter inserter = new HttpInfluxdbInserter(LOG, config);

        // SSL handshake should fail since the server only runs with a
        // self-signed certificate and, hence, cert verification should fail
        try {
            inserter.insert(metricValues());
            fail("server not expected to pass cert verification");
        } catch (InfluxdbInserterException e) {
            // expected
            assertTrue(Stacktrace.causeChain(e).stream()
                    .anyMatch(err -> err.getClass().equals(SSLHandshakeException.class)));
        }
    }

    /**
     * Verifies that {@link HttpInfluxdbInserter} uses a proper query endpoint
     * URL based on its configuration.
     */
    @Test
    public void queryUrlGeneration() {
        boolean https = false;
        InfluxdbSecurityConfig security = new InfluxdbSecurityConfig(https, null, false, false);
        InfluxdbSystemHistorianConfig config = new InfluxdbSystemHistorianConfig("some.host", 8086, "mydb", security,
                null, null);

        // http
        HttpInfluxdbInserter inserter = new HttpInfluxdbInserter(LOG, config);
        assertThat(inserter.queryUrl(), is("http://some.host:8086/query"));

        https = true;
        security = new InfluxdbSecurityConfig(https, null, false, false);
        config = new InfluxdbSystemHistorianConfig("some.host", 8888, "mydb", security, null, null);

        // https
        inserter = new HttpInfluxdbInserter(LOG, config);
        assertThat(inserter.queryUrl(), is("https://some.host:8888/query"));
    }

    /**
     * Verifies that {@link HttpInfluxdbInserter} uses a proper write URL based
     * on its configuration.
     */
    @Test
    public void writeUrlGeneration() {
        boolean https = false;
        InfluxdbSecurityConfig security = new InfluxdbSecurityConfig(https, null, false, false);
        InfluxdbSystemHistorianConfig config = new InfluxdbSystemHistorianConfig("some.host", 8086, "mydb", security,
                null, null);

        // http
        HttpInfluxdbInserter inserter = new HttpInfluxdbInserter(LOG, config);
        assertThat(inserter.writeUrl(), is("http://some.host:8086/write"));

        https = true;
        security = new InfluxdbSecurityConfig(https, null, false, false);
        config = new InfluxdbSystemHistorianConfig("some.host", 8888, "mydb", security, null, null);

        // https
        inserter = new HttpInfluxdbInserter(LOG, config);
        assertThat(inserter.writeUrl(), is("https://some.host:8888/write"));
    }

    /**
     * Creates a fake influxdb server that listens to http requests and does not
     * require client authentication.
     *
     * @param influxdbFake
     * @param port
     * @return
     */
    private Server startNoAuthHttpServer(FakeInfluxdbEndpoint influxdbFake, int port) throws Exception {
        ServletDefinition servlet = new ServletDefinition.Builder().servlet(influxdbFake).servletPath("/").build();
        Server server = ServletServerBuilder.create().httpPort(port).addServlet(servlet).build();
        server.start();
        return server;
    }

    /**
     * Creates a fake influxdb server that listens to https requests and does
     * not require client authentication.
     *
     * @param influxdbFake
     * @param port
     * @return
     */
    private Server startNoAuthHttpsServer(FakeInfluxdbEndpoint influxdbFake, int port) throws Exception {
        ServletDefinition servlet = new ServletDefinition.Builder().servlet(influxdbFake).servletPath("/")
                .requireBasicAuth(false).build();
        Server server = ServletServerBuilder.create().httpsPort(port).sslKeyStoreType(SslKeyStoreType.PKCS12)
                .sslKeyStorePath(SERVER_DIR + "/server_keystore.p12").sslKeyStorePassword("serverpassword")
                .sslRequireClientCert(false).addServlet(servlet).build();

        server.start();
        return server;
    }

    /**
     * Creates a fake influxdb server that listens to https requests and
     * requires basic auth client authentication (as given in the
     * {@code security-realm.properties} file).
     *
     * @param influxdbFake
     * @param port
     * @return
     */
    private Server startAuthHttpsServer(FakeInfluxdbEndpoint influxdbFake, int port) throws Exception {
        ServletDefinition servlet = new ServletDefinition.Builder().servlet(influxdbFake).servletPath("/")
                .requireBasicAuth(true).realmFile(SERVER_DIR + "/security-realm.properties").requireRole("USER")
                .build();
        Server server = ServletServerBuilder.create().httpsPort(port).sslKeyStoreType(SslKeyStoreType.PKCS12)
                .sslKeyStorePath(SERVER_DIR + "/server_keystore.p12").sslKeyStorePassword("serverpassword")
                .sslRequireClientCert(false).addServlet(servlet).build();
        server.start();
        return server;
    }

    private Collection<MetricValue> metricValues() {
        MetricValue value1 = new MetricValue("metric1", 1.0, UtcTime.parse("2016-01-01T12:00:00.000Z"));
        MetricValue value2 = new MetricValue("metric.2", 2.0, UtcTime.parse("2016-01-01T12:01:01.100Z"));
        Map<String, String> tags = Maps.of("region", "us-east-1", "host", "srv1");
        MetricValue value3 = new MetricValue("metric3", 1.0, UtcTime.parse("2016-01-01T12:02:02.222Z"), tags);
        return Arrays.asList(value1, value2, value3);
    }

}
