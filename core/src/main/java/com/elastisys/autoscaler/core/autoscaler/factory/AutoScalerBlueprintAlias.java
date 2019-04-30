package com.elastisys.autoscaler.core.autoscaler.factory;

import com.elastisys.autoscaler.core.alerter.impl.standard.StandardAlerter;
import com.elastisys.autoscaler.core.cloudpool.impl.StandardCloudPoolProxy;
import com.elastisys.autoscaler.core.metronome.impl.standard.StandardMetronome;
import com.elastisys.autoscaler.core.monitoring.impl.standard.StandardMonitoringSubsystem;
import com.elastisys.autoscaler.core.prediction.impl.standard.StandardPredictionSubsystem;

/**
 * Supported short-hand aliases that can be used for subsystem implementation
 * classes in {@link AutoScalerBlueprint}s.
 *
 * @see AutoScalerBlueprint
 */
public enum AutoScalerBlueprintAlias {
    /** Blueprint alias for the {@link StandardAlerter} class. */
    StandardAlerter(StandardAlerter.class.getName()),
    /** Blueprint alias for the {@link StandardMetronome} class. */
    StandardMetronome(StandardMetronome.class.getName()),
    /** Blueprint alias for the StandardMonitoringS class. */
    StandardMonitoringSubsystem(StandardMonitoringSubsystem.class.getName()),
    /** Blueprint alias for the {@link StandardPredictionSubsystem} class. */
    StandardPredictionSubsystem(StandardPredictionSubsystem.class.getName()),
    /** Blueprint alias for the {@link StandardCloudPoolProxy} class. */
    StandardCloudPoolProxy(StandardCloudPoolProxy.class.getName());

    /**
     * The fully (package-)qualified class name of the subsystem implementation
     * class that this alias refers to.
     */
    private final String qualifiedClassName;

    private AutoScalerBlueprintAlias(String implementationClass) {
        this.qualifiedClassName = implementationClass;
    }

    /**
     * Returns the fully (package-)qualified class name of the subsystem
     * implementation class that this alias refers to.
     *
     * @return
     */
    public String getQualifiedClassName() {
        return this.qualifiedClassName;
    }
}
