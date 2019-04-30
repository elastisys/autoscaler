package com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy;

import static com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy.ScalingPolicyTestUtils.prediction;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.prediction.impl.standard.api.ScalingPolicy;
import com.elastisys.autoscaler.core.prediction.impl.standard.scalingpolicy.ScalingPolicyChain;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Verifies the behavior of the {@link ScalingPolicyChain}.
 */
@SuppressWarnings("unchecked")
public class TestScalingPolicyChain {
    private static final Logger LOG = LoggerFactory.getLogger(TestScalingPolicyChain.class);

    private final ScalingPolicy policy1 = mock(ScalingPolicy.class);
    private final ScalingPolicy policy2 = mock(ScalingPolicy.class);

    @Before
    public void onSetup() {
        FrozenTime.setFixed(UtcTime.parse("2013-01-01T12:00:00.000+02:00"));
    }

    @Test
    public void applyWithEmptyChain() {
        ScalingPolicy policyChain = new ScalingPolicyChain(LOG, new ArrayList<ScalingPolicy>());

        Optional<Double> inputPrediction = prediction(1.0);
        Optional<Double> outputPrediction = policyChain.apply(emptyPool(), inputPrediction);
        assertThat(inputPrediction, is(outputPrediction));
    }

    @Test
    public void applyWithSinglePolicy() {
        ScalingPolicy policyChain = new ScalingPolicyChain(LOG, asList(this.policy1));
        Optional<Double> policy1output = Optional.of(2.0);
        whenInvoked(this.policy1).thenReturn(policy1output);

        Optional<Double> inputPrediction = prediction(1.0);
        Optional<Double> outputPrediction = policyChain.apply(emptyPool(), inputPrediction);
        assertThat(outputPrediction, is(policy1output));
    }

    /**
     * Test chaining of multiple scaling policies (and that the output of each
     * {@link ScalingPolicy} is passed as input to the next
     * {@link ScalingPolicy}).
     */
    @Test
    public void applyWithMultiplePolicies() {
        ScalingPolicy policyChain = new ScalingPolicyChain(LOG, asList(this.policy1, this.policy2));
        Optional<Double> inputPrediction = prediction(0.5);
        Optional<Double> policy1output = Optional.of(1.0);
        Optional<Double> policy2output = Optional.of(2.0);
        whenInvoked(this.policy1, inputPrediction).thenReturn(policy1output);
        whenInvoked(this.policy2, policy1output).thenReturn(policy2output);

        Optional<Double> outputPrediction = policyChain.apply(emptyPool(), inputPrediction);
        assertThat(outputPrediction, is(policy2output));
    }

    private OngoingStubbing<Optional<Double>> whenInvoked(ScalingPolicy policy) {
        Class<Optional<Double>> doubleOptionalClass = (Class<Optional<Double>>) Optional.of(1.0).getClass();
        return when(policy.apply(any(Optional.class), any(doubleOptionalClass)));
    }

    private OngoingStubbing<Optional<Double>> whenInvoked(ScalingPolicy policy, Optional<Double> withInput) {
        return when(policy.apply(any(Optional.class), argThat(is(withInput))));
    }

    private Optional<PoolSizeSummary> emptyPool() {
        return Optional.of(new PoolSizeSummary(0, 0, 0));
    }
}
