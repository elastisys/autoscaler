package com.elastisys.autoscaler.metricstreamers.influxdb.stream.errors;

/**
 * Thrown to indicate a problem with querying InfluxDB.
 */
public class InfluxdbQueryException extends RuntimeException {

    public InfluxdbQueryException() {
        super();
    }

    public InfluxdbQueryException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InfluxdbQueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public InfluxdbQueryException(String message) {
        super(message);
    }

    public InfluxdbQueryException(Throwable cause) {
        super(cause);
    }

}
