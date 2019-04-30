package com.elastisys.autoscaler.core.prediction.impl.standard;

import static com.elastisys.autoscaler.core.api.types.ServiceStatus.State.STARTED;
import static com.elastisys.autoscaler.core.api.types.ServiceStatus.State.STOPPED;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.ScalingPoliciesConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.StandardPredictionSubsystemConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictionTestUtils;
import com.elastisys.autoscaler.core.prediction.impl.standard.stubs.PredictorStub;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.file.FileUtils;

/**
 * Verifies proper behavior when validating and applying configurations of
 * different kinds to the {@link StandardPredictionSubsystem}.
 */
public class TestStandardPredictionSubsystemConfiguration {

    static Logger logger = LoggerFactory.getLogger(TestStandardPredictionSubsystemConfiguration.class);
    private final EventBus bus = mock(EventBus.class);
    private final ScheduledExecutorService executorService = mock(ScheduledExecutorService.class);

    private static final double machineDeltaTolerance = 0.1d;
    private static final TimeInterval overprovisioningGracePeriod = TimeInterval.seconds(300);

    /** Object under test. */
    private StandardPredictionSubsystem predictionSubsystem;

    @Before
    public void onSetup() {
        MetricStreamer metricStreamer = PredictionTestUtils.createMetricStreamerStub("load.rate", "mem.used.rate",
                "http.total.accesses.rate", "cpu.user.rate");
        MonitoringSubsystem monitoringSubsystem = PredictionTestUtils.createMonitoringSubsystemStub(metricStreamer);

        this.predictionSubsystem = new StandardPredictionSubsystem(logger, this.bus, this.executorService,
                monitoringSubsystem, FileUtils.cwd());
    }

    /**
     * Verifies that validating and applying a complete configuration works.
     */
    @Test
    public void configureWithCompleteConfiguration() {
        StandardPredictionSubsystemConfig config = completeConfig();
        this.predictionSubsystem.validate(config);
        this.predictionSubsystem.configure(config);

        assertThat(this.predictionSubsystem.getPredictorRegistry().getConfiguration(), is(config.getPredictors()));
        assertThat(this.predictionSubsystem.getCapacityMapper().getConfiguration(), is(config.getCapacityMappings()));
        assertThat(this.predictionSubsystem.getAggregator().getConfiguration(), is(config.getAggregator()));
        assertThat(this.predictionSubsystem.getScalingPolicyEnforcer().getConfiguration(),
                is(config.getScalingPolicies()));
        assertThat(this.predictionSubsystem.getCapacityLimitRegistry().getConfiguration(),
                is(config.getCapacityLimits()));
    }

