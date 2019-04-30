package com.elastisys.autoscaler.metricstreamers.cloudwatch.tasks;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.joda.time.Interval;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.Statistic;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchStatistic;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * A {@link Callable} task that, when executed, requests statistics for a
 * particular metric to be fetched from AWS CloudWatch in a given region.
 * <p/>
 * The returned {@link GetMetricStatisticsResponse} object stores the query
 * metric in its {@code label} field and will store the list of
 * {@link Datapoint}s sorted in increasing order of time stamp (oldest first).
 */
public class GetMetricStatisticsTask implements Callable<GetMetricStatisticsResult> {

    /** AWS access key id for the account to be used. */
    private final String awsAccessKeyId;
    /** AWS secret access key for the account to be used. */
    private final String awsSecretAccessKey;
    /** The AWS region that the request will be sent to. */
    private final String region;

    /**
     * The Amazon CloudWatch namespace of the metric to fetch.
     * <p/>
     * See <a href=
     * "http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/cloudwatch_concepts.html"
     * >CloudWatch concepts</a> for an explanation.
     */
    private final String namespace;

    /** The CloudWatch metric that to retrieve data points for. */
    private final String metric;

    /**
     * The aggregation methods to apply to metric values. Can be any of "Sum",
     * "Average", "Minimum", "Maximum" and "SampleCount". The returned
     * {@link Datapoint}s will contain one value for each specified statistic.
     */
    private final List<CloudWatchStatistic> statistics;

    /**
     * The granularity of returned metric values (in seconds). The
     * {@code statistics} function will aggregate metric values with this level
     * of granularity. Must be at least {@code 60} seconds and must be a
     * multiple of {@code 60}.
     */
    private final TimeInterval period;

    /**
     * The dimensions (key-value pairs) used to narrow down the result set. Only
     * data points matching the specified dimensions will be returned.
     * <p/>
     * See <a href=
     * "http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/cloudwatch_concepts.html"
     * >CloudWatch concepts</a> for an explanation. May be <code>null</code>.
     */
    private final Map<String, String> dimensions;

    /**
     * The time interval for the query. Only {@link Datapoint}s whose time stamp
     * lies within this interval will be returned.
     */
    private final Interval queryInterval;

    /**
     * Constructs a new {@link GetMetricStatisticsTask} request.
     *
     * @param awsAccessKeyId
     *            AWS access key id for the account to be used.
     * @param awsSecretAccessKey
     *            AWS secret access key for the account to be used.
     * @param region
     *            The AWS region that the request will be sent to.
     * @param namespace
     *            The Amazon CloudWatch namespace of the metric to fetch.
     * @param metric
     *            The CloudWatch metric that to retrieve data points for.
     * @param period
     *            The granularity of returned metric values (in seconds). The
     *            {@code statistics} function will aggregate metric values with
     *            this level of granularity. Must be at least {@code 60} seconds
     *            and must be a multiple of {@code 60}.
     * @param dimensions
     *            The dimensions (key-value pairs) used to narrow down the
     *            result set. Only data points matching the specified dimensions
     *            will be returned.
     * @param queryInterval
     *            The time interval for the query. Only {@link Datapoint}s whose
     *            time stamp lies within this interval will be returned. May be
     *            <code>null</code>.
     * @param statistic
     *            The aggregation method(s) to apply to metric values. Can be
     *            any of "Sum", "Average", "Minimum", "Maximum" and
     *            "SampleCount". The returned {@link Datapoint}s will contain
     *            one value for each specified statistic.
     */
    public GetMetricStatisticsTask(String awsAccessKeyId, String awsSecretAccessKey, String region, String namespace,
            String metric, List<CloudWatchStatistic> statistics, TimeInterval period, Map<String, String> dimensions,
            Interval queryInterval) {
        this.awsAccessKeyId = awsAccessKeyId;
        this.awsSecretAccessKey = awsSecretAccessKey;
        this.region = region;
        this.namespace = namespace;
        this.metric = metric;
        this.statistics = statistics;
        this.period = period;
        this.dimensions = Optional.ofNullable(dimensions).orElse(Collections.emptyMap());
        this.queryInterval = queryInterval;
    }

    @Override
    public GetMetricStatisticsResult call() throws Exception {

        AmazonCloudWatchClient cloudWatchApi = new AmazonCloudWatchClient(
                new BasicAWSCredentials(this.awsAccessKeyId, this.awsSecretAccessKey));
        cloudWatchApi.setRegion(RegionUtils.getRegion(this.region));
        GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()//
                .withNamespace(this.namespace) //
                .withMetricName(this.metric) //
                .withDimensions(toDimensionSet(this.dimensions)) //
                .withStatistics(toStatistics(this.statistics)) //
                .withPeriod((int) this.period.getSeconds()) //
                .withStartTime(this.queryInterval.getStart().toDate()) //
                .withEndTime(this.queryInterval.getEnd().toDate());

        GetMetricStatisticsResult response = cloudWatchApi.getMetricStatistics(request);
        // sort data points by increasing time
        List<Datapoint> sorted = response.getDatapoints().stream()
                .sorted((p1, p2) -> p1.getTimestamp().compareTo(p2.getTimestamp())).collect(Collectors.toList());
        response.setDatapoints(sorted);
        return response;
    }

    private Statistic[] toStatistics(List<CloudWatchStatistic> statistics) {
        Statistic[] array = new Statistic[statistics.size()];
        for (int i = 0; i < statistics.size(); i++) {
            array[i] = statistics.get(i).toStatistic();
        }
        return array;
    }

    private Set<Dimension> toDimensionSet(Map<String, String> dimensions) {
        Set<Dimension> dimensionSet = new HashSet<>();
        for (Entry<String, String> dimension : dimensions.entrySet()) {
            dimensionSet.add(new Dimension().withName(dimension.getKey()).withValue(dimension.getValue()));
        }
        return dimensionSet;
    }
}
