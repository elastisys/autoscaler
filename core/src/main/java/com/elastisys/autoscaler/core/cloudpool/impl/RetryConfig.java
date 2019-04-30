package com.elastisys.autoscaler.core.cloudpool.impl;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Describes how many times a {@link StandardCloudPoolProxy} should retry a
 * request to its backend {@link CloudPool}, and the initial delay to use for
 * the exponential backoff.
 *
 * @see StandardCloudPoolProxy
 */
public class RetryConfig {

    /** The maximum number of attempts to make for a given request. */
    private final Integer maxAttempts;

    /**
     * Delay after first attempt. This delay will grow exponentially with the
     * number of attempts (factor: 2^1, 2^2, 2^3, ... etc). A value of zero
     * makes the effective delay zero between all attempts.
     */
    private final TimeInterval initialDelay;

    /**
     * Creates a {@link RetryConfig}.
     *
     * @param maxAttempts
     *            The maximum number of attempts to make for a given request.
     * @param initialDelay
     *            Delay after first attempt. This delay will grow exponentially
     *            with the number of attempts (factor: 2^1, 2^2, 2^3, ... etc).
     *            A value of zero makes the effective delay zero between all
     *            attempts.
     */
    public RetryConfig(Integer maxAttempts, TimeInterval initialDelay) {
        this.maxAttempts = maxAttempts;
        this.initialDelay = initialDelay;
    }

    /**
     * The maximum number of attempts to make for a given request.
     *
     * @return
     */
    public Integer getMaxAttempts() {
        return this.maxAttempts;
    }

    /**
     * Delay after first attempt. This delay will grow exponentially with the
     * number of attempts (factor: 2^1, 2^2, 2^3, ... etc). A value of zero
     * makes the effective delay zero between all attempts.
     *
     * @return
     */
    public TimeInterval getInitialDelay() {
        return this.initialDelay;
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.maxAttempts != null, "retries: missing maxAttempts");
        checkArgument(this.initialDelay != null, "retries: missing initialDelay");
        try {
            this.initialDelay.validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("retries: initialDelay: " + e.getMessage(), e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.maxAttempts, this.initialDelay);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RetryConfig) {
            RetryConfig that = (RetryConfig) obj;
            return Objects.equals(this.maxAttempts, that.maxAttempts) //
                    && Objects.equals(this.initialDelay, that.initialDelay);
        }
        return false;
    }
}
