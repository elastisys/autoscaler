package com.elastisys.autoscaler.core.metronome.impl.standard;

import static com.elastisys.scale.cloudpool.api.types.MachineState.PENDING;
import static com.elastisys.scale.cloudpool.api.types.MachineState.REJECTED;
import static com.elastisys.scale.cloudpool.api.types.MachineState.REQUESTED;
import static com.elastisys.scale.cloudpool.api.types.MachineState.RUNNING;
import static com.elastisys.scale.cloudpool.api.types.MachineState.TERMINATED;
import static com.elastisys.scale.cloudpool.api.types.MachineState.TERMINATING;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.alerter.api.types.AlertTopics;
import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxyException;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetric;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Verifies the behavior of the {@link MachinePoolReporter} class.
 */
@SuppressWarnings("rawtypes")
public class TestMachinePoolReporter {

    static Logger logger = LoggerFactory.getLogger(TestMachinePoolReporter.class);

    private static DateTime NOW = UtcTime.parse("2015-10-13T12:00:00.000Z");
    /** cloudProvider tag used when no machines at all exist in the pool. */
    private static final String NO_CLOUD = "N/A";
    /** region tag used when no machines at all exist in the pool. */
    private static final String NO_REGION = "N/A";
    /** machineSize tag used when no machines at all exist in the pool. */
    private static final String NO_SIZE = "N/A";

    /** Object under test. */
    private MachinePoolReporter reporter;

    // Mocks
    private EventBus eventBusMock = mock(EventBus.class);
    private CloudPoolProxy cloudPoolMock = mock(CloudPoolProxy.class);

    @Before
    public void onSetup() {
        this.reporter = new MachinePoolReporter(logger, this.eventBusMock, this.cloudPoolMock);
    }

    /**
     * On an empty machine pool, a zero pool size count should be reported for
     * each {@link MachineState} with tags.
     */
    @Test
    public void onEmptyMachinePool() throws CloudPoolProxyException {
        FrozenTime.setFixed(NOW);
        MachinePool pool = new PoolBuilder(NOW).build();
        // set up mocked responses
        when(this.cloudPoolMock.getMachinePool()).thenReturn(pool);

        // execute test
        verifyNoMoreInteractions(this.eventBusMock);
        verifyNoMoreInteractions(this.cloudPoolMock);
        this.reporter.run();

        // verify calls to mock objects
        verify(this.cloudPoolMock).getMachinePool();
        verifyNoMoreInteractions(this.cloudPoolMock);

        verify(this.eventBusMock).post(poolSizeEvent(0, REQUESTED, NO_CLOUD, NO_REGION, NO_SIZE));
        verify(this.eventBusMock).post(poolSizeEvent(0, PENDING, NO_CLOUD, NO_REGION, NO_SIZE));
        verify(this.eventBusMock).post(poolSizeEvent(0, RUNNING, NO_CLOUD, NO_REGION, NO_SIZE));
        verify(this.eventBusMock).post(poolSizeEvent(0, REJECTED, NO_CLOUD, NO_REGION, NO_SIZE));
        verify(this.eventBusMock).post(poolSizeEvent(0, TERMINATING, NO_CLOUD, NO_REGION, NO_SIZE));
        verify(this.eventBusMock).post(poolSizeEvent(0, TERMINATED, NO_CLOUD, NO_REGION, NO_SIZE));
        verifyNoMoreInteractions(this.eventBusMock);
    }

