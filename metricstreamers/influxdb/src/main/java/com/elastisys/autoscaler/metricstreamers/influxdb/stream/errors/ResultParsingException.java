package com.elastisys.autoscaler.metricstreamers.influxdb.stream.errors;

/**
 * Thrown to indicate a problem with parsing InfluxDB query results.
 */
public class ResultParsingException extends InfluxdbQueryException {

    public ResultParsingException() {
        super();
    }

    public ResultParsingException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ResultParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResultParsingException(String message) {
        super(message);
    }

    public ResultParsingException(Throwable cause) {
        super(cause);
    }

}
