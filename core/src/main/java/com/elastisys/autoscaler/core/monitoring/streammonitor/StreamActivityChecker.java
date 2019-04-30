package com.elastisys.autoscaler.core.monitoring.streammonitor;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.alerter.api.types.AlertTopics;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.precond.Preconditions;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonElement;

/**
 * Tracks metric stream activity (by listening for metrics sent on the
 * {@link EventBus}) and emits warning {@link Alert}s on the {@link EventBus}
 * when a {@link MetricStream} has been silent for too long.
 * <p/>
 * Needs to be started before use.
 *
 * @see MetricStreamMonitor
 */
public class StreamActivityChecker {

    /** State for an active metric stream. */
    static final String ACTIVE = "active";
    /** State for an inactive metric stream. */
    static final String INACTIVE = "inactive";
    /** {@link Alert} tag used for a metric stream identifier. */
    static final String METRIC_STREAM_ID = "metricStreamId";
    /** {@link Alert} tag used for a metric stream state. */
    static final String METRIC_STREAM_STATE = "metricStreamState";
    /** {@link Alert} tag used for last activity observation. */
    static final String LAST_OBSERVED_ACTIVITY = "lastObservedActivity";

    /**
     * The {@link MonitoringSubsystem}, whose {@link MetricStream}s are to be
     * checked.
     */
    private final MonitoringSubsystem<?> monitoringSubsystem;

    private final Logger logger;
    /**
     * The {@link EventBus} on which to listen for stream activity and on which
     * to send warning alerts when too long periods of inactivity are detected
     * for metric streams.
     */
    private final EventBus eventBus;

    /**
     * Indicates if this {@link StreamActivityChecker} has registered with the
     * {@link EventBus} to start tracking metric stream activity.
     */
    private boolean started;

    /**
     * Tracks the time of the latest activity (data point arrival) seen on every
     * metric stream.
     */
    private final Map<MetricStream, DateTime> latestActivityMap;

    /**
     * Tracks the time of the latest inactivity alert posted for metric streams
     * that have exceeded the {@link #maxTolerableInactivity}. When activity is
     * observed on a {@link MetricStream}, its entry should be removed from this
     * map.
     */
    private final Map<MetricStream, DateTime> latestInactivityAlertMap;

    /**
     * Creates a {@link StreamActivityChecker}.
     *
     * @param logger
     *            A {@link Logger} instance.
     * @param monitoringSubsystem
     *            The {@link MonitoringSubsystem}, whose {@link MetricStream}s
     *            are to be checked.
     * @param eventBus
     *            The {@link EventBus} on which to listen for stream activity
     *            and on which to send warning alerts when too long periods of
     *            inactivity are detected for metric streams.
     */
    public StreamActivityChecker(Logger logger, MonitoringSubsystem<?> monitoringSubsystem, EventBus eventBus) {
        this.logger = logger;
        this.monitoringSubsystem = monitoringSubsystem;
        this.eventBus = eventBus;

        this.latestActivityMap = new ConcurrentHashMap<>();
        this.latestInactivityAlertMap = new ConcurrentHashMap<>();

    }

    /**
     * Register on event bus to start listening on metric stream activity.
     */
    public void start() {
        if (this.started) {
            return;
        }
        this.eventBus.register(this);
        this.started = true;
    }

    /**
     * Unregister from event bus to stop listening on metric stream activity.
     */
    public void stop() {
        if (!this.started) {
            return;
        }
        this.eventBus.unregister(this);
        this.started = false;
    }

    /**
     * Determines if any of the metric streams have been inactive for longer
     * than {@code maxTolerableInactivity}, and if so, sends out a warning
     * {@link Alert} on the {@link EventBus}. If a previously inactive
     * {@link MetricStream} has recovered, a notice {@link Alert} is sent out to
     * indicate this fact.
     * <p/>
     * Note: depending on the duplicateSuppression setting of the alerter, the
     * alert may be suppressed .
     *
     * @param maxTolerableInactivity
     *            The longest period of silence that is accepted on a
     *            {@link MetricStream} before an {@link Alert} is raised on the
     *            event bus.
     *
     * @see java.lang.Runnable#run()
     */
    public void alertOnActivityChange(Duration maxTolerableInactivity) {
        Preconditions.checkState(this.started,
                "streamActivityChecker: cannot check metric stream inactivity before being started");
        this.logger.debug("checking metric stream activity ...");

        List<MetricStream> currentStreams = getPublishedStreams();
        // forget about all tracked metric streams that are no longer in use
        this.latestActivityMap.keySet().retainAll(currentStreams);
        this.latestInactivityAlertMap.keySet().retainAll(currentStreams);

        for (MetricStream metricStream : currentStreams) {
            Duration timeOfInactivity = timeOfInactivity(metricStream);
            this.logger.debug("stream '{}' inactive for {} seconds, deemed inactive after {} seconds",
                    metricStream.getId(), timeOfInactivity.getStandardSeconds(),
                    maxTolerableInactivity.getStandardSeconds());
            if (timeOfInactivity.isLongerThan(maxTolerableInactivity)) {
                sendInactivityAlert(metricStream, maxTolerableInactivity);
            } else {
                // if metric stream has been reported inactive, send out an
                // alert to notify of the stream being active again
                if (this.latestInactivityAlertMap.remove(metricStream) != null) {
                    sendRecoveryAlert(metricStream);
                }
            }
        }
    }