    /**
     * Verify that correct machine counts are reported for each
     * {@link MachineState} when downstream machines are running in a single
     * cloud.
     */
    @Test
    public void onSingleCloudPool() throws Exception {
        FrozenTime.setFixed(NOW);
        // set up mocked responses
        List<Machine> ec2Pool = new CloudPoolBuilder("AWS_EC2", "us-east-1", "m1.medium")
                .machine("i-1", MachineState.REQUESTED).machine("i-2", MachineState.PENDING)
                .machine("i-3", MachineState.PENDING).machine("i-4", MachineState.RUNNING)
                .machine("i-5", MachineState.RUNNING).machine("i-6", MachineState.RUNNING)
                .machine("i-7", MachineState.TERMINATING).machine("i-8", MachineState.TERMINATED)
                .machine("i-9", MachineState.TERMINATED).build();
        MachinePool pool = new PoolBuilder(NOW).machines(ec2Pool).build();
        when(this.cloudPoolMock.getMachinePool()).thenReturn(pool);

        // execute test
        verifyNoMoreInteractions(this.eventBusMock);
        verifyNoMoreInteractions(this.cloudPoolMock);
        this.reporter.run();

        // verify calls to mock objects
        verify(this.cloudPoolMock).getMachinePool();
        verifyNoMoreInteractions(this.cloudPoolMock);
        // verify that autoscaler.cloudpool.size events were posted
        verify(this.eventBusMock).post(poolSizeEvent(1, REQUESTED, "AWS_EC2", "us-east-1", "m1.medium"));
        verify(this.eventBusMock).post(poolSizeEvent(2, PENDING, "AWS_EC2", "us-east-1", "m1.medium"));
        verify(this.eventBusMock).post(poolSizeEvent(3, RUNNING, "AWS_EC2", "us-east-1", "m1.medium"));
        verify(this.eventBusMock).post(poolSizeEvent(0, REJECTED, "AWS_EC2", "us-east-1", "m1.medium"));
        verify(this.eventBusMock).post(poolSizeEvent(1, TERMINATING, "AWS_EC2", "us-east-1", "m1.medium"));
        verify(this.eventBusMock).post(poolSizeEvent(2, TERMINATED, "AWS_EC2", "us-east-1", "m1.medium"));
        verifyNoMoreInteractions(this.eventBusMock);
    }

    /**
     * Verify that correct machine counts are reported for each
     * {@link MachineState} when machines are running on multiple clouds (that
     * is, with several downstream cloud pools aggregated by a splitter).
     */
    @Test
    public void onMultipleCloudPools() throws Exception {
        FrozenTime.setFixed(NOW);
        // set up mocked responses
        List<Machine> ec2Pool = new CloudPoolBuilder("AWS_EC2", "us-east-1", "m1.medium")
                .machine("i-1", MachineState.PENDING).machine("i-2", MachineState.PENDING)
                .machine("i-3", MachineState.RUNNING).machine("i-4", MachineState.RUNNING)
                .machine("i-5", MachineState.RUNNING).machine("i-6", MachineState.TERMINATING)
                .machine("i-7", MachineState.TERMINATED).machine("i-8", MachineState.TERMINATED).build();
        List<Machine> cityCloudPool = new CloudPoolBuilder("CityCloud", "Kna1", "1C-1G")
                .machine("c-1", MachineState.PENDING).machine("c-2", MachineState.RUNNING)
                .machine("c-3", MachineState.RUNNING).machine("c-4", MachineState.TERMINATED).build();
        MachinePool pool = new PoolBuilder(NOW).machines(ec2Pool).machines(cityCloudPool).build();
        when(this.cloudPoolMock.getMachinePool()).thenReturn(pool);

        // execute test
        verifyNoMoreInteractions(this.eventBusMock);
        verifyNoMoreInteractions(this.cloudPoolMock);
        this.reporter.run();

        // verify calls to mock objects
        verify(this.cloudPoolMock).getMachinePool();
        verifyNoMoreInteractions(this.cloudPoolMock);
        // verify that autoscaler.cloudpool.size events were posted
        verify(this.eventBusMock).post(poolSizeEvent(0, REQUESTED, "AWS_EC2", "us-east-1", "m1.medium"));
        verify(this.eventBusMock).post(poolSizeEvent(2, PENDING, "AWS_EC2", "us-east-1", "m1.medium"));
        verify(this.eventBusMock).post(poolSizeEvent(3, RUNNING, "AWS_EC2", "us-east-1", "m1.medium"));
        verify(this.eventBusMock).post(poolSizeEvent(1, TERMINATING, "AWS_EC2", "us-east-1", "m1.medium"));
        verify(this.eventBusMock).post(poolSizeEvent(2, MachineState.TERMINATED, "AWS_EC2", "us-east-1", "m1.medium"));
        verify(this.eventBusMock).post(poolSizeEvent(0, REJECTED, "AWS_EC2", "us-east-1", "m1.medium"));

        verify(this.eventBusMock).post(poolSizeEvent(0, REQUESTED, "CityCloud", "Kna1", "1C-1G"));
        verify(this.eventBusMock).post(poolSizeEvent(1, PENDING, "CityCloud", "Kna1", "1C-1G"));
        verify(this.eventBusMock).post(poolSizeEvent(2, RUNNING, "CityCloud", "Kna1", "1C-1G"));
        verify(this.eventBusMock).post(poolSizeEvent(0, TERMINATING, "CityCloud", "Kna1", "1C-1G"));
        verify(this.eventBusMock).post(poolSizeEvent(1, TERMINATED, "CityCloud", "Kna1", "1C-1G"));
        verify(this.eventBusMock).post(poolSizeEvent(0, REJECTED, "CityCloud", "Kna1", "1C-1G"));

        verifyNoMoreInteractions(this.eventBusMock);
    }

