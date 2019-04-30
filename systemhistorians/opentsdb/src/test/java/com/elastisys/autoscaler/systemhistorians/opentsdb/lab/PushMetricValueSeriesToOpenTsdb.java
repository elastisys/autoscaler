package com.elastisys.autoscaler.systemhistorians.opentsdb.lab;

import java.util.Map;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.systemhistorians.opentsdb.OpenTsdbInserter;
import com.elastisys.autoscaler.systemhistorians.opentsdb.OpenTsdbSocketInserter;
import com.elastisys.scale.commons.util.collection.Maps;

/**
 * A simple lab program that pushes an entire series of metric values (with a
 * given start-time, end-time, and data point spacing) to OpenTSDB over a TCP
 * socket.
 *
 *
 *
 */
public class PushMetricValueSeriesToOpenTsdb {
    static Logger logger = LoggerFactory.getLogger(PushMetricValueSeriesToOpenTsdb.class);

    // OpenTSDB server
    private static final String openTsdbHost = "<IP-ADRESS>";
    private static final int openTsdbPort = 4242;

    // Data points to be inserted
    private static final String metric = "my.new.metric";
    private static final DateTime startTime = DateTime.parse("2013-04-04T08:00:00+02:00");
    private static final DateTime endTime = DateTime.parse("2013-04-04T12:00:00+02:00");
    private static final int dataPointSpacingInSeconds = 10;

    public static void main(String[] args) {
        // Note: all data points are inserted and stored with timestamps
        // represented in seconds since epoch (UTC).
        DateTime t = startTime;
        while (t.isBefore(endTime)) {
            MetricValue nextDataPoint = newDataPoint(metric, t, t.getMillis());
            logger.info("Inserting data point: " + nextDataPoint);
            OpenTsdbInserter opentsdb = new OpenTsdbSocketInserter(logger, openTsdbHost, openTsdbPort);
            try {
                opentsdb.insert(nextDataPoint);
            } catch (Exception e) {
                logger.warn("Failed to insert data point");
            }
            t = t.plusSeconds(dataPointSpacingInSeconds);
        }
    }

    private static MetricValue newDataPoint(String metric, DateTime timestamp, double value) {
        // a data point needs to have at least on tag
        Map<String, String> tags = Maps.of("host", "localhost");
        MetricValue dataPoint = new MetricValue(metric, value, timestamp, tags);
        return dataPoint;
    }
}
