package com.elastisys.autoscaler.metricstreamers.cloudwatch.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.CloudWatchMetricStreamer;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Represents a metric stream definition published for subscription by a
 * {@link CloudWatchMetricStreamer}. {@link CloudWatchMetricStreamDefinition}s
 * are configured through a {@link CloudWatchMetricStreamerConfig}.
 * <p/>
 * For more details on the meaning of the different parameters, refer to the
 * <a href=
 * "http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/cloudwatch_concepts.html"
 * >CloudWatch concepts introduction</a>, the <a href=
 * "http://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html"
 * >Amazon CloudWatch API specification</a> and the <a href=
 * "http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/CW_Support_For_AWS.html"
 * >CloudWatch Metrics, Namespaces, and Dimensions Reference</a>.
 *
 * @see CloudWatchMetricStreamerConfig
 * @see CloudWatchMetricStreamer
 */
public class CloudWatchMetricStreamDefinition {
    /**
     * The maximum number of data points that can be requested in a single call
     * to the CloudWatch API.
     */
    public static final long CLOUDWATCH_MAX_DATA_POINTS = 1440L;
    public static final Boolean DEFAULT_RATE_CONVERSION = false;

    /**
     * The id of the metric stream. This is the id that will be used by clients
     * wishing to subscribe to {@link MetricValue}s for this metric stream.
     * Required.
     */
    private final String id;
    /**
     * The Amazon CloudWatch namespace of the metric to fetch. See the <a href=
     * "http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/cloudwatch_concepts.html"
     * >CloudWatch concepts</a> for an explanation. Required.
     */
    private final String namespace;
    /**
     * The CloudWatch metric that the metric stream retrieves
     * {@link MetricValue}s for. Required.
     */
    private final String metric;
    /**
     * The aggregation method to apply to the set of metric values. Can be any
     * of "Sum", "Average", "Minimum", "Maximum" and "SampleCount". Required.
     */
    private final CloudWatchStatistic statistic;

    /**
     * Specifies the time period over which to apply the statistic and, hence,
     * the spacing between data points in the aggregated data. Must be at least
     * {@code 60} seconds and must be a multiple of {@code 60}. Required.
     */
    private final TimeInterval period;

    /**
     * The dimensions (key-value pairs) used to narrow down the set of streamed
     * metric values. Only metric values matching the specified dimensions will
     * be returned.
     * <p/>
     * As an example, for EC2 instance metrics (namespace @code{@codeAWS/EC2}),
     * {@code InstanceId} can be used to only return values for a certain
     * instance and {@code AutoScalingGroupName} to narrow down queries to only
     * ask for metrics from a certain Auto Scaling Group. May be
     * <code>null</code>.
     */
    private final Map<String, String> dimensions;

    /**
     * When <code>true</code> the stream will feed the change rate of the metric
     * rather than the absolute values of the metric.
     */
    private final Boolean convertToRate;

    /**
     * The minimum age of requested data points. The {@link MetricStreamer} will
     * never request values newer than this from CloudWatch. As such, this value
     * can be regarded as the expected "settling time" of new data points.
     * <p/>
     * When requesting aggregate metric data points from CloudWatch that are
     * recent, there is always a risk of seeing partial/incomplete results
     * before metric values from all instances have been registered. Typically,
     * it takes a couple of minutes for all instances to report their values to
     * CloudWatch.
     * <p/>
     * To avoid seeing incomplete results, set this value to around {@code 120}
     * seconds.
     * <p/>
     * May be <code>null</code>. Default is to use no data settling time.
     */
    private final TimeInterval dataSettlingTime;

    /**
     * The maximum time period that a single query will attempt to fetch in a
     * single call. A query with a longer time interval will be run
     * incrementally, each fetching a sub-interval of this duration. This type
     * of incremental retrieval of large {@link QueryResultSet}s limits the
     * amount of (memory) resources involved in processing large queries. May be
     * <code>null</code>. Default: {@value #MAX_QUERY_CHUNK_SIZE}.
     */
    private final TimeInterval queryChunkSize;

