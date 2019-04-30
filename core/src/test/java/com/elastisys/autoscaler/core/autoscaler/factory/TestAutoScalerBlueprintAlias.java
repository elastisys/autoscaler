package com.elastisys.autoscaler.core.autoscaler.factory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.core.alerter.impl.standard.StandardAlerter;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerBlueprintAlias;
import com.elastisys.autoscaler.core.cloudpool.impl.StandardCloudPoolProxy;
import com.elastisys.autoscaler.core.metronome.impl.standard.StandardMetronome;
import com.elastisys.autoscaler.core.monitoring.impl.standard.StandardMonitoringSubsystem;
import com.elastisys.autoscaler.core.prediction.impl.standard.StandardPredictionSubsystem;

/**
 * Verifies that the {@link AutoScalerBlueprintAlias}es are mapped to the
 * correct implementation classes.
 */
public class TestAutoScalerBlueprintAlias {

    @Test
    public void verifyAliasToClassMappings() {
        assertThat(AutoScalerBlueprintAlias.StandardAlerter.getQualifiedClassName(),
                is(StandardAlerter.class.getName()));
        assertThat(AutoScalerBlueprintAlias.StandardCloudPoolProxy.getQualifiedClassName(),
                is(StandardCloudPoolProxy.class.getName()));
        assertThat(AutoScalerBlueprintAlias.StandardMetronome.getQualifiedClassName(),
                is(StandardMetronome.class.getName()));
        assertThat(AutoScalerBlueprintAlias.StandardMonitoringSubsystem.getQualifiedClassName(),
                is(StandardMonitoringSubsystem.class.getName()));
        assertThat(AutoScalerBlueprintAlias.StandardPredictionSubsystem.getQualifiedClassName(),
                is(StandardPredictionSubsystem.class.getName()));

    }
}
