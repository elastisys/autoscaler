package com.elastisys.autoscaler.core.alerter.api.types;

import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.metronome.api.Metronome;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.streammonitor.MetricStreamMonitor;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;

/**
 * A collection of alert topics used to categorize {@link AlertMessage}s sent
 * out on the {@link AutoScaler}'s event bus.
 *
 * @see AlertMessage
 */
public enum AlertTopics {
    /**
     * Alert message topic indicating that the {@link PredictionSubsystem} is
     * experiencing problems in performing predictions.
     */
    PREDICTION_FAILURE("/predictionSubsystem/prediction/FAILURE"),
    /**
     * Alert message topic indicating that the {@link Metronome} is experiencing
     * problems.
     */
    RESIZE_ITERATION_FAILURE("/metronome/resizeIteration/FAILURE"),
    /**
     * Alert message topic indicating that the {@link Metronome} is experiencing
     * problems.
     */
    METRONOME_FAILURE("/metronome/FAILURE"),
    /**
     * Alert message topic indicating that the machine pool size has changed.
     */
    POOL_SIZE_CHANGED("/machinePool/CHANGED"),
    /**
     * Alert message topic indicating that the {@link MetricStreamMonitor} has
     * new information on the stream activity for a {@link MetricStream}.
     */
    STREAM_ACTIVITY_UPDATE("/metricStreamMonitor/streamActivity/UPDATE"),
    /**
     * Alert message topic indicating that the {@link AccountingSubsystem}
     * failed to authenticate with the configured account credentials.
     */
    AUTHENTICATION_FAILURE("/accounting/authentication/FAILED"),
    /**
     * Alert message topic indicating that the {@link AccountingSubsystem}
     * failed to report accounting metrics.
     */
    ACCOUNTING_REPORT_FAILURE("/accounting/report/FAILED");

    /**
     * The topic path of this {@link AlertTopics}, which is used to indicate
     * what part of the system that {@link AlertMessage}s with this topic stem
     * from.
     */
    private final String topicPath;

    private AlertTopics(String topicPath) {
        this.topicPath = topicPath;
    }

    /**
     * Returns the topic path of this {@link AlertTopics}, which is used to
     * indicate what part of the system that {@link AlertMessage}s with this
     * topic stem from.
     *
     * @return
     */
    public String getTopicPath() {
        return this.topicPath;
    }
}
