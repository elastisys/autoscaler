package com.elastisys.autoscaler.metricstreamers.ceilometer.lab;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.openjdk.jol.info.GraphLayout;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerFunction;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.Downsampling;
import com.elastisys.autoscaler.metricstreamers.ceilometer.stream.CeilometerMetricStream;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.openstack.ApiAccessConfig;
import com.elastisys.scale.commons.openstack.AuthConfig;
import com.elastisys.scale.commons.openstack.AuthV2Credentials;
import com.elastisys.scale.commons.openstack.OSClientFactory;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.common.base.Stopwatch;

/**
 * Lab program that can be used to experiment with running an
 * {@link CeilometerMetricStream} against an OpenStack Ceilometer endpoint.
 */
public class CeilometerMetricStreamLab {

    private static final Logger LOG = LoggerFactory.getLogger(CeilometerMetricStreamLab.class);

    private static final AuthConfig openstackAuth = new AuthConfig(System.getenv("OS_AUTH_URL"), //
            new AuthV2Credentials(System.getenv("OS_TENANT_NAME"), System.getenv("OS_USERNAME"),
                    System.getenv("OS_PASSWORD")),
            null);
    private static String region = "RegionOne";

    private static final String streamId = "requestrate.stream";
    private static final String meter = "network.services.lb.total.connections.rate";
    private static final String resourceId = null;
    private static final Downsampling downsampling = new Downsampling(CeilometerFunction.Average,
            TimeInterval.seconds(3600));
    private static final boolean rateConversion = false;
    private static final TimeInterval dataSettlingTime = null;
    private static final TimeInterval queryChunkSize = new TimeInterval(30L, TimeUnit.DAYS);

    public static void main(String[] args) throws Exception {
        OSFactory.enableHttpLoggingFilter(true);

        CeilometerMetricStreamDefinition streamDefinition = new CeilometerMetricStreamDefinition(streamId, meter,
                resourceId, downsampling, rateConversion, dataSettlingTime, queryChunkSize);
        streamDefinition.validate();
        MetricStream metricStream = new CeilometerMetricStream(LOG,
                new OSClientFactory(new ApiAccessConfig(openstackAuth, region)), streamDefinition);

        DateTime start = UtcTime.parse("2017-01-27T00:00:00.000Z");
        DateTime end = UtcTime.now();
        Interval interval = new Interval(start, end);

        Stopwatch stopwatch = Stopwatch.createStarted();

        // TODO: downsampling?
        // QueryOptions queryHints = new QueryOptions(
        // new Downsample(new TimeInterval(5L, TimeUnit.MINUTES),
        // DownsampleFunction.MEAN));
        QueryOptions queryHints = null;
        QueryResultSet resultSet = metricStream.query(interval, queryHints);
        int chunk = 0;
        while (resultSet.hasNext()) {
            chunk++;
            long fetchStartMillis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            List<MetricValue> values = resultSet.fetchNext().getMetricValues();
            long fetchEndMillis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            if (!values.isEmpty()) {
                LOG.info("chunk {}: fetched {} values in {} ms", chunk, values.size(),
                        fetchEndMillis - fetchStartMillis);
                LOG.info("  first value: {}", values.get(0));
                LOG.info("  last value: {}", values.get(values.size() - 1));
                LOG.info("  total size in MB: {}", GraphLayout.parseInstance(values).totalSize() / 1024.0 / 1024.0);
                for (MetricValue value : values) {
                    LOG.debug("value: {}", value);
                }

            }
        }

        LOG.info("entire fetch took {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
}
