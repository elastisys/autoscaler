package com.elastisys.autoscaler.metricstreamers.streamjoiner.stream;

import static java.util.Arrays.asList;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.metronome.api.MetronomeEvent;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamException;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.impl.EmptyResultSet;
import com.elastisys.autoscaler.metricstreamers.streamjoiner.AlertTopic;
import com.elastisys.autoscaler.metricstreamers.streamjoiner.config.MetricStreamDefinition;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertBuilder;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Creates a new {@link JoiningMetricStream} which listens to the
 * {@link EventBus} for metric values on the {@link MetricStream}s that it has
 * been instructed to observe. Whenever values have been observed on all streams
 * and are not farther apart than
 * {@link MetricStreamDefinition#getMaxTimeDiff()}, the join script is run to
 * produce a new {@link MetricValue} that is sent over the {@link EventBus} as a
 * {@link MetricStreamMessage}.
 * <p/>
 * The client is responsible for calling the {@link #start()} method, which
 * causes the {@link JoiningMetricStream} to register with the {@link EventBus}
 * and start listening for metrics. When the client is done using the
 * {@link JoiningMetricStream} it should call {@link #stop()} to ensure that the
 * {@link JoiningMetricStream} unregisters from the {@link EventBus} and stops
 * processing events.
 */
public class JoiningMetricStream implements MetricStream {

    private final Logger logger;
    private final EventBus eventBus;
    private final JoiningMetricStreamConfig config;

    /**
     * Tracks the latest metric observation for each {@link MetricStream} that
     * the {@link JoiningMetricStream} follows. Note: use of {@link HashMap}
     * should be thread-safe as we never structurally change the map after its
     * creation (we only update values for existing keys).
     */
    private final Map<MetricStream, MetricValue> observations = new HashMap<>();

    public JoiningMetricStream(Logger logger, EventBus eventBus, JoiningMetricStreamConfig config) {
        this.logger = logger;
        this.eventBus = eventBus;
        this.config = config;

        for (MetricStream inputStream : config.getInputStreams().values()) {
            this.observations.put(inputStream, null);
        }
    }

    @Override
    public String getId() {
        return this.config.getId();
    }

    @Override
    public String getMetric() {
        return this.config.getMetric();
    }

    /**
     * A no-op. The {@link JoiningMetricStream} does not support historical
     * metric queries.
     */
    @Override
    public QueryResultSet query(Interval timeInterval, QueryOptions options) throws MetricStreamException {
        // note: does not support queries for historical values
        return new EmptyResultSet();
    }

    /**
     * Register this {@link JoiningMetricStream} with the {@link EventBus} to
     * start processing metric values.
     */
    public void start() {
        this.logger.debug("metric stream {} now listening for input metric streams {}", this.config.getId(),
                inputStreamIds());
        this.eventBus.register(this);
    }

    /**
     * Unregister {@link JoiningMetricStream} from the {@link EventBus}. From
     * this point on, the {@link JoiningMetricStream} is useless.
     */
    public void stop() {
        this.logger.debug("stopping metric stream {}", this.config.getId());
        this.eventBus.unregister(this);
    }

    /**
     * Called whenever a metric batch is sent on the {@link EventBus}.
     *
     * @param message
     */
    @Subscriber
    public void onMetricEvent(MetricStreamMessage metricBatch) {
        // if it doesn't origin from a monitored metric stream, ignore it
        if (!inputStreamIds().contains(metricBatch.getId())) {
            return;
        }
        // if it doesn't contain any values, ignore it
        if (metricBatch.getMetricValues().isEmpty()) {
            return;
        }

        MetricStream inputStream = inputStream(metricBatch.getId());
        MetricValue receivedMetric = newestMetric(metricBatch);
        MetricValue latestStreamObservation = this.observations.get(inputStream);
        if (latestStreamObservation != null && receivedMetric.getTime().isBefore(latestStreamObservation.getTime())) {
            // ignore metric, as it was delivered out-of-order
            this.logger.debug("{}: ignoring metric {} (older than the most recent observation for input stream {})",
                    getId(), receivedMetric, inputStream.getId());
            return;
        }
        // register the observation
        this.observations.put(inputStream, receivedMetric);
        this.logger.debug("{}: received observation {}", getId(), receivedMetric);

        // if observations exist for all streams and are sufficiently close in
        // time, run join-script and produce a new value
        if (sufficientlyCloseObservations()) {
            try {
                this.logger.debug("{}: running join-script on {}", getId(), observationsToString());
                double value = runJoinScript();
                this.logger.debug("{}: produced joined metric value {}", getId(), value);
                this.eventBus.post(
                        new MetricStreamMessage(getId(), asList(new MetricValue(getMetric(), value, UtcTime.now()))));
                // trigger a new resize iteration
                this.eventBus.post(MetronomeEvent.RESIZE_ITERATION);
            } catch (Exception e) {
                String message = String.format("%s: join script failed", getId());
                String detail = String.format("%s: %s", getId(), e.getMessage());
                Alert alert = AlertBuilder.create().topic(AlertTopic.JOIN_SCRIPT_FAILURE.getTopicPath())
                        .severity(AlertSeverity.ERROR).message(message).details(detail)
                        .addMetadata("metricStream", getId()).build();
                this.eventBus.post(alert);
                this.logger.error(detail, e);
            }
        }
    }

    /**
     * Extracts the newest {@link MetricValue} from a collection of values.
     *
     * @param event
     * @return
     */
    private MetricValue newestMetric(MetricStreamMessage event) {
        // only save the most recent value from stream
        List<MetricValue> metrics = event.getMetricValues();
        MetricValue lastValue = metrics.get(metrics.size() - 1);
        return lastValue;
    }

    private String observationsToString() {
        StringWriter writer = new StringWriter();
        writer.append("{");

        for (Entry<MetricStream, MetricValue> entry : this.observations.entrySet()) {
            writer.append(String.format(" %s:%s", entry.getKey().getId(), entry.getValue().getValue()));
        }
        writer.append(" }");
        return writer.toString();
    }

    private double runJoinScript() throws ScriptException, JoinScriptException {
        Bindings bindings = this.config.getJoinScript().getEngine().createBindings();
        for (String inputStreamAlias : this.config.getInputStreams().keySet()) {
            MetricStream inputStream = this.config.getInputStreams().get(inputStreamAlias);
            MetricValue latestMetric = this.observations.get(inputStream);
            bindings.put(inputStreamAlias, latestMetric.getValue());
        }
        Object result = this.config.getJoinScript().eval(bindings);
        if (result == null) {
            throw new JoinScriptException(String.format("joinScript returned null (must return a number)"));
        }
        if (!Number.class.isAssignableFrom(result.getClass())) {
            throw new JoinScriptException(
                    String.format("joinScript returned a value of type %s (must return a number)", result.getClass()));
        }
        Number number = Number.class.cast(result);
        return number.doubleValue();
    }

    /**
     * Returns <code>true</code> if an observation has been made for all tracked
     * input {@link MetricStream}s and these observations are all within
     * {@link JoiningMetricStreamConfig#getMaxTimeDiff()} of each other.
     *
     * @return
     */
    private boolean sufficientlyCloseObservations() {
        if (this.observations.containsValue(null)) {
            // at least one stream is lacking an observation
            return false;
        }

        // check that observations are not too far apart
        DateTime oldest = null;
        DateTime newest = null;
        for (MetricValue metric : this.observations.values()) {
            if (oldest == null || metric.getTime().isBefore(oldest)) {
                oldest = metric.getTime();
            }
            if (newest == null || metric.getTime().isAfter(newest)) {
                newest = metric.getTime();
            }
        }

        long diffMillis = new Duration(oldest, newest).getMillis();
        boolean sufficientlyClose = diffMillis <= this.config.getMaxTimeDiff().getMillis();
        this.logger.debug("{}: observations are {} ms apart{}", getId(), diffMillis,
                sufficientlyClose ? "" : " (too far apart to run join script)");
        return sufficientlyClose;
    }

    private MetricStream inputStream(String id) {
        Optional<MetricStream> stream = this.config.getInputStreams().values().stream()
                .filter(it -> it.getId().equals(id)).findFirst();
        if (!stream.isPresent()) {
            throw new IllegalArgumentException("no inputStream with id: " + id);
        }
        return stream.get();
    }

    /**
     * Returns the ids of all {@link MetricStream}s being subscribed to.
     *
     * @return
     */
    private Set<String> inputStreamIds() {
        return this.config.getInputStreams().values().stream().map(MetricStream::getId).collect(Collectors.toSet());
    }
}
