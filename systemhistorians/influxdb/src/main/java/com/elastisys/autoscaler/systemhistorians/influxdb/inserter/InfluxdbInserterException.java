package com.elastisys.autoscaler.systemhistorians.influxdb.inserter;

/**
 * Thrown by an {@link InfluxdbInserter} to indicate a problem to write data.
 *
 * @see InfluxdbInserter
 */
public class InfluxdbInserterException extends RuntimeException {

    public InfluxdbInserterException() {
        super();
    }

    public InfluxdbInserterException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InfluxdbInserterException(String message, Throwable cause) {
        super(message, cause);
    }

    public InfluxdbInserterException(String message) {
        super(message);
    }

    public InfluxdbInserterException(Throwable cause) {
        super(cause);
    }

}
