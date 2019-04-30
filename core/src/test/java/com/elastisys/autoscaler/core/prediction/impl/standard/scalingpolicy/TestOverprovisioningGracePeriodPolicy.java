package com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy;

import static com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy.ScalingPolicyTestUtils.prediction;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.prediction.impl.standard.api.ScalingPolicy;
import com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy.MachineDeltaTolerancePolicy;
import com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy.OverprovisioningGracePeriodPolicy;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link OverprovisioningGracePeriodPolicy}.
 */
public class TestOverprovisioningGracePeriodPolicy {
    private final static Logger LOG = LoggerFactory.getLogger(TestOverprovisioningGracePeriodPolicy.class);

    private static final TimeInterval overprovisioningGracePeriod = TimeInterval.seconds(600);

    @Before
    public void onSetup() {
        FrozenTime.setFixed(UtcTime.parse("2015-01-01T12:00:00.000Z"));
    }

    @Test
    public void applyToScaleDownPrediction() {
        // ten minute (600 second) grace period before allowing scale-down to go
        // through
        ScalingPolicy policy = new OverprovisioningGracePeriodPolicy(LOG, overprovisioningGracePeriod);

        // not sufficient time of overprovisioning: suppress scale-down
        assertThat(policy.apply(desiredSize(2), prediction(1.0)), is(prediction(2.0)));
        // still not sufficient time of overprovisioning: suppress scale-down
        FrozenTime.tick(200);
        assertThat(policy.apply(desiredSize(2), prediction(1.0)), is(prediction(2.0)));
        // still not sufficient time of overprovisioning: suppress scale-down
        FrozenTime.tick(200);
        assertThat(policy.apply(desiredSize(2), prediction(1.0)), is(prediction(2.0)));
        // sufficient time of overprovisioning: allow scale-down
        FrozenTime.tick(200);
        assertThat(policy.apply(desiredSize(2), prediction(1.0)), is(prediction(1.0)));
    }

    /**
     * Verifies that the {@link OverprovisioningGracePeriodPolicy} can correctly
     * identify a scale-down suggestion and tracks when it observed the first
     * such suggestion, in an uninterrupted series of scale-down predictions.
     */
    @Test
    public void verifyTrackingOfFirstScaleDownDecision() {
        DateTime t = UtcTime.now();
        // ten minute (600 second) grace period before allowing scale-down to go
        // through
        OverprovisioningGracePeriodPolicy policy = new OverprovisioningGracePeriodPolicy(LOG,
                overprovisioningGracePeriod);

        // no first observed scale-down prediction yet
        Optional<DateTime> absent = Optional.empty();
        assertThat(policy.getFirstScaleDownPrediction(), is(absent));

        // not a scale-down prediction (1.05 indicates a needed capacity of 2)
        DateTime t1 = t.plusSeconds(60);
        assertThat(policy.apply(desiredSize(2), prediction(1.05)), is(prediction(1.05)));
        assertThat(policy.getFirstScaleDownPrediction(), is(absent));

        // scale-down prediction (0.95 indicates a needed capacity of 1)
        FrozenTime.tick(60);
        DateTime t2 = UtcTime.now();
        // not sufficient period of scale-down => suppress
        assertThat(policy.apply(desiredSize(2), prediction(0.95)), is(prediction(2)));
        // start tracking first scale-down prediction
        assertThat(policy.getFirstScaleDownPrediction().get(), is(t2));

        // scale-down prediction (0.90 indicates a needed capacity of 1)
        FrozenTime.tick(60);
        // not sufficient period of scale-down => suppress
        assertThat(policy.apply(desiredSize(2), prediction(0.90)), is(prediction(2)));
        // still same time for first scale-down prediction
        assertThat(policy.getFirstScaleDownPrediction().get(), is(t2));

        // verify that first scale-down time is reset when not a scale-down
        // suggestion
        FrozenTime.tick(60);
        assertThat(policy.apply(desiredSize(2), prediction(1.03)), is(prediction(1.03)));
        // first scale-down suggestion time reset
        assertThat(policy.getFirstScaleDownPrediction(), is(absent));

        // new scale-down prediction (0.90 indicates a needed capacity of 1)
        FrozenTime.tick(60);
        DateTime t5 = UtcTime.now();
        // not sufficient period of scale-down => suppress
        assertThat(policy.apply(desiredSize(2), prediction(0.90)), is(prediction(2)));
        // still same time for first scale-down prediction
        assertThat(policy.getFirstScaleDownPrediction().get(), is(t5));

        // scale-down prediction after grace period has passed
        FrozenTime.tick(600);
        // sufficient period of scale-down => pass through
        assertThat(policy.apply(desiredSize(2), prediction(0.85)), is(prediction(0.85)));
        // still same time for first scale-down prediction
        assertThat(policy.getFirstScaleDownPrediction().get(), is(t5));
    }

    @Test
    public void applyToNonScaleDownPrediction() {
        ScalingPolicy policy = new OverprovisioningGracePeriodPolicy(LOG, overprovisioningGracePeriod);
        // scale-up prediction: should always be allowed
        assertThat(policy.apply(desiredSize(1), prediction(1.05)), is(prediction(1.05)));
        // stay put prediction: should always be allowed
        assertThat(policy.apply(desiredSize(1), prediction(1.0)), is(prediction(1.0)));

    }

    @Test(expected = IllegalArgumentException.class)
    public void applyToNullPool() {
        ScalingPolicy policy = new OverprovisioningGracePeriodPolicy(LOG, overprovisioningGracePeriod);
        policy.apply(null, prediction(1.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void applyToNullPrediction() {
        ScalingPolicy policy = new OverprovisioningGracePeriodPolicy(LOG, overprovisioningGracePeriod);
        policy.apply(desiredSize(2), null);
    }

    /**
     * When the current pool size is unknown (for example, due to the cloud
     * pool/cloud provider being temporarily malfunctioning), the
     * {@link MachineDeltaTolerancePolicy} will not perform any enforcement, but
     * just let the prediction pass through (there is nothing it can do without
     * knowing the current pool size).
     * <p/>
     * Although this may lead to some unwanted scale-ins at times of pool
     * outage, this inconvenience is hopefully small in comparison to the outage
     * itself.
     */
    @Test
    public void applyWhenCurrentPoolSizeUnknown() {
        ScalingPolicy policy = new OverprovisioningGracePeriodPolicy(LOG, overprovisioningGracePeriod);
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
