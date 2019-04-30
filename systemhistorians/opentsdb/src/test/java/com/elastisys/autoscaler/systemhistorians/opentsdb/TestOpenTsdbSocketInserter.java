package com.elastisys.autoscaler.systemhistorians.opentsdb;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link OpenTsdbSocketInserter}.
 */
public class TestOpenTsdbSocketInserter {
    static Logger logger = LoggerFactory.getLogger(TestOpenTsdbSocketInserter.class);

    /** Object under test. */
    private OpenTsdbSocketInserter inserter;

    /** Socket server used to respond to inserter requests. */
    private FixedReplySocketServer server;
    private Thread serverThread;

    @Before
    public void onSetup() throws Exception {
        Optional<String> empty = Optional.empty();
        this.server = new FixedReplySocketServer(empty, 0);
        this.serverThread = new Thread(this.server);
        this.serverThread.start();
        this.server.awaitStartup();

        int serverPort = this.server.getListenPort();
        this.inserter = new OpenTsdbSocketInserter(logger, "localhost", serverPort);
    }

    @After
    public void onTearDown() throws IOException, InterruptedException {
        this.server.close();
        this.serverThread.join();
    }

    /**
     * Verifies that the {@link OpenTsdbSocketInserter} succeeds when it does
     * not receive any (error) response from the OpenTSDB server.
     * <p/>
     * Note: in the current version of the protocol, the OpenTSDB server only
     * replies if an error occurs.
     */
    @Test
    public void insertWithNoServerResponse() throws OpenTsdbException {
        // simulated opentsdb server should respond with success (absent reply)
        Optional<String> absent = Optional.empty();
        this.server.setResponse(absent);

        // send message to opentsdb server
        Map<String, String> tags = Maps.of("host", "localhost");
        this.inserter.insert(new MetricValue("metric", 1.0, UtcTime.now(), tags));
    }

    /**
     * Verifies that the {@link OpenTsdbSocketInserter} fails when it receives
     * an (error) response from the OpenTSDB server.
     * <p/>
     * Note: in the current version of the protocol, the OpenTSDB server only
     * replies if an error occurs.
     *
     * @throws OpenTsdbException
     */
    @Test(expected = OpenTsdbException.class)
    public void insertWithServerErrorResponse() throws OpenTsdbException {
        // simulated opentsdb server should respond with error message
        this.server.setResponse(Optional.of("illegal argument: unrecognized metric"));

        // send message to opentsdb server
        Map<String, String> tags = Maps.of("host", "localhost");
        this.inserter.insert(new MetricValue("metric", 1.0, UtcTime.now(), tags));

    }
}
