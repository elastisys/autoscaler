package com.elastisys.autoscaler.metricstreamers.influxdb.stream.errors;

/**
 * Thrown to indicate that a query resulted in an error response, for example
 * due to the query being malformed (the details are in the message field of the
 * exception).
 *
 */
public class QueryErrorException extends InfluxdbQueryException {

    public QueryErrorException() {
        super();
    }

    public QueryErrorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public QueryErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryErrorException(String message) {
        super(message);
    }

    public QueryErrorException(Throwable cause) {
        super(cause);
    }

}