    /**
     * Verify behavior when the {@link CloudPoolProxy} fails to retrieve machine
     * pool.
     */
    @Test
    public void testRunWhenCloudPoolFails() throws Exception {
        FrozenTime.setFixed(NOW);
        // set up mocked responses
        when(this.cloudPoolMock.getMachinePool()).thenThrow(new CloudPoolProxyException("couldn't connect"));

        // execute test
        verifyNoMoreInteractions(this.eventBusMock);
        verifyNoMoreInteractions(this.cloudPoolMock);
        this.reporter.run();

        // verify calls to mock objects
        verify(this.cloudPoolMock).getMachinePool();
        verifyNoMoreInteractions(this.cloudPoolMock);
        // an error event should be posted
        verify(this.eventBusMock).post(
                argThat(is(AlertMatcher.alert(AlertTopics.METRONOME_FAILURE.getTopicPath(), AlertSeverity.WARN))));
        verifyNoMoreInteractions(this.eventBusMock);
    }

    /**
     * The cloudpool size event report should carry the timestamp of wallclock
     * time that the report was made, not the timestamp of the
     * {@link MachinePool} observation, which may be cached.
     */
    @Test
    public void verifyReportedTimestamp() throws CloudPoolProxyException {
        // pool time differs from current time
        DateTime poolObservationTime = NOW.minusMinutes(2);
        DateTime clockTime = NOW;
        FrozenTime.setFixed(clockTime);

        // set up mocked responses
        List<Machine> ec2Pool = new CloudPoolBuilder("AWS_EC2", "us-east-1", "m1.medium")
                .machine("i-1", MachineState.RUNNING).build();
        MachinePool pool = new PoolBuilder(poolObservationTime).machines(ec2Pool).build();
        when(this.cloudPoolMock.getMachinePool()).thenReturn(pool);

        // execute test
        verifyNoMoreInteractions(this.eventBusMock);
        verifyNoMoreInteractions(this.cloudPoolMock);
        this.reporter.run();

        // verify calls to mock objects
        verify(this.cloudPoolMock).getMachinePool();
        verifyNoMoreInteractions(this.cloudPoolMock);
        // verify that autoscaler.cloudpool.size events were posted with the
        // current wall clock time as timestamp, and not the pool observation
        // time
        verify(this.eventBusMock).post(poolSizeEvent(0, REQUESTED, clockTime, "AWS_EC2", "us-east-1", "m1.medium"));
        verify(this.eventBusMock).post(poolSizeEvent(0, PENDING, clockTime, "AWS_EC2", "us-east-1", "m1.medium"));
        verify(this.eventBusMock).post(poolSizeEvent(1, RUNNING, clockTime, "AWS_EC2", "us-east-1", "m1.medium"));
        verify(this.eventBusMock).post(poolSizeEvent(0, TERMINATING, clockTime, "AWS_EC2", "us-east-1", "m1.medium"));
        verify(this.eventBusMock).post(poolSizeEvent(0, TERMINATED, clockTime, "AWS_EC2", "us-east-1", "m1.medium"));
        verify(this.eventBusMock).post(poolSizeEvent(0, REJECTED, clockTime, "AWS_EC2", "us-east-1", "m1.medium"));
        verifyNoMoreInteractions(this.eventBusMock);
    }

