package com.elastisys.autoscaler.core.prediction.impl.standard.predictor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.prediction.impl.standard.aggregator.Aggregator;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonElement;

/**
 * Exercises {@link PredictorConfig}.
 */
public class TestPredictorConfig {

    private static final String id = "p1";
    private static final String type = "some.PredictorClass";
    private static final State state = State.STOPPED;
    private static final String metricStream = "cpu.stream";
    private static final JsonElement parameters = JsonUtils.parseJsonString("{\"alpha\": 1.0}").getAsJsonObject();

    /**
     * should be possible to supply explicit values for all fields.
     */
    @Test
    public void complete() {
        PredictorConfig config = new PredictorConfig(id, type, state, metricStream, parameters);
        config.validate();

        assertThat(config.getId(), is(id));
        assertThat(config.getType(), is(type));
        assertThat(config.getState(), is(state));
        assertThat(config.getMetricStream(), is(metricStream));
        assertThat(config.getParameters(), is(parameters));
    }

    /**
     * state and config are optional.
     */
    @Test
    public void defaults() {
        State nullState = null;
        JsonElement nullConfig = null;
        PredictorConfig config = new PredictorConfig(id, type, nullState, metricStream, nullConfig);
        config.validate();

        assertThat(config.getState(), is(PredictorConfig.DEFAULT_STATE));
        assertThat(config.getParameters(), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingId() {
        new PredictorConfig(null, type, state, metricStream, parameters).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingType() {
        new PredictorConfig(id, null, state, metricStream, parameters).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingMetricStream() {
        new PredictorConfig(id, type, state, null, parameters).validate();
    }

    /**
     * A predictor id needs to be a valid JavaScript identifier as it is to be
     * used in an {@link Aggregator} JavaScript.
     */
    @Test
    public void illegalPredictorId() {
        try {
            String illegalJavaScriptId = "p-1";
            new PredictorConfig(illegalJavaScriptId, type, state, metricStream, parameters).validate();
            fail("expected to fail validation");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("is not a valid JavaScript identifier"));
        }
    }

    @Test
    public void validationOfJavaScriptIdentifiers() {
        List<String> validIdentifiers = Arrays.asList("myStream", "stream1", "my_stream", "$Stream", "$$");
        for (String validIdentifier : validIdentifiers) {
            try {
                PredictorConfig.validJavaScriptIdentifier(validIdentifier);
            } catch (IllegalArgumentException e) {
                fail("unexpectedly failed to validate JavaScript identifier: " + e.getMessage());
            }
        }

        List<String> invalidIdentifiers = Arrays.asList("1stream", "*stream", " stream", "!stream", "p-1", "p+1");
        for (String invalidIdentifier : invalidIdentifiers) {
            try {
                PredictorConfig.validJavaScriptIdentifier(invalidIdentifier);
                fail("expected validation of JavaScript identifier to fail: " + invalidIdentifier);
            } catch (IllegalArgumentException e) {
                // expected
            }
        }
    }
}
