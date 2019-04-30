package com.elastisys.autoscaler.metricstreamers.influxdb.stream;

import static java.lang.String.format;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.metricstreamers.influxdb.resultset.ResultSet;
import com.elastisys.autoscaler.metricstreamers.influxdb.stream.errors.InfluxdbConnectException;
import com.elastisys.scale.commons.net.http.Http;
import com.elastisys.scale.commons.net.http.HttpRequestResponse;

/**
 * Executes a single remote query against a particular InfluxDB server.
 */
public class QueryCall implements Callable<List<MetricValue>> {
    private final Logger logger;
    private final Http httpClient;
    private final URI queryUrl;
    private final String metricName;

    public QueryCall(Logger logger, Http httpClient, URI queryUrl, String metricName) {
        this.logger = logger;
        this.httpClient = httpClient;
        this.queryUrl = queryUrl;
        this.metricName = metricName;
    }

    @Override
    public List<MetricValue> call() throws Exception {
        HttpRequestResponse response;
        try {
            this.logger.debug("sending query: {}", this.queryUrl, null);
            response = this.httpClient.execute(new HttpGet(this.queryUrl));
            if (this.logger.isTraceEnabled()) {
                this.logger.trace("response: {}", response.getResponseBody());
            }
        } catch (HttpResponseException e) {
            throw new InfluxdbConnectException(
                    format("influxdb server responded with %d status code:\n%s", e.getStatusCode(), e.getMessage()), e);
        } catch (IOException e) {
            throw new InfluxdbConnectException("failed to send query to influxdb: " + e.getMessage(), e);
        }

        // parse server response (json -> MetricValue)
        ResultSet resultSet = ResultSet.Parser.parse(response.getResponseBody());
        return new ResultSetConverter(this.metricName).toMetricValues(resultSet);
    }

}
