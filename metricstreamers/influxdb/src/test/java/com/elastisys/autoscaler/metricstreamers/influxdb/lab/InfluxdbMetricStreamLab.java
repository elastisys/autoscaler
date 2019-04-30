package com.elastisys.autoscaler.metricstreamers.influxdb.lab;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.openjdk.jol.info.GraphLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.MetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.Query;
import com.elastisys.autoscaler.metricstreamers.influxdb.stream.InfluxdbMetricStream;
import com.elastisys.autoscaler.metricstreamers.influxdb.stream.MetricStreamConfig;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Lab program that can be used to experiment with running an
 * {@link InfluxdbMetricStream} against an InfluxDB server. The InfluxDB server
 * needs to be set up separately.
 */
public class InfluxdbMetricStreamLab {

    private static final Logger LOG = LoggerFactory.getLogger(InfluxdbMetricStreamLab.class);

    /** InfluxDB host. */
    private static String host = "localhost";
    /** InfluxDB API port. */
    private static int port = 8086;

    public static void main(String[] args) throws Exception {
        Query query = Query.builder().select("non_negative_derivative(max(value),1s)").from("request_count").where(null)
                .groupBy("time(1m) fill(none)").build();

        TimeInterval dataSettlingTime = null;
        TimeInterval queryChunkSize = new TimeInterval(30L, TimeUnit.DAYS);
        MetricStreamDefinition streamDef = new MetricStreamDefinition("request_rate", null, "mydb", query,
                dataSettlingTime, queryChunkSize);
        streamDef.validate();
        MetricStreamConfig config = new MetricStreamConfig(host, port, null, streamDef);
        InfluxdbMetricStream metricStream = new InfluxdbMetricStream(LOG, config);

        DateTime start = UtcTime.parse("2016-01-01T00:00:00.000Z");
        DateTime end = UtcTime.parse("2017-01-01T00:00:00.000Z");
        Interval interval = new Interval(start, end);

        StopWatch stopwatch = StopWatch.createStarted();

        // QueryOptions queryHints = new QueryOptions(
        // new Downsample(new TimeInterval(5L, TimeUnit.MINUTES),
        // DownsampleFunction.MEAN));
        QueryOptions queryHints = null;
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
                LOG.info("  total size in MB: {}", GraphLayout.parseInstance(values).totalSize() / 1024.0 / 1024.0);
            }
        }

        LOG.info("entire fetch took {} ms", stopwatch.getTime(TimeUnit.MILLISECONDS));
    }
}
