package com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query;

/**
 * Thrown by a {@link QueryResultSet} on failure to fetch a
 * {@link QueryResultPage}.
 */
public class PageFetchException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PageFetchException() {
        super();
    }

    public PageFetchException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public PageFetchException(String message, Throwable cause) {
        super(message, cause);
    }

    public PageFetchException(String message) {
        super(message);
    }

    public PageFetchException(Throwable cause) {
        super(cause);
    }
}
