package com.elastisys.autoscaler.predictors.rulebased.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.autoscaler.predictors.rulebased.RuleBasedPredictor;
import com.elastisys.autoscaler.predictors.rulebased.rule.ScalingRule;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

/**
 * Represents {@link PredictorConfig} parameters for a
 * {@link RuleBasedPredictor}.
 */
public class RuleBasedPredictorParams {
    public static final TimeInterval DEFAULT_COOLDOWN_PERIOD = new TimeInterval(10L, TimeUnit.MINUTES);

    /**
     * The time period that the {@link RuleBasedPredictor} will remain passive
     * after a scaling rule has been triggered, in order to allow changes to
     * have a chance to take effect before another scaling rule is triggered.
     * May be <code>null</code>. Default is {@link #DEFAULT_COOLDOWN_PERIOD}.
     */
    private final TimeInterval cooldownPeriod;

    /**
     * The collection of {@link ScalingRule}s to be enforced. The
     * {@link ScalingRule}s are evaluated in order by the
     * {@link RuleBasedPredictor}.
     * <p/>
     * During a prediction iteration, the first {@link ScalingRule} to be
     * satisfied is triggered (the rest are ignored) and marks the start of a
     * cooldown period.
     */
    private final List<ScalingRule> scalingRules;

    /**
     * Constructs a new {@link RuleBasedPredictor}.
     *
     * @param cooldownPeriod
     *            The time period that the {@link RuleBasedPredictor} will
     *            remain passive after a scaling rule has been triggered, in
     *            order to allow changes to have a chance to take effect before
     *            another scaling rule is triggered. May be <code>null</code>.
     *            Default is {@link #DEFAULT_COOLDOWN_PERIOD}.
     * @param scalingRules
     *            The collection of {@link ScalingRule}s to be enforced. The
     *            {@link ScalingRule}s are evaluated in order by the
     *            {@link RuleBasedPredictor}.
     *            <p/>
     *            During a prediction iteration, the first {@link ScalingRule}
     *            to be satisfied is triggered (the rest are ignored) and marks
     *            the start of a cooldown period. May be <code>null</code>,
     *            which is equivalent to giving an empty list.
     */
    public RuleBasedPredictorParams(TimeInterval cooldownPeriod, List<ScalingRule> scalingRules) {
        this.cooldownPeriod = cooldownPeriod;
        this.scalingRules = scalingRules;
    }

    /**
     * Factory method that parses out an {@link RuleBasedPredictorParams } from
     * a JSON representation, or fails with a {@link JsonSyntaxException}.
     *
     * @param jsonConfig
     *            The JSON representation of the
     *            {@link RuleBasedPredictorParams}.
     * @return The parsed {@link RuleBasedPredictorParams}.
     */
    public static RuleBasedPredictorParams parse(JsonElement jsonConfig) {
        return JsonUtils.toObject(jsonConfig, RuleBasedPredictorParams.class);
    }

    /**
     * Performs basic validation of this {@link RuleBasedPredictorParams}.
     *
     * @throws IllegalArgumentException
     *             if validation fails.
     */
    public void validate() throws IllegalArgumentException {
        getCooldownPeriod().validate();
        checkArgument(getCooldownPeriod().getSeconds() >= 0, "cooldownPeriod must be a positive duration");
        for (ScalingRule rule : getScalingRules()) {
            rule.validate();
        }
    }

    /**
     * Returns the time period that the {@link RuleBasedPredictor} will remain
     * passive after a scaling rule has been triggered, in order to allow
     * changes to have a chance to take effect before another scaling rule is
     * triggered.
     *
     * @return
     */
    public TimeInterval getCooldownPeriod() {
        return Optional.ofNullable(this.cooldownPeriod).orElse(DEFAULT_COOLDOWN_PERIOD);
    }

    /**
     * Returns the collection of {@link ScalingRule}s to be enforced. The
     * {@link ScalingRule}s are evaluated in order by the
     * {@link RuleBasedPredictor}.
     * <p/>
     * During a prediction iteration, the first {@link ScalingRule} to be
     * satisfied is triggered (the rest are ignored) and marks the start of a
     * cooldown period.
     *
     * @return
     */
    public List<ScalingRule> getScalingRules() {
        return Optional.ofNullable(this.scalingRules).orElse(Collections.emptyList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCooldownPeriod(), getScalingRules());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RuleBasedPredictorParams) {
            RuleBasedPredictorParams that = (RuleBasedPredictorParams) obj;
            return Objects.equals(getCooldownPeriod(), that.getCooldownPeriod())
                    && Objects.equals(getScalingRules(), that.getScalingRules());
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

}
