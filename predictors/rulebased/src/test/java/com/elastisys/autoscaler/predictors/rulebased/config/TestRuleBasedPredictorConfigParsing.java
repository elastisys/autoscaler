package com.elastisys.autoscaler.predictors.rulebased.config;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Test;

import com.elastisys.autoscaler.predictors.rulebased.config.RuleBasedPredictorParams;
import com.elastisys.autoscaler.predictors.rulebased.rule.Condition;
import com.elastisys.autoscaler.predictors.rulebased.rule.ResizeUnit;
import com.elastisys.autoscaler.predictors.rulebased.rule.ScalingRule;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.google.gson.JsonObject;

/**
 * Tests parsing of JSON-formatted {@link RuleBasedPredictorParams} to its Java
 * counterpart.
 */
public class TestRuleBasedPredictorConfigParsing {

    @Test
    public void testParseConfigWithAboveCondition() throws IOException {
        JsonObject jsonConfig = JsonUtils.parseJsonResource("rulebased/rulebased-predictor-config1.json")
                .getAsJsonObject();

        RuleBasedPredictorParams config = RuleBasedPredictorParams.parse(jsonConfig.get("parameters"));
        assertThat(config, is(new RuleBasedPredictorParams(TimeInterval.seconds(180),
                asList(new ScalingRule(Condition.ABOVE, 80, TimeInterval.seconds(180), 10, ResizeUnit.PERCENT)))));
    }

    @Test
    public void testParseConfigWithBelowCondition() throws IOException {
        JsonObject jsonConfig = JsonUtils.parseJsonResource("rulebased/rulebased-predictor-config2.json")
                .getAsJsonObject();

        RuleBasedPredictorParams config = RuleBasedPredictorParams.parse(jsonConfig.get("parameters"));
        assertThat(config, is(new RuleBasedPredictorParams(TimeInterval.seconds(180),
                asList(new ScalingRule(Condition.BELOW, 20, TimeInterval.seconds(180), -10, ResizeUnit.PERCENT)))));
    }

    @Test
    public void testParseConfigWithExactlyConditionAndInstanceUnit() throws IOException {
        JsonObject jsonConfig = JsonUtils.parseJsonResource("rulebased/rulebased-predictor-config3.json")
                .getAsJsonObject();

        RuleBasedPredictorParams config = RuleBasedPredictorParams.parse(jsonConfig.get("parameters"));
        assertThat(config, is(new RuleBasedPredictorParams(TimeInterval.seconds(180),
                asList(new ScalingRule(Condition.EXACTLY, 50, TimeInterval.seconds(180), 1, ResizeUnit.INSTANCES)))));
    }
}
