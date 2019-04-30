package com.elastisys.autoscaler.metricstreamers.cloudwatch.lab;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.openjdk.jol.info.GraphLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.Downsample;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.DownsampleFunction;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchStatistic;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.stream.CloudWatchMetricStream;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.stream.MetricStreamConfig;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Lab program that runs queries against an OpenTSDB server via a
 * {@link CloudWatchMetricStream}.
 */
public class CloudWatchMetricStreamLab {

    static final Logger LOG = LoggerFactory.getLogger(CloudWatchMetricStreamLab.class);

    // TODO: set to region where your CloudWatch endpoint is hosted
    private static final String region = System.getenv("AWS_DEFAULT_REGION");
    private static final String awsAccessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String awsSecretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");

    // TODO: define stream
    private static final String id = "requests.stream";
    private static final String namespace = "AWS/ELB";
    private static final String metric = "RequestCount";
    private static final CloudWatchStatistic statistic = CloudWatchStatistic.Sum;
    private static final TimeInterval period = new TimeInterval(5L, TimeUnit.MINUTES);
    private static final boolean convertToRate = false;
    private static final Map<String, String> dimensions = Maps.of("LoadBalancerName",
            "a36d97c25a7f711e696f60251ceab1c6");
    private static final TimeInterval dataSettlingTime = TimeInterval.seconds(60);

    private static final TimeInterval queryChunkSize = new TimeInterval(1L, TimeUnit.DAYS);

    public static void main(String[] args) throws Exception {
        CloudWatchMetricStreamDefinition metricStreamDefinition = new CloudWatchMetricStreamDefinition(id, namespace,
                metric, statistic, period, convertToRate, dimensions, dataSettlingTime, queryChunkSize);
        metricStreamDefinition.validate();

        CloudWatchMetricStream stream = new CloudWatchMetricStream(LOG,
                new MetricStreamConfig(awsAccessKeyId, awsSecretAccessKey, region, metricStreamDefinition));

        DateTime start = UtcTime.parse("2017-01-22T00:00:00.000Z");
        DateTime end = UtcTime.parse("2017-01-23T00:00:00.000Z");
        Interval interval = new Interval(start, end);

        StopWatch stopwatch = StopWatch.createStarted();
        QueryOptions queryHints = new QueryOptions(
                new Downsample(new TimeInterval(10L, TimeUnit.MINUTES), DownsampleFunction.SUM));
        // QueryOptions queryHints = null;
        QueryResultSet resultSet = stream.query(interval, queryHints);
        int chunk = 0;
        while (resultSet.hasNext()) {
            chunk++;
            long fetchStartMillis = stopwatch.getTime(TimeUnit.MILLISECONDS);
            List<MetricValue> values = resultSet.fetchNext().getMetricValues();
            long fetchEndMillis = stopwatch.getTime(TimeUnit.MILLISECONDS);
            LOG.info("chunk {}: fetched {} values in {} ms", chunk, values.size(), fetchEndMillis - fetchStartMillis);
            if (!values.isEmpty()) {
                LOG.info("  first value: {}", values.get(0));
                LOG.info("  last value: {}", values.get(values.size() - 1));
                LOG.info(" total size in MB: {}", GraphLayout.parseInstance(values).totalSize() / 1024.0 / 1024.0);
            }
        }

        LOG.info("entire fetch took {} ms", stopwatch.getTime(TimeUnit.MILLISECONDS));
    }

}
