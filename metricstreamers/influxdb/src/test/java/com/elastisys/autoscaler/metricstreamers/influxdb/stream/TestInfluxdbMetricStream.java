package com.elastisys.autoscaler.metricstreamers.influxdb.stream;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLHandshakeException;

import org.eclipse.jetty.server.Server;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.Downsample;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.DownsampleFunction;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.PageFetchException;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultPage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.MetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.Query;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.SecurityConfig;
import com.elastisys.autoscaler.metricstreamers.influxdb.stream.errors.InfluxdbConnectException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.server.ServletDefinition;
import com.elastisys.scale.commons.server.ServletServerBuilder;
import com.elastisys.scale.commons.server.SslKeyStoreType;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link InfluxdbMetricStream} on a fake InfluxDB backend server
 * to verify that correct client settings are applied.
 */
public class TestInfluxdbMetricStream {

    private static Logger LOG = LoggerFactory.getLogger(TestInfluxdbMetricStream.class);

    /** Directory where fake server security settings are stored. */
    private static final String SERVER_DIR = "src/test/resources/fakeserver";
    /**
     * Sample response document that can be used to set up faked responses for
     * the fake server.
     */
    private static final File RESPONSE = new File(SERVER_DIR + "/response.json");

    /**
     * Sample response document with multiple colums. Used to prepare the fake
     * server with a response.
     */
    private static final File MULTI_COLUMN_RESPONSE = new File(SERVER_DIR + "/multi-column-response.json");

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
     * Verifies that the {@link InfluxdbFetcher} builds a proper query based on
     * the input config and time interval.
     */
    @Test
    public void queryBuilding() throws Exception {
        FakeInfluxdbQueryEndpoint influxdbFake = createInfluxdbFake(RESPONSE);
        this.server = startNoAuthHttpServer(influxdbFake, this.port);

        // minimal query
        Query query = Query.builder().select("\"value\"").from("\"cpu\"").build();
        InfluxdbMetricStream metricStream = new InfluxdbMetricStream(LOG,
                new MetricStreamConfig("localhost", this.port, null, streamDef("cpu.stream", "mydb", query)));
        QueryResultSet resultSet = metricStream.query(lastFiveMins(), null);
        resultSet.fetchNext();
        // verify the right query that was received by the this.server
        assertThat(influxdbFake.getLastCallParameters().get("q").get(0), is(
                "SELECT \"value\" FROM \"cpu\" WHERE '2016-01-01T11:55:00.000Z' <= time AND time <= '2016-01-01T12:00:00.000Z'"));

        // aggregation function
        query = Query.builder().select("sum(value)").from("cpu").build();
        metricStream = new InfluxdbMetricStream(LOG,
                new MetricStreamConfig("localhost", this.port, null, streamDef("cpu.stream", "mydb", query)));
        resultSet = metricStream.query(lastFiveMins(), null);
        resultSet.fetchNext();
        assertThat(influxdbFake.getLastCallParameters().get("q").get(0), is(
                "SELECT sum(value) FROM cpu WHERE '2016-01-01T11:55:00.000Z' <= time AND time <= '2016-01-01T12:00:00.000Z'"));

        // downsampling
        query = Query.builder().select("mean(value)").from("cpu").groupBy("time(1m) fill(none)").build();
        metricStream = new InfluxdbMetricStream(LOG,
                new MetricStreamConfig("localhost", this.port, null, streamDef("cpu.stream", "mydb", query)));
        resultSet = metricStream.query(lastFiveMins(), null);
        resultSet.fetchNext();
        assertThat(influxdbFake.getLastCallParameters().get("q").get(0), is(
                "SELECT mean(value) FROM cpu WHERE '2016-01-01T11:55:00.000Z' <= time AND time <= '2016-01-01T12:00:00.000Z' GROUP BY time(1m) fill(none)"));

        // rate conversion
        query = Query.builder().select("non_negative_derivative(max(value),1s)").from("cpu")
                .groupBy("time(1m) fill(none)").build();
        metricStream = new InfluxdbMetricStream(LOG,
                new MetricStreamConfig("localhost", this.port, null, streamDef("cpu.stream", "mydb", query)));
        resultSet = metricStream.query(lastFiveMins(), null);
        resultSet.fetchNext();
        assertThat(influxdbFake.getLastCallParameters().get("q").get(0), is(
                "SELECT non_negative_derivative(max(value),1s) FROM cpu WHERE '2016-01-01T11:55:00.000Z' <= time AND time <= '2016-01-01T12:00:00.000Z' GROUP BY time(1m) fill(none)"));

        // filter tags
        query = Query.builder().select("non_negative_derivative(max(value),1s)").from("cpu")
                .where("region =~ /us-.*/ AND cloud = 'aws'").groupBy("time(1m) fill(none)").build();
        metricStream = new InfluxdbMetricStream(LOG,
                new MetricStreamConfig("localhost", this.port, null, streamDef("cpu.stream", "mydb", query)));
        resultSet = metricStream.query(lastFiveMins(), null);
        resultSet.fetchNext();
        assertThat(influxdbFake.getLastCallParameters().get("q").get(0), is(
                "SELECT non_negative_derivative(max(value),1s) FROM cpu WHERE '2016-01-01T11:55:00.000Z' <= time AND time <= '2016-01-01T12:00:00.000Z' AND region =~ /us-.*/ AND cloud = 'aws' GROUP BY time(1m) fill(none)"));
    }

