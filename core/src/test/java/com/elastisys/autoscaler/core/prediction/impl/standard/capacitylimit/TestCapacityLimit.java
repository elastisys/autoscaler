package com.elastisys.autoscaler.core.prediction.impl.standard.capacitylimit;

import static com.elastisys.autoscaler.core.prediction.impl.standard.capacitylimit.CapacityLimitTestUtils.inEffectAt;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.autoscaler.core.prediction.impl.standard.config.CapacityLimitConfig;

public class TestCapacityLimit {

    /**
     * Verifies that a capacity limit can be set up that is active for the
     * (entire) last day of each month.
     */
    @Test
    public void testEntireLastDayOfMonthLimit() {
        // The entire last day of every month -- that is, between [00:00, 24:00)
        String cronExpression = "* * 00-23 L * ? *";
        CapacityLimitConfig limit = new CapacityLimitConfig("mylimit", 1L, cronExpression, 1, 2);
        // verify rule effectiveness on last day of November
        assertFalse(inEffectAt(limit, "2011-11-29T23:59:59.000Z"));
        assertTrue(inEffectAt(limit, "2011-11-30T00:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-11-30T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-11-30T23:59:59.000Z"));
        assertFalse(inEffectAt(limit, "2011-12-01T00:00:00.000Z"));
        // verify rule effectiveness on last day of October
        assertFalse(inEffectAt(limit, "2011-10-30T23:59:59.000Z"));
        assertTrue(inEffectAt(limit, "2011-10-31T00:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-10-31T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-10-31T23:59:59.000Z"));
        assertFalse(inEffectAt(limit, "2011-11-01T00:00:00.000Z"));
    }

    /**
     * Verifies that a capacity limit can be set up that is active for part of
     * the last day of each month: 10:00:00 - 21:59:59
     */
    @Test
    public void testPartialLastDayOfMonthLimit() {
        // Every last day of month between [10:00, 22:00)
        String cronExpression = "* * 10-21 L * ? *";
        CapacityLimitConfig limit = new CapacityLimitConfig("mylimit", 1L, cronExpression, 1, 2);
        assertFalse(inEffectAt(limit, "2011-11-30T09:59:59.000Z"));
        assertTrue(inEffectAt(limit, "2011-11-30T10:00:00Z"));
        assertTrue(inEffectAt(limit, "2011-11-30T21:59:59Z"));
        assertFalse(inEffectAt(limit, "2011-11-30T22:00:00Z"));
    }

    /**
     * Verifies that a capacity limit can be set up that is active every Friday
     */
    @Test
    public void testEveryFridayLimit() {
        // Every Friday between 00:00:00 and 23:59:59
        String cronExpression = "* * 00-23 ? * FRI *";
        CapacityLimitConfig limit = new CapacityLimitConfig("mylimit", 1L, cronExpression, 1, 2);
        // verify effectiveness only on Fridays ...
        assertFalse(inEffectAt(limit, "2011-11-07T12:00:00.000Z")); // Monday
        assertFalse(inEffectAt(limit, "2011-11-08T12:00:00.000Z")); // Tuesday
        assertFalse(inEffectAt(limit, "2011-11-09T12:00:00.000Z")); // Wednesday
        assertFalse(inEffectAt(limit, "2011-11-10T12:00:00.000Z")); // Thursday
        assertTrue(inEffectAt(limit, "2011-11-11T12:00:00.000Z")); // Friday
        assertFalse(inEffectAt(limit, "2011-11-12T12:00:00.000Z")); // Saturday
        assertFalse(inEffectAt(limit, "2011-11-13T12:00:00.000Z")); // Sunday

        // verify that limit is active on some other Fridays ...
        assertTrue(inEffectAt(limit, "2011-10-14T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-10-21T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-10-28T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-11-04T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-11-11T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-11-18T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-11-25T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-12-02T12:00:00.000Z"));

        // verify some more timestamps within a Friday ...
        assertTrue(inEffectAt(limit, "2011-11-04T00:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-11-04T23:59:59.000Z"));
    }

