package com.elastisys.autoscaler.core.monitoring.impl.standard.config;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamMonitorConfig;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamerConfig;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.SystemHistorianConfig;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.stubs.MetricStreamerStubConfig;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.stubs.MetricStreamerStubStreamDefinition;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.stubs.SystemHistorianStubConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.google.gson.JsonObject;

public class MonitoringSystemTestUtils {

    /**
     * Creates a sample {@link MetricStreamerConfig}.
     *
     * @return
     */
    public static MetricStreamerConfig metricStreamerConfig() {
        return new MetricStreamerConfig("some.MetricStreamerImpl", metricStreamerConfigDocument());
    }

    public static MetricStreamerConfig metricStreamerConfig2() {
        return new MetricStreamerConfig("some.MetricStreamerImpl2", metricStreamerConfigDocument2());
    }

    /**
     * Creates an illegal {@link MetricStreamerConfig} without implementation
     * class.
     *
     * @return
     */
    public static MetricStreamerConfig illegalMetricStreamerConfig() {
        return new MetricStreamerConfig(null, metricStreamerConfigDocument());
    }

    /**
     * Creates a sample {@link SystemHistorianConfig}.
     *
     * @return
     */
    public static SystemHistorianConfig systemHistorianConfig() {
        return new SystemHistorianConfig("some.SystemHistorianImpl", systemHistorianConfigDocument());
    }

    /**
     * Creates an illegal {@link SystemHistorianConfig} without implementation
     * class.
     *
     * @return
     */
    public static SystemHistorianConfig illegalSystemHistorianConfig() {
        return new SystemHistorianConfig(null, systemHistorianConfigDocument());
    }

    /**
     * Creates a sample {@link MetricStreamMonitorConfig}.
     *
     * @return
     */
    public static MetricStreamMonitorConfig metricStreamMonitorConfig() {
        TimeInterval checkInterval = new TimeInterval(300L, TimeUnit.SECONDS);
        TimeInterval maxTolerableInactivity = new TimeInterval(1800L, TimeUnit.SECONDS);
        return new MetricStreamMonitorConfig(checkInterval, maxTolerableInactivity);
    }

    /**
     * Creates an illegal {@link MetricStreamMonitorConfig} (bad
     * maxTolerableInactivity).
     *
     * @return
     */
    public static MetricStreamMonitorConfig illegalMetricStreamMonitorConfig() {
        TimeInterval checkInterval = new TimeInterval(300L, TimeUnit.SECONDS);
        TimeInterval illegalMaxTolerableInactivity = JsonUtils.toObject(JsonUtils.parseJsonString("{\"time\": 1800}"),
                TimeInterval.class);
        return new MetricStreamMonitorConfig(checkInterval, illegalMaxTolerableInactivity);
    }

    /**
     * Returns a sample {@link SystemHistorianStubConfig} as JSON.
     *
     * @return
     */
    public static JsonObject systemHistorianConfigDocument() {
        SystemHistorianStubConfig systemHistorianStubConfig = new SystemHistorianStubConfig("some.host", 12345);
        return JsonUtils.toJson(systemHistorianStubConfig).getAsJsonObject();
    }

    /**
     * Returns a sample {@link MetricStreamerStubConfig} as JSON.
     *
     * @return
     */
    public static JsonObject metricStreamerConfigDocument() {
        MetricStreamerStubConfig metricStreamerStubConfig = new MetricStreamerStubConfig("some.host", 12345, 60,
                Arrays.asList(new MetricStreamerStubStreamDefinition("cpu.percent.stream", "cpu.percent", "AVG")));
        return JsonUtils.toJson(metricStreamerStubConfig).getAsJsonObject();
    }

    public static JsonObject metricStreamerConfigDocument2() {
        MetricStreamerStubConfig metricStreamerStubConfig = new MetricStreamerStubConfig("other.host", 12345, 60,
                Arrays.asList(new MetricStreamerStubStreamDefinition("mem.usage.stream", "mem.usage", "SUM")));
        return JsonUtils.toJson(metricStreamerStubConfig).getAsJsonObject();
    }
}
