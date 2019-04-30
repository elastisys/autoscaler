package com.elastisys.autoscaler.core.prediction.impl.standard;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.prediction.impl.standard.StandardPredictionSubsystem;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.AggregatorConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.CapacityLimitConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.CapacityMappingConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.ScalingPoliciesConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.StandardPredictionSubsystemConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Verifies translation of JSON configuration documents to their corresponding
 * Java classes for the {@link StandardPredictionSubsystem}.
 */
public class TestStandardPredictionSubsystemConfigParsing {
    static Logger logger = LoggerFactory.getLogger(TestStandardPredictionSubsystemConfigParsing.class);

    private static final double machineDeltaTolerance = 0.1d;
    private static final TimeInterval overprovisioningGracePeriod = TimeInterval.seconds(300);

    /**
     * Verifies proper translation of a {@link StandardPredictionSubsystem} JSON
     * configuration to its java type: {@link StandardPredictionSubsystemConfig}
     * .
     *
     * @throws IOException
     * @throws ConfigurationException
     */
    @Test
    public void parseJsonConfiguration() throws IOException {
        JsonObject jsonConfig = JsonUtils.parseJsonResource("predictionSubsystem/predictionSubsystem-config.json")
                .getAsJsonObject();
        StandardPredictionSubsystemConfig config = new Gson().fromJson(jsonConfig.get("predictionSubsystem"),
                StandardPredictionSubsystemConfig.class);

        assertThat(config.getPredictors(), is(expectedPredictors(jsonConfig)));
        assertThat(config.getCapacityMappings(), is(expectedCapacityMappings()));
        assertThat(config.getAggregator(), is(expectedAggregatorConfig()));
        assertThat(config.getScalingPolicies(), is(expectedScalingPolicies()));
        assertThat(config.getCapacityLimits(), is(expectedCapacityLimits()));
    }

    private ScalingPoliciesConfig expectedScalingPolicies() {
        return new ScalingPoliciesConfig(machineDeltaTolerance, overprovisioningGracePeriod);
    }

    private List<PredictorConfig> expectedPredictors(JsonObject jsonConfig) throws IOException {
        JsonArray predictorConfigs = jsonConfig.get("predictionSubsystem").getAsJsonObject().get("predictors")
                .getAsJsonArray();
        JsonElement expectedP1Config = predictorConfigs.get(0).getAsJsonObject().get("parameters");
        JsonElement expectedP2Config = predictorConfigs.get(1).getAsJsonObject().get("parameters");

        return Arrays.asList(//
                new PredictorConfig("p1", "com.elastisys.scale.core.test.stubs.PredictorStub", State.STARTED,
                        "http.total.accesses.rate.stream", expectedP1Config), //
                new PredictorConfig("p2", "com.elastisys.scale.core.test.stubs.PredictorStub", State.STARTED,
                        "cpu.user.percent.stream", expectedP2Config));
    }

    private List<CapacityMappingConfig> expectedCapacityMappings() {
        return Arrays.asList( //
                new CapacityMappingConfig("cpu.user.percent", 100.0), //
                new CapacityMappingConfig("http.total.accesses", 250.0));
    }

    private List<CapacityLimitConfig> expectedCapacityLimits() {
        return Arrays.asList( //
                new CapacityLimitConfig("baseline", 1, "* * * * * ? *", 2, 4), //
                new CapacityLimitConfig("friday-heat", 1, "* * 10-21 ? * FRI *", 20, 40));
    }

    private AggregatorConfig expectedAggregatorConfig() {
        return new AggregatorConfig("Math.max(p1, p2);");
    }
}
