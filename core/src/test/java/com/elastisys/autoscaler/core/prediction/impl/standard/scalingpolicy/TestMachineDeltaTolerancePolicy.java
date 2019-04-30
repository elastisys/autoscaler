package com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy;

import static com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy.ScalingPolicyTestUtils.prediction;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.prediction.impl.standard.api.ScalingPolicy;
import com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy.MachineDeltaTolerancePolicy;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;

/**
 * Exercises the {@link MachineDeltaTolerancePolicy}.
 */
public class TestMachineDeltaTolerancePolicy {
    private final static Logger LOG = LoggerFactory.getLogger(TestMachineDeltaTolerancePolicy.class);

    @Test
    public void apply() {
        ScalingPolicy policy = new MachineDeltaTolerancePolicy(LOG, 0.10);

        // too small difference: suppress scale-up suggestion
        assertThat(policy.apply(desiredSize(1), prediction(1.05)), is(prediction(1.0)));
        // too small difference: suppress scale-down suggestion
        assertThat(policy.apply(desiredSize(2), prediction(1.95)), is(prediction(2.0)));
        // too small difference: keep pool size
        assertThat(policy.apply(desiredSize(1), prediction(1.0)), is(prediction(1.0)));

        // sufficient difference: allow scale-up suggestion
        assertThat(policy.apply(desiredSize(1), prediction(1.15)), is(prediction(1.15)));
        // sufficient difference: allow scale-down suggestion
        assertThat(policy.apply(desiredSize(2), prediction(1.80)), is(prediction(1.80)));

    }

    @Test(expected = IllegalArgumentException.class)
    public void applyToNullPool() {
        ScalingPolicy policy = new MachineDeltaTolerancePolicy(LOG, 0.10);
        policy.apply(null, prediction(1.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void applyToNullPrediction() {
        ScalingPolicy policy = new MachineDeltaTolerancePolicy(LOG, 0.10);
        policy.apply(desiredSize(3), null);
    }

    /**
     * When the current pool size is unknown (for example, due to the cloud
     * pool/cloud provider being temporarily malfunctioning), the
     * {@link MachineDeltaTolerancePolicy} will not perform any enforcement, but
     * just let the prediction pass through (there is nothing it can do without
     * knowing the current pool size).
     * <p/>
     * Although this may lead to some unwanted oscillations at times of pool
     * outage, this inconvenience is hopefully small in comparison to the outage
     * itself.
     */
    @Test
    public void applyWhenCurrentPoolSizeUnknown() {
        ScalingPolicy policy = new MachineDeltaTolerancePolicy(LOG, 0.10);
        Optional<PoolSizeSummary> absentPoolSize = Optional.empty();
        Optional<Double> result = policy.apply(absentPoolSize, prediction(10.0));
        assertThat(result.get(), is(10.0));

        result = policy.apply(absentPoolSize, prediction(100.0));
        assertThat(result.get(), is(100.0));
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
