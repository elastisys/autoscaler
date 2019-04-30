package com.elastisys.autoscaler.metricstreamers.influxdb.stream;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamException;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.impl.EmptyResultSet;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.MetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.SecurityConfig;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.http.Http;
import com.elastisys.scale.commons.net.http.HttpBuilder;
import com.elastisys.scale.commons.net.url.UrlUtils;
import com.elastisys.scale.commons.util.time.TimeUtils;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * A {@link MetricStream} that retrieves values from an InfluxDB server.
 */
public class InfluxdbMetricStream implements MetricStream {

    private final Logger logger;
    private final MetricStreamConfig config;

    public InfluxdbMetricStream(Logger logger, MetricStreamConfig config) {
        this.logger = logger;
        this.config = config;
    }

    @Override
    public String getId() {
        return stream().getId();
    }

    @Override
    public String getMetric() {
        return stream().getMetricName();
    }

    @Override
    public QueryResultSet query(Interval interval, QueryOptions options) throws MetricStreamException {
        MetricStreamDefinition streamDef = stream();

        // make sure we don't request too recent (unsettled) data
        TimeInterval dataSettlingTime = streamDef.getDataSettlingTime();
        if (dataSettlingTime != null) {
            DateTime dataSettledPoint = UtcTime.now().minus(dataSettlingTime.getMillis());
            if (interval.isAfter(dataSettledPoint)) {
                this.logger.info("ignoring influxdb metric retrieval: requested time interval {} requests data "
                        + "more recent than the stream's data settling point {}", interval, dataSettledPoint);
                return new EmptyResultSet();
            }
            // adjust end of interval to not request too recent (unsettled) data
            if (interval.getEnd().isAfter(dataSettledPoint)) {
                this.logger.debug("adjusting query interval end to not exceed data settling point (%s)",
                        dataSettledPoint);
                interval = new Interval(interval.getStart(), dataSettledPoint);
            }
        }

        // no support for custom downsampling
        if (options != null && options.getDownsample().isPresent()) {
            this.logger.info("ignoring query hint: {}", options.getDownsample().get());
        }

        Http httpClient = prepareHttpClient();
        // breaks query into chunks which are incrementally fetched in case of a
        // query spanning a long time-frame
        List<Interval> subQueryIntervals = TimeUtils.splitInterval(interval, queryChunkSize());
        List<QueryCall> subQueries = new ArrayList<>();
        for (Interval subQueryInterval : subQueryIntervals) {
            String query = InfluxdbQueryBuilder.buildQuery(streamDef, subQueryInterval);
            this.logger.debug("preparing (sub)query: {}", query);
            subQueries.add(new QueryCall(this.logger, httpClient, queryUrl(query), streamDef.getMetricName()));
        }
        return new LazyInfluxdbResultSet(this.logger, subQueries);
    }

    private URI queryUrl(String query) throws MetricStreamException {
        boolean pretty = false;
        String url = String.format("%s?pretty=%s&db=%s&q=%s", queryUrl(this.config), pretty, stream().getDatabase(),
                query);
        try {
            return UrlUtils.encodeHttpUrl(url);
        } catch (Exception e) {
            throw new MetricStreamException("invalid influxdb query URL: " + e.getMessage(), e);
        }
    }

    /**
     * Prepares a {@link Http} client for connecting to InfluxDB according to
     * the settings in a configuration.
     *
     * @return
     */
    private Http prepareHttpClient() {
        // generate http client
        HttpBuilder httpBuilder = Http.builder();
        if (this.config.getSecurity().isPresent()) {
            SecurityConfig security = this.config.getSecurity().get();
            if (security.useHttps()) {
                if (security.getAuth() != null) {
                    this.logger.debug("setting basic auth credentials");
                    httpBuilder.clientBasicAuth(security.getAuth());
                }
                this.logger.debug("verify host cert: {}", security.shouldVerifyCert());
                httpBuilder.verifyHostCert(security.shouldVerifyCert());
                this.logger.debug("verify host name: {}", security.shouldVerifyHost());
                httpBuilder.verifyHostname(security.shouldVerifyHost());
            }
        }
        Http http = httpBuilder.build();
        return http;
    }

    /**
     * Creates a base URL to connect to the targeted InfluxDB query endpoint.
     *
     * @param config
     * @return
     */
    private String queryUrl(MetricStreamConfig config) {
        String protocol = "http";
        if (config.getSecurity().isPresent()) {
            protocol = config.getSecurity().get().useHttps() ? "https" : "http";
        }
        String url = String.format("%s://%s:%d/query", protocol, config.getHost(), config.getPort());
        return url;
    }

    private MetricStreamDefinition stream() {
        return this.config.getStreamDefinition();
    }

    private Duration queryChunkSize() {
        return Duration.millis(this.config.getStreamDefinition().getQueryChunkSize().getMillis());
    }
}