    /**
     * Constructs a new {@link CloudWatchMetricStreamDefinition}.
     * <p/>
     * See the <a href=
     * "http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/cloudwatch_concepts.html"
     * >CloudWatch concepts</a> for an explanation of the parameters.
     *
     * @param id
     *            The id of the metric stream. This is the id that will be used
     *            by clients wishing to subscribe to {@link MetricValue}s for
     *            this metric stream. Required.
     * @param namespace
     *            The Amazon CloudWatch namespace of the metric to fetch.
     *            Required.
     * @param metric
     *            The CloudWatch metric that the metric stream retrieves
     *            {@link MetricValue}s for. Required.
     * @param statistic
     *            The aggregation method to apply to the set of metric values.
     *            Can be any of "Sum", "Average", "Minimum", "Maximum" and
     *            "SampleCount". Required.
     * @param period
     *            Specifies the time period over which to apply the statistic
     *            and, hence, the spacing between data points in the aggregated
     *            data. Must be at least {@code 60} seconds and must be a
     *            multiple of {@code 60}. Required.
     * @param convertToRate
     *            When <code>true</code> the stream will feed the change rate of
     *            the metric rather than the absolute values of the metric.
     * @param dimensions
     *            The dimensions (key-value pairs) used to narrow down the set
     *            of streamed metric values. Only metric values matching the
     *            specified dimensions will be returned.
     *            <p/>
     *            As an example, for EC2 instance metrics (namespace @code
     *            {@codeAWS/EC2}), {@code InstanceId} can be used to only return
     *            values for a certain instance and {@code AutoScalingGroupName}
     *            to narrow down queries to only ask for metrics from a certain
     *            Auto Scaling Group. May be <code>null</code>.
     * @param dataSettlingTime
     *            The minimum age of requested data points. The
     *            {@link MetricStreamer} will never request values newer than
     *            this from CloudWatch. As such, this value can be regarded as
     *            the expected "settling time" of new data points.
     *            <p/>
     *            When requesting aggregate metric data points from CloudWatch
     *            that are recent, there is always a risk of seeing
     *            partial/incomplete results before metric values from all
     *            instances have been registered. Typically, it takes a couple
     *            of minutes for all instances to report their values to
     *            CloudWatch.
     *            <p/>
     *            May be <code>null</code>. Default is to use no data settling
     *            time.
     * @param queryChunkSize
     *            The maximum time period that a single query will attempt to
     *            fetch in a single call. A query with a longer time interval
     *            will be run incrementally, each fetching a sub-interval of
     *            this duration. This type of incremental retrieval of large
     *            {@link QueryResultSet}s limits the amount of (memory)
     *            resources involved in processing large queries. May be
     *            <code>null</code>. Default: {@link #MAX_QUERY_CHUNK_SIZE}
     */
    public CloudWatchMetricStreamDefinition(String id, String namespace, String metric, CloudWatchStatistic statistic,
            TimeInterval period, Boolean convertToRate, Map<String, String> dimensions, TimeInterval dataSettlingTime,
            TimeInterval queryChunkSize) {
        this.id = id;
        this.namespace = namespace;
        this.metric = metric;
        this.statistic = statistic;
        this.period = period;
        this.convertToRate = convertToRate;
        this.dimensions = dimensions;
        this.dataSettlingTime = dataSettlingTime;
        this.queryChunkSize = queryChunkSize;
    }

    /**
     * Returns the id of the metric stream. This is the id that will be used by
     * clients wishing to subscribe to {@link MetricValue}s for this metric
     * stream.
     *
     * @return
     */
    public String getId() {
        return this.id;
    }

    /**
     * Returns the Amazon CloudWatch namespace of the metric to fetch.
     *
     * @return
     */
    public String getNamespace() {
        return this.namespace;
    }

    /**
     * Returns the CloudWatch metric that the metric stream retrieves
     * {@link MetricValue}s for.
     *
     * @return
     */
    public String getMetric() {
        return this.metric;
    }

    /**
     * Returns the dimensions (key-value pairs) used to narrow down the set of
     * streamed metric values. Only metric values matching the specified
     * dimensions will be returned.
     * <p/>
     * As an example, for EC2 instance metrics (namespace @code {@codeAWS/EC2}),
     * {@code InstanceId} can be used to only return values for a certain
     * instance and {@code AutoScalingGroupName} to narrow down queries to only
     * ask for metrics from a certain Auto Scaling Group.
     *
     * @return
     */
    public Map<String, String> getDimensions() {
        return Optional.ofNullable(this.dimensions).orElse(Collections.emptyMap());
    }

    /**
     * Returns the aggregation method to apply to the set of metric values. Can
     * be any of "Sum", "Average", "Minimum", "Maximum" and "SampleCount".
     *
     * @return
     */
    public CloudWatchStatistic getStatistic() {
        return this.statistic;
    }

    /**
     * Returns Specifies the time period over which to apply the statistic and,
     * hence, the spacing between data points in the aggregated data. Must be at
     * least {@code 60} seconds and must be a multiple of {@code 60}.
     *
     * @return
     */
    public TimeInterval getPeriod() {
        return this.period;
    }

    /**
     * When <code>true</code> the stream will feed the change rate of the metric
     * rather than the absolute values of the metric.
     *
     * @return
     */
    public boolean isConvertToRate() {
        return Optional.ofNullable(this.convertToRate).orElse(DEFAULT_RATE_CONVERSION);
    }

