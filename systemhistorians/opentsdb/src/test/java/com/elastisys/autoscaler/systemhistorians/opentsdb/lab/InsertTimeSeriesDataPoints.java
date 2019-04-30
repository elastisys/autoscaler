package com.elastisys.autoscaler.systemhistorians.opentsdb.lab;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetric;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.autoscaler.systemhistorians.opentsdb.OpenTsdbSystemHistorian;
import com.elastisys.autoscaler.systemhistorians.opentsdb.config.OpenTsdbSystemHistorianConfig;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * A lab-style program that reports data points using the
 * {@link OpenTsdbSystemHistorian} by wiring up just enough of the system to
 * make this work. Data point values are read from standard input, and the
 * timestamp will be the current time. Data points are reported against the
 * {@link SystemMetric#PREDICTION} metric.
 * <p>
 * The program takes no command-line parameters, reads standard input to get
 * metric values, and exits when the user closes standard input. Since OpenTSDB
 * only allows one value per metric and timestamp, and timestamps are at second
 * granularity, the user is advised to not be too quick at typing values.
 */
public class InsertTimeSeriesDataPoints {
    private static final Logger LOG = LoggerFactory.getLogger(InsertTimeSeriesDataPoints.class);

    public static OpenTsdbSystemHistorian getHistorian(String filename, Injector injector) throws IOException {
        JsonObject json = JsonUtils.parseJsonResource(filename).getAsJsonObject();

        OpenTsdbSystemHistorianConfig config = new Gson().fromJson(json.getAsJsonObject("systemHistorian"),
                OpenTsdbSystemHistorianConfig.class);

        OpenTsdbSystemHistorian historian = injector.getInstance(OpenTsdbSystemHistorian.class);

        final EventBus eventBus = injector.getInstance(EventBus.class);
        eventBus.register(historian);

        historian.validate(config);

        historian.configure(config);

        return historian;
    }

    public static void main(String[] args) throws IOException {
        Injector injector = Guice.createInjector(new GuiceModule());

        OpenTsdbSystemHistorian historian = getHistorian("systemhistorian.json", injector);
        historian.start();

        final EventBus eventBus = injector.getInstance(EventBus.class);
        final Logger logger = injector.getInstance(Logger.class);

        final Scanner scanner = new Scanner(System.in);
        final Map<String, String> tags = new HashMap<>();
        tags.put("from", InsertTimeSeriesDataPoints.class.getSimpleName());
        tags.put("predictor", "manual");
        tags.put("runTimestamp", "" + UtcTime.now().getMillis());

        while (scanner.hasNextDouble()) {
            final double predictionValue = scanner.nextDouble();

            logger.debug("Posting " + predictionValue + " to event bus");

            eventBus.post(new SystemMetricEvent(
                    new MetricValue(SystemMetric.PREDICTION.getMetricName(), predictionValue, UtcTime.now(), tags)));
        }

        historian.stop();
        scanner.close();
    }

    private static class GuiceModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(Logger.class).toInstance(LoggerFactory.getLogger(OpenTsdbSystemHistorian.class));
            bind(EventBus.class).toInstance(new SynchronousEventBus(LOG));
        }
    }
}
