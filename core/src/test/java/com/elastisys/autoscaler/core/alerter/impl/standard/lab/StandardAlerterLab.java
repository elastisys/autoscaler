package com.elastisys.autoscaler.core.alerter.impl.standard.lab;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.alerter.api.types.AlertTopics;
import com.elastisys.autoscaler.core.alerter.impl.standard.StandardAlerter;
import com.elastisys.autoscaler.core.alerter.impl.standard.config.StandardAlerterConfig;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.AsynchronousEventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.mail.iap.Protocol;

/**
 * A lab program that runs a {@link StandardAlerter} and submits an
 * {@link AlertMessage} which will get sent by the configured {@link Protocol}
 * handlers.
 * <p/>
 * If the {@link HttpSubscriptionHandler} is exercised, the
 * {@link HttpEchoServerMain} class may be used to set up a http endpoint to
 * receive messages.
 *
 * @see StandardAlerter
 * @see HttpEchoServerMain
 */
public class StandardAlerterLab {
    private static final Logger logger = LoggerFactory.getLogger(StandardAlerterLab.class);
    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private static final EventBus eventBus = new AsynchronousEventBus(executorService, logger);

    public static void main(String[] args) throws Exception {
        // String alerterConfig = "alerter/alerter_http.json";
        String alerterConfig = "alerter/alerter_smtp.json";
        StandardAlerter alerter = createAndConfigureAlerter(alerterConfig);
        logger.debug("Created {}", alerter);
        alerter.start();

        DateTime now = UtcTime.now();
        eventBus.post(new Alert(AlertTopics.PREDICTION_FAILURE.getTopicPath(), AlertSeverity.ERROR, now,
                "1: should be forwarded to subscription", null));
        eventBus.post(new Alert(AlertTopics.PREDICTION_FAILURE.getTopicPath(), AlertSeverity.INFO, now,
                "2: should NOT be forwarded to subscription", null));
        eventBus.post(new Alert(AlertTopics.PREDICTION_FAILURE.getTopicPath(), AlertSeverity.WARN, now,
                "3: should also be forwarded to subscription", null));

        logger.info("Shutting down ...");
        executorService.shutdown();
        executorService.awaitTermination(60, TimeUnit.SECONDS);
        alerter.stop();
    }

    /**
     * Creates and configures a {@link StandardAlerter} according to the
     * specified JSON configuration file.
     *
     * @param resourceFile
     *            A JSON-formatted {@link StandardAlerter} configuration file.
     * @return
     * @throws Exception
     */
    private static StandardAlerter createAndConfigureAlerter(String resourceFile) throws Exception {
        JsonObject json = JsonUtils.parseJsonResource(resourceFile).getAsJsonObject();
        JsonElement alerterObject = json.get("alerter");

        StandardAlerterConfig config = JsonUtils.toObject(alerterObject, StandardAlerterConfig.class);
        System.out.println(config);

        StandardAlerter alerter = new StandardAlerter(UUID.randomUUID(), "autoScaler1", logger, eventBus);
        alerter.validate(config);
        alerter.configure(config);

        return alerter;
    }
}
