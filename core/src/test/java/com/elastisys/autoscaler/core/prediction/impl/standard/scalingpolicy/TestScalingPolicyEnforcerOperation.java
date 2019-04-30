package com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy;

import static com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy.ScalingPolicyTestUtils.prediction;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.prediction.impl.standard.api.ScalingPolicy;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.ScalingPoliciesConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy.MachineDeltaTolerancePolicy;
import com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy.OverprovisioningGracePeriodPolicy;
import com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy.ScalingPolicyEnforcer;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Verifies the operation of the {@link ScalingPolicyEnforcer}.
 */
@SuppressWarnings("unchecked")
public class TestScalingPolicyEnforcerOperation {
    private static final Logger logger = LoggerFactory.getLogger(TestScalingPolicyEnforcerOperation.class);

    /** Object under test. */
    private ScalingPolicyEnforcer policyEnforcer;

    @Before
    public void onSetup() {
        this.policyEnforcer = new ScalingPolicyEnforcer(logger);
        FrozenTime.setFixed(UtcTime.parse("2015-01-01T12:00:00.000Z"));
    }

    @Test
    public void withMachineDeltaTolerancePolicy() {
        ScalingPoliciesConfig config = new ScalingPoliciesConfig(0.1, TimeInterval.seconds(0));
        this.policyEnforcer.validate(config);
        this.policyEnforcer.configure(config);

        // check policies
        assertPolicies(policyClasses(MachineDeltaTolerancePolicy.class), this.policyEnforcer.getScalingPolicies());

        /*
         * apply to predictions
         */

        // too small difference => ignore scale-up suggestion
        assertThat(this.policyEnforcer.apply(desiredSize(1), prediction(1.05)), is(prediction(1.0)));
        // too small difference => ignore scale-down suggestion
        assertThat(this.policyEnforcer.apply(desiredSize(1), prediction(0.95)), is(prediction(1.0)));

        // sufficient difference => allow scale-up suggestion
        assertThat(this.policyEnforcer.apply(desiredSize(1), prediction(1.15)), is(prediction(1.15)));
        // sufficient difference => allow scale-down suggestion
        assertThat(this.policyEnforcer.apply(desiredSize(1), prediction(0.80)), is(prediction(0.8)));
    }

    @Test
    public void withOverprovisioningGracePeriodPolicy() {
        ScalingPoliciesConfig config = new ScalingPoliciesConfig(0.0, TimeInterval.seconds(600));
        this.policyEnforcer.validate(config);
        this.policyEnforcer.configure(config);

        // check policies
        assertPolicies(policyClasses(OverprovisioningGracePeriodPolicy.class),
                this.policyEnforcer.getScalingPolicies());

        /*
         * apply to predictions
         */

        // to scale-up prediction => should always be allowed.
        assertThat(this.policyEnforcer.apply(desiredSize(1), prediction(1.05)), is(prediction(1.05)));

        // not sufficient time of overprovisioning => suppress
        assertThat(this.policyEnforcer.apply(desiredSize(2), prediction(1.0)), is(prediction(2.0)));
        // not sufficient time of overprovisioning => suppress
        FrozenTime.tick(300);
        assertThat(this.policyEnforcer.apply(desiredSize(2), prediction(1.0)), is(prediction(2.0)));
        FrozenTime.tick(300);
        // sufficient time of overprovisioning => allow
        assertThat(this.policyEnforcer.apply(desiredSize(2), prediction(1.0)), is(prediction(1.0)));
    }

    /**
     * When the current pool size is unknown (for example, due to the cloud
     * pool/cloud provider being temporarily malfunctioning), most scaling
     * policies can not perform any enforcement, but should just let the
     * prediction pass through (there is nothing to enforce without knowing the
     * current pool size).
     * <p/>
     * Although this may lead to some unwanted oscillations at times of pool
     * outage, this inconvenience is hopefully small in comparison to the outage
     * itself.
     */
    @Test
    public void applyWhenCurrentPoolSizeUnknown() throws Exception {
        ScalingPoliciesConfig config = new ScalingPoliciesConfig(0.1, TimeInterval.seconds(600));
        this.policyEnforcer.validate(config);
        this.policyEnforcer.configure(config);

        Optional<PoolSizeSummary> poolSize = Optional.empty();
        assertThat(this.policyEnforcer.apply(poolSize, prediction(10.5)), is(prediction(10.5)));
    }

    @Test
    public void withMultiplePolicies() {
        ScalingPoliciesConfig config = new ScalingPoliciesConfig(0.1, TimeInterval.seconds(600));
        this.policyEnforcer.validate(config);
        this.policyEnforcer.configure(config);

        // check policies
        assertPolicies(policyClasses(MachineDeltaTolerancePolicy.class, OverprovisioningGracePeriodPolicy.class),
                this.policyEnforcer.getScalingPolicies());
    }

    private void assertPolicies(List<Class<? extends ScalingPolicy>> expectedPolicies,
            List<ScalingPolicy> actualPolicies) {
        assertThat(actualPolicies.size(), is(expectedPolicies.size()));
        for (int i = 0; i < actualPolicies.size(); i++) {
            assertThat(actualPolicies.get(i), is(instanceOf(expectedPolicies.get(i))));
        }
    }

    private List<Class<? extends ScalingPolicy>> policyClasses(Class<? extends ScalingPolicy>... policyClasses) {
        List<Class<? extends ScalingPolicy>> classes = new ArrayList<>();
        for (Class<? extends ScalingPolicy> scalingPolicy : policyClasses) {
            classes.add(scalingPolicy);
        }
        return classes;
    }

    /**
     * A {@link PoolSizeSummary} with a given desired size (allocated and active
     * sizes of pool ignored since field is not involved in any policy
     * calculations).
     *
     * @param desired
     * @return
     */
    private Optional<PoolSizeSummary> desiredSize(int desired) {
        // for the prediction policy, allocated and active size doesn't matter
        return Optional.of(new PoolSizeSummary(desired, 0, 0));
    }

}
