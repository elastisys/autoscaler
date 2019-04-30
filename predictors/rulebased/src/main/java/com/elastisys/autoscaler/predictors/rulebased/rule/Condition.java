package com.elastisys.autoscaler.predictors.rulebased.rule;

/**
 * Represents the collection of valid threshold conditions that can be used to
 * define a {@link ScalingRule}.
 * 
 * @see ScalingRule
 * 
 * 
 */
public enum Condition {
    /**
     * Scaling rule condition that triggers when the metric value is above the
     * threshold.
     */
    ABOVE(1.0),
    /**
     * Scaling rule condition that triggers when the metric value is below the
     * threshold.
     */
    BELOW(-1.0),
    /**
     * Scaling rule condition that triggers when the metric value has the same
     * value as the threshold.
     */
    EXACTLY(0.0);

    /**
     * The sign of the condition. Zero for {@link #EXACTLY}, -1 for
     * {@link #BELOW} and +1 for {@link #ABOVE}.
     */
    private double sign;

    private Condition(double sign) {
        this.sign = sign;
    }

    /**
     * Evaluates the condition on two values in in-fix notation:
     * {@code a Condition b}. For example {@code a > b}.
     * 
     * @param a
     *            The left-hand value
     * @param b
     *            The right-hand value.
     * @return <code>true</code> if the condition is satisfied,
     *         <code>false</code> otherwise.
     */
    public boolean evaluate(Double a, Double b) {
        return Math.signum(a.compareTo(b)) == this.sign;
    }
}