    /**
     * Verify that the {@link InfluxdbFetcher} correctly parses responses from
     * the InfluxDB endpoint.
     */
    @Test
    public void resultParsing() throws Exception {
        FakeInfluxdbQueryEndpoint influxdbFake = createInfluxdbFake(RESPONSE);
        this.server = startNoAuthHttpServer(influxdbFake, this.port);

        Query query = Query.builder().select("system").from("cpu").build();
        InfluxdbMetricStream metricStream = new InfluxdbMetricStream(LOG,
                new MetricStreamConfig("localhost", this.port, null, streamDef("cpu.system", "mydb", query)));
        QueryResultSet resultSet = metricStream.query(lastFiveMins(), null);
        QueryResultPage page = resultSet.fetchNext();

        // verify that response data points were correctly parsed
        List<MetricValue> expectedValues = asList(
                new MetricValue("cpu.system", 0.1, UtcTime.parse("2016-01-01T11:55:00.000Z")),
                new MetricValue("cpu.system", 0.2, UtcTime.parse("2016-01-01T11:58:00.000Z")),
                new MetricValue("cpu.system", 0.3, UtcTime.parse("2016-01-01T11:59:00.000Z")));
        assertThat(page.getMetricValues(), is(expectedValues));
    }

    /**
     * {@link InfluxdbFetcher} should only care about the first selected
     * field/column (if more than one field is selected, its result values are
     * silently ignored).
     */
    @Test
    public void ignoreAdditionalResultColumns() throws Exception {
        FakeInfluxdbQueryEndpoint influxdbFake = createInfluxdbFake(MULTI_COLUMN_RESPONSE);
        this.server = startNoAuthHttpServer(influxdbFake, this.port);

        // note: select two fields: 'SELECT system, user FROM cpu'
        Query query = Query.builder().select("mean(system), mean(user)").from("cpu").groupBy("time(5m) fill(none)")
                .build();
        InfluxdbMetricStream metricStream = new InfluxdbMetricStream(LOG,
                new MetricStreamConfig("localhost", this.port, null, streamDef("cpu.system", "mydb", query)));
        QueryResultSet resultSet = metricStream.query(lastFiveMins(), null);
        QueryResultPage page = resultSet.fetchNext();

        // verify that only the first column (mean(system)) was processed in the
        // result
        List<MetricValue> expectedValues = asList(
                new MetricValue("cpu.system", 0.20, UtcTime.parse("2016-01-01T11:50:00Z")),
                new MetricValue("cpu.system", 0.21, UtcTime.parse("2016-01-01T11:55:00Z")),
                new MetricValue("cpu.system", 0.22, UtcTime.parse("2016-01-01T12:00:00Z")));
        assertThat(page.getMetricValues(), is(expectedValues));
    }

