package com.elastisys.autoscaler.metricstreamers.streamjoiner;

import com.elastisys.scale.commons.net.alerter.Alert;

/**
 * A collection of alert topics used to categorize {@link Alert}s sent by the
 * {@link MetricStreamJoiner}.
 */
public enum AlertTopic {
    /**
     * Alert topic indicating that the join script failed to produce a proper
     * result.
     */
    JOIN_SCRIPT_FAILURE("/monitoringSubsystem/metricStreamJoiner/joinScript/FAILURE");

    /**
     * The topic path of this {@link AlertTopic}, which is used to indicate what
     * part of the system this {@link Alert} stems from.
     */
    private final String topicPath;

    private AlertTopic(String topicPath) {
        this.topicPath = topicPath;
    }

    /**
     * Returns the topic path of this {@link AlertTopic}, which is used to
     * indicate what part of the system this {@link Alert} stems from.
     *
     * @return
     */
    public String getTopicPath() {
        return this.topicPath;
    }
}