package com.elastisys.autoscaler.systemhistorians.influxdb.inserter.impl;

import static java.lang.String.format;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.systemhistorians.influxdb.config.InfluxdbSecurityConfig;
import com.elastisys.autoscaler.systemhistorians.influxdb.config.InfluxdbSystemHistorianConfig;
import com.elastisys.autoscaler.systemhistorians.influxdb.inserter.InfluxdbInserter;
import com.elastisys.autoscaler.systemhistorians.influxdb.inserter.InfluxdbInserterException;
import com.elastisys.autoscaler.systemhistorians.influxdb.serializer.InfluxdbDataPointSerializer;
import com.elastisys.scale.commons.net.http.Http;
import com.elastisys.scale.commons.net.http.HttpBuilder;
import com.elastisys.scale.commons.net.http.HttpBuilderException;
import com.elastisys.scale.commons.net.url.UrlUtils;

/**
 * An {@link InfluxdbInserter} that operates over the
 * <a href="https://docs.influxdata.com/influxdb/v1.0/guides/writing_data/">HTTP
 * API</a>.
 *
 */
public class HttpInfluxdbInserter implements InfluxdbInserter {
    /** A logger instance. */
    private final Logger logger;
    /** Config that describes how to report to InfluxDB. */
    private final InfluxdbSystemHistorianConfig config;

    public HttpInfluxdbInserter(Logger logger, InfluxdbSystemHistorianConfig config) {
        this.logger = logger;
        this.config = config;
    }

    @Override
    public void insert(Collection<MetricValue> values) throws InfluxdbInserterException {

        try {
            Http client = prepareHttpClient();

            createDatabaseIfNotExists(client, this.config.getDatabase());
            writeDatapoints(client, values);
        } catch (Exception e) {
            throw new InfluxdbInserterException("failed to insert datapoints: " + e.getMessage(), e);
        }
    }

    private void writeDatapoints(Http client, Collection<MetricValue> values) {
        this.logger.debug("inserting {} datapoint(s) ...", values.size());

        try {
            List<String> serializedPoints = values.stream().map(new InfluxdbDataPointSerializer())
                    .collect(Collectors.toList());
            String batch = String.join("\n", serializedPoints);

            String url = format("%s?db=%s", writeUrl(), this.config.getDatabase());
            HttpPost post = new HttpPost(url);
            post.setEntity(new StringEntity(batch, ContentType.TEXT_PLAIN));
            client.execute(post);
        } catch (HttpResponseException e) {
            throw new InfluxdbInserterException(
                    format("system historian: failed to write: influxdb server responded with %d status code:\n%s",
                            e.getStatusCode(), e.getMessage()),
                    e);
        } catch (Exception e) {
            throw new InfluxdbInserterException(format("system historian: failed to write: %s", e.getMessage()), e);
        }
    }

    /**
     * Creates the system historian InfluxDB database if it doesn't already
     * exist.
     *
     * @param client
     * @param database
     * @throws InfluxdbInserterException
     */
    private void createDatabaseIfNotExists(Http client, String database) throws InfluxdbInserterException {
        try {
            // database creation is idempotent
            String createDbStmt = format("CREATE DATABASE \"%s\"", database);
            String createDbUrl = format("%s?q=%s", queryUrl(), createDbStmt);
            this.logger.debug("creating database (if it doesn't already exist): {}", createDbUrl);
            client.execute(new HttpPost(UrlUtils.encodeHttpUrl(createDbUrl)));
        } catch (HttpResponseException e) {
            throw new InfluxdbInserterException(format(
                    "system historian: failed to create database: influxdb server responded with %d status code:\n%s",
                    e.getStatusCode(), e.getMessage()), e);
        } catch (Exception e) {
            throw new InfluxdbInserterException(
                    format("system historian: failed to create database %s: %s", database, e.getMessage()), e);
        }
    }

    /**
     * Prepares a {@link Http} client for connecting to InfluxDB according to
     * the settings in a configuration.
     *
     * @param config
     * @return
     */
    private Http prepareHttpClient() throws HttpBuilderException {
        // generate http client
        HttpBuilder httpBuilder = Http.builder();
        if (this.config.getSecurity().isPresent()) {
            InfluxdbSecurityConfig security = this.config.getSecurity().get();
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
     * Returns the URL of the InfluxDB query endpoint
     * ({@code http(s)://<host>:<port>/query}.
     *
     * @return
     */
    String queryUrl() {
        return format("%s/%s", baseUrl(), "query");
    }

    /**
     * Returns the URL of the InfluxDB write endpoint
     * ({@code http(s)://<host>:<port>/write}.
     *
     * @return
     */
    String writeUrl() {
        return format("%s/%s", baseUrl(), "write");
    }

    /**
     * Returns the base URL of the InfluxDB server
     * ({@code http(s)://<host>:<port>}.
     *
     * @return
     */
    String baseUrl() {
        String protocol = "http";
        if (this.config.getSecurity() != null && this.config.getSecurity().isPresent()) {
            protocol = this.config.getSecurity().get().useHttps() ? "https" : "http";
        }
        return format("%s://%s:%d", protocol, this.config.getHost(), this.config.getPort());
    }

}
