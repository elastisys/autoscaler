package com.elastisys.autoscaler.core.prediction.api.types;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.core.prediction.api.types.PredictionUnit;
import com.elastisys.autoscaler.core.prediction.api.types.PredictionValue;

/**
 * Tests the {@link PredictionValue} class.
 * 
 * 
 * 
 */
public class TestPredictionValue {

    private static final double VALUE = 1.0;
    private static final PredictionUnit UNIT = PredictionUnit.METRIC;

    /** Object under test. */
    private PredictionValue predictionValue;

    @Before
    public void onSetup() {
        this.predictionValue = new PredictionValue(VALUE, UNIT);
        assertThat(this.predictionValue, is(predictionValue(VALUE, UNIT)));
    }

    @Test
    public void testCopyMethods() {
        assertThat(this.predictionValue.withValue(111.0), is(predictionValue(111.0, UNIT)));

        assertThat(this.predictionValue.withUnit(PredictionUnit.COMPUTE),
                is(predictionValue(VALUE, PredictionUnit.COMPUTE)));

    }

    private PredictionValue predictionValue(double value, PredictionUnit unit) {
        return new PredictionValue(value, unit);
    }

}
