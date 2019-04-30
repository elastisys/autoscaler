package com.elastisys.autoscaler.core.prediction.impl.standard.aggregator;

import com.elastisys.autoscaler.core.prediction.impl.standard.aggregator.Aggregator;
import com.elastisys.autoscaler.core.prediction.impl.standard.aggregator.Aggregator.AggregatorInput;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.AggregatorConfig;

/**
 * Utility class for {@link Aggregator} tests.
 * 
 * 
 * 
 */
public class AggregatorTestUtils {

    private AggregatorTestUtils() {
        throw new IllegalStateException("not intended to be instantiated.");
    }

    /**
     * Returns a Javascript expression intended for use as {@link Aggregator}
     * expression that given an {@link AggregatorInput} {@code input} evaluates
     * to the maximum predicted value.
     * 
     * @return A javascript represented as a string.
     */
    public static String maxAggregatorExpression() {
        return "Math.max.apply(Math, input.predictions.map( function(p){return p.prediction;} ))";
    }

    /**
     * Creates a {@link AggregatorConfig} with a given JavaScript expression.
     * 
     * @param javascript
     *            The JavaScript expression.
     * @return
     */
    public static AggregatorConfig config(String javascript) {
        return new AggregatorConfig(javascript);
    }

}
