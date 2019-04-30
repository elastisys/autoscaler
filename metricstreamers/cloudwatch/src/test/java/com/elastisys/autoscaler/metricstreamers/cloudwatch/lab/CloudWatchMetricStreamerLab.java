package com.elastisys.autoscaler.metricstreamers.cloudwatch.lab;

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
import com.elastisys.autoscaler.metricstreamers.cloudwatch.CloudWatchMetricStreamer;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchMetricStreamerConfig;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchStatistic;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;
import com.elastisys.scale.commons.eventbus.impl.AsynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;

/**
 * Lab program that sets up a {@link CloudWatchMetricStreamer} and prints any
 * new values that appears on the stream.
 */
public class CloudWatchMetricStreamerLab {

    static final Logger LOG = LoggerFactory.getLogger(CloudWatchMetricStreamerLab.class);

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private static final EventBus eventBus = new AsynchronousEventBus(executor, LOG);

    // TODO: set to region where your CloudWatch endpoint is hosted
    private static final String region = System.getenv("AWS_DEFAULT_REGION");
    private static final String awsAccessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String awsSecretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");
    private static final TimeInterval pollInterval = new TimeInterval(10L, TimeUnit.SECONDS);

    // TODO: define stream
    private static final String id = "cpu.avg.stream";
    private static final String namespace = "AWS/EC2";
    private static final String metric = "CPUUtilization";
    private static final CloudWatchStatistic statistic = CloudWatchStatistic.Average;
    private static final TimeInterval period = TimeInterval.seconds(60);
    private static final boolean convertToRate = false;
    private static final Map<String, String> dimensions = Maps.of();
    private static final TimeInterval dataSettlingTime = TimeInterval.seconds(60);

    public static void main(String[] args) throws Exception {
        MetricStreamer<CloudWatchMetricStreamerConfig> metricStreamer = new CloudWatchMetricStreamer(LOG, executor,
                eventBus);

        List<CloudWatchMetricStreamDefinition> metricStreams = Arrays.asList(new CloudWatchMetricStreamDefinition(id,
                namespace, metric, statistic, period, convertToRate, dimensions, dataSettlingTime, null));
        metricStreamer.configure(new CloudWatchMetricStreamerConfig(awsAccessKeyId, awsSecretAccessKey, region,
                pollInterval, metricStreams));
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
