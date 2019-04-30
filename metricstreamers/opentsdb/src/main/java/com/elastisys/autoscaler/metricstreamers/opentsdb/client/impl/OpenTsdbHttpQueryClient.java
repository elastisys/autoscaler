package com.elastisys.autoscaler.metricstreamers.opentsdb.client.impl;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.metricstreamers.opentsdb.client.OpenTsdbQueryClient;
import com.elastisys.autoscaler.metricstreamers.opentsdb.parser.OpenTsdbMetricValueParser;
import com.elastisys.scale.commons.net.http.Http;
import com.elastisys.scale.commons.net.http.HttpRequestResponse;
import com.elastisys.scale.commons.net.url.UrlUtils;

/**
 * A OpenTSDB query client that can be used to pose queries against an OpenTSDB
 * server according to the <a href="http://opentsdb.net/http-api.html">OpenTSDB
 * HTTP API</a>.
 * <p/>
 * The {@link OpenTsdbHttpQueryClient} allows "raw" queries to be posed against
 * OpenTSDB.
 */
public class OpenTsdbHttpQueryClient implements OpenTsdbQueryClient {

    /** A {@link Logger} that log output will be written to. */
    private final Logger logger;

    /**
     * Constructs a {@link OpenTsdbHttpQueryClient} that uses a default
     * {@link Logger} instance.
     *
     */
    public OpenTsdbHttpQueryClient() {
        this(LoggerFactory.getLogger(OpenTsdbHttpQueryClient.class));
    }

    /**
     * Constructs a {@link OpenTsdbHttpQueryClient} with a specified
     * {@link Logger} instance.
     *
     * @param logger
     *            A {@link Logger} that log output will be written to.
     */
    public OpenTsdbHttpQueryClient(Logger logger) {
        this.logger = logger;
    }

    @Override
    public List<MetricValue> query(String queryUrl) throws Exception {
        this.logger.debug("Query URL: " + queryUrl);
        URI encodedUrl = UrlUtils.encodeHttpUrl(queryUrl);
        this.logger.trace("Encoded query URL: " + encodedUrl);

        String response = doGet(encodedUrl.toString());
        List<MetricValue> metricValues = parseMetricValues(response);
        Collections.sort(metricValues);
        return metricValues;
    }

    private String doGet(final String queryUrl) throws Exception {
        HttpRequestResponse response = Http.builder().build().execute(new HttpGet(queryUrl));
        // check status code on response (should be 200)
        int statusCode = response.getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            throw new HttpResponseException(statusCode,
                    String.format("OpenTSDB query '%s' failed with status code: %d", queryUrl, statusCode));
        }

        return response.getResponseBody();
    }

    private List<MetricValue> parseMetricValues(String responseMessage) throws IllegalStateException, IOException {
        Objects.requireNonNull(responseMessage, "Query response message cannot be null");

        List<MetricValue> values = new ArrayList<>();
        String[] lines = responseMessage.split("\n");
        for (String line : lines) {
            if (line.trim().equals("")) {
                // ignore empty lines
                continue;
            }
            values.add(OpenTsdbMetricValueParser.parseMetricValue(line));
        }
        return values;
    }
}
