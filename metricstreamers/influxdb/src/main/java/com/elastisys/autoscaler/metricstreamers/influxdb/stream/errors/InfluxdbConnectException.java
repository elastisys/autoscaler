package com.elastisys.autoscaler.metricstreamers.influxdb.stream.errors;

/**
 * Thrown to indicate a problem to connect with an InfluxDB server.
 */
public class InfluxdbConnectException extends InfluxdbQueryException {

    public InfluxdbConnectException() {
        super();
    }

    public InfluxdbConnectException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InfluxdbConnectException(String message, Throwable cause) {
        super(message, cause);
    }

    public InfluxdbConnectException(String message) {
        super(message);
    }

    public InfluxdbConnectException(Throwable cause) {
        super(cause);
    }

}
