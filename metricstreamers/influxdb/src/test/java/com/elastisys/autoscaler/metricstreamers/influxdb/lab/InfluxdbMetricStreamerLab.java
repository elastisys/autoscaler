package com.elastisys.autoscaler.metricstreamers.influxdb.lab;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.metricstreamers.influxdb.InfluxdbMetricStreamer;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.InfluxdbMetricStreamerConfig;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.MetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.Query;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;
import com.elastisys.scale.commons.eventbus.impl.AsynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Lab program that sets up an {@link InfluxdbMetricStreamer} and prints any new
 * values that appears on the stream.
 */
public class InfluxdbMetricStreamerLab {

    static final Logger LOG = LoggerFactory.getLogger(InfluxdbMetricStreamerLab.class);

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private static final EventBus eventBus = new AsynchronousEventBus(executor, LOG);

    /** InfluxDB host. */
    private static String host = "localhost";
    /** InfluxDB API port. */
    private static int port = 8086;
    private static final TimeInterval pollInterval = new TimeInterval(10L, TimeUnit.SECONDS);

    private static final Query query = Query.builder().select("value").from("request_count").where(null).groupBy(null)
            .build();
    private static final TimeInterval dataSettlingTime = null;
    private static final TimeInterval queryChunkSize = new TimeInterval(30L, TimeUnit.DAYS);

    public static void main(String[] args) throws Exception {
        MetricStreamer<InfluxdbMetricStreamerConfig> metricStreamer = new InfluxdbMetricStreamer(LOG, executor,
                eventBus);

        // create metric streamer

        List<MetricStreamDefinition> streamDefinitions = asList(new MetricStreamDefinition("request_rate.stream", null,
                "mydb", query, dataSettlingTime, queryChunkSize));
        metricStreamer.configure(new InfluxdbMetricStreamerConfig(host, port, null, pollInterval, streamDefinitions));
        metricStreamer.start();

        // subscribe to stream
        eventBus.register(new MetricStreamListener());

        System.err.println("Waiting for metric stream values. Ctrl-D to exit.");
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
        }
        scanner.close();

        executor.shutdownNow();
    }

    private static class MetricStreamListener {

        @Subscriber
        public void onEvent(MetricStreamMessage message) {
            LOG.info("{}: new metric stream values received: {}", message.getId(), message.getMetricValues());
        }
    }
}