    /**
     * Returns the minimum age of requested data points. The
     * {@link MetricStreamer} will never request values newer than this from
     * CloudWatch. As such, this value can be regarded as the expected "settling
     * time" of new data points.
     * <p/>
     * When requesting aggregate metric data points from CloudWatch that are
     * recent, there is always a risk of seeing partial/incomplete results
     * before metric values from all instances have been registered. Typically,
     * it takes a couple of minutes for all instances to report their values to
     * CloudWatch.
     *
     * @return
     */
    public TimeInterval getDataSettlingTime() {
        return Optional.ofNullable(this.dataSettlingTime).orElse(new TimeInterval(0L, TimeUnit.SECONDS));
    }

    /**
     * The maximum time period that a single query will attempt to fetch in a
     * single call. A query with a longer time interval will be run
     * incrementally, each fetching a sub-interval of this duration. This type
     * of incremental retrieval of large {@link QueryResultSet}s limits the
     * amount of (memory) resources involved in processing large queries.
     *
     * @return
     */
    public TimeInterval getQueryChunkSize() {
        return Optional.ofNullable(this.queryChunkSize)
                .orElse(TimeInterval.seconds(getPeriod().getSeconds() * CLOUDWATCH_MAX_DATA_POINTS));
    }

    /**
     * Validates that this {@link CloudWatchMetricStreamDefinition} contains
     * sufficient information to allow a valid AWS CloudWatch query to be built.
     * If it does, the method will just return. If it is found to be invalid, a
     * {@link IllegalArgumentException} is thrown.
     *
     * @throws IllegalArgumentException
     *             If this {@link CloudWatchMetricStreamDefinition} is invalid.
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(this.id != null, "metricStream: missing id");
        checkArgument(this.namespace != null, "metricStream: missing namespace");
        checkArgument(NamespaceValidator.isValid(this.namespace), "metricStream: illegal CloudWatch namespace '%s'",
                this.namespace);
        checkArgument(this.metric != null, "metricStream: missing metric");

        checkArgument(this.statistic != null, "metricStream: missing statistic");
        checkArgument(this.period != null, "metricStream: missing period");
        checkArgument(this.period.getSeconds() >= 60, "metricStream: minimum period is 60 seconds");
        checkArgument(this.period.getSeconds() % 60 == 0, "metricStream: period must be a multiple of 60 seconds");

        checkArgument(getDataSettlingTime().getMillis() >= 0,
                "metricStream: dataSettlingTime must be a positive duration");
        checkArgument(getQueryChunkSize().getSeconds() > 0, "metricStream: queryChunkSize must be a positive duration");
        checkArgument(getQueryChunkSize().getSeconds() % 60 == 0,
                "metricStream: queryChunkSize must be a multiple of 60 seconds");
        checkArgument(getQueryChunkSize().getSeconds() <= this.period.getSeconds() * CLOUDWATCH_MAX_DATA_POINTS,
                "metricStream: queryChunkSize too long: The maximum number of data points returned from a single call is 1440 "
                        + "and the smallest query granularity is 1 minute. "
                        + "With the given queryChunkSize an attempt could be made to fetch %s data points.",
                getQueryChunkSize().getSeconds() / this.period.getSeconds());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.namespace, this.metric, this.statistic, this.period, this.convertToRate,
                this.dimensions, this.dataSettlingTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CloudWatchMetricStreamDefinition) {
            CloudWatchMetricStreamDefinition that = (CloudWatchMetricStreamDefinition) obj;
            return Objects.equals(this.id, that.id) && Objects.equals(this.namespace, that.namespace)//
                    && Objects.equals(this.metric, that.metric) //
                    && Objects.equals(this.statistic, that.statistic) //
                    && Objects.equals(this.period, that.period)//
                    && Objects.equals(this.convertToRate, that.convertToRate) //
                    && Objects.equals(this.dimensions, that.dimensions)//
                    && Objects.equals(this.dataSettlingTime, that.dataSettlingTime);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    /**
     * Creates a field-by-field copy of this object with the period field
     * substituted in the returned clone.
     *
     * @param period
     * @return
     */
    public CloudWatchMetricStreamDefinition withPeriod(TimeInterval period) {
        return new CloudWatchMetricStreamDefinition(this.id, this.namespace, this.metric, this.statistic, period,
                this.convertToRate, this.dimensions, this.dataSettlingTime, this.queryChunkSize);
    }

    /**
     * Creates a field-by-field copy of this object with the statistic field
     * substituted in the returned clone.
     *
     * @param statistic
     * @return
     */
    public CloudWatchMetricStreamDefinition withStatistic(CloudWatchStatistic statistic) {
        return new CloudWatchMetricStreamDefinition(this.id, this.namespace, this.metric, statistic, this.period,
                this.convertToRate, this.dimensions, this.dataSettlingTime, this.queryChunkSize);
    }

}
