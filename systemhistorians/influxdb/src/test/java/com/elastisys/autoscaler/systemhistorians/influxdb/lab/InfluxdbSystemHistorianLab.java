package com.elastisys.autoscaler.systemhistorians.influxdb.lab;

import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetric;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.autoscaler.systemhistorians.influxdb.InfluxdbSystemHistorian;
import com.elastisys.autoscaler.systemhistorians.influxdb.config.InfluxdbSecurityConfig;
import com.elastisys.autoscaler.systemhistorians.influxdb.config.InfluxdbSystemHistorianConfig;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.AsynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.util.time.UtcTime;

public class InfluxdbSystemHistorianLab {
    private static final Logger LOG = LoggerFactory.getLogger(InfluxdbSystemHistorian.class);

    private static UUID autoScalerUuid = UUID.randomUUID();
    private static String autoScalerId = "autoscaler1";
    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
    private static EventBus eventBus = new AsynchronousEventBus(executor, LOG);

    /** InfluxDB server host. */
    private static String host = "localhost";
    /** InfluxDB server API port. */
    private static int port = 8086;
    /** InfluxDB database. */
    private static String database = "mydb";

    public static void main(String[] args) {
        InfluxdbSystemHistorianConfig config = config();

        InfluxdbSystemHistorian historian = new InfluxdbSystemHistorian(autoScalerUuid, autoScalerId, LOG, executor,
                eventBus);
        historian.validate(config);
        historian.configure(config);
        historian.start();

        System.err.println("Write values on stdin. Ctrl-D to quit.");
        final Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextDouble()) {
            final double value = scanner.nextDouble();
            LOG.debug("Posting " + value + " to event bus");
            eventBus.post(new SystemMetricEvent(
                    new MetricValue(SystemMetric.CLOUDPOOL_SIZE.getMetricName(), value, UtcTime.now())));
        }

        historian.stop();
        scanner.close();
        executor.shutdownNow();
    }

    private static InfluxdbSystemHistorianConfig config() {
        boolean https = false;
        BasicCredentials auth = null;
        boolean verifyCert = false;
        boolean verifyHost = false;
        InfluxdbSecurityConfig security = new InfluxdbSecurityConfig(https, auth, verifyCert, verifyHost);

        TimeInterval pushInterval = new TimeInterval(10L, TimeUnit.SECONDS);
        int maxBatchSize = 3;
        return new InfluxdbSystemHistorianConfig(host, port, database, security, pushInterval, maxBatchSize);
    }
}
