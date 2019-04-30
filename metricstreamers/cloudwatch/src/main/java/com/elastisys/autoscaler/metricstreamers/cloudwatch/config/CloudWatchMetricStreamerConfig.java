package com.elastisys.autoscaler.metricstreamers.cloudwatch.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.CloudWatchMetricStreamer;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Carries configuration parameters for the {@link CloudWatchMetricStreamer}.
 *
 * @see CloudWatchMetricStreamer
 */
public class CloudWatchMetricStreamerConfig {
    /** Default poll interval when none is specified in configuration. */
    static final TimeInterval DEFAULT_POLL_INTERVAL = new TimeInterval(30L, TimeUnit.SECONDS);
    /**
     * The access key ID for the AWS account. See
     * <a href="https://aws-portal.amazon.com/gp/aws/securityCredentials"
     * >description</a>.
     */
    private final String accessKeyId;
    /**
     * The secret access key for the AWS account. See
     * <a href="https://aws-portal.amazon.com/gp/aws/securityCredentials"
     * >description</a>.
     */
    private final String secretAccessKey;

    /** The Amazon EC2 region hosting the CloudWatch service to connect to. */
    private final String region;
    /** The polling interval for {@link MetricStream}s. */
    private final TimeInterval pollInterval;
    /** The collection of published metric streams. */
    private final List<CloudWatchMetricStreamDefinition> metricStreams;

    /**
     * Creates a new {@link CloudWatchMetricStreamerConfig} with the specified
     * configuration values.
     *
     * @param region
     *            The Amazon EC2 region hosting the CloudWatch service to
     *            connect to.
     * @param pollInterval
     *            The polling interval for {@link MetricStream}s.
     * @param metricStreams
     *            The collection of published metric streams.
     */
    public CloudWatchMetricStreamerConfig(String accessKeyId, String secretAccessKey, String region,
            TimeInterval pollInterval, List<CloudWatchMetricStreamDefinition> metricStreams) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.region = region;
        this.pollInterval = pollInterval;
        this.metricStreams = metricStreams;
    }

    /**
     * Returns the access key ID for the AWS account.
     *
     * @return
     */
    public String getAccessKeyId() {
        return this.accessKeyId;
    }

    /**
     * Returns the secret access key for the AWS account.
     *
     * @return
     */
    public String getSecretAccessKey() {
        return this.secretAccessKey;
    }

    /**
     * Returns the Amazon EC2 region hosting the CloudWatch service to connect
     * to.
     *
     * @return
     */
    public String getRegion() {
        return this.region;
    }

    /**
     * Returns the polling interval (given in seconds) for metric streams.
     *
     * @return
     */
    public TimeInterval getPollInterval() {
        return Optional.ofNullable(this.pollInterval).orElse(DEFAULT_POLL_INTERVAL);
    }

    /**
     * Returns the collection of subscribable metric streams.
     *
     * @return
     */
    public List<CloudWatchMetricStreamDefinition> getMetricStreams() {
        return Optional.ofNullable(this.metricStreams).orElse(Collections.emptyList());
    }

    /**
     * Validates the configuration (checks if there are any obvious errors).
     *
     * @throws IllegalArgumentException
     *             Thrown if there are obvious errors, such as missing values
     *             the values are out of permissible ranges.
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(this.accessKeyId != null, "metricStreamer: missing accessKeyId");
        checkArgument(this.secretAccessKey != null, "metricStreamer: missing secretAccessKey");
        checkArgument(this.region != null, "metricStreamer: missing region");

        checkArgument(getPollInterval().getMillis() >= 1, "metricStreamer: pollInterval must be a positive duration");
        try {
            getMetricStreams().forEach(stream -> stream.validate());
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("metricStreamer: metricStreams: %s", e.getMessage()), e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.accessKeyId, this.secretAccessKey, this.region, this.pollInterval, this.metricStreams);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CloudWatchMetricStreamerConfig) {
            CloudWatchMetricStreamerConfig that = (CloudWatchMetricStreamerConfig) obj;
            return Objects.equals(this.accessKeyId, that.accessKeyId)
                    && Objects.equals(this.secretAccessKey, that.secretAccessKey)
                    && Objects.equals(this.region, that.region) //
                    && Objects.equals(this.pollInterval, that.pollInterval)
                    && Objects.equals(this.metricStreams, that.metricStreams);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    /**
     * Returns a copy of this {@link CloudWatchMetricStreamerConfig} with an
     * additional {@link CloudWatchMetricStreamDefinition}. Note that the
     * instance that this method is invoked on remains unchanged.
     *
     * @param stream
     *            The {@link CloudWatchMetricStreamDefinition} that will be
     *            added in the returned copy.
     * @return A copy of this {@link CloudWatchMetricStreamerConfig} with the
     *         stream definition added.
     */
    public CloudWatchMetricStreamerConfig withStreamDefinition(CloudWatchMetricStreamDefinition stream) {
        // create copy of streams
        List<CloudWatchMetricStreamDefinition> streams = new ArrayList<>(getMetricStreams());
        streams.add(stream);
        return new CloudWatchMetricStreamerConfig(getAccessKeyId(), getSecretAccessKey(), getRegion(),
                getPollInterval(), streams);
    }
}
