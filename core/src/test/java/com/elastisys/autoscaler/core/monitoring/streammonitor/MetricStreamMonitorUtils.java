package com.elastisys.autoscaler.core.monitoring.streammonitor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;

public class MetricStreamMonitorUtils {

    /**
     * Sets up a {@link MonitoringSubsystem} with a fake {@link MetricStreamer}
     * publishing the given {@link MetricStream}s.
     *
     * @param metricStreams
     *            The {@link MetricStream}s that are to be publiehed by the fake
     *            {@link MetricStreamer}.
     * @return
     */
    public static MonitoringSubsystem setupFakeMetricStreams(List<MetricStream> metricStreams) {
        // set up dummy monitoring system with metric streams
        MetricStreamer mockMetricStreamer = mock(MetricStreamer.class);
        MonitoringSubsystem monitoringSubsystem = mock(MonitoringSubsystem.class);
        when(monitoringSubsystem.getMetricStreamers()).thenReturn(Arrays.asList(mockMetricStreamer));
        // the mock metric streamer publishes our fake metric streams
        when(mockMetricStreamer.getMetricStreams()).thenReturn(metricStreams);
        for (MetricStream metricStream : metricStreams) {
            when(mockMetricStreamer.getMetricStream(metricStream.getId())).thenReturn(metricStream);
        }
        return monitoringSubsystem;
    }
}
