package com.elastisys.autoscaler.metricstreamers.opentsdb.lab;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.metricstreamers.opentsdb.client.OpenTsdbQueryClient;
import com.elastisys.autoscaler.metricstreamers.opentsdb.client.impl.OpenTsdbHttpQueryClient;

/**
 * A simple lab program for posing raw HTTP questions against an OpenTSDB
 * server.
 */
public class OpenTsdbRawHttpClient {
    private final static Logger logger = LoggerFactory.getLogger(OpenTsdbRawHttpClient.class);

    public static void main(String[] args) throws Exception {
        String timezone = "UTC";
        String metric = "my.new.metric";
        boolean asRate = false;
        String rate = asRate ? ":rate" : "";

        String queryUrl = "http://<IP-ADRRESS>:4242/q?" + "tz=" + timezone + "&" + "start=2013/04/04-06:00:00:00&"
                + "end=2013/04/04-06:10:00&" + "m=sum" + rate + ":" + metric + "{host=*}&ascii&nocache";
        OpenTsdbQueryClient queryClient = new OpenTsdbHttpQueryClient(logger);
        List<MetricValue> values = queryClient.query(queryUrl);
        logger.debug(values.size() + " value(s)");
        if (!values.isEmpty()) {
            logger.debug("  First: " + values.get(0));
            logger.debug("  Last: " + values.get(values.size() - 1));
        }
        // for (MetricValue value : values) {
        // logger.debug(" " + value);
        // }
    }
}
