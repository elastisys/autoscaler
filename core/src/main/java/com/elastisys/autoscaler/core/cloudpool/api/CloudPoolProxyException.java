package com.elastisys.autoscaler.core.cloudpool.api;

/**
 * Thrown by a {@link CloudPoolProxy} to indicate an error condition.
 *
 * @see CloudPoolProxy
 *
 */
public class CloudPoolProxyException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@link CloudPoolProxyException}.
     */
    public CloudPoolProxyException() {
        super();
    }

    /**
     * Constructs a new {@link CloudPoolProxyException}.
     *
     * @param message
     * @param cause
     */
    public CloudPoolProxyException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link CloudPoolProxyException}.
     *
     * @param message
     */
    public CloudPoolProxyException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link CloudPoolProxyException}.
     *
     * @param cause
     */
    public CloudPoolProxyException(Throwable cause) {
        super(cause);
    }
}
