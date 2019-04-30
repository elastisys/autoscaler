package com.elastisys.autoscaler.metricstreamers.streamjoiner;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamException;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.metricstreamers.streamjoiner.config.MetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.streamjoiner.config.MetricStreamJoinerConfig;
import com.elastisys.autoscaler.metricstreamers.streamjoiner.stream.JoiningMetricStream;
import com.elastisys.autoscaler.metricstreamers.streamjoiner.stream.JoiningMetricStreamConfig;
import com.elastisys.scale.commons.eventbus.EventBus;

/**
 * A {@link MetricStreamJoiner} is a "meta metric streamer", which consumes one
 * or more metric streams and produces a single new metric stream by applying a
 * join script to the consumed stream values.
 *
 * The `MetricStreamJoiner`'s operation can be illustrated by the following
 * schematical image:
 * <p/>
 *
 * <pre>
 * <code>
 * MetricStream a -> |
 *                   |
 * MetricStream b -> | MetricStreamJoiner d: joinScript(${a}, ${b}, ${c})
 *                   |
 * MetricStream c -> |
 * </code>
 * </pre>
 *
 * {@link MetricStreamJoiner} listens for values from existing
 * {@link MetricStream}s `a`, `b`, `c` and whenever a new value is received from
 * either stream (and at least one value has been observed on every stream and
 * their timestamps are within `maxTimeDifference` distance of each other) the
 * joiner stream `d` produces a new metric value being its "join function"
 * applied to the most recently observed value from every stream.
 * <p/>
 * The `joinScript` can be an arbitrary JavaScript expression like
 *
 * <pre>
 * <code>
 *   a + b + c
 * </code>
 * </pre>
 *
 * or
 *
 * <pre>
 * <code>
 *   if (a + b > c) {
 *     a + b;
 *   } else {
 *     c;
 *   }
 * </code>
 * </pre>
 *
 * <i>The only requirement that the `joinScript` must satisfy is that the end
 * result of executing the last script statement/expression must be a single
 * numerical value</i>.
 */
public class MetricStreamJoiner implements MetricStreamer<MetricStreamJoinerConfig> {
    /** {@link Logger} instance. */
    private final Logger logger;
    /** {@link EventBus} on which to listen for metrics. */
    private final EventBus eventBus;
    /**
     * The list of {@link MetricStreamer}s declared prior to this
     * {@link MetricStreamer} and which can be used as references in input
     * streams.
     */
    private final List<MetricStreamer<?>> priorDeclaredMetricStreamers;
    private boolean started;

    /** The configuration set for the {@link MetricStreamJoiner}. */
    private MetricStreamJoinerConfig config;
    /** The current list of published {@link MetricStream}s. */
    private final List<JoiningMetricStream> metricStreams;

    /**
     * Creates a {@link MetricStreamJoiner}.
     *
     * @param logger
     *            {@link Logger} instance.
     * @param eventBus
     *            {@link EventBus} on which to listen for metrics.
     * @param priorDeclaredMetricStreamers
     *            The list of {@link MetricStreamer}s declared prior to this
     *            {@link MetricStreamer} and which can be used as references in
     *            input streams. May be <code>null</code>, which is interpreted
     *            as an empty list.
     */
    @Inject
    public MetricStreamJoiner(Logger logger, EventBus eventBus, List<MetricStreamer<?>> priorDeclaredMetricStreamers) {
        this.logger = logger;
        this.eventBus = eventBus;
        this.priorDeclaredMetricStreamers = Optional.ofNullable(priorDeclaredMetricStreamers)
                .orElse(Collections.emptyList());

        this.started = false;
        this.config = null;
        this.metricStreams = new CopyOnWriteArrayList<>();
    }

    @Override
    public void validate(MetricStreamJoinerConfig configuration) throws IllegalArgumentException {
        checkArgument(configuration != null, "MetricStreamJoiner: missing configuration");
        configuration.validate();
    }

    @Override
    public void configure(MetricStreamJoinerConfig newConfig) throws IllegalArgumentException {
        checkArgument(newConfig != null, "MetricStreamJoiner: missing configuration");

        if (newConfig.equals(this.config)) {
            this.logger.debug("no configuration changes. ignoring new config ...");
            return;
        }

        newConfig.validate();

        // a metric stream's inputStreams must either refer to a metric stream
        // defined by a different MetricStreamer or a joining metric stream
        // declared prior to it.
        validateInputStreamReferences(newConfig);

        boolean needsRestart = isStarted();
        if (needsRestart) {
            stop();
        }

        List<JoiningMetricStream> newMetricStreams = buildMetricStreams(newConfig.getMetricStreams());

        this.config = newConfig;
        this.metricStreams.clear();
        this.metricStreams.addAll(newMetricStreams);

        if (needsRestart) {
            start();
        }
    }