    /**
     * In case a data settling time was specified, the query time interval
     * should always be adjusted to not include too recent (unsettled) data.
     */
    @Test
    public void adjustQueryForDataSettlingTime() throws Exception {
        FakeInfluxdbQueryEndpoint influxdbFake = createInfluxdbFake(RESPONSE);
        this.server = startNoAuthHttpServer(influxdbFake, this.port);

        // data settling time: 2 minutes
        Query query = Query.builder().select("\"system\"").from("\"cpu\"").build();
        InfluxdbMetricStream metricStream = new InfluxdbMetricStream(LOG, new MetricStreamConfig("localhost", this.port,
                null,
                streamDef("cpu.system", "mydb", query).withDataSettlingTime(new TimeInterval(2L, TimeUnit.MINUTES))));
        QueryResultSet resultSet = metricStream.query(lastFiveMins(), null);
        resultSet.fetchNext();

        // verify that the query time interval end was adjusted to not include
        // present time 12:00:00 but be truncated at the data settling point
        // (now - 2min == 11:58:00)
        assertThat(influxdbFake.getLastCallParameters().get("q").get(0), is(
                "SELECT \"system\" FROM \"cpu\" WHERE '2016-01-01T11:55:00.000Z' <= time AND time <= '2016-01-01T11:58:00.000Z'"));
    }

    /**
     * As long as the query interval is shorter than the max query chunk size,
     * query won't be split into sub-queries.
     */
    @Test
    public void singleChunkResponseWhenQueryIntervalShorterThanMaxChunkSize() throws Exception {
        FakeInfluxdbQueryEndpoint influxdbFake = createInfluxdbFake(RESPONSE);
        this.server = startNoAuthHttpServer(influxdbFake, this.port);

        Query query = Query.builder().select("\"system\"").from("\"cpu\"").build();
        TimeInterval queryChunkSize = new TimeInterval(1L, TimeUnit.HOURS);
        InfluxdbMetricStream metricStream = new InfluxdbMetricStream(LOG, new MetricStreamConfig("localhost", this.port,
                null, streamDef("cpu.system", "mydb", query).withQueryChunkSize(queryChunkSize)));

        // query interval within max chunk size
        Interval interval = new Interval(UtcTime.parse("2017-01-01T12:00:00.000Z"),
                UtcTime.parse("2017-01-01T13:00:00.000Z"));
        QueryResultSet resultSet = metricStream.query(interval, null);
        assertTrue(resultSet.hasNext());
        resultSet.fetchNext();
        // verify the right query that was received by the this.server
        assertThat(influxdbFake.getLastCallParameters().get("q").get(0), is(
                "SELECT \"system\" FROM \"cpu\" WHERE '2017-01-01T12:00:00.000Z' <= time AND time <= '2017-01-01T13:00:00.000Z'"));
        assertFalse(resultSet.hasNext());
    }

