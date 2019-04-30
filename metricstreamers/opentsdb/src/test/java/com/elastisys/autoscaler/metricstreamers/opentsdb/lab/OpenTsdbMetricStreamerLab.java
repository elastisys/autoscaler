package com.elastisys.autoscaler.metricstreamers.opentsdb.lab;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.DownsampleFunction;
import com.elastisys.autoscaler.metricstreamers.opentsdb.OpenTsdbMetricStreamer;
import com.elastisys.autoscaler.metricstreamers.opentsdb.config.OpenTsdbMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.opentsdb.config.OpenTsdbMetricStreamerConfig;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.DownsamplingSpecification;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.MetricAggregator;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;
import com.elastisys.scale.commons.eventbus.impl.AsynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;

/**
 * Lab program that sets up an {@link OpenTsdbMetricStreamer} and prints any new
 * values that appears on the stream.
 */
public class OpenTsdbMetricStreamerLab {

    static final Logger LOG = LoggerFactory.getLogger(OpenTsdbMetricStreamerLab.class);

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private static final EventBus eventBus = new AsynchronousEventBus(executor, LOG);

    private static final String openTsdbHost = "localhost";
    private static final int openTsdbPort = 4242;
    private static final TimeInterval pollInterval = new TimeInterval(10L, TimeUnit.SECONDS);

    private static final boolean convertToRate = false;
    private static final DownsamplingSpecification downsampling = new DownsamplingSpecification(
            new TimeInterval(5L, TimeUnit.MINUTES), DownsampleFunction.MEAN);

    private static final Map<String, List<String>> tags = Maps.of();
    private static final TimeInterval dataSettlingTime = new TimeInterval(30L, TimeUnit.SECONDS);
    private static final TimeInterval queryChunkSize = new TimeInterval(30L, TimeUnit.DAYS);

    public static void main(String[] args) throws Exception {
        MetricStreamer<OpenTsdbMetricStreamerConfig> metricStreamer = new OpenTsdbMetricStreamer(LOG, executor,
                eventBus);

        // create metric streamer

        List<OpenTsdbMetricStreamDefinition> streamDefinitions = Arrays
                .asList(new OpenTsdbMetricStreamDefinition("request_count.stream", "request_count",
                        MetricAggregator.SUM, convertToRate, downsampling, tags, dataSettlingTime, queryChunkSize));
        metricStreamer.configure(
                new OpenTsdbMetricStreamerConfig(openTsdbHost, openTsdbPort, pollInterval, streamDefinitions));
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
