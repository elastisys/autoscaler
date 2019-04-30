package com.elastisys.autoscaler.predictors.reactive.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.predictors.reactive.config.ReactivePredictorParams;

/**
 * Exercises the {@link ReactivePredictorParams}.
 */
public class TestLastReadingPredictorConfiguration {

    /**
     * safetyMargin should be possible to set.
     */
    @Test
    public void withSafetyMargin() {
        ReactivePredictorParams config = new ReactivePredictorParams(10.0);
        config.validate();
        assertThat(config.getSafetyMargin(), is(10.0));
    }

    /**
     * safetyMargin is optional. Default: 0.0
     */
    @Test
    public void defaults() {
        ReactivePredictorParams config = new ReactivePredictorParams(null);
        config.validate();
        assertThat(config.getSafetyMargin(), is(0.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void withIllegalSafetyMargin() {
        new ReactivePredictorParams(-1.0).validate();
    }

}
