package com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.prediction.impl.standard.config.ScalingPoliciesConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy.ScalingPolicyEnforcer;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Verifies the configuration behavior of the {@link ScalingPolicyEnforcer}.
 */
public class TestScalingPolicyEnforcerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(TestScalingPolicyEnforcerConfiguration.class);

    private static final double machineDeltaTolerance = 0.1d;
    private static final TimeInterval overprovisioningGracePeriod = TimeInterval.seconds(300);

    /** Object under test. */
    private ScalingPolicyEnforcer policyEnforcer;

    @Before
    public void onSetup() {
        this.policyEnforcer = new ScalingPolicyEnforcer(logger);
    }

    @Test
    public void configureWithDefaultConfiguration() {
        ScalingPoliciesConfig config = new ScalingPoliciesConfig(null, null);
        this.policyEnforcer.validate(config);
        this.policyEnforcer.configure(config);
        assertThat(this.policyEnforcer.getConfiguration(), is(config));
    }

    @Test
    public void configureWithCustomConfiguration() {
        ScalingPoliciesConfig config = new ScalingPoliciesConfig(machineDeltaTolerance, overprovisioningGracePeriod);
        this.policyEnforcer.validate(config);
        this.policyEnforcer.configure(config);
        assertThat(this.policyEnforcer.getConfiguration(), is(config));
    }

    @Test
    public void reconfigure() {
        // configure
        ScalingPoliciesConfig config = new ScalingPoliciesConfig(null, null);
        this.policyEnforcer.validate(config);
        this.policyEnforcer.configure(config);
        assertThat(this.policyEnforcer.getConfiguration(), is(config));

        // re-configure
        config = new ScalingPoliciesConfig(machineDeltaTolerance, overprovisioningGracePeriod);
        this.policyEnforcer.validate(config);
        this.policyEnforcer.configure(config);
        assertThat(this.policyEnforcer.getConfiguration(), is(config));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithNull() {
        this.policyEnforcer.validate(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithBadMachineDeltaTolerance() {
        this.policyEnforcer.validate(new ScalingPoliciesConfig(-1.0, overprovisioningGracePeriod));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithBadOverprovisioningGracePeriod() {
        TimeInterval negativePeriod = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": -1, \"unit\": \"seconds\"}"), TimeInterval.class);
        this.policyEnforcer.validate(new ScalingPoliciesConfig(0.0, negativePeriod));
    }

}
