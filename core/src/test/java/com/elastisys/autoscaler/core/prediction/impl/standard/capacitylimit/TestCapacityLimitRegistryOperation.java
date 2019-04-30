package com.elastisys.autoscaler.core.prediction.impl.standard.capacitylimit;

import static com.elastisys.autoscaler.core.prediction.impl.standard.capacitylimit.CapacityLimitTestUtils.config;
import static com.elastisys.autoscaler.core.prediction.impl.standard.capacitylimit.CapacityLimitTestUtils.configs;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the application logic of the {@link CapacityLimitRegistry}.
 */
public class TestCapacityLimitRegistryOperation {
    static Logger logger = LoggerFactory.getLogger(TestCapacityLimitRegistryOperation.class);

    private EventBus eventBus = mock(EventBus.class);

    /** Object under test. */
    private CapacityLimitRegistry limitRegistry;

    @Before
    public void onSetup() {
        this.limitRegistry = new CapacityLimitRegistry(logger, this.eventBus);
    }

    @Test
    public void limitOnAbsentPrediction() {
        this.limitRegistry.configure(configs(config("l1", 1, "* * * * * ? *", 2, 4)));
        Optional<Double> absent = Optional.empty();
        Optional<Integer> limit = this.limitRegistry.limit(absent, UtcTime.now());
        assertThat(limit.isPresent(), is(false));
    }

    @Test
    public void limitWithoutLimitRules() {
        this.limitRegistry.configure(configs());
        Optional<Double> prediction = Optional.of(2.0);
        Optional<Integer> limit = this.limitRegistry.limit(prediction, UtcTime.now());
        assertThat(limit.isPresent(), is(true));
        assertThat(limit.get(), is(2));
    }

    @Test
    public void limitOnPredictionBelowMin() {
        this.limitRegistry.configure(configs(config("l1", 1, "* * * * * ? *", 2, 4)));
        Optional<Double> prediction = Optional.of(1.0);
        Optional<Integer> limit = this.limitRegistry.limit(prediction, UtcTime.now());
        assertThat(limit.isPresent(), is(true));
        assertThat(limit.get(), is(2));
    }

    @Test
    public void limitOnPredictionAboveMax() {
        this.limitRegistry.configure(configs(config("l1", 1, "* * * * * ? *", 2, 4)));
        Optional<Double> prediction = Optional.of(6.0);
        Optional<Integer> limit = this.limitRegistry.limit(prediction, UtcTime.now());
        assertThat(limit.isPresent(), is(true));
        assertThat(limit.get(), is(4));
    }

    @Test
    public void limitOnPredictionAlreadyWithinBounds() {
        this.limitRegistry.configure(configs(config("l1", 1, "* * * * * ? *", 2, 4)));
        Optional<Double> prediction = Optional.of(2.0);
        Optional<Integer> limit = this.limitRegistry.limit(prediction, UtcTime.now());
        assertThat(limit.isPresent(), is(true));
        assertThat(limit.get(), is(2));

        prediction = Optional.of(3.0);
        limit = this.limitRegistry.limit(prediction, UtcTime.now());
        assertThat(limit.isPresent(), is(true));
        assertThat(limit.get(), is(3));

        prediction = Optional.of(4.0);
        limit = this.limitRegistry.limit(prediction, UtcTime.now());
        assertThat(limit.isPresent(), is(true));
        assertThat(limit.get(), is(4));
    }

    /**
     * Verifies limit activation when several rules are configured.
     */
    @Test
    public void limitWithMultipleRules() {
        this.limitRegistry.configure(configs(config("l1", 1, "* * * * * ? *", 2, 4),
                config("l2", 2, "* * 10-21 ? * FRI *", 5, 10), config("l3", 3, "* * 12-13 ? * FRI *", 7, 14)));
        Optional<Double> prediction1 = Optional.of(1.0);
        Optional<Double> prediction100 = Optional.of(100.0);

        // baseline (l1) rule should be active
        DateTime monday = UtcTime.parse("2013-01-07T12:00:00.000Z");
        assertThat(this.limitRegistry.limit(prediction1, monday).get(), is(2));
        assertThat(this.limitRegistry.limit(prediction100, monday).get(), is(4));

        // friday rule (l2) should be active
        DateTime fridayNight = UtcTime.parse("2013-01-04T20:00:00.000Z");
        assertThat(this.limitRegistry.limit(prediction1, fridayNight).get(), is(5));
        assertThat(this.limitRegistry.limit(prediction100, fridayNight).get(), is(10));

        // friday noon rule (l3) should be active
        DateTime fridayNoon = UtcTime.parse("2013-01-04T12:00:00.000Z");
        assertThat(this.limitRegistry.limit(prediction1, fridayNoon).get(), is(7));
        assertThat(this.limitRegistry.limit(prediction100, fridayNoon).get(), is(14));
    }

    /**
     * Make sure that when fed a fractional prediction, the resulting prediction
     * is always rounded up to the nearest higher integer value.
     *
     * @throws Exception
     */
    @Test
    public void testFractionalRoundUp() throws Exception {
        this.limitRegistry.configure(configs(config("l1", 1, "* * * * * ? *", 2, 5)));
        Optional<Double> prediction = Optional.of(2.2);
        Optional<Integer> limit = this.limitRegistry.limit(prediction, UtcTime.now());
        assertThat(limit.isPresent(), is(true));
        assertThat(limit.get(), is(3));

        prediction = Optional.of(2.8);
        limit = this.limitRegistry.limit(prediction, UtcTime.now());
        assertThat(limit.isPresent(), is(true));
        assertThat(limit.get(), is(3));

        prediction = Optional.of(3.05);
        limit = this.limitRegistry.limit(prediction, UtcTime.now());
        assertThat(limit.isPresent(), is(true));
        assertThat(limit.get(), is(4));

        prediction = Optional.of(3.99);
        limit = this.limitRegistry.limit(prediction, UtcTime.now());
        assertThat(limit.isPresent(), is(true));
        assertThat(limit.get(), is(4));
    }
}
