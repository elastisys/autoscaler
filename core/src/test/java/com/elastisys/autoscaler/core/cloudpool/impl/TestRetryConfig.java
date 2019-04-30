package com.elastisys.autoscaler.core.cloudpool.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.core.cloudpool.impl.RetryConfig;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Exercise {@link RetryConfig}.
 */
public class TestRetryConfig {

    private static final Integer MAX_ATTEMPTS = 3;
    private static final TimeInterval INITIAL_DELAY = TimeInterval.seconds(2);

    @Test
    public void basicSanity() {
        RetryConfig config = new RetryConfig(MAX_ATTEMPTS, INITIAL_DELAY);
        config.validate();

        assertThat(config.getMaxAttempts(), is(MAX_ATTEMPTS));
        assertThat(config.getInitialDelay(), is(INITIAL_DELAY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingMaxAttempts() {
        Integer nullMaxAttempts = null;
        new RetryConfig(nullMaxAttempts, INITIAL_DELAY).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingInitialDelay() {
        TimeInterval nullInitialDelay = null;
        new RetryConfig(MAX_ATTEMPTS, nullInitialDelay).validate();
    }

}
