package com.elastisys.autoscaler.predictors.rulebased.rule;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.predictors.rulebased.rule.Condition;

/**
 * Exercises the {@link Condition} class.
 * 
 * 
 * 
 */
public class TestCondition {

    @Test
    public void testAbove() {
        assertThat(Condition.ABOVE.evaluate(1.0, 0.0), is(true));
        assertThat(Condition.ABOVE.evaluate(1.0, 1.0), is(false));
        assertThat(Condition.ABOVE.evaluate(0.0, 1.0), is(false));

        assertThat(Condition.ABOVE.evaluate(10.0, 0.0), is(true));
        assertThat(Condition.ABOVE.evaluate(-1.0, -2.0), is(true));
        assertThat(Condition.ABOVE.evaluate(-2.0, -1.0), is(false));
    }

    @Test
    public void testExactly() {
        assertThat(Condition.EXACTLY.evaluate(0.0, 0.0), is(true));
        assertThat(Condition.EXACTLY.evaluate(1.0, 1.0), is(true));
        assertThat(Condition.EXACTLY.evaluate(-1.0, -1.0), is(true));
        assertThat(Condition.EXACTLY.evaluate(10.0, 10.0), is(true));

        assertThat(Condition.EXACTLY.evaluate(0.0, 1.0), is(false));
        assertThat(Condition.EXACTLY.evaluate(1.0, 0.0), is(false));
        assertThat(Condition.EXACTLY.evaluate(-1.0, 0.0), is(false));
        assertThat(Condition.EXACTLY.evaluate(-1.0, -0.9), is(false));
    }

    @Test
    public void testBelow() {
        assertThat(Condition.BELOW.evaluate(0.0, 1.0), is(true));
        assertThat(Condition.BELOW.evaluate(1.0, 2.0), is(true));
        assertThat(Condition.BELOW.evaluate(-2.0, -1.0), is(true));
        assertThat(Condition.BELOW.evaluate(-3.0, -1.0), is(true));
        assertThat(Condition.BELOW.evaluate(3.0, 10.0), is(true));

        assertThat(Condition.BELOW.evaluate(0.0, 0.0), is(false));
        assertThat(Condition.BELOW.evaluate(1.0, 0.0), is(false));
        assertThat(Condition.BELOW.evaluate(-1.0, -2.0), is(false));
        assertThat(Condition.BELOW.evaluate(-1.0, -1.1), is(false));
    }

}