    private List<JoiningMetricStream> buildMetricStreams(List<MetricStreamDefinition> streamDefs) {
        List<JoiningMetricStream> newMetricStreams = new ArrayList<>();

        for (MetricStreamDefinition streamDef : streamDefs) {
            // retrieve references for all input MetricStreams
            Map<String, MetricStream> inputStreams = new HashMap<>();
            for (Entry<String, String> inputStream : streamDef.getInputStreams().entrySet()) {
                String inputStreamAlias = inputStream.getKey();
                String inputStreamId = inputStream.getValue();

                // the input stream can reference either an external stream
                // (defined by another MetricStreamer) or a prior declared local
                // (joining) metric stream.
                List<MetricStream> declaredStreams = concat(newMetricStreams, externalStreams());
                inputStreams.put(inputStreamAlias, getMetricStreamById(declaredStreams, inputStreamId));
            }

            JoiningMetricStreamConfig conf = new JoiningMetricStreamConfig(streamDef.getId(), streamDef.getMetric(),
                    streamDef.getMaxTimeDiff(), inputStreams, streamDef.getCompiledJoinScript());
            newMetricStreams.add(new JoiningMetricStream(this.logger, this.eventBus, conf));
        }

        return newMetricStreams;
    }

    /**
     * Returns a {@link MetricStream} with a particular id from a list of
     * {@link MetricStreamer}s or throws an {@link IllegalArgumentException} if
     * no stream with the given id can be found.
     *
     * @param candidates
     *            The {@link MetricStream}s to search.
     * @param id
     *            The id to look for.
     * @return
     */
    private MetricStream getMetricStreamById(List<MetricStream> candidates, String id) throws IllegalArgumentException {
        Optional<MetricStream> match = candidates.stream().filter(it -> it.getId().equals(id)).findFirst();
        if (match.isPresent()) {
            return match.get();
        }

        throw new IllegalArgumentException(String.format("referenced metricStream with id %s could not be found", id));

    }

    /**
     * Referenced input streams in the different {@link JoiningMetricStream}s
     * must refer to streams declared by other {@link MetricStreamer}s or by
     * prior streams declared in this {@link MetricStreamJoiner}.
     *
     * @param conf
     */
    private void validateInputStreamReferences(MetricStreamJoinerConfig conf) throws IllegalArgumentException {
        List<MetricStreamDefinition> metricStreams = conf.getMetricStreams();
        for (int i = 0; i < metricStreams.size(); i++) {
            MetricStreamDefinition metricStream = metricStreams.get(i);
            List<String> priorDeclaredLocalStreamIds = metricStreams.subList(0, i).stream().map(it -> it.getId())
                    .collect(toList());

            for (String referencedMetricStreamId : metricStream.getInputStreams().values()) {
                if (!externalStreamIds().contains(referencedMetricStreamId)
                        && !priorDeclaredLocalStreamIds.contains(referencedMetricStreamId)) {
                    throw new IllegalArgumentException(String.format(
                            "metricStream %s references stream %s which is neither defined by "
                                    + "another MetricStreamer nor a prior declared JoiningMetricStream",
                            metricStream.getId(), referencedMetricStreamId));
                }
            }
        }

    }

    /**
     * Returns the ids of all {@link MetricStream}s that are defined by other
     * {@link MetricStreamer}s (declared prior to this {@link MetricStreamer}.
     *
     * @return
     */
    private List<String> externalStreamIds() {
        return externalStreams().stream().map(MetricStream::getId).collect(toList());
    }

    /**
     * Returns the ids of all {@link MetricStream}s that are defined by other
     * {@link MetricStreamer}s (declared prior to this {@link MetricStreamer}.
     *
     * @return
     */
    private List<MetricStream> externalStreams() {
        List<MetricStream> externalStreams = new ArrayList<>();

        for (MetricStreamer<?> externalMetricStreamer : this.priorDeclaredMetricStreamers) {
            externalStreams.addAll(externalMetricStreamer.getMetricStreams());
        }

        return externalStreams;
    }

    @Override
    public void start() throws IllegalStateException {
        ensureConfigured();

        if (isStarted()) {
            return;
        }

        for (JoiningMetricStream metricStream : this.metricStreams) {
            metricStream.start();
        }

        this.started = true;
    }

    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }

        for (JoiningMetricStream metricStream : this.metricStreams) {
            metricStream.stop();
        }
        this.metricStreams.clear();

        this.started = false;
    }

    @Override
    public ServiceStatus getStatus() {
        return new ServiceStatus.Builder().started(isStarted()).build();
    }

    @Override
    public MetricStreamJoinerConfig getConfiguration() {
        return this.config;
    }

    @Override
    public Class<MetricStreamJoinerConfig> getConfigurationClass() {
        return MetricStreamJoinerConfig.class;
    }

    @Override
    public List<MetricStream> getMetricStreams() {
        ensureConfigured();

        return Collections.unmodifiableList(this.metricStreams);
    }

    @Override
    public MetricStream getMetricStream(String id) throws IllegalArgumentException {
        ensureConfigured();

        Optional<JoiningMetricStream> match = this.metricStreams.stream().filter(it -> it.getId().equals(id))
                .findFirst();
        if (!match.isPresent()) {
            throw new IllegalArgumentException(String.format("no metric stream with id %s found", id));
        }
        return match.get();
    }

    @Override
    public void fetch() throws MetricStreamException, IllegalStateException {
        ensureStarted();
        // no-op
    }

    private boolean isStarted() {
        return this.started;
    }

    private void ensureConfigured() throws IllegalStateException {
        checkState(this.config != null, "attempt to use metric streamer before being configured");
    }

    private void ensureStarted() throws IllegalStateException {
        ensureConfigured();
        checkState(isStarted(), "attempt to use metric streamer before being started");
    }

    private List<MetricStream> concat(List<? extends MetricStream> list1, List<? extends MetricStream> list2) {
        List<MetricStream> concatenation = new ArrayList<>(list1);
        concatenation.addAll(list2);
        return concatenation;
    }
}