    /**
     * A query that exceed queryChunkSize should be split into several
     * sub-queries which are fetched incrementally.
     */
    @Test
    public void chunkedQueryFetchingOnLargeQueries() throws Exception {
        FakeInfluxdbQueryEndpoint influxdbFake = createInfluxdbFake(RESPONSE);
        this.server = startNoAuthHttpServer(influxdbFake, this.port);

        Query query = Query.builder().select("\"system\"").from("\"cpu\"").build();
        TimeInterval queryChunkSize = new TimeInterval(1L, TimeUnit.HOURS);
        InfluxdbMetricStream metricStream = new InfluxdbMetricStream(LOG, new MetricStreamConfig("localhost", this.port,
                null, streamDef("cpu.system", "mydb", query).withQueryChunkSize(queryChunkSize)));

        // run a query that should be fetched in three separate sub-queries
        Interval interval = new Interval(UtcTime.parse("2017-01-01T12:00:00.000Z"),
                UtcTime.parse("2017-01-01T14:30:00.000Z"));
        QueryResultSet resultSet = metricStream.query(interval, null);
        // first sub-query
        assertTrue(resultSet.hasNext());
        resultSet.fetchNext();
        assertThat(influxdbFake.getLastCallParameters().get("q").get(0), is(
                "SELECT \"system\" FROM \"cpu\" WHERE '2017-01-01T12:00:00.000Z' <= time AND time <= '2017-01-01T13:00:00.000Z'"));
        // second sub-query
        assertTrue(resultSet.hasNext());
        resultSet.fetchNext();
        assertThat(influxdbFake.getLastCallParameters().get("q").get(0), is(
                "SELECT \"system\" FROM \"cpu\" WHERE '2017-01-01T13:00:00.000Z' <= time AND time <= '2017-01-01T14:00:00.000Z'"));
        // third sub-query
        assertTrue(resultSet.hasNext());
        resultSet.fetchNext();
        assertThat(influxdbFake.getLastCallParameters().get("q").get(0), is(
                "SELECT \"system\" FROM \"cpu\" WHERE '2017-01-01T14:00:00.000Z' <= time AND time <= '2017-01-01T14:30:00.000Z'"));

        assertFalse(resultSet.hasNext());
    }

    /**
     * Verify that the {@link InfluxdbFetcher} can be configured to connect over
     * plain HTTP.
     */
    @Test
    public void connectOverHttp() throws Exception {
        FakeInfluxdbQueryEndpoint influxdbFake = createInfluxdbFake(RESPONSE);
        this.server = startNoAuthHttpServer(influxdbFake, this.port);

        boolean https = false;
        BasicCredentials auth = null;
        boolean verifyCert = false;
        boolean verifyHost = false;
        SecurityConfig security = new SecurityConfig(https, auth, verifyCert, verifyHost);
        Query query = Query.builder().select("\"system\"").from("\"cpu\"").build();
        MetricStreamConfig config = new MetricStreamConfig("localhost", this.port, Optional.of(security),
                streamDef("cpu.stream", "mydb", query));

        // no call has reached the server yet
        assertThat(influxdbFake.getLastCallUrl(), is(nullValue()));

        InfluxdbMetricStream metricStream = new InfluxdbMetricStream(LOG, config);
        QueryResultSet resultSet = metricStream.query(lastFiveMins(), null);
        resultSet.fetchNext();

        // verify that the call reached the server
        assertThat(influxdbFake.getLastCallUrl(), is(not(nullValue())));
        assertTrue(influxdbFake.getLastCallUrl().startsWith("http"));
    }

    /**
     * Verify that the {@link InfluxdbFetcher} can be configured to connect over
     * HTTPS.
     */
    @Test
    public void connectOverHttps() throws Exception {
        FakeInfluxdbQueryEndpoint influxdbFake = createInfluxdbFake(RESPONSE);
        this.server = startNoAuthHttpsServer(influxdbFake, this.port);

        boolean https = true;
        BasicCredentials auth = null;
        boolean verifyCert = false;
        boolean verifyHost = false;
        SecurityConfig security = new SecurityConfig(https, auth, verifyCert, verifyHost);
        Query query = Query.builder().select("\"system\"").from("\"cpu\"").build();
        MetricStreamConfig config = new MetricStreamConfig("localhost", this.port, Optional.of(security),
                streamDef("cpu.stream", "mydb", query));

        // no call has reached the server yet
        assertThat(influxdbFake.getLastCallUrl(), is(nullValue()));

        InfluxdbMetricStream metricStream = new InfluxdbMetricStream(LOG, config);
        QueryResultSet resultSet = metricStream.query(lastFiveMins(), null);
        resultSet.fetchNext();

        // verify that the call reached the server
        assertThat(influxdbFake.getLastCallUrl(), is(not(nullValue())));
        assertTrue(influxdbFake.getLastCallUrl().startsWith("https"));
    }

