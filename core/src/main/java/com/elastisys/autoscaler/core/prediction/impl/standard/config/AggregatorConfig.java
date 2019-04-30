package com.elastisys.autoscaler.core.prediction.impl.standard.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * A JavaScript expression to be used as an aggregation function to convert a
 * list of compute unit predictions into a single (aggregated) prediction.
 * <p/>
 * The JavaScript expression must return a single numeric value.
 *
 *
 */
public class AggregatorConfig {

    /** A JavaScript expression that must return a single numeric value. */
    private final String expression;

    /**
     * Constructs a new {@link AggregatorConfig}.
     *
     * @param expression
     *            A JavaScript expression that must return a single numeric
     *            value.
     */
    public AggregatorConfig(String expression) {
        this.expression = expression;
    }

    /**
     * Returns the JavaScript expression.
     *
     * @return
     */
    public String getExpression() {
        return this.expression;
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.expression != null, "aggregator: missing expression");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AggregatorConfig) {
            AggregatorConfig that = (AggregatorConfig) obj;
            return Objects.equals(this.expression, that.expression);

        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.expression);
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }

}
