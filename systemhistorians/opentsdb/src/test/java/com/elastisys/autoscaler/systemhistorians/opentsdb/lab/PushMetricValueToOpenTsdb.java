package com.elastisys.autoscaler.systemhistorians.opentsdb.lab;

import java.util.Map;
import java.util.Scanner;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.systemhistorians.opentsdb.OpenTsdbException;
import com.elastisys.autoscaler.systemhistorians.opentsdb.OpenTsdbInserter;
import com.elastisys.autoscaler.systemhistorians.opentsdb.OpenTsdbSocketInserter;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * A simple lab program that reads metric values from {@code stdin} and pushes
 * them to OpenTSDB over a TCP socket.
 */
public class PushMetricValueToOpenTsdb {
    static Logger logger = LoggerFactory.getLogger(PushMetricValueToOpenTsdb.class);

    public static void main(String[] args) {
        String openTsdbHost = "<IP-ADDRESS>";
        int openTsdbPort = 4242;
        String metric = "my.metric";

        prompt(metric);
        Scanner stdin = new Scanner(System.in);
        while (stdin.hasNext()) {
            MetricValue nextDataPoint = newDataPoint(metric, stdin.nextDouble());
            logger.info("Inserting data point: " + nextDataPoint);
            OpenTsdbInserter inserter = new OpenTsdbSocketInserter(logger, openTsdbHost, openTsdbPort);
            try {
                inserter.insert(nextDataPoint);
                logger.info("Successfully inserted value.");
            } catch (OpenTsdbException e) {
                logger.error("Failed to insert value: " + e.getMessage(), e);
            }
            prompt(metric);
        }
        stdin.close();
    }

    private static void prompt(String metric) {
        System.err.printf("Enter next value for metric '%s' (CTRL-D to exit): ", metric);
    }

    private static MetricValue newDataPoint(String metric, double value) {
        DateTime timestamp = UtcTime.now();
        // a data point needs to have at least on tag
        Map<String, String> tags = Maps.of("key", "value");
        // Map<String, String> tags = ImmutableMap.of();
        MetricValue dataPoint = new MetricValue(metric, value, timestamp, tags);
        return dataPoint;
    }
}
