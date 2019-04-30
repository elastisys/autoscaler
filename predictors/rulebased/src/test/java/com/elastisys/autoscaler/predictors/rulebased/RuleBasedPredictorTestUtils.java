package com.elastisys.autoscaler.predictors.rulebased;

import static java.util.Arrays.asList;

import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.predictors.rulebased.RuleBasedPredictor;
import com.elastisys.autoscaler.predictors.rulebased.config.RuleBasedPredictorParams;
import com.elastisys.autoscaler.predictors.rulebased.rule.Condition;
import com.elastisys.autoscaler.predictors.rulebased.rule.ResizeUnit;
import com.elastisys.autoscaler.predictors.rulebased.rule.ScalingRule;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.google.gson.JsonElement;

/**
 * Test utilities for {@link RuleBasedPredictor} tests.
 */
public class RuleBasedPredictorTestUtils {

    /**
     * Constructs a {@link Predictor}-specific configuration for a
     * {@link RuleBasedPredictor}.
     *
     * @param cooldown
     *            cooldown period
     * @param rules
     *            scaling rules
     * @return
     */
    public static JsonElement customConfig(TimeInterval cooldown, ScalingRule... rules) {
        RuleBasedPredictorParams ruleBasedPredictorConfig = new RuleBasedPredictorParams(cooldown, asList(rules));
        return JsonUtils.toJson(ruleBasedPredictorConfig);
    }

    /**
     * Creates a {@link ScalingRule}.
     *
     * @return
     */
    public static ScalingRule rule(Condition condition, double threshold, TimeInterval period, double resize,
            ResizeUnit unit) {
        return new ScalingRule(condition, threshold, period, resize, unit);
    }
}
