package com.elastisys.autoscaler.metricstreamers.streamjoiner.lab;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.metricstreamers.streamjoiner.MetricStreamJoiner;
import com.elastisys.autoscaler.metricstreamers.streamjoiner.config.MetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.streamjoiner.config.MetricStreamJoinerConfig;
import com.elastisys.autoscaler.metricstreamers.streamjoiner.stream.JoiningMetricStream;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;
import com.elastisys.scale.commons.eventbus.impl.AsynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Lab program that sets up a {@link MetricStreamJoiner} and prints any new
 * values that appears on the stream. The user can enter fake metrics on stdin
 * from the input {@link MetricStream}s of the {@link JoiningMetricStream} (one
 * of {@link #CPU_USER_METRIC_STREAM} and {@link #CPU_SYSTEM_METRIC_STREAM}).
 */
public class MetricStreamJoinerLab {

    static final Logger LOG = LoggerFactory.getLogger(MetricStreamJoinerLab.class);

    private static final String CPU_USER_METRIC_STREAM = "cpu.user.stream";
    private static final String CPU_SYSTEM_METRIC_STREAM = "cpu.system.stream";

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private static final EventBus eventBus = new AsynchronousEventBus(executor, LOG);

    public static void main(String[] args) throws Exception {
        MetricStreamer mockMetricStreamer = setUpMockMetricStreamer(CPU_USER_METRIC_STREAM, CPU_SYSTEM_METRIC_STREAM);

        MetricStreamer<MetricStreamJoinerConfig> metricStreamer = new MetricStreamJoiner(LOG, eventBus,
                asList(mockMetricStreamer));

        // create metric streamer

        Map<String, String> inputStreams = Maps.of(//
                "cpuUser", CPU_USER_METRIC_STREAM, //
                "cpuSystem", CPU_SYSTEM_METRIC_STREAM);
        TimeInterval maxTimeDiff = TimeInterval.seconds(10);
        List<String> joinScript = asList("cpuUser + cpuSystem");
        MetricStreamDefinition streamDef = new MetricStreamDefinition("cpu.total", "cpu", inputStreams, maxTimeDiff,
                joinScript);
        MetricStreamJoinerConfig config = new MetricStreamJoinerConfig(asList(streamDef));

        metricStreamer.configure(config);
        metricStreamer.start();

        // subscribe to stream
        eventBus.register(new MetricStreamListener());

        System.err.println("Enter metric stream values: <metricStream> <metric> <value>. Ctrl-D to exit.");
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            try {
                eventBus.post(parseMetric(scanner));
            } catch (Exception e) {
                System.err.println("failed to parse metric value");
                System.err.println("\nplease enter a value of form: <metricStream> <metric> <value>");
            }
        }
        scanner.close();

        executor.shutdownNow();
    }

    private static MetricStreamMessage parseMetric(Scanner scanner) {
        String[] tokens = scanner.nextLine().split("\\s+");

        String metricStream = tokens[0];
        String metric = tokens[1];
        double value = Double.valueOf(tokens[2]);
        MetricValue metricValue = new MetricValue(metric, value, UtcTime.now());

        return new MetricStreamMessage(metricStream, asList(metricValue));
    }

    /**
     * Creates a mocked {@link MetricStreamer} with the specified stream
     * identifiers.
     *
     * @param streamIds
     * @return
     */
    private static MetricStreamer setUpMockMetricStreamer(String... streamIds) {
        MetricStreamer mockMetricStreamer = mock(MetricStreamer.class);
        List<MetricStream> mockedStreams = new ArrayList<>();
        for (String streamId : streamIds) {
            MetricStream mockedMetricStream = mock(MetricStream.class);
            when(mockedMetricStream.getId()).thenReturn(streamId);
            mockedStreams.add(mockedMetricStream);
        }
        when(mockMetricStreamer.getMetricStreams()).thenReturn(mockedStreams);
        return mockMetricStreamer;
    }

    private static class MetricStreamListener {

        @Subscriber
        public void onEvent(MetricStreamMessage message) {
            LOG.info("{}: new metric stream values received: {}", message.getId(), message.getMetricValues());
        }
    }
}
