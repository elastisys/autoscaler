package com.elastisys.autoscaler.metricstreamers.opentsdb.stream;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamException;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.Downsample;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.impl.EmptyResultSet;
import com.elastisys.autoscaler.metricstreamers.opentsdb.client.OpenTsdbQueryClient;
import com.elastisys.autoscaler.metricstreamers.opentsdb.config.OpenTsdbMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.DownsamplingSpecification;
import com.elastisys.scale.commons.util.time.TimeUtils;

/**
 * A {@link MetricStream} that retrieves values from an OpenTSDB server.
 */
public class OpenTsdbMetricStream implements MetricStream {

    private final Logger logger;
    private final OpenTsdbQueryClient queryClient;
    private final MetricStreamConfig config;

    public OpenTsdbMetricStream(Logger logger, OpenTsdbQueryClient queryClient, MetricStreamConfig config) {
        this.logger = logger;
        this.queryClient = queryClient;
        this.config = config;
    }

    @Override
    public String getId() {
        return stream().getId();
    }

    @Override
    public String getMetric() {
        return stream().getMetric();
    }

    @Override
    public QueryResultSet query(Interval interval, QueryOptions options) throws MetricStreamException {
        OpenTsdbMetricStreamDefinition stream = stream();
        // skip query if requested interval is too recent, with respect to the
        // data settling time of the metric stream
        DateTime dataSettlingPoint = stream.getDataSettlingPoint();
        if (!interval.getStart().isBefore(dataSettlingPoint)) {
            this.logger.info(
                    "ignoring OpenTSDB metric retrieval: " + "requested time interval"
                            + " {} requests data more recent than the " + "stream's data settling point {}",
                    interval, dataSettlingPoint);
            return new EmptyResultSet();
        }
        interval = stream.adjustForDataSettlingTime(interval);

        // check query hints to see if custom downsampling was requested
        if (options != null && options.getDownsample().isPresent()) {
            Downsample customDownsampling = options.getDownsample().get();
            this.logger.debug("overriding downsampling with query hint: {}", customDownsampling);
            stream = stream.withDownsampling(
                    new DownsamplingSpecification(customDownsampling.getInterval(), customDownsampling.getFunction()));
        }

        // breaks query into chunks which are incrementally fetched in case of a
        // query spanning a long time-frame
        List<Interval> subQueryIntervals = TimeUtils.splitInterval(interval, queryChunkSize());
        List<QueryCall> subQueries = new ArrayList<>();
        for (Interval subQueryInterval : subQueryIntervals) {
            String queryUrl = buildQueryUrl(stream, subQueryInterval);
            this.logger.debug("preparing (sub)query: {}", queryUrl);
            subQueries.add(new QueryCall(this.queryClient, queryUrl, subQueryInterval));
        }
        return new LazyOpenTsdbResultSet(this.logger, subQueries);
    }

    /**
     * Builds a complete HTTP query URL to send to a OpenTSDB server for a given
     * {@link MetricStreamSubscription} and time {@link Interval}.
     *
     * @param stream
     *
     * @param subscription
     * @param interval
     * @param config
     * @return
     */
    private String buildQueryUrl(OpenTsdbMetricStreamDefinition stream, Interval interval) {
        String query = stream.makeQuery(interval);
        String queryUrl = "http://" + this.config.getHost() + ":" + this.config.getPort() + query;
        return queryUrl;
    }

    private Duration queryChunkSize() {
        return Duration.millis(this.config.getStreamDefinition().getQueryChunkSize().getMillis());
    }

    private OpenTsdbMetricStreamDefinition stream() {
        return this.config.getStreamDefinition();
    }

}
