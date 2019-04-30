package com.elastisys.autoscaler.core.metronome.impl.standard;

import static com.elastisys.scale.cloudpool.api.types.Machine.inState;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.alerter.api.types.AlertTopics;
import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetric;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.exception.Stacktrace;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * A {@link Runnable} task run periodically by the {@link StandardMetronome}
 * that, for every invocation of its {@link #run()} method, reports the current
 * layout of the {@link MachinePool}.
 * <p/>
 * The current pool layout is reported to the {@link SystemHistorian} and
 * {@link AccountingSubsystem} by posting a collection of
 * {@link SystemMetricEvent}s on the {@link AutoScaler}'s {@link EventBus} that,
 * for each cloudProvider-region-machineSize combination, indicates the number
 * of pool members in each valid {@link MachineState}.
 *
 * @see StandardMetronome
 */
@SuppressWarnings("rawtypes")
public class MachinePoolReporter implements Runnable {

    private final Logger logger;
    private final EventBus eventBus;

    /** Queried for the current machine pool members. */
    private final CloudPoolProxy cloudPool;

    public MachinePoolReporter(Logger logger, EventBus eventBus, CloudPoolProxy cloudPool) {
        this.logger = logger;
        this.eventBus = eventBus;
        this.cloudPool = cloudPool;
    }

    @Override
    public void run() {
        try {
            this.logger.debug("running cloud pool membership reporter");
            MachinePool machinePool = this.cloudPool.getMachinePool();
            reportPoolMembership(machinePool);
        } catch (Throwable e) {
            String errorMsg = String.format("failed to report cloud pool members: %s\n%s", e.getMessage(),
                    Stacktrace.toString(e));
            this.eventBus.post(new Alert(AlertTopics.METRONOME_FAILURE.getTopicPath(), AlertSeverity.WARN,
                    UtcTime.now(), errorMsg, null));
            this.logger.error(errorMsg);
        }
    }

    /**
     * Posts a collection of {@link SystemMetric} values to the
     * {@link SystemHistorian}, indicating the current number of machine pool
     * members grouped by cloudProvider, region, machineSize and machineState.
     *
     * @param machinePool
     */
    private void reportPoolMembership(MachinePool machinePool) {
        poolMembershipMetrics(machinePool).forEach(metric -> postSystemMetric(metric));
    }

    /**
     * Returns the pool membership metrics to be reported for a given
     * {@link MachinePool} snapshot. Each datapoint indicates the number of
     * machine pool members observed in a certain
     * cloudProvider-region-machineSize-machineState combination.
     *
     * @param machinePool
     * @return
     */
    List<MetricValue> poolMembershipMetrics(MachinePool machinePool) {
        List<MetricValue> datapoints = new ArrayList<>();
        // timestamp to set on all reported data points
        DateTime timestamp = UtcTime.now();

        // group machines by cloudpool origin (cloudProvider/region/machineSize)
        Map<CloudPoolOrigin, Set<Machine>> machinesByOrigin = new HashMap<>();
        machinePool.getMachines().forEach(machine -> {
            CloudPoolOrigin originPool = new CloudPoolOrigin(machine);
            Set<Machine> poolMachines = machinesByOrigin.getOrDefault(originPool, new HashSet<>());
            poolMachines.add(machine);
            machinesByOrigin.put(originPool, poolMachines);
        });

        // if no downstream machines exist, report zero values for a dummy
        // origin
        if (machinesByOrigin.isEmpty()) {
            machinesByOrigin.put(new CloudPoolOrigin("N/A", "N/A", "N/A"), Collections.emptySet());
        }

        log(machinesByOrigin);

        // for each group of machines: report the number of machines observed in
        // each valid runtime state
        MachineState[] validStates = MachineState.values();
        for (CloudPoolOrigin origin : machinesByOrigin.keySet()) {
            for (MachineState state : validStates) {
                List<Machine> poolMembersInState = machinesByOrigin.get(origin).stream().filter(inState(state))
                        .collect(Collectors.toList());
                Map<String, String> tags = Maps.of(//
                        "cloudProvider", origin.getCloudProvider(), //
                        "region", origin.getRegion(), //
                        "machineSize", origin.getMachineSize(), //
                        "machineState", state.name());
                datapoints.add(new MetricValue(SystemMetric.CLOUDPOOL_SIZE.getMetricName(), poolMembersInState.size(),
                        timestamp, tags));
            }
        }

        return datapoints;
    }

    /**
     * Debug output of all machines grouped by their cloud pool origin.
     *
     * @param machinesByOrigin
     */
    private void log(Map<CloudPoolOrigin, Set<Machine>> machinesByOrigin) {
        StringWriter writer = new StringWriter();
        writer.append("machines by origin:\n");
        for (CloudPoolOrigin origin : machinesByOrigin.keySet()) {
            Set<Machine> machines = machinesByOrigin.get(origin);
            writer.append(String.format("  Origin pool: %s (%d machines):\n    ", origin, machines.size()));
            List<String> machinesShort = machines.stream().map(Machine.toShortString()).collect(Collectors.toList());
            String machinesStr = String.join("\n    ", machinesShort);
            writer.append(machinesStr + "\n");
        }
        this.logger.debug(writer.toString());
    }

    /**
     * Posts a {@link MetricValue} on the {@link AutoScaler} {@link EventBus}
     * for later handling by the {@link SystemHistorian}.
     *
     * @param value
     */
    private void postSystemMetric(MetricValue value) {
        this.eventBus.post(new SystemMetricEvent(value));
    }

    /**
     * Represents a {@link Machine}'s cloud pool origin, as a combination of a
     * {@code cloudProvider} (e.g. {@code AWS-EC2}), a region (e.g.
     * {@code us-east-1}), and a {@code machineSize} (for example,
     * {@code m1.small}).
     *
     */
    static class CloudPoolOrigin {
        private final String cloudProvider;
        private final String region;
        private final String machineSize;

        public CloudPoolOrigin(Machine machine) {
            this(machine.getCloudProvider(), machine.getRegion(), machine.getMachineSize());
        }

        public CloudPoolOrigin(String cloudProvider, String region, String machineSize) {
            this.cloudProvider = cloudProvider;
            this.region = region;
            this.machineSize = machineSize;
        }

        /**
         * The cloud provider of this origin. For example {@code AWS-EC2}.
         *
         * @return
         */
        public String getCloudProvider() {
            return this.cloudProvider;
        }

        /**
         * The region of this origin. For example {@code us-east-1}.
         *
         * @return
         */
        public String getRegion() {
            return this.region;
        }

        /**
         * Machine size of this origin. For example, {@code m1.small}.
         *
         * @return
         */
        public String getMachineSize() {
            return this.machineSize;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.cloudProvider, this.region, this.machineSize);
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof CloudPoolOrigin) {
                CloudPoolOrigin that = (CloudPoolOrigin) object;
                return Objects.equals(this.cloudProvider, that.cloudProvider)
                        && Objects.equals(this.region, that.region)
                        && Objects.equals(this.machineSize, that.machineSize);
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format("%s:%s:%s", this.cloudProvider, this.region, this.machineSize);
        }
    }
}