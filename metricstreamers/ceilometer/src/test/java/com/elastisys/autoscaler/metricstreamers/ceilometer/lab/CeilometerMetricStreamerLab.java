package com.elastisys.autoscaler.metricstreamers.ceilometer.lab;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.metricstreamers.ceilometer.CeilometerMetricStreamer;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerMetricStreamerConfig;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.Downsampling;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;
import com.elastisys.scale.commons.eventbus.impl.AsynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.openstack.AuthConfig;
import com.elastisys.scale.commons.openstack.AuthV2Credentials;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Lab program that sets up an {@link CeilometerMetricStreamer} and prints any
 * new values that appears on the stream.
 */
public class CeilometerMetricStreamerLab {

    static final Logger LOG = LoggerFactory.getLogger(CeilometerMetricStreamerLab.class);

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private static final EventBus eventBus = new AsynchronousEventBus(executor, LOG);

    private static final AuthConfig openstackAuth = new AuthConfig(System.getenv("OS_AUTH_URL"), new AuthV2Credentials(
            System.getenv("OS_TENANT_NAME"), System.getenv("OS_USERNAME"), System.getenv("OS_PASSWORD")), null);
    private static String region = "RegionOne";
    private static final TimeInterval pollInterval = TimeInterval.seconds(10);

    private static final String streamId = "requestrate.stream";
    private static final String meter = "network.services.lb.total.connections.rate";
    private static final String resourceId = null;
    private static final Downsampling downsampling = null;// new
                                                          // Downsampling(CeilometerFunction.Average,
                                                          // TimeInterval.seconds(300));
    private static final boolean rateConversion = false;
    private static final TimeInterval dataSettlingTime = null;
    private static final TimeInterval queryChunkSize = new TimeInterval(30L, TimeUnit.DAYS);

    public static void main(String[] args) throws Exception {
        OSFactory.enableHttpLoggingFilter(true);

        MetricStreamer<CeilometerMetricStreamerConfig> metricStreamer = new CeilometerMetricStreamer(LOG, executor,
                eventBus);

        // create metric streamer

        List<CeilometerMetricStreamDefinition> streamDefinitions = Arrays.asList(new CeilometerMetricStreamDefinition(
                streamId, meter, resourceId, downsampling, rateConversion, dataSettlingTime, queryChunkSize));
        metricStreamer
                .configure(new CeilometerMetricStreamerConfig(openstackAuth, region, pollInterval, streamDefinitions));
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
            LOG.info("{}: new metric stream values received at time {}", message.getId(), UtcTime.now());
            for (MetricValue value : message.getMetricValues()) {
                LOG.info("  {}", value);
            }

        }
    }
}