    /**
     * Verifies that validating and applying a minimal configuration works.
     */
    @Test
    public void configureWithMinimalConfiguration() {
        StandardPredictionSubsystemConfig config = minimalConfig();
        this.predictionSubsystem.validate(config);
        this.predictionSubsystem.configure(config);

        assertThat(this.predictionSubsystem.getPredictorRegistry().getConfiguration(), is(config.getPredictors()));
        assertThat(this.predictionSubsystem.getCapacityMapper().getConfiguration(), is(config.getCapacityMappings()));
        assertThat(this.predictionSubsystem.getAggregator().getConfiguration(), is(config.getAggregator()));
        assertThat(this.predictionSubsystem.getScalingPolicyEnforcer().getConfiguration(),
                is(config.getScalingPolicies()));
        assertThat(this.predictionSubsystem.getCapacityLimitRegistry().getConfiguration(),
                is(config.getCapacityLimits()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithNull() {
        this.predictionSubsystem.validate(null);
    }

    /**
     * It is okay for configuration to not contain any predictors. Default is an
     * empty list.
     */
    @Test
    public void configureWithMissingPredictors() {
        this.predictionSubsystem.validate(configMissingPredictors());
        this.predictionSubsystem.configure(configMissingPredictors());
        assertThat(this.predictionSubsystem.getPredictorRegistry().getConfiguration(), is(Collections.emptyList()));
    }

    /**
     * It is okay for configuration to not contain any capacity mappings.
     * Default is an empty list.
     */
    @Test
    public void configureWithMissingCapacityMappings() {
        this.predictionSubsystem.validate(configMissingCapacityMappings());
        this.predictionSubsystem.configure(configMissingCapacityMappings());
        assertThat(this.predictionSubsystem.getCapacityMapper().getConfiguration(), is(Collections.emptyList()));
    }

    /**
     * Verify validation failure of incomplete configuration.
     */
    @Test(expected = IllegalArgumentException.class)
    public void configureWithMissingAggregator() {
        this.predictionSubsystem.validate(configMissingAggregator());
    }

    /**
     * It is okay for configuration to not contain any capacity limits. Default
     * is an empty list.
     */
    @Test
    public void configureWithMissingCapacityLimits() {
        this.predictionSubsystem.validate(configMissingCapacityLimits());
        this.predictionSubsystem.configure(configMissingCapacityLimits());
        assertThat(this.predictionSubsystem.getCapacityLimitRegistry().getConfiguration(), is(Collections.emptyList()));
    }

    /**
     * It is okay for configuration to not contain any scaling policies. A
     * default is provided.
     */
    @Test
    public void configureWithMissingScalingPolicies() {
        this.predictionSubsystem.validate(configMissingScalingPolicies());
        this.predictionSubsystem.configure(configMissingScalingPolicies());
        assertThat(this.predictionSubsystem.getScalingPolicyEnforcer().getConfiguration(),
                is(StandardPredictionSubsystemConfig.DEFAULT_SCALING_POLICIES));
    }

    @Test
    public void reconfigureWhenStopped() {
        // configure and verify configuration and state
        this.predictionSubsystem.configure(completeConfig());
        assertThat(this.predictionSubsystem.getConfiguration(), is(completeConfig()));
        assertThat(this.predictionSubsystem.getStatus().getState(), is(STOPPED));

        // re-configure and check that config was updated and state unchanged
        this.predictionSubsystem.configure(completeConfig2());
        assertThat(this.predictionSubsystem.getConfiguration(), is(completeConfig2()));
        assertThat(this.predictionSubsystem.getStatus().getState(), is(STOPPED));
    }

    @Test
    public void reconfigureWhenStarted() {
        // configure, start and verify configuration and state
        this.predictionSubsystem.configure(completeConfig());
        this.predictionSubsystem.start();
        assertThat(this.predictionSubsystem.getConfiguration(), is(completeConfig()));
        assertThat(this.predictionSubsystem.getStatus().getState(), is(STARTED));

        // re-configure and check that config was updated and state unchanged
        this.predictionSubsystem.configure(completeConfig2());
        assertThat(this.predictionSubsystem.getConfiguration(), is(completeConfig2()));
        assertThat(this.predictionSubsystem.getStatus().getState(), is(STARTED));
    }

    private StandardPredictionSubsystemConfig minimalConfig() {
        return StandardPredictionSubsystemConfig.Builder.create().withAggregator("Math.max(p1, p2);").build();
    }

    private StandardPredictionSubsystemConfig completeConfig() {
        return StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p1", PredictorStub.class, State.STARTED, "http.total.accesses.rate")
                .withPredictor("p2", PredictorStub.class, State.STARTED, "cpu.user.rate")
                .withCapacityMapping("cpu.user.rate", 100.0).withCapacityMapping("http.total.accesses.rate", 250.0)
                .withAggregator("Math.max(p1, p2);")
                .withScalingPolicies(new ScalingPoliciesConfig(machineDeltaTolerance, overprovisioningGracePeriod))
                .withCapacityLimit("baseline", 1, "* * * * * ? *", 2, 4)
                .withCapacityLimit("fridays", 2, "* * 10-21 ? * FRI *", 20, 40).build();
    }

    private StandardPredictionSubsystemConfig completeConfig2() {
        return StandardPredictionSubsystemConfig.Builder.create()
                .withPredictor("p11", PredictorStub.class, State.STARTED, "mem.used.rate")
                .withPredictor("p21", PredictorStub.class, State.STARTED, "load.rate")
                .withCapacityMapping("mem.used.rate", 1024.0).withCapacityMapping("load.rate", 100.0)
                .withAggregator("Math.min(p11, p21);").withCapacityLimit("base", 1, "* * * * * ? *", 1, 2)
                .withCapacityLimit("saturdays", 2, "* * 10-21 ? * SAT *", 5, 10).build();
    }

    private StandardPredictionSubsystemConfig configMissingPredictors() {
        return StandardPredictionSubsystemConfig.Builder.create().withPredictors(null)
                .withAggregator("Math.max(p1, p2);").build();
    }

    private StandardPredictionSubsystemConfig configMissingCapacityMappings() {
        return StandardPredictionSubsystemConfig.Builder.create().withAggregator("Math.max(p1, p2);")
                .withCapacityMappings(null).build();
    }

    private StandardPredictionSubsystemConfig configMissingAggregator() {
        return StandardPredictionSubsystemConfig.Builder.create().withAggregator(null).build();
    }

    private StandardPredictionSubsystemConfig configMissingCapacityLimits() {
        return StandardPredictionSubsystemConfig.Builder.create().withAggregator("Math.max(p1, p2);")
                .withCapacityLimits(null).build();
    }

    private StandardPredictionSubsystemConfig configMissingScalingPolicies() {
        return StandardPredictionSubsystemConfig.Builder.create().withAggregator("Math.max(p1, p2);")
                .withScalingPolicies(null).build();
    }

}
