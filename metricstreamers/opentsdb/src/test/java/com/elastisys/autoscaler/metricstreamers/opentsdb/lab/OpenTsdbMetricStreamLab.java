package com.elastisys.autoscaler.metricstreamers.opentsdb.lab;

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
import com.elastisys.autoscaler.metricstreamers.opentsdb.client.impl.OpenTsdbHttpQueryClient;
import com.elastisys.autoscaler.metricstreamers.opentsdb.config.OpenTsdbMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.DownsamplingSpecification;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.MetricAggregator;
import com.elastisys.autoscaler.metricstreamers.opentsdb.stream.MetricStreamConfig;
import com.elastisys.autoscaler.metricstreamers.opentsdb.stream.OpenTsdbMetricStream;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Lab program that runs queries against an OpenTSDB server via a
 * {@link OpenTsdbMetricStream}.
 */
public class OpenTsdbMetricStreamLab {

    static final Logger LOG = LoggerFactory.getLogger(OpenTsdbMetricStreamLab.class);

    private static final String openTsdbHost = "localhost";
    private static final int openTsdbPort = 4242;

    private static final boolean convertToRate = false;
    private static final DownsamplingSpecification downsampling = new DownsamplingSpecification(
            new TimeInterval(5L, TimeUnit.MINUTES), DownsampleFunction.MEAN);

    private static final Map<String, List<String>> tags = Maps.of();
    private static final TimeInterval dataSettlingTime = new TimeInterval(30L, TimeUnit.SECONDS);
    private static final TimeInterval queryChunkSize = new TimeInterval(30L, TimeUnit.DAYS);

    public static void main(String[] args) throws Exception {
        OpenTsdbMetricStreamDefinition streamDef = new OpenTsdbMetricStreamDefinition("request_count.stream",
                "request_count", MetricAggregator.SUM, convertToRate, downsampling, tags, dataSettlingTime,
                queryChunkSize);
        MetricStreamConfig config = new MetricStreamConfig(openTsdbHost, openTsdbPort, streamDef);

        OpenTsdbMetricStream metricStream = new OpenTsdbMetricStream(LOG, new OpenTsdbHttpQueryClient(LOG), config);

        DateTime start = UtcTime.parse("2016-01-01T00:00:00.000Z");
        DateTime end = UtcTime.parse("2017-01-01T00:00:00.000Z");
        Interval interval = new Interval(start, end);

        StopWatch stopwatch = StopWatch.createStarted();
        QueryOptions queryHints = new QueryOptions(
                new Downsample(new TimeInterval(10L, TimeUnit.MINUTES), DownsampleFunction.MEAN));
        // QueryOptions queryHints = null;
        QueryResultSet resultSet = metricStream.query(interval, queryHints);
        int chunk = 0;
        while (resultSet.hasNext()) {
            chunk++;
            long fetchStartMillis = stopwatch.getTime(TimeUnit.MILLISECONDS);
            List<MetricValue> values = resultSet.fetchNext().getMetricValues();
            long fetchEndMillis = stopwatch.getTime(TimeUnit.MILLISECONDS);
            if (!values.isEmpty()) {
                LOG.info("chunk {}: fetched {} values in {} ms", chunk, values.size(),
                        fetchEndMillis - fetchStartMillis);
                LOG.info("  first value: {}", values.get(0));
                LOG.info("  last value: {}", values.get(values.size() - 1));
                LOG.info(" total size in MB: {}", GraphLayout.parseInstance(values).totalSize() / 1024.0 / 1024.0);
            }
        }

        LOG.info("entire fetch took {} ms", stopwatch.getTime(TimeUnit.MILLISECONDS));
    }

}