    /**
     * All reported datapoints for a given machine pool observation should carry
     * the same timestamp.
     */
    @Test
    public void verifySameTimestampForAllDatapoints() throws CloudPoolProxyException {
        MachinePoolReporter poolReporter = new MachinePoolReporter(logger, this.eventBusMock, this.cloudPoolMock);

        // create a sizeable pool to make sure processing takes a
        // non-negligeble time to process
        CloudPoolBuilder poolBuilder = new CloudPoolBuilder("AWS_EC2", "us-east-1", "m1.medium");
        for (int i = 0; i < 100; i++) {
            poolBuilder.machine("i-" + (i + 1), MachineState.RUNNING);
        }
        MachinePool ec2Pool = new MachinePool(poolBuilder.build(), NOW);
        List<MetricValue> metrics = poolReporter.poolMembershipMetrics(ec2Pool);

        Set<DateTime> timestamps = metrics.stream().map(MetricValue::getTime).collect(Collectors.toSet());
        assertThat(timestamps.size(), is(1));
    }

    /**
     * Creates a {@link SystemMetric#CLOUDPOOL_SIZE} event for a given number of
     * machines in a given {@link MachineState} from a given cloud provider and
     * with a given machine size (instance type). The current test time is used
     * as event timestamp.
     *
     * @param numMachines
     *            The number of cloud pool machines in the given state.
     * @param inState
     *            The {@link MachineState} of the cloud pool members.
     * @param cloudProvider
     *            The origin cloud provider of the reported machines. May be
     *            <code>null</code> in which case no {@code cloudProvider} tag
     *            is created.
     * @param region
     *            The cloud region/zone that the reported machines originate
     *            from. May be <code>null</code> in which case no {@code region}
     *            tag is created.
     * @param machineSize
     *            The size (instance type) of the reported machines. May be
     *            <code>null</code> in which case no {@code machineSize} tag is
     *            created.
     * @return
     */
    private SystemMetricEvent poolSizeEvent(int numMachines, MachineState inState, String cloudProvider, String region,
            String machineSize) {
        return poolSizeEvent(numMachines, inState, UtcTime.now(), cloudProvider, region, machineSize);
    }

    /**
     * Creates a {@link SystemMetric#CLOUDPOOL_SIZE} event for a given number of
     * machines in a given {@link MachineState} from a given cloud provider and
     * with a given machine size (instance type) that was observed at a
     * particular time.
     *
     * @param numMachines
     *            The number of cloud pool machines in the given state.
     * @param inState
     *            The {@link MachineState} of the cloud pool members.
     * @param poolObservationTime
     *            The time at which the pool was observed.
     * @param cloudProvider
     *            The origin cloud provider of the reported machines. May be
     *            <code>null</code> in which case no {@code cloudProvider} tag
     *            is created.
     * @param region
     *            The cloud region/zone that the reported machines originate
     *            from. May be <code>null</code> in which case no {@code region}
     *            tag is created.
     * @param machineSize
     *            The size (instance type) of the reported machines. May be
     *            <code>null</code> in which case no {@code machineSize} tag is
     *            created.
     * @return
     */
    private SystemMetricEvent poolSizeEvent(int numMachines, MachineState inState, DateTime poolObservationTime,
            String cloudProvider, String region, String machineSize) {
        Map<String, String> expectedTags = new HashMap<>();
        expectedTags.put("machineState", inState.name());
        if (cloudProvider != null) {
            expectedTags.put("cloudProvider", cloudProvider);
        }
        if (region != null) {
            expectedTags.put("region", region);
        }
        if (machineSize != null) {
            expectedTags.put("machineSize", machineSize);
        }
        return new SystemMetricEvent(new MetricValue(SystemMetric.CLOUDPOOL_SIZE.getMetricName(), numMachines,
                poolObservationTime, expectedTags));
    }

