package com.elastisys.autoscaler.predictors.rulebased.rule;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.List;
import java.util.Objects;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.predictors.rulebased.RuleBasedPredictor;
import com.elastisys.autoscaler.predictors.rulebased.config.RuleBasedPredictorParams;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Represents a {@link ScalingRule} for a {@link RuleBasedPredictor}. A scaling
 * rule triggers a scaling action when the watched metric value has satisfied a
 * condition for a sufficient amount of time.
 * <p/>
 * When the value of a the monitored metric data series has breached a threshold
 * for sufficiently long a scaling action is triggered. The {@link ScalingRule}
 * can be tested against a metric value sequence by invoking the
 * {@link #isSatisfiedBy(List)} method.
 * <p/>
 * The semantics of a scaling rule can be expressed in the following way:
 * <p/>
 * <code>
 * when the metric value has been &lt;condition&gt; &lt;threshold&gt; for &lt;period&gt; seconds, adjust the capacity by &lt;resize&gt; &lt;unit&gt;
 * </code> For example:
 * <p/>
 * <code>
 * when the metric value has been &lt;ABOVE&gt; &lt;80&gt; for &lt;600&gt; seconds, adjust the capacity by &lt;+10&gt; &lt;PERCENT&gt;
 * </code>
 *
 * @see RuleBasedPredictor
 * @see RuleBasedPredictorParams
 */
public class ScalingRule {
    /**
     * The condition used to compare the aggregated value with the threshold.
     */
    private final Condition condition;
    /**
     * The threshold that, when breached for a sufficiently long time, triggers
     * the resize action.
     */
    private final double threshold;
    /**
     * The evaluation period that the threshold condition must hold true before
     * triggering a resize.
     */
    private final TimeInterval period;
    /**
     * The size of the capacity adjustment that is triggered when the threshold
     * condition has been <code>true</code> for sufficiently long.
     */
    private final double resize;
    /**
     * The unit that the resize increment is specified in.
     */
    private final ResizeUnit unit;

    /**
     * Constructs a new {@link ScalingRule}.
     *
     * @param condition
     *            The condition used to compare the aggregated value with the
     *            threshold.
     * @param threshold
     *            The threshold that, when breached for a sufficiently long
     *            time, triggers the resize action.
     * @param period
     *            The evaluation period that the threshold condition must hold
     *            true before triggering a resize.
     * @param resize
     *            The size of the capacity adjustment that is triggered when the
     *            threshold condition has been <code>true</code> for
     *            sufficiently long.
     * @param unit
     *            The unit that the resize increment is specified in.
     */
    public ScalingRule(Condition condition, double threshold, TimeInterval period, double resize, ResizeUnit unit) {
        this.condition = condition;
        this.threshold = threshold;
        this.period = period;
        this.resize = resize;
        this.unit = unit;
    }

    /**
     * Performs basic validation of this {@link ScalingRule}.
     *
     * @throws IllegalArgumentException
     *             if validation fails.
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(this.condition != null, "scaling rule condition cannot be null");
        checkArgument(this.unit != null, "scaling rule unit cannot be null");
        checkArgument(this.period != null, "scaling rule missing evaluation period");
        checkArgument(this.period.getSeconds() >= 0, "scaling rule evaluation period must be a positive duration");
    }

    /**
     * Returns the condition used to compare the aggregated value with the
     * threshold.
     *
     * @return
     */
    public Condition getCondition() {
        return this.condition;
    }

    /**
     * Returns the threshold that, when breached for a sufficiently long time,
     * triggers the resize action.
     *
     * @return
     */
    public double getThreshold() {
        return this.threshold;
    }

    /**
     * Returns the evaluation period that the threshold condition must hold true
     * before triggering a resize.
     *
     * @return
     */
    public TimeInterval getPeriod() {
        return this.period;
    }

    /**
     * Returns the size of the capacity adjustment that is triggered when the
     * threshold condition has been <code>true</code> for sufficiently long.
     *
     * @return
     */
    public double getResize() {
        return this.resize;
    }

    /**
     * Returns the unit that the resize increment is specified in.
     *
     * @return
     */
    public ResizeUnit getUnit() {
        return this.unit;
    }

    /**
     * A tuple-like object that returns both the outcome result of the rule
     * constraint checks and also a reason message with details of what caused
     * the result.
     */
    public class RuleOutcome {
        private boolean satisfied;
        private String reason;

        public RuleOutcome(boolean satisfied, String reason) {
            this.satisfied = satisfied;
            this.reason = reason;
        }

        public boolean isSatisfied() {
            return this.satisfied;
        }

        public String getReason() {
            return this.reason;
        }
    }

    /**
     * Evaluates this {@link ScalingRule} against a sequence of metric values
     * and returns <code>true</code> if the metric sequence satisfies this
     * {@link ScalingRule} (that is, the metric values have satisfied the
     * threshold condition for a sufficiently long time).
     *
     * @param metricSequence
     *            A metric sequence that is assumed to be sorted in order of
     *            increasing time (newest metric value last).
     * @return RuleOutcome
     */
    public RuleOutcome isSatisfiedBy(List<DataPoint> metricSequence) {
        Objects.requireNonNull(metricSequence, "metric sequence cannot be null");

        if (metricSequence.size() < 2) {
            return new RuleOutcome(false, "Not enough metric data points (cannot evaluate period length)");
        }

        DataPoint lastValue = metricSequence.get(metricSequence.size() - 1);
        if (!this.condition.evaluate(lastValue.getValue(), this.threshold)) {
            return new RuleOutcome(false,
                    String.format("Threshold condition not true on last metric observation: %s", lastValue.toString()));
        }
        DateTime newest = lastValue.getTime();

        // see if threshold condition has been satisfied for sufficiently long
        for (int i = metricSequence.size() - 2; i >= 0; i--) {
            DataPoint value = metricSequence.get(i);
            Duration satisfactionPeriod = new Duration(value.getTime(), newest);
            if (!this.condition.evaluate(value.getValue(), this.threshold)) {
                return new RuleOutcome(false,
                        String.format(
                                "Threshold was not met for the period of %d seconds, "
                                        + "metric data point: %s was seen %d seconds ago",
                                this.period.getSeconds(), value.toString(), satisfactionPeriod.getStandardSeconds()));
            }
            if (satisfactionPeriod.getStandardSeconds() >= this.period.getSeconds()) {
                return new RuleOutcome(true, String.format("Satisfied for %d seconds",
                        this.period.getSeconds() - satisfactionPeriod.getStandardSeconds()));
            }
        }

        return new RuleOutcome(false, "Have not seen metrics old enough to satisfy scaling rule period");
    }

    /**
     * Returns the resize increment that results from applying this
     * {@link ScalingRule} to a machine pool of a given size.
     * <p/>
     * <i>Note: the scaling rule will never suggest a (negative) increment that
     * brings the machine pool to a size smaller than one instance.</i>
     *
     * @param machinePoolSize
     *            The size of the machine pool to be resized.
     * @return The pool resize increment that results from applying this
     *         {@link ScalingRule} to the machine pool.
     */
    public int getResizeIncrement(int machinePoolSize) {
        int increment = 0;
        switch (this.unit) {
        case INSTANCES:
            increment = closestIncrement(this.resize);
            break;
        case PERCENT:
            increment = closestIncrement(this.resize / 100.0 * machinePoolSize);
            break;
        default:
            throw new IllegalArgumentException(String.format("unrecognized resize unit %s", this.unit.name()));
        }

        // prevent pool size from going below one instance
        if (machinePoolSize + increment < 1) {
            if (machinePoolSize == 0) {
                return 0;
            }
            // suggest terminating all but one instance
            return -(machinePoolSize - 1);
        }
        return increment;
    }

    /**
     * Returns the closest integer resize increment from a fractional resize
     * increment. For a negative increment, ⌊increment⌋ is returned. For a
     * positive increment, ⌈increment⌉ is returned.
     *
     * @param fractionalIncrement
     *            fractional resize increment (for example, 1.1 or -0.2).
     * @return
     */
    private int closestIncrement(double fractionalIncrement) {
        if (fractionalIncrement < 0) {
            return (int) Math.floor(fractionalIncrement);
        } else {
            return (int) Math.ceil(fractionalIncrement);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.condition, this.threshold, this.period, this.resize, this.unit);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScalingRule) {
            ScalingRule that = (ScalingRule) obj;
            return Objects.equals(this.condition, that.condition) && Objects.equals(this.threshold, that.threshold)
                    && Objects.equals(this.period, that.period) && Objects.equals(this.resize, that.resize)
                    && Objects.equals(this.unit, that.unit);

        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

}
