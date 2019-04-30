package com.elastisys.autoscaler.predictors.rulebased.rule;

import static com.elastisys.autoscaler.predictors.rulebased.rule.ResizeUnit.INSTANCES;
import static com.elastisys.autoscaler.predictors.rulebased.rule.ResizeUnit.PERCENT;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.predictors.rulebased.rule.Condition;
import com.elastisys.autoscaler.predictors.rulebased.rule.ResizeUnit;
import com.elastisys.autoscaler.predictors.rulebased.rule.ScalingRule;
import com.elastisys.autoscaler.predictors.rulebased.rule.ScalingRule.RuleOutcome;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Exercises the {@link ScalingRule} class.
 */
public class TestScalingRule {

    private ScalingRule rule;

    @Before
    public void onSetup() {
        // "add 1 instance when metric values have been > 80.0 for 180s"
        this.rule = new ScalingRule(Condition.ABOVE, 80.0, TimeInterval.seconds(180), 1.0, ResizeUnit.INSTANCES);
    }

    private boolean isSatisfiedBy(List<DataPoint> series) {
        RuleOutcome outcome = this.rule.isSatisfiedBy(series);
        return outcome.isSatisfied();
    }

    @Test(expected = NullPointerException.class)
    public void evaluateAgainstNullMetricSeries() {
        this.rule.isSatisfiedBy(null);
    }

    /**
     * Rule should never evaluate to true for a singleton series (it has an
     * undefined period of threshold condition satisfaction).
     */
    @Test
    public void evaluateAgainstSingletonMetricSeries() {
        assertThat(isSatisfiedBy(values(value(85.0, 0))), is(false));
    }

    /**
     * Test against metric series that for which the rule should trigger.
     */
    @Test
    public void evaluateAgainstSatisfyingMetricSeries() {
        // all values satisfying threshold condition for sufficiently long
        List<DataPoint> series = values(value(85.0, 0), value(85.0, 180));
        assertThat(isSatisfiedBy(series), is(true));
        series = values(value(85.0, 0), value(85.0, 240));
        assertThat(isSatisfiedBy(series), is(true));
        series = values(value(95.0, 0), value(80.1, 120), value(81.0, 180), value(85.0, 240));
        assertThat(isSatisfiedBy(series), is(true));

        // head values don't satisfy condition but tail ones do
        series = values(value(10.0, 0), value(20.0, 60), value(85.0, 120), value(90.0, 300));
        assertThat(isSatisfiedBy(series), is(true));
        series = values(value(10.0, 0), value(20.0, 60), value(85.0, 120), value(81.0, 180), value(90.0, 360));
        assertThat(isSatisfiedBy(series), is(true));

    }

    /**
     * Test against metric series that for which the rule should not trigger.
     */
    @Test
    public void evaluateAgainstNonSatisfyingMetricSeries() {
        // too small values
        List<DataPoint> series = values(value(75.0, 0), value(70.0, 180));
        assertThat(isSatisfiedBy(series), is(false));

        // no current stretch of values where threshold condition is true
        series = values(value(88.0, 0), value(78.0, 100), value(90.0, 180));
        assertThat(isSatisfiedBy(series), is(false));

        // threshold condition hasn't been true long enough
        series = values(value(85.0, 0), value(90.0, 179));
        assertThat(isSatisfiedBy(series), is(false));

        // condition broken
        series = values(value(85.0, 0), value(90.0, 179), value(79.0, 180), value(95.0, 240));
        assertThat(isSatisfiedBy(series), is(false));
        series = values(value(75.0, 0), value(90.0, 179), value(89.0, 180), value(95.0, 240));
        assertThat(isSatisfiedBy(series), is(false));
    }

    @Test
    public void getResizeIncrementWithInstancesResizeUnit() {
        // zero increment
        assertThat(rule(0.0, INSTANCES).getResizeIncrement(0), is(0));
        assertThat(rule(0.0, INSTANCES).getResizeIncrement(10), is(0));

        // positive increments
        assertThat(rule(1.0, INSTANCES).getResizeIncrement(0), is(1));
        assertThat(rule(2.0, INSTANCES).getResizeIncrement(1), is(2));
        assertThat(rule(2.2, INSTANCES).getResizeIncrement(10), is(3));
        assertThat(rule(10.0, INSTANCES).getResizeIncrement(10), is(10));

        // negative increments
        assertThat(rule(-1.0, INSTANCES).getResizeIncrement(0), is(0));
        assertThat(rule(-2.0, INSTANCES).getResizeIncrement(1), is(0));
        assertThat(rule(-2.0, INSTANCES).getResizeIncrement(2), is(-1));
        assertThat(rule(-2.0, INSTANCES).getResizeIncrement(3), is(-2));
        assertThat(rule(-2.2, INSTANCES).getResizeIncrement(2), is(-1));
        assertThat(rule(-2.2, INSTANCES).getResizeIncrement(3), is(-2));
        assertThat(rule(-10.0, INSTANCES).getResizeIncrement(10), is(-9));
        assertThat(rule(-11, INSTANCES).getResizeIncrement(10), is(-9));
    }

    @Test
    public void getResizeIncrementWithPercentResizeUnit() {
        // zero increment
        assertThat(rule(0.0, PERCENT).getResizeIncrement(0), is(0));
        assertThat(rule(0.0, PERCENT).getResizeIncrement(10), is(0));

        // positive increments
        assertThat(rule(10.0, PERCENT).getResizeIncrement(0), is(0));
        assertThat(rule(10.0, PERCENT).getResizeIncrement(1), is(1));
        assertThat(rule(10.0, PERCENT).getResizeIncrement(10), is(1));
        assertThat(rule(10.0, PERCENT).getResizeIncrement(11), is(2));
        assertThat(rule(20.0, PERCENT).getResizeIncrement(10), is(2));
        assertThat(rule(20.0, PERCENT).getResizeIncrement(11), is(3));

        // negative increments
        assertThat(rule(-10.0, PERCENT).getResizeIncrement(0), is(0));
        assertThat(rule(-10.0, PERCENT).getResizeIncrement(1), is(0));
        assertThat(rule(-10.0, PERCENT).getResizeIncrement(10), is(-1));
        assertThat(rule(-10.0, PERCENT).getResizeIncrement(11), is(-2));
        assertThat(rule(-20.0, PERCENT).getResizeIncrement(10), is(-2));
        assertThat(rule(-20.0, PERCENT).getResizeIncrement(11), is(-3));
        assertThat(rule(-40.0, PERCENT).getResizeIncrement(2), is(-1));
        assertThat(rule(-80.0, PERCENT).getResizeIncrement(2), is(-1));
        assertThat(rule(-110.0, PERCENT).getResizeIncrement(1), is(0));
        assertThat(rule(-110.0, PERCENT).getResizeIncrement(2), is(-1));
    }

    private static ScalingRule rule(double resizeIncrement, ResizeUnit resizeUnit) {
        return new ScalingRule(Condition.EXACTLY, 0.0, TimeInterval.seconds(180), resizeIncrement, resizeUnit);
    }

    private static DataPoint value(double value, int secondsFromEpoch) {
        return new MetricValue("metric", value, new DateTime(secondsFromEpoch * 1000, DateTimeZone.UTC));
    }

    private static List<DataPoint> values(DataPoint... values) {
        return Arrays.asList(values);
    }

}
