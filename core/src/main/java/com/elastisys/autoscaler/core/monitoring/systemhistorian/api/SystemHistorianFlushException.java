package com.elastisys.autoscaler.core.monitoring.systemhistorian.api;

/**
 * Thrown by a {@link SystemHistorian} to indicate a problem to write to its
 * backend store.
 */
public class SystemHistorianFlushException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SystemHistorianFlushException() {
        super();
    }

    public SystemHistorianFlushException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SystemHistorianFlushException(String message, Throwable cause) {
        super(message, cause);
    }

    public SystemHistorianFlushException(String message) {
        super(message);
    }

    public SystemHistorianFlushException(Throwable cause) {
        super(cause);
    }

}