    private void sendRecoveryAlert(MetricStream metricStream) {
        String message = String.format("previously inactive metric stream \"%s\" appears to have recovered",
                metricStream.getId());
        Map<String, JsonElement> tags = Maps.of( //
                METRIC_STREAM_ID, JsonUtils.toJson(metricStream.getId()), //
                METRIC_STREAM_STATE, JsonUtils.toJson(ACTIVE));
        Alert alert = new Alert(AlertTopics.STREAM_ACTIVITY_UPDATE.getTopicPath(), AlertSeverity.NOTICE, UtcTime.now(),
                message, null, tags);
        this.eventBus.post(alert);
    }

    /**
     * Post a low activity warning on the {@link EventBus} for a
     * {@link MetricStream}.
     *
     * @param metricStream
     */
    private void sendInactivityAlert(MetricStream metricStream, Duration maxTolerableInactivity) {
        String message = format("metric stream \"%s\" has been inactive for more than %d second(s)",
                metricStream.getId(), maxTolerableInactivity.getStandardSeconds());
        this.logger.warn(message);
        sendInactivityAlert(metricStream, message);
    }

    private void sendInactivityAlert(MetricStream metricStream, String message) {
        Map<String, JsonElement> tags = Maps.of( //
                METRIC_STREAM_ID, JsonUtils.toJson(metricStream.getId()), //
                METRIC_STREAM_STATE, JsonUtils.toJson(INACTIVE), //
                LAST_OBSERVED_ACTIVITY, JsonUtils.toJson(getLatestActivity(metricStream)));

        Alert alert = new Alert(AlertTopics.STREAM_ACTIVITY_UPDATE.getTopicPath(), AlertSeverity.WARN, UtcTime.now(),
                message, null, tags);
        this.eventBus.post(alert);
        this.latestInactivityAlertMap.put(metricStream, UtcTime.now());
    }

    /**
     * Returns all currently published streams.
     *
     * @return
     */
    private List<MetricStream> getPublishedStreams() {
        List<MetricStream> all = new ArrayList<>();

        List<MetricStreamer<?>> metricStreamers = this.monitoringSubsystem.getMetricStreamers();
        for (MetricStreamer<?> metricStreamer : metricStreamers) {
            all.addAll(metricStreamer.getMetricStreams());
        }
        return all;
    }

    /**
     * Returns the duration since the last activity was seen on a stream.
     *
     * @param stream
     * @return
     */
    Duration timeOfInactivity(MetricStream stream) {
        DateTime latestActivity = getLatestActivity(stream);
        return new Duration(latestActivity, UtcTime.now());
    }

    /**
     * Returns the time of the latest activity seen on a {@link MetricStream}.
     * <p/>
     * If the stream hasn't been encountered before and hasn't seen any activity
     * yet, we set its latest activity to be the current time. This time will
     * then be returned as time of latest activity for the stream, until a
     * metric value arrives on the stream.
     *
     * @param metricStream
     * @return
     */
    DateTime getLatestActivity(MetricStream metricStream) {
        if (!this.latestActivityMap.containsKey(metricStream)) {
            // new stream without prior activity, set a fake latest activity
            // time to present time
            this.latestActivityMap.put(metricStream, UtcTime.now());
        }

        return this.latestActivityMap.get(metricStream);
    }

    /**
     * When a value is observed on a {@link MetricStream}, we register that the
     * observation was made.
     *
     * @param message
     */
    @Subscriber
    public void onMetricStreamEvent(MetricStreamMessage message) {
        String metricStreamId = message.getId();
        MetricStream originStream = findOriginMetricStream(metricStreamId);
        this.latestActivityMap.put(originStream, UtcTime.now());
    }

    private MetricStream findOriginMetricStream(String streamId) {
        for (MetricStreamer<?> metricStreamer : this.monitoringSubsystem.getMetricStreamers()) {
            try {
                return metricStreamer.getMetricStream(streamId);
            } catch (IllegalArgumentException e) {
                // not found, try next
            }
        }
        throw new NoSuchElementException(String.format("no metric stream with id %s was found", streamId));
    }
}