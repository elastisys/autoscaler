package com.elastisys.autoscaler.systemhistorians.opentsdb;

/**
 * Thrown to indicate an error condition while communicating with an OpenTSDB
 * server.
 */
public class OpenTsdbException extends Exception {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@link OpenTsdbException}.
     */
    public OpenTsdbException() {
        super();
    }

    /**
     * Constructs a new {@link OpenTsdbException}.
     *
     * @param message
     * @param cause
     */
    public OpenTsdbException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link OpenTsdbException}.
     *
     * @param message
     */
    public OpenTsdbException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link OpenTsdbException}.
     *
     * @param cause
     */
    public OpenTsdbException(Throwable cause) {
        super(cause);
    }
}
