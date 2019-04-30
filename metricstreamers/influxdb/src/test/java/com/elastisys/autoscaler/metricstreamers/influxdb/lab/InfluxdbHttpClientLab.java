package com.elastisys.autoscaler.metricstreamers.influxdb.lab;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.commons.net.http.Http;
import com.elastisys.scale.commons.net.http.HttpRequestResponse;
import com.elastisys.scale.commons.net.url.UrlUtils;

/**
 * Lab program for playing with the InfluxDB HTTP API. An InfluxDB server needs
 * to be set up separately.
 *
 */
public class InfluxdbHttpClientLab {
    private static Logger LOG = LoggerFactory.getLogger(InfluxdbHttpClientLab.class);

    private static String protocol = "http";
    /** InfluxDB host. */
    private static String host = "localhost";
    /** InfluxDB API port. */
    private static int port = 8086;

    public static void main(String[] args) throws HttpResponseException, IOException, URISyntaxException {
        Http http = Http.builder().build();

        String db = "mydb";
        String query = "SELECT MEAN(value) FROM cpu WHERE '2016-09-16T12:00:00.000Z' <= time AND time <= '2016-09-16T13:00:00.000Z' GROUP BY region, host";
        boolean pretty = true;

        String url = String.format("%s://%s:%d/query?pretty=%s&db=%s&q=%s", protocol, host, port, pretty, db, query);

        HttpRequestResponse response = http.execute(new HttpGet(UrlUtils.encodeHttpUrl(url)));
        LOG.debug("{}", response.getStatusCode());
        LOG.debug(response.getResponseBody());
    }
}
