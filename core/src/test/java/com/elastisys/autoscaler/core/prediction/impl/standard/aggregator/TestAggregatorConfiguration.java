package com.elastisys.autoscaler.core.prediction.impl.standard.aggregator;

import static com.elastisys.autoscaler.core.prediction.impl.standard.aggregator.AggregatorTestUtils.config;
import static com.elastisys.autoscaler.core.prediction.impl.standard.aggregator.AggregatorTestUtils.maxAggregatorExpression;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.prediction.impl.standard.aggregator.Aggregator;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.AggregatorConfig;

/**
 * Tests that verify the configuration behavior of an {@link Aggregator}.
 */
public class TestAggregatorConfiguration {
    static Logger logger = LoggerFactory.getLogger(TestAggregatorConfiguration.class);

    /** Object under test. */
    private Aggregator aggregator;

    @Before
    public void onSetup() {
        this.aggregator = new Aggregator(logger);
        // pre-test sanity check
        assertNull(this.aggregator.getConfiguration());
    }

    @Test
    public void configureValidJavaScript() {
        AggregatorConfig config = config(maxAggregatorExpression());
        this.aggregator.validate(config);
        this.aggregator.configure(config);

        assertThat(this.aggregator.getConfiguration(), is(config));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureInvalidSyntaxJavaScript() {
        // closing parenthesis missing
        AggregatorConfig config = config(
                "Math.max.apply(Math, input.predictions.map( function(p){return p.prediction;})");
        this.aggregator.validate(config);
    }

}
