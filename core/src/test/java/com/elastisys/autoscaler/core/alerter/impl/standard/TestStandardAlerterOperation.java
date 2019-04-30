package com.elastisys.autoscaler.core.alerter.impl.standard;

import static com.elastisys.autoscaler.core.api.types.ServiceStatus.State.STARTED;
import static com.elastisys.autoscaler.core.api.types.ServiceStatus.State.STOPPED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.alerter.impl.standard.config.StandardAlerterConfig;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Exercises the logic of the {@link StandardAlerter}.
 */
public class TestStandardAlerterOperation {

    private static final String AUTOSCALER_ID = "autoScaler-1";
    private static final UUID AUTOSCALER_UUID = UUID.randomUUID();
    private static final String HTTP_ALERTER_CONFIG = "alerter/alerter_http.json";
    private static final String SMTP_ALERTER_CONFIG = "alerter/alerter_smtp.json";

    private final Logger logger = LoggerFactory.getLogger(TestStandardAlerterOperation.class);
    private final EventBus eventBus = new SynchronousEventBus(this.logger);

    /** Object under test. */
    private StandardAlerter alerter;

    @Before
    public void onSetup() {
        FrozenTime.setFixed(UtcTime.parse("2015-01-01T12:00:00.000Z"));
        this.alerter = new StandardAlerter(AUTOSCALER_UUID, AUTOSCALER_ID, this.logger, this.eventBus);
    }

    @Test
    public void configure() throws Exception {
        StandardAlerterConfig alerterConfig = loadConfig(HTTP_ALERTER_CONFIG);
        this.alerter.validate(alerterConfig);
        this.alerter.configure(alerterConfig);
        assertThat(this.alerter.getConfiguration(), is(loadConfig(HTTP_ALERTER_CONFIG)));
    }

    @Test
    public void reconfigure() throws Exception {
        // configure
        StandardAlerterConfig alerterConfig = loadConfig(HTTP_ALERTER_CONFIG);
        this.alerter.validate(alerterConfig);
        this.alerter.configure(alerterConfig);
        assertThat(this.alerter.getConfiguration(), is(loadConfig(HTTP_ALERTER_CONFIG)));

        // re-configure
        alerterConfig = loadConfig(SMTP_ALERTER_CONFIG);
        this.alerter.validate(alerterConfig);
        this.alerter.configure(alerterConfig);
        assertThat(this.alerter.getConfiguration(), is(loadConfig(SMTP_ALERTER_CONFIG)));
        assertThat(this.alerter.getConfiguration(), is(not(loadConfig(HTTP_ALERTER_CONFIG))));
    }

    @Test
    public void startStopSanity() throws IOException {
        assertThat(this.alerter.getStatus().getState(), is(STOPPED));
        StandardAlerterConfig alerterConfig = loadConfig(HTTP_ALERTER_CONFIG);

        this.alerter.validate(alerterConfig);
        assertThat(this.alerter.getStatus().getState(), is(STOPPED));
        this.alerter.configure(alerterConfig);
        assertThat(this.alerter.getStatus().getState(), is(STOPPED));

        this.alerter.start();
        assertThat(this.alerter.getStatus().getState(), is(STARTED));
        // start is idempotent
        this.alerter.start();
        assertThat(this.alerter.getStatus().getState(), is(STARTED));

        this.alerter.stop();
        assertThat(this.alerter.getStatus().getState(), is(STOPPED));
        // stop is idempotent
        this.alerter.stop();
        assertThat(this.alerter.getStatus().getState(), is(STOPPED));
    }

    @Test(expected = IllegalArgumentException.class)
    public void incorrectConfiguration() throws IOException {
        configureAlerter("alerter/alerter_http_invalid.json");
    }

    @Test(expected = IllegalStateException.class)
    public void startBeforeConfigured() throws IOException {
        assertThat(this.alerter.getStatus().getState(), is(STOPPED));

        StandardAlerterConfig alerterConfig = loadConfig(HTTP_ALERTER_CONFIG);
        this.alerter.validate(alerterConfig);
        assertThat(this.alerter.getStatus().getState(), is(STOPPED));

        this.alerter.start();
    }

