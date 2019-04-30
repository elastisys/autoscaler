package com.elastisys.autoscaler.core.prediction.impl.standard.capacitylimit;

import static com.elastisys.autoscaler.core.prediction.impl.standard.capacitylimit.CapacityLimitTestUtils.config;
import static com.elastisys.autoscaler.core.prediction.impl.standard.capacitylimit.CapacityLimitTestUtils.configs;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.prediction.impl.standard.config.CapacityLimitConfig;
import com.elastisys.scale.commons.eventbus.EventBus;

/**
 * Verifies the configuration behavior of the {@link CapacityLimitRegistry}.
 */
public class TestCapacityLimitRegistryConfiguration {
    static Logger logger = LoggerFactory.getLogger(TestCapacityLimitRegistryConfiguration.class);

    private EventBus eventBus = mock(EventBus.class);

    /** Object under test. */
    private CapacityLimitRegistry limitRegistry;

    @Before
    public void onSetup() {
        this.limitRegistry = new CapacityLimitRegistry(logger, this.eventBus);
    }

    @Test
    public void configureWithEmptyConfiguration() {
        List<CapacityLimitConfig> config = configs();
        this.limitRegistry.validate(config);
        this.limitRegistry.configure(config);

        assertThat(this.limitRegistry.getConfiguration(), is(config));
    }

    @Test
    public void configureWithSingleLimit() {
        List<CapacityLimitConfig> config = configs(config("l1", 1, "* * * * * ? *", 2, 4));
        this.limitRegistry.validate(config);
        this.limitRegistry.configure(config);

        assertThat(this.limitRegistry.getConfiguration(), is(config));
    }

    @Test
    public void configureWithMultipleLimits() {
        List<CapacityLimitConfig> config = configs(config("l1", 1, "* * * * * ? *", 2, 4),
                config("l2", 2, "* * * * * ? *", 5, 10));
        this.limitRegistry.validate(config);
        this.limitRegistry.configure(config);

        assertThat(this.limitRegistry.getConfiguration(), is(config));
    }

    @Test
    public void reconfigureLimits() {
        // configure
        List<CapacityLimitConfig> config = configs();
        this.limitRegistry.validate(config);
        this.limitRegistry.configure(config);
        assertThat(this.limitRegistry.getConfiguration(), is(config));

        // re-configure
        config = configs(config("l1", 1, "* * * * * ? *", 2, 4));
        this.limitRegistry.validate(config);
        this.limitRegistry.configure(config);
        assertThat(this.limitRegistry.getConfiguration(), is(config));

    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithNull() {
        this.limitRegistry.validate((List<CapacityLimitConfig>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithMissingId() {
        this.limitRegistry.validate(configs(config(null, 1, "* * * * * ? *", 2, 4)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithMissingSchedule() {
        this.limitRegistry.validate(configs(config("l1", 1, null, 2, 4)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithIllegalSchedule() {
        this.limitRegistry.validate(configs(config("l1", 1, "* * *", 2, 4)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithNegativeMinLimit() {
        this.limitRegistry.validate(configs(config("l1", 1, "* * * * * ? *", -1, 4)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithMinLimitGreaterThanMaxLimit() {
        this.limitRegistry.validate(configs(config("l1", 1, "* * * * * ? *", 2, 1)));
    }
}
