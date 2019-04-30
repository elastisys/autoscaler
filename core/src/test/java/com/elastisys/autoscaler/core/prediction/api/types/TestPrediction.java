package com.elastisys.autoscaler.core.prediction.api.types;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.api.types.PredictionUnit;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Tests the {@link Prediction} class.
 *
 *
 *
 */
public class TestPrediction {
    private static final double VALUE = 1.0;
    private static final PredictionUnit UNIT = PredictionUnit.METRIC;
    private static final String METRIC = "cpu/cpu-user";
    private static final DateTime TIME = UtcTime.now();

    /** Object under test. */
    private Prediction prediction;

    @Before
    public void onSetup() {
        this.prediction = new Prediction(VALUE, UNIT, METRIC, TIME);
        assertThat(this.prediction, is(prediction(VALUE, UNIT, METRIC, TIME)));
    }

    /**
     * Test copy methods (where a single field is replaced).
     */
    @Test
    public void testCopyMethods() {
        assertThat(this.prediction.withValue(3.0), is(prediction(3.0, UNIT, METRIC, TIME)));

        assertThat(this.prediction.withUnit(PredictionUnit.COMPUTE),
                is(prediction(VALUE, PredictionUnit.COMPUTE, METRIC, TIME)));

        assertThat(this.prediction.withMetric("new-metric"), is(prediction(VALUE, UNIT, "new-metric", TIME)));

        DateTime now = UtcTime.now();
        assertThat(this.prediction.withTimestamp(now), is(prediction(VALUE, UNIT, METRIC, now)));
    }

    public static final Prediction prediction(double value, PredictionUnit unit, String metric, DateTime timestamp) {
        return new Prediction(value, unit, metric, timestamp);
    }
}