    /**
     * Verifies that a capacity limit can be set up that is active on the last
     * weekday of every month.
     */
    @Test
    public void testLastWeekdayLimit() {
        // The last weekday of every month between 10:00 and 22:00
        String cronExpression = "* * 00-23 LW * ? *";
        CapacityLimitConfig limit = new CapacityLimitConfig("mylimit", 1L, cronExpression, 1, 2);
        // verify effectiveness only on last weekday of a particular month ...
        assertFalse(inEffectAt(limit, "2011-11-28T12:00:00.000Z")); // Monday
        assertFalse(inEffectAt(limit, "2011-11-29T12:00:00.000Z")); // Tuesday
        assertTrue(inEffectAt(limit, "2011-11-30T12:00:00.000Z")); // Wednesday
        assertFalse(inEffectAt(limit, "2011-12-01T12:00:00.000Z")); // Thursday

        // verify that limit is active on some other last month weekdays ...
        assertTrue(inEffectAt(limit, "2011-01-31T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-02-28T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-03-31T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-04-29T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-05-31T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-06-30T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-07-29T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-08-31T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-09-30T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-10-31T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-11-30T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-12-30T12:00:00.000Z"));

        // verify some more timestamps within a last weekday of a month ...
        assertTrue(inEffectAt(limit, "2011-04-29T00:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-04-29T23:59:59.000Z"));
    }

    /**
     * Verify that a capacity limit can be created that is always active.
     */
    @Test
    public void testAlwaysActiveCapacityLimit() {
        CapacityLimitConfig limit = new CapacityLimitConfig("L1", 1L, "* * * * * ? *", 1, 2);
        // verify that limit is active on several years ...
        assertTrue(inEffectAt(limit, "2011-01-01T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2012-01-01T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2013-01-01T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2014-01-01T12:00:00.000Z"));

        // verify that limit is active on several months ...
        assertTrue(inEffectAt(limit, "2011-01-01T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-02-01T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-03-01T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-04-01T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-10-01T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-11-01T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-12-01T12:00:00.000Z"));

        // verify that limit is active on several days within a month ...
        assertTrue(inEffectAt(limit, "2011-02-01T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-02-02T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-02-03T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-02-27T12:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-02-28T12:00:00.000Z"));

        // verify that limit is active on several hours within a day ...
        assertTrue(inEffectAt(limit, "2011-02-01T00:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-02-01T01:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-02-01T22:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-02-01T23:59:59.000Z"));

        // verify that limit is active on several minutes within an hour ...
        assertTrue(inEffectAt(limit, "2011-02-01T01:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-02-01T01:01:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-02-01T01:58:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-02-01T01:59:59.000Z"));

        // verify that limit is active on several seconds within a minute ...
        assertTrue(inEffectAt(limit, "2011-02-01T01:00:00.000Z"));
        assertTrue(inEffectAt(limit, "2011-02-01T01:00:01.000Z"));
        assertTrue(inEffectAt(limit, "2011-02-01T01:00:02.000Z"));
        assertTrue(inEffectAt(limit, "2011-02-01T01:00:58.000Z"));
        assertTrue(inEffectAt(limit, "2011-02-01T01:00:59.000Z"));
    }

    @Test
    public void testAgainstNonUtcTime() {
        CapacityLimitConfig limit = new CapacityLimitConfig("L1", 1L, "* * 12-14 * * ? *", 1, 2);
        // 10:00:00 UTC
        assertFalse(limit.inEffectAt(new DateTime("2011-01-01T12:00:00.000+02:00")));
        // 11:00:00 UTC
        assertFalse(limit.inEffectAt(new DateTime("2011-01-01T13:00:00.000+02:00")));

        // 12:00:00 UTC
        assertTrue(limit.inEffectAt(new DateTime("2011-01-01T14:00:00.000+02:00")));
        // 13:00:00 UTC
        assertTrue(limit.inEffectAt(new DateTime("2011-01-01T15:00:00.000+02:00")));
        // 14:00:00 UTC
        assertTrue(limit.inEffectAt(new DateTime("2011-01-01T16:00:00.000+02:00")));

        // 15:00 UTC
        assertFalse(limit.inEffectAt(new DateTime("2011-01-01T17:00:00.000+02:00")));
    }
}
