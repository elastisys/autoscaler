package com.elastisys.autoscaler.core.prediction.impl.standard.capacitylimit;

import static com.elastisys.autoscaler.core.prediction.impl.standard.capacitylimit.CapacityLimitTestUtils.config;
import static com.elastisys.autoscaler.core.prediction.impl.standard.capacitylimit.CapacityLimitTestUtils.configs;
import static com.elastisys.autoscaler.core.prediction.impl.standard.capacitylimit.CapacityLimitTestUtils.maxLimitEvent;
import static com.elastisys.autoscaler.core.prediction.impl.standard.capacitylimit.CapacityLimitTestUtils.minLimitEvent;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Optional;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Verifies that the {@link CapacityLimitRegistry} posts the proper
 * min/max-activation events on the event bus.
 */
public class TestCapacityLimitRegistryEventing {
    static Logger logger = LoggerFactory.getLogger(TestCapacityLimitRegistryEventing.class);

    private EventBus eventBus = mock(EventBus.class);

    /** Object under test. */
    private CapacityLimitRegistry limitRegistry;

    @Before
    public void onSetup() {
        // Note: to see what events are posted on the bus, replace the mocked
        // eventbus with a real one and register this object as a listener.
        // this.eventBus = new
        // AsyncEventBus(Executors.newSingleThreadExecutor());
        // this.eventBus.register(this);

        this.limitRegistry = new CapacityLimitRegistry(logger, this.eventBus);
    }

    @Subscriber
    public void onEvent(Object event) {
        logger.debug("event: " + event);
    }

    @Test
    public void checkPostedMinMaxLimitEvents() {
        this.limitRegistry.configure(configs(config("l1", 1, "* * * * * ? *", 2, 4),
                config("l2", 2, "* * 10-21 ? * FRI *", 5, 10), config("l3", 3, "* * 12-13 ? * FRI *", 7, 14)));
        Optional<Double> prediction1 = Optional.of(1.0);

        // baseline (l1) rule should be active
        DateTime monday = UtcTime.parse("2013-01-07T12:00:00.000Z");
        assertThat(this.limitRegistry.limit(prediction1, monday).get(), is(2));
        verify(this.eventBus).post(minLimitEvent(2, monday, "l1"));
        verify(this.eventBus).post(maxLimitEvent(4, monday, "l1"));
        reset(this.eventBus);

        // friday rule (l2) should be active
        DateTime fridayNight = UtcTime.parse("2013-01-04T20:00:00.000Z");
        assertThat(this.limitRegistry.limit(prediction1, fridayNight).get(), is(5));
        verify(this.eventBus).post(minLimitEvent(5, fridayNight, "l2"));
        verify(this.eventBus).post(maxLimitEvent(10, fridayNight, "l2"));
        reset(this.eventBus);

        // friday noon rule (l3) should be active
        DateTime fridayNoon = UtcTime.parse("2013-01-04T12:00:00.000Z");
        assertThat(this.limitRegistry.limit(prediction1, fridayNoon).get(), is(7));
        verify(this.eventBus).post(minLimitEvent(7, fridayNoon, "l3"));
        verify(this.eventBus).post(maxLimitEvent(14, fridayNoon, "l3"));

        verifyNoMoreInteractions(this.eventBus);
    }

    /**
     * No min-/max- limit activation should be reported when prediction is
     * absent (and no capacity limit needs to be applied).
     */
    @Test
    public void checkPostedEventsWhenPredictionIsEmpty() {
        this.limitRegistry.configure(configs(config("l1", 1, "* * * * * ? *", 2, 4)));
        Optional<Double> absent = Optional.empty();
        Optional<Integer> limit = this.limitRegistry.limit(absent, UtcTime.now());
        assertThat(limit.isPresent(), is(false));

        // no events should be posted in this case
        verifyNoMoreInteractions(this.eventBus);
    }

    /**
     * No min-/max- limit activation should be reported when no capacity limits
     * are configured.
     */
    @Test
    public void checkPostedEventsWhenNoLimitRulesAreConfigured() {
        this.limitRegistry.configure(configs());
        Optional<Double> prediction = Optional.of(2.0);
        Optional<Integer> limit = this.limitRegistry.limit(prediction, UtcTime.now());
        assertThat(limit.isPresent(), is(true));
        assertThat(limit.get(), is(2));

        // no events should be posted in this case
        verifyNoMoreInteractions(this.eventBus);
    }

}