    /**
     * Verify that the {@link InfluxdbFetcher} can be configured to connect with
     * basic authentication.
     */
    @Test
    public void connectWithBasicAuth() throws Exception {
        FakeInfluxdbQueryEndpoint influxdbFake = createInfluxdbFake(RESPONSE);
        this.server = startAuthHttpsServer(influxdbFake, this.port);

        boolean https = true;
        BasicCredentials auth = new BasicCredentials("user", "password");
        boolean verifyCert = false;
        boolean verifyHost = false;
        SecurityConfig security = new SecurityConfig(https, auth, verifyCert, verifyHost);
        Query query = Query.builder().select("\"system\"").from("\"cpu\"").build();
        MetricStreamConfig config = new MetricStreamConfig("localhost", this.port, Optional.of(security),
                streamDef("cpu.stream", "mydb", query));

        // no call has reached the server yet
        assertThat(influxdbFake.getLastCallUrl(), is(nullValue()));

        InfluxdbMetricStream metricStream = new InfluxdbMetricStream(LOG, config);
        QueryResultSet resultSet = metricStream.query(lastFiveMins(), null);
        resultSet.fetchNext();

        // verify that the call reached the server
        assertThat(influxdbFake.getLastCallUrl(), is(not(nullValue())));
        assertTrue(influxdbFake.getLastCallUrl().startsWith("https"));
        // verify that basic auth header was included
        String expectedAuthHeader = String.format("Basic %s", Base64Utils.toBase64("user:password"));
        assertThat(influxdbFake.getLastCallHeaders().get("Authorization"), is(expectedAuthHeader));
    }

    /**
     * A 401 (Unauthorized) server response should result in an
     * {@link InfluxdbConnectException}.
     */
    @Test
    public void connectOnAuthFailure() throws Exception {
        FakeInfluxdbQueryEndpoint influxdbFake = createInfluxdbFake(RESPONSE);
        this.server = startAuthHttpsServer(influxdbFake, this.port);

        boolean https = true;
        BasicCredentials auth = new BasicCredentials("user", "wrong-password");
        boolean verifyCert = false;
        boolean verifyHost = false;
        SecurityConfig security = new SecurityConfig(https, auth, verifyCert, verifyHost);
        Query query = Query.builder().select("\"system\"").from("\"cpu\"").build();
        MetricStreamConfig config = new MetricStreamConfig("localhost", this.port, Optional.of(security),
                streamDef("cpu.stream", "mydb", query));

        InfluxdbMetricStream metricStream = new InfluxdbMetricStream(LOG, config);
        QueryResultSet resultSet = metricStream.query(lastFiveMins(), null);
        try {
            resultSet.fetchNext();
            fail("expected to fail");
        } catch (PageFetchException e) {
            assertThat(e.getCause(), is(instanceOf(InfluxdbConnectException.class)));
        }
    }

    /**
     * When asked to verify the server's certificate, the query must fail if the
     * server certificate is not up to standards.
     */
    @Test
    public void connectWithCertVerification() throws Exception {
        FakeInfluxdbQueryEndpoint influxdbFake = createInfluxdbFake(RESPONSE);
        this.server = startAuthHttpsServer(influxdbFake, this.port);

        boolean https = true;
        BasicCredentials auth = new BasicCredentials("user", "password");
        boolean verifyCert = true;
        boolean verifyHost = false;
        SecurityConfig security = new SecurityConfig(https, auth, verifyCert, verifyHost);
        Query query = Query.builder().select("\"system\"").from("\"cpu\"").build();
        MetricStreamConfig config = new MetricStreamConfig("localhost", this.port, Optional.of(security),
                streamDef("cpu.stream", "mydb", query));

        InfluxdbMetricStream metricStream = new InfluxdbMetricStream(LOG, config);
        QueryResultSet resultSet = metricStream.query(lastFiveMins(), null);
        // SSL handshake should fail since the server only runs with a
        // self-signed certificate and, hence, cert verification should fail
        try {
            resultSet.fetchNext();
            fail("server not expected to pass cert verification");
        } catch (PageFetchException e) {
            // expected
            assertThat(e.getCause(), is(instanceOf(InfluxdbConnectException.class)));
            assertThat(e.getCause().getCause(), is(instanceOf(SSLHandshakeException.class)));
        }
    }

