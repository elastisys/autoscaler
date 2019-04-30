package com.elastisys.autoscaler.core.monitoring.metricstreamer.api;

/**
 * Thrown by a {@link MetricStream} to indicate a problem to execute a query.
 */
public class MetricStreamException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public MetricStreamException() {
        super();
    }

    public MetricStreamException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public MetricStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public MetricStreamException(String message) {
        super(message);
    }

    public MetricStreamException(Throwable cause) {
        super(cause);
    }

}
