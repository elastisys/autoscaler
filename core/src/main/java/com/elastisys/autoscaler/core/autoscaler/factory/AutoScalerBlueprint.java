package com.elastisys.autoscaler.core.autoscaler.factory;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.autoscaler.core.alerter.api.Alerter;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.metronome.api.Metronome;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * A descriptor that specifies the specific subsystem implementations to use for
 * an {@link AutoScaler} instance. The descriptor is passed to an
 * {@link AutoScalerFactory} to produce an {@link AutoScaler} instance.
 * <p/>
 * The subsystem implementation classes can either be specified as fully
 * (package-)qualified class names, or using the short-hand aliases in the
 * {@link AutoScalerBlueprintAlias} enumeration.
 *
 * @see AutoScaler
 * @see AutoScalerFactory
 * @see AutoScalerBlueprintAlias
 */
public class AutoScalerBlueprint {

    /** The identifier of the {@link AutoScaler} to build. */
    private final String id;
    /** The {@link MonitoringSubsystem} implementation class. */
    private final String monitoringSubsystem;
    /** The {@link Alerter} subsystem implementation class. */
    private final String alerter;
    /** The {@link Metronome} subsystem implementation class. */
    private final String metronome;
    /** The {@link PredictionSubsystem} subsystem implementation class. */
    private final String predictionSubsystem;
    /** The {@link CloudPoolProxy} subsystem implementation class. */
    private final String cloudPool;

    /**
     * Creates an {@link AutoScalerBlueprint}.
     *
     * @param id
     *            The identifier of the {@link AutoScaler} to build.
     * @param monitoringSubsystem
     *            The {@link MonitoringSubsystem} implementation class.
     * @param alerter
     *            The {@link Alerter} subsystem implementation class.
     * @param metronome
     *            The {@link Metronome} subsystem implementation class.
     * @param predictionSubsystem
     *            The {@link PredictionSubsystem} subsystem implementation
     *            class.
     * @param cloudPoolProxy
     *            The {@link CloudPoolProxy} subsystem implementation class.
     */
    public AutoScalerBlueprint(String id, String monitoringSubsystem, String alerter, String metronome,
            String predictionSubsystem, String cloudPoolProxy) {
        this.id = id;
        this.monitoringSubsystem = monitoringSubsystem;
        this.alerter = alerter;
        this.metronome = metronome;
        this.predictionSubsystem = predictionSubsystem;
        this.cloudPool = cloudPoolProxy;
    }

    /**
     * @return
     */
    public Optional<String> id() {
        return Optional.ofNullable(this.id);
    }

    /**
     * Class name of the {@link MonitoringSubsystem} implementation to use.
     *
     * @return
     */
    public Optional<String> monitoringSubsystem() {
        return getQualifiedClass(this.monitoringSubsystem);
    }

    /**
     * Class name of the {@link Alerter} implementation to use.
     *
     * @return
     */
    public Optional<String> alerter() {
        return getQualifiedClass(this.alerter);
    }

    /**
     * Class name of the {@link Metronome} implementation to use.
     *
     * @return
     */
    public Optional<String> metronome() {
        return getQualifiedClass(this.metronome);
    }

    /**
     * Class name of the {@link PredictionSubsystem} implementation to use.
     *
     * @return
     */
    public Optional<String> predictionSubsystem() {
        return getQualifiedClass(this.predictionSubsystem);
    }

    /**
     * Class name of the {@link CloudPoolProxy} implementation to use.
     *
     * @return
     */
    public Optional<String> cloudPool() {
        return getQualifiedClass(this.cloudPool);
    }

    /**
     * Returns the fully qualified class name for a blueprint subsystem entry.
     *
     * @param subsystemEntry
     *            A blueprint subsystem entry. Can either be one of the
     *            recognized {@link AutoScalerBlueprintAlias}es or a fully
     *            qualified class name.
     * @return
     */
    private Optional<String> getQualifiedClass(String subsystemEntry) {
        if (isAlias(subsystemEntry)) {
            return Optional.of(aliasToQualifiedClass(subsystemEntry));
        }
        // not an alias: should be a fully qualified class name
        return Optional.ofNullable(subsystemEntry);
    }

    /**
     * See if a given blueprint subsystem entry is given as a fully qualified
     * class or as an alias.
     *
     * @param blueprintSubsystemEntry
     * @return
     */
    private boolean isAlias(String blueprintSubsystemEntry) {
        if (blueprintSubsystemEntry == null) {
            return false;
        }
        try {
            // try to parse subsystem specifier
            AutoScalerBlueprintAlias.valueOf(blueprintSubsystemEntry);
            // parsing succeeded: a recognized alias
            return true;
        } catch (IllegalArgumentException e) {
            // parsing failed: not a recognized alias
            return false;
        }
    }

    /**
     * Converts a blueprint alias to its fully qualified class name.
     *
     * @param alias
     *            The {@link AutoScalerBlueprintAlias}.
     * @return
     */
    private String aliasToQualifiedClass(String alias) {
        return AutoScalerBlueprintAlias.valueOf(alias).getQualifiedClassName();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.alerter, this.cloudPool, this.monitoringSubsystem, this.metronome,
                this.predictionSubsystem);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AutoScalerBlueprint) {
            AutoScalerBlueprint that = (AutoScalerBlueprint) obj;
            return Objects.equals(this.id, that.id) && Objects.equals(this.alerter, that.alerter)
                    && Objects.equals(this.cloudPool, that.cloudPool)
                    && Objects.equals(this.monitoringSubsystem, that.monitoringSubsystem)
                    && Objects.equals(this.metronome, that.metronome)
                    && Objects.equals(this.predictionSubsystem, that.predictionSubsystem);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