    /**
     * When query specifies a downsampling parameter, that should take
     * precedence over the one configured for the stream.
     */
    @Test
    @Ignore // TODO: with the current query config, there is no straight-forward
            // way to apply a different downsampling interval and function. a
            // more restrictive query format is needed to support downsampling
            // hints, but would the sacrifice be worth it?
    public void queryHints() throws Exception {
        FakeInfluxdbQueryEndpoint influxdbFake = createInfluxdbFake(RESPONSE);
        this.server = startNoAuthHttpServer(influxdbFake, this.port);

        Query query = Query.builder().select("mean(\"system\")").from("\"cpu\"").groupBy("time(1m) fill(none)").build();
        InfluxdbMetricStream metricStream = new InfluxdbMetricStream(LOG,
                new MetricStreamConfig("localhost", this.port, null, streamDef("cpu.stream", "mydb", query)));

        // give downsampling as a query hint => should override stream config
        QueryOptions queryOptions = new QueryOptions(
                new Downsample(new TimeInterval(5L, TimeUnit.MINUTES), DownsampleFunction.MAX));
        QueryResultSet resultSet = metricStream.query(lastFiveMins(), queryOptions);
        resultSet.fetchNext();
        assertThat(influxdbFake.getLastCallParameters().get("q").get(0), is(
                "SELECT max(\"value\") FROM \"cpu\" WHERE '2016-01-01T11:55:00.000Z' <= time AND time <= '2016-01-01T12:00:00.000Z' GROUP BY time(300s) fill(none)"));

    }

    /**
     * Creates a fake influxdb server that listens to http requests and does not
     * require client authentication.
     *
     * @param influxdbFake
     * @param port
     * @return
     */
    private Server startNoAuthHttpServer(FakeInfluxdbQueryEndpoint influxdbFake, int port) throws Exception {
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
    private Server startNoAuthHttpsServer(FakeInfluxdbQueryEndpoint influxdbFake, int port) throws Exception {
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
    private Server startAuthHttpsServer(FakeInfluxdbQueryEndpoint influxdbFake, int port) throws Exception {
        ServletDefinition servlet = new ServletDefinition.Builder().servlet(influxdbFake).servletPath("/")
                .requireBasicAuth(true).realmFile(SERVER_DIR + "/security-realm.properties").requireRole("USER")
                .build();
        Server server = ServletServerBuilder.create().httpsPort(port).sslKeyStoreType(SslKeyStoreType.PKCS12)
                .sslKeyStorePath(SERVER_DIR + "/server_keystore.p12").sslKeyStorePassword("serverpassword")
                .sslRequireClientCert(false).addServlet(servlet).build();
        server.start();
        return server;
    }

    /**
     * Returns a stream definition with a given query.
     *
     * @return
     */
    private MetricStreamDefinition streamDef(String streamId, String db, Query query) {
        TimeInterval dataSettlingTime = null;
        TimeInterval queryChunkSize = null;
        MetricStreamDefinition stream = new MetricStreamDefinition(streamId, null, db, query, dataSettlingTime,
                queryChunkSize);
        return stream;
    }

    /**
     * Returns a time interval of the last five minutes (in test time).
     *
     * @return
     */
    private Interval lastFiveMins() {
        return new Interval(NOW.minusMinutes(5), NOW);
    }

    /**
     * Creates a {@link FakeInfluxdbQueryEndpoint} set up to always respond with
     * a given response document .
     *
     * @param responseFile
     *            File containing the JSON document to respond to client queries
     *            with.
     * @return
     */
    private FakeInfluxdbQueryEndpoint createInfluxdbFake(File responseFile) {
        return new FakeInfluxdbQueryEndpoint(JsonUtils.parseJsonFile(responseFile));
    }
}
