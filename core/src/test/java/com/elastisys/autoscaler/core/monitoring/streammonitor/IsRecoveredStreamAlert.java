package com.elastisys.autoscaler.core.monitoring.streammonitor;

import static com.elastisys.autoscaler.core.monitoring.streammonitor.StreamActivityChecker.ACTIVE;
import static com.elastisys.autoscaler.core.monitoring.streammonitor.StreamActivityChecker.METRIC_STREAM_ID;
import static com.elastisys.autoscaler.core.monitoring.streammonitor.StreamActivityChecker.METRIC_STREAM_STATE;
import static com.elastisys.scale.commons.net.alerter.AlertSeverity.NOTICE;

import java.util.Map;
import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.joda.time.DateTime;

import com.elastisys.autoscaler.core.alerter.api.types.AlertTopics;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.google.gson.JsonElement;

/**
 * Matches an {@link AlertMessage} notifying of recovered activity for a given
 * {@link MetricStream}.
 */
public class IsRecoveredStreamAlert extends TypeSafeMatcher<Alert> {

    private final MetricStream expectedMetricStream;
    private final DateTime expectedTimestamp;

    public IsRecoveredStreamAlert(MetricStream metricStream, DateTime expectedTime) {
        this.expectedMetricStream = metricStream;
        this.expectedTimestamp = expectedTime;
    }

    @Override
    public boolean matchesSafely(Alert someAlert) {
        Map<String, JsonElement> tags = someAlert.getMetadata();
        return Objects.equals(AlertTopics.STREAM_ACTIVITY_UPDATE.getTopicPath(), someAlert.getTopic())
                && Objects.equals(this.expectedTimestamp, someAlert.getTimestamp())
                && Objects.equals(someAlert.getSeverity(), NOTICE)
                && Objects.equals(tags.get(METRIC_STREAM_ID).getAsString(), this.expectedMetricStream.getId())
                && Objects.equals(tags.get(METRIC_STREAM_STATE).getAsString(), ACTIVE);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("metric stream recovery alert for stream '%s' at time '%s'",
                this.expectedMetricStream.getId(), this.expectedTimestamp));
    }

    @Factory
    public static <T> Matcher<Alert> isRecoveredStreamAlert(MetricStream expectedMetricStream,
            DateTime expectedTimestamp) {
        return new IsRecoveredStreamAlert(expectedMetricStream, expectedTimestamp);
    }
}