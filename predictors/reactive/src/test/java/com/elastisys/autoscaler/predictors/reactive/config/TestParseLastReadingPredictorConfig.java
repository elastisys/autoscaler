package com.elastisys.autoscaler.predictors.reactive.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Test;

import com.elastisys.autoscaler.predictors.reactive.config.ReactivePredictorParams;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Tests parsing of JSON-formatted {@link ReactivePredictorParams} to its
 * Java counterpart.
 *
 */
public class TestParseLastReadingPredictorConfig {

    /**
     * Parse a json configuration that specifies an explicit
     * {@code safetyMargin}.
     */
    @Test
    public void parseCompleteConfig() throws IOException {
        JsonObject json = JsonUtils.parseJsonResource("reactive/complete-config.json").getAsJsonObject();
        JsonObject jsonConfig = json.get("parameters").getAsJsonObject();
        ReactivePredictorParams config = ReactivePredictorParams.parse(jsonConfig);

        assertThat(config.getSafetyMargin(), is(jsonConfig.get("safetyMargin").getAsDouble()));
    }

    /**
     * Parse a json configuration that does not specify an explicit
     * {@code safetyMargin}.
     */
    @Test
    public void parseConfigRelyingOnDefaults() throws IOException {
        JsonObject json = JsonUtils.parseJsonResource("reactive/minimal-config.json").getAsJsonObject();
        JsonObject jsonConfig = json.get("parameters").getAsJsonObject();
        ReactivePredictorParams config = ReactivePredictorParams.parse(jsonConfig);

        assertThat(config.getSafetyMargin(), is(0.0));
    }
}
