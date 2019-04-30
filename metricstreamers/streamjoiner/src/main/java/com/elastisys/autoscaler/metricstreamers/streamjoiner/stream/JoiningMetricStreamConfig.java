package com.elastisys.autoscaler.metricstreamers.streamjoiner.stream;

import java.util.Map;
import java.util.Objects;

import javax.script.CompiledScript;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * A configuration used by a {@link JoiningMetricStream}.
 *
 * @see JoiningMetricStream
 */
public class JoiningMetricStreamConfig {

    /**
     * The id of the metric stream. This is the id that will be used by clients
     * wishing to subscribe to this metric stream.
     */
    private final String id;

    /**
     * The name of the metric produced by this metric stream. This is the metric
     * that will be set for produced {@link MetricValue}s.
     */
    private final String metric;

    /**
     * The maximum difference in time between observed metric stream values for
     * the joined metric stream to apply its {@link #joinScript} and produce a
     * new value. If stream metrics are farther apart than this, no new metric
     * value is produced on the joined stream.
     */
    private final TimeInterval maxTimeDiff;

    /**
     * The input {@link MetricStream}s that are to be joined. Keys are <i>metric
     * stream aliases</i> and values are {@link MetricStream}s.
     */
    private final Map<String, MetricStream> inputStreams;

    /**
     * The JavaScript that will join values read from the {@link #inputStreams}.
     */
    private final CompiledScript joinScript;

    /**
     * Creates a {@link JoiningMetricStreamConfig}.
     *
     * @param id
     *            The id of the metric stream. This is the id that will be used
     *            by clients wishing to subscribe to this metric stream.
     * @param metric
     *            The name of the metric produced by this metric stream. This is
     *            the metric that will be set for produced {@link MetricValue}s.
     * @param maxTimeDiff
     *            The maximum difference in time between observed metric stream
     *            values for the joined metric stream to apply its
     *            {@link #joinScript} and produce a new value. If stream metrics
     *            are farther apart than this, no new metric value is produced
     *            on the joined stream.
     * @param inputStreams
     *            The input {@link MetricStream}s that are to be joined. Keys
     *            are <i>metric stream aliases</i> and values are
     *            {@link MetricStream}s.
     * @param joinScript
     */
    public JoiningMetricStreamConfig(String id, String metric, TimeInterval maxTimeDiff,
            Map<String, MetricStream> inputStreams, CompiledScript joinScript) {
        this.id = Objects.requireNonNull(id, "id: must not be null");
        this.metric = Objects.requireNonNull(metric, "metric: must not be null");
        this.maxTimeDiff = Objects.requireNonNull(maxTimeDiff, "maxTimeDiff: must not be null");
        this.inputStreams = Objects.requireNonNull(inputStreams, "inputStreams: must not be null");
        this.joinScript = Objects.requireNonNull(joinScript, "joinScript: must not be null");
    }

    /**
     * The id of the metric stream. This is the id that will be used by clients
     * wishing to subscribe to this metric stream.
     *
     * @return
     */
    public String getId() {
        return this.id;
    }

    /**
     * The name of the metric produced by this metric stream. This is the metric
     * that will be set for produced {@link MetricValue}s.
     *
     * @return
     */
    public String getMetric() {
        return this.metric;
    }

    /**
     * The maximum difference in time between observed metric stream values for
     * the joined metric stream to apply its {@link #joinScript} and produce a
     * new value. If stream metrics are farther apart than this, no new metric
     * value is produced on the joined stream.
     *
     * @return
     */
    public TimeInterval getMaxTimeDiff() {
        return this.maxTimeDiff;
    }

    /**
     * The input {@link MetricStream}s that are to be joined. Keys are <i>metric
     * stream aliases</i> and values are {@link MetricStream}s.
     *
     * @return
     */
    public Map<String, MetricStream> getInputStreams() {
        return this.inputStreams;
    }

    /**
     * The JavaScript that will join values read from the {@link #inputStreams}.
     *
     * @return
     */
    public CompiledScript getJoinScript() {
        return this.joinScript;
    }

}
