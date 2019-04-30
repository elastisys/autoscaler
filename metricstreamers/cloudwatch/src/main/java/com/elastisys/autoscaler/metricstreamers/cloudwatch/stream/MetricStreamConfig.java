package com.elastisys.autoscaler.metricstreamers.cloudwatch.stream;

import java.util.Objects;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchMetricStreamDefinition;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Settings for a {@link CloudWatchMetricStream}, which describes what query to
 * run when fetching values.
 */
public class MetricStreamConfig {

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

    /**
     * Describes what query to run for this particular {@link MetricStream}.
     */
    private final CloudWatchMetricStreamDefinition metricStreamDef;

    /**
     * Creates a new {@link MetricStreamConfig}.
     *
     * @param accessKeyId
     * @param secretAccessKey
     * @param region
     * @param metricStreamDef
     */
    public MetricStreamConfig(String accessKeyId, String secretAccessKey, String region,
            CloudWatchMetricStreamDefinition metricStreamDef) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.region = region;
        this.metricStreamDef = metricStreamDef;
    }

    public String getAccessKeyId() {
        return this.accessKeyId;
    }

    public String getSecretAccessKey() {
        return this.secretAccessKey;
    }

    public String getRegion() {
        return this.region;
    }

    public CloudWatchMetricStreamDefinition getMetricStreamDef() {
        return this.metricStreamDef;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.accessKeyId, this.secretAccessKey, this.region, this.metricStreamDef);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetricStreamConfig) {
            MetricStreamConfig that = (MetricStreamConfig) obj;
            return Objects.equals(this.accessKeyId, that.accessKeyId) //
                    && Objects.equals(this.secretAccessKey, that.secretAccessKey) //
                    && Objects.equals(this.region, that.region) //
                    && Objects.equals(this.metricStreamDef, that.metricStreamDef);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