    /**
     * A builder for a list of {@link Machine}s, all originating from the same
     * cloud provider and of the same size (instance type).
     *
     */
    private static class CloudPoolBuilder {
        private final List<Machine> machines = new ArrayList<>();
        private final String cloudProvider;
        private final String region;
        private final String machineSize;

        public CloudPoolBuilder(String cloudProvider, String region, String machineSize) {
            this.cloudProvider = cloudProvider;
            this.region = region;
            this.machineSize = machineSize;
        }

        public List<Machine> build() {
            return Collections.unmodifiableList(this.machines);
        }

        public CloudPoolBuilder machine(String id, MachineState state) {
            this.machines.add(Machine.builder().id(id).machineState(state).cloudProvider(this.cloudProvider)
                    .region(this.region).machineSize(this.machineSize).build());
            return this;
        }
    }

    private static class PoolBuilder {
        private final List<Machine> machines = new ArrayList<>();
        private final DateTime timestamp;

        public PoolBuilder(DateTime timestamp) {
            this.timestamp = timestamp;
        }

        public MachinePool build() {
            return new MachinePool(this.machines, this.timestamp);
        }

        public PoolBuilder machines(Machine... machines) {
            this.machines.addAll(Arrays.asList(machines));
            return this;
        }

        public PoolBuilder machines(List<Machine>... machineLists) {
            for (List<Machine> machineList : machineLists) {
                this.machines.addAll(machineList);
            }
            return this;
        }
    }

    private static class MachinePoolBuilder {
        private int numRequested = 0;
        private int numPending = 0;
        private int numRunning = 0;
        private int numTerminating = 0;
        private int numTerminated = 0;
        private int numRejected = 0;
        private DateTime timestamp;

        public MachinePoolBuilder(DateTime timestamp) {
            this.timestamp = timestamp;
        }

        public MachinePool build() {
            int id = 0;
            List<Machine> machines = new ArrayList<>();
            for (int i = 0; i < this.numRequested; i++) {
                machines.add(Machine.builder().id("i-" + (++id)).machineState(MachineState.REQUESTED).build());
            }
            for (int i = 0; i < this.numPending; i++) {
                machines.add(Machine.builder().id("i-" + (++id)).machineState(MachineState.PENDING)
                        .launchTime(UtcTime.now().minusHours(i)).build());
            }
            for (int i = 0; i < this.numRunning; i++) {
                machines.add(Machine.builder().id("i-" + (++id)).machineState(MachineState.RUNNING)
                        .launchTime(UtcTime.now().minusHours(i)).publicIps(asList("1.2.3.4")).build());
            }
            for (int i = 0; i < this.numTerminating; i++) {
                machines.add(Machine.builder().id("i-" + (++id)).machineState(MachineState.TERMINATING)
                        .launchTime(UtcTime.now().minusHours(i)).build());
            }
            for (int i = 0; i < this.numTerminated; i++) {
                machines.add(Machine.builder().id("i-" + (++id)).machineState(MachineState.TERMINATED)
                        .launchTime(UtcTime.now().minusHours(i)).build());
            }
            for (int i = 0; i < this.numRejected; i++) {
                machines.add(Machine.builder().id("i-" + (++id)).machineState(MachineState.REJECTED).build());
            }
            return new MachinePool(machines, this.timestamp);
        }

        public MachinePoolBuilder requested(int machines) {
            this.numRequested = machines;
            return this;
        }

        public MachinePoolBuilder pending(int machines) {
            this.numPending = machines;
            return this;
        }

        public MachinePoolBuilder running(int machines) {
            this.numRunning = machines;
            return this;
        }

        public MachinePoolBuilder terminating(int machines) {
            this.numTerminating = machines;
            return this;
        }

        public MachinePoolBuilder terminated(int machines) {
            this.numTerminated = machines;
            return this;
        }

        public MachinePoolBuilder rejected(int machines) {
            this.numRejected = machines;
            return this;
        }

    }
}
