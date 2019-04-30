package com.elastisys.autoscaler.metricstreamers.influxdb.stream.errors;

/**
 * Trown to indicate that an InfluxDB query returned a field/column with
 * non-numerical data.
 */
public class NonNumericalValueException extends ResultParsingException {

    public NonNumericalValueException() {
        super();
    }

    public NonNumericalValueException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public NonNumericalValueException(String message, Throwable cause) {
        super(message, cause);
    }

    public NonNumericalValueException(String message) {
        super(message);
    }

    public NonNumericalValueException(Throwable cause) {
        super(cause);
    }

}