    /**
     * Sets up an HTTP {@link AlertMessageSubscription} and a HTTP endpoint and
     * verifies that matching {@link Alert}s are properly forwarded to the
     * subscriber (HTTP) endpoint.
     *
     * @throws IOException
     */
    @Test
    public void testAlertMessageForwardingToHttpSubscriber() throws Exception {
        int httpPort = HostUtils.findFreePorts(1).get(0);
        String contextPath = "/subscriber";
        String webhookUrl = "http://localhost:" + httpPort + contextPath;
        HttpAlerterConfig webhookConfig = AlerterTestUtils.httpAlerter(Arrays.asList(webhookUrl), "WARN|ERROR");
        StandardAlerterConfig config = new StandardAlerterConfig(null, Arrays.asList(webhookConfig));
        this.alerter.validate(config);
        this.alerter.configure(config);
        this.alerter.start();

        RequestLoggingHttpServer server = new RequestLoggingHttpServer(contextPath, httpPort);
        try {
            server.start();
            // post matching message, verify it is forwarded to subscriber
            Alert message1 = new Alert("topic", AlertSeverity.ERROR, UtcTime.now(), "message1", null);
            this.eventBus.post(message1);
            assertThat(server.getPostedMessages().size(), is(1));
            assertTrue(messageReceived(server, message1));

            // post non-matching message, verify it is suppressed
            Alert message2 = new Alert("topic", AlertSeverity.INFO, new DateTime(), "message2", null);
            this.eventBus.post(message2);
            assertThat(server.getPostedMessages().size(), is(1));
            assertFalse(messageReceived(server, message2));

            // stop alerter, post matching message, verify it is suppressed
            this.alerter.stop();
            Alert message3 = new Alert("topic", AlertSeverity.ERROR, new DateTime(), "message3", null);
            this.eventBus.post(message3);
            assertThat(server.getPostedMessages().size(), is(1));
            assertFalse(messageReceived(server, message3));

            // restart alerter, post matching message, verify it is forwarded
            this.alerter.start();
            Alert message4 = new Alert("topic", AlertSeverity.ERROR, new DateTime(), "message4", null);
            this.eventBus.post(message4);
            assertThat(server.getPostedMessages().size(), is(2));
            assertTrue(messageReceived(server, message4));
        } finally {
            server.stop();
        }
    }

    /**
     * Sets up an HTTP {@link AlertMessageSubscription} and a HTTP endpoint and
     * verifies that duplicate {@link Alert}s are suppressed for the specified
     * duplicate suppression period.
     *
     * @throws IOException
     */
    @Test
    public void testDuplicateSuppression() throws Exception {
        int httpPort = HostUtils.findFreePorts(1).get(0);
        String contextPath = "/subscriber";
        String webhookUrl = "http://localhost:" + httpPort + contextPath;
        HttpAlerterConfig webhookConfig = AlerterTestUtils.httpAlerter(Arrays.asList(webhookUrl), "WARN|ERROR");

        // 3 minute duplicate suppression
        TimeInterval duplicateSuppression = new TimeInterval(3L, TimeUnit.MINUTES);
        StandardAlerterConfig config = new StandardAlerterConfig(null, Arrays.asList(webhookConfig),
                duplicateSuppression);
        this.alerter.validate(config);
        this.alerter.configure(config);
        this.alerter.start();

        Alert alert = new Alert("topic", AlertSeverity.ERROR, UtcTime.now(), "message1", null);

        RequestLoggingHttpServer server = new RequestLoggingHttpServer(contextPath, httpPort);
        try {
            server.start();
            // post alert, verify it is forwarded to subscriber
            this.eventBus.post(alert);
            assertThat(server.getPostedMessages().size(), is(1));

            // post duplicate alert, verify it is suppressed
            FrozenTime.tick(60);
            this.eventBus.post(alert);
            assertThat(server.getPostedMessages().size(), is(1));

            // post duplicate alert, should still be suppressed
            FrozenTime.tick(60);
            this.eventBus.post(alert);
            assertThat(server.getPostedMessages().size(), is(1));

            // suppression period has passed, alert should be delivered
            FrozenTime.tick(61);
            this.eventBus.post(alert);
            assertThat(server.getPostedMessages().size(), is(2));
        } finally {
            server.stop();
        }
    }

    /**
     * Returns <code>true</code> if a given {@link Alert} has been received by a
     * HTTP server.
     * <p/>
     * The check is carried out by trying to find an occurence of the
     * {@link Alert} message text in the list of messages received thus far by
     * the server.
     *
     * @param server
     *            The HTTP server.
     * @param message
     *            The {@link Alert} to look for.
     * @return
     */
    private boolean messageReceived(RequestLoggingHttpServer server, Alert message) {
        return server.getPostedMessages().stream().anyMatch(it -> it.contains(message.getMessage()));
    }

    private void configureAlerter(String filename) throws IOException {
        StandardAlerterConfig config = loadConfig(filename);

        this.alerter.validate(config);
        this.alerter.configure(config);
    }

    private static StandardAlerterConfig loadConfig(String resourceFile) throws IOException {
        JsonObject json = JsonUtils.parseJsonResource(resourceFile).getAsJsonObject();
        JsonElement alerterObject = json.get("alerter");
        StandardAlerterConfig config = JsonUtils.toObject(alerterObject, StandardAlerterConfig.class);
        return config;
    }
}
