package com.elastisys.autoscaler.core.autoscaler.builder;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.core.alerter.impl.standard.StandardAlerter;
import com.elastisys.autoscaler.core.autoscaler.builder.AutoScalerBuilder.Defaults;
import com.elastisys.autoscaler.core.cloudpool.impl.StandardCloudPoolProxy;
import com.elastisys.autoscaler.core.metronome.impl.standard.StandardMetronome;
import com.elastisys.autoscaler.core.monitoring.impl.standard.StandardMonitoringSubsystem;
import com.elastisys.autoscaler.core.prediction.impl.standard.StandardPredictionSubsystem;

/**
 * Checks the default subsystem implementation classes.
 */
public class TestDefaults {

    @Test
    public void testDefaultsSubsystemClasses() {
        assertThat(Defaults.MONITORING_SUBSYSTEM.getName(), is(StandardMonitoringSubsystem.class.getName()));
        assertThat(Defaults.ALERTER.getName(), is(StandardAlerter.class.getName()));
        assertThat(Defaults.METRONOME.getName(), is(StandardMetronome.class.getName()));
        assertThat(Defaults.PREDICTION_SUBSYSTEM.getName(), is(StandardPredictionSubsystem.class.getName()));
        assertThat(Defaults.CLOUD_POOL_PROXY.getName(), is(StandardCloudPoolProxy.class.getName()));
    }
}
