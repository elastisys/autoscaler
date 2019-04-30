package com.elastisys.autoscaler.metricstreamers.streamjoiner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.metricstreamers.streamjoiner.config.MetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.streamjoiner.config.MetricStreamJoinerConfig;
import com.elastisys.autoscaler.metricstreamers.streamjoiner.stream.JoiningMetricStream;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;

/**
 * Exercises the basic operations (configure/start/stop) of the
 * {@link InfluxdbMetricStreamer}.
 */
public class TestMetricStreamJoiner {
    private static final String STREAM_REFERENCE_CPU_SYSTEM = "cpu.system.sum.stream";
    private static final String STREAM_REFERENCE_CPU_USER = "cpu.user.sum.stream";
    private static final String STREAM_REFERENCE_MEM_REQUESTED = "mem.requested.sum.stream";
    private static final String STREAM_REFERENCE_MEM_ALLOCATABLE = "mem.allocatable.sum.stream";

    private static final Logger LOG = LoggerFactory.getLogger(TestMetricStreamJoiner.class);
    private static final EventBus eventBus = mock(EventBus.class);

    private static final String ID1 = "total.cpu.stream";
    private static final String METRIC1 = "cpu_busy";
    private static final Map<String, String> INPUT_STREAMS1 = Maps.of(//
            "cpu_system", STREAM_REFERENCE_CPU_SYSTEM, //
            "cpu_user", STREAM_REFERENCE_CPU_USER);
    private static final TimeInterval MAX_TIME_DIFF1 = TimeInterval.seconds(10);
    private static final List<String> JOIN_SCRIPT1 = Arrays.asList(//
            "cpu_system + cpu_user");

    private static final String ID2 = "mem.utilization.stream";
    private static final String METRIC2 = "mem.utilization";
    private static final Map<String, String> INPUT_STREAMS2 = Maps.of(//
            "memRequested", STREAM_REFERENCE_MEM_REQUESTED, //
            "memAllocatable", STREAM_REFERENCE_MEM_ALLOCATABLE);
    private static final TimeInterval MAX_TIME_DIFF2 = TimeInterval.seconds(0);
    private static final List<String> JOIN_SCRIPT2 = Arrays.asList(//
            "memRequested / memAllocatable");

    /**
     * A mocked {@link MetricStreamer} that is given as an input to the
     * {@link MetricStreamJoiner}, simulating that the mocked
     * {@link MetricStreamer} was declared prior to the
     * {@link MetricStreamJoiner}. This means that the
     * {@link MetricStreamJoiner} is allowed to reference {@link MetricStream}s
     * from this {@link MetricStreamer} in its input streams.
     */
    private MetricStreamer<?> mockMetricStreamer = mock(MetricStreamer.class);

    /** Object under test. */
    private MetricStreamJoiner metricStreamer;

    @Before
    public void beforeTestMethod() {
        this.metricStreamer = new MetricStreamJoiner(LOG, eventBus, Arrays.asList(this.mockMetricStreamer));
    }

    /**
     * It should be possible for an inputStream to reference a
     * {@link MetricStream} declared by a {@link MetricStreamer} defined before
     * the {@link MetricStreamJoiner}.
     */
    @Test
    public void configureWithReferenceToExternalMetricStream() throws Exception {
        setUpReferencableMetricStreams(STREAM_REFERENCE_CPU_SYSTEM, STREAM_REFERENCE_CPU_USER);

        assertThat(this.metricStreamer.getConfiguration(), is(nullValue()));
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));

        MetricStreamJoinerConfig config = config(streamDef(ID1, METRIC1, INPUT_STREAMS1, MAX_TIME_DIFF1, JOIN_SCRIPT1));
        this.metricStreamer.configure(config);
        assertThat(this.metricStreamer.getConfiguration(), is(config));
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));

        // check metric streams
        assertThat(this.metricStreamer.getMetricStreams().size(), is(1));
    }

    /**
     * It should not be possible to configure an inputStream that is not
     * published by another {@link MetricStreamer} in the
     * {@link MonitoringSubsystem}.
     */
    @Test
    public void configureWithReferenceToNonExistingMetricStream() {
        setUpReferencableMetricStreams(STREAM_REFERENCE_CPU_SYSTEM, STREAM_REFERENCE_CPU_USER);

        assertThat(this.metricStreamer.getConfiguration(), is(nullValue()));
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));

        // note: input streams refer to non-existing metric streams
        MetricStreamJoinerConfig config = config(streamDef(ID2, METRIC2, INPUT_STREAMS2, MAX_TIME_DIFF2, JOIN_SCRIPT2));
        try {
            this.metricStreamer.configure(config);
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage()
                    .contains("references stream mem.requested.sum.stream which is neither defined by another "
                            + "MetricStreamer nor a prior declared JoiningMetricStream"));
        }
    }

    /**
     * It is okay for a {@link JoiningMetricStream} to use a prior declared
     * {@link JoiningMetricStream} as an inputStream (it needs to be prior
     * declared to avoid cyclic dependencies).
     */
    @Test
    public void configureWithReferenceToPriorDeclaredJoiningMetricStream() {
        setUpReferencableMetricStreams(STREAM_REFERENCE_CPU_SYSTEM, STREAM_REFERENCE_CPU_USER);

        Map<String, String> inputStreams1 = Maps.of(//
                "cpu_system", STREAM_REFERENCE_CPU_SYSTEM, //
                "cpu_user", STREAM_REFERENCE_CPU_USER);
        List<String> joinScript1 = Arrays.asList("cpu_system + cpu_user");
        MetricStreamDefinition stream1 = new MetricStreamDefinition(ID1, METRIC1, inputStreams1, null, joinScript1);

        // note: references stream1
        Map<String, String> inputStreams2 = Maps.of("cpu_total", ID1);
        List<String> joinScript2 = Arrays.asList("100 * cpu_total");
        MetricStreamDefinition stream2 = new MetricStreamDefinition("cpu.total.percent.stream", "cpu.total.percent",
                inputStreams2, null, joinScript2);

        MetricStreamJoinerConfig config = config(stream1, stream2);
        this.metricStreamer.configure(config);
        assertThat(this.metricStreamer.getConfiguration(), is(config));
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));

        // check metric streams
        assertThat(this.metricStreamer.getMetricStreams().size(), is(2));
    }

    /**
     * A reconfiguration should replace the existing configuration.
     */
    @Test
    public void reconfigure() throws Exception {
        setUpReferencableMetricStreams(STREAM_REFERENCE_CPU_SYSTEM, STREAM_REFERENCE_CPU_USER,
                STREAM_REFERENCE_MEM_ALLOCATABLE, STREAM_REFERENCE_MEM_REQUESTED);

        MetricStreamJoinerConfig config = config(streamDef(ID1, METRIC1, INPUT_STREAMS1, MAX_TIME_DIFF1, JOIN_SCRIPT1));
        this.metricStreamer.configure(config);
        assertThat(this.metricStreamer.getConfiguration(), is(config));
        assertThat(this.metricStreamer.getMetricStreams().size(), is(1));

        MetricStreamJoinerConfig newConfig = config(
                streamDef(ID2, METRIC2, INPUT_STREAMS2, MAX_TIME_DIFF2, JOIN_SCRIPT2));
        this.metricStreamer.configure(newConfig);
        assertThat(this.metricStreamer.getConfiguration(), is(newConfig));
        assertThat(this.metricStreamer.getMetricStreams().size(), is(1));
    }

    /**
     * A reconfiguration should preserve the start-state.
     */
    @Test
    public void reconfigureWhenStarted() {
        setUpReferencableMetricStreams(STREAM_REFERENCE_CPU_SYSTEM, STREAM_REFERENCE_CPU_USER,
                STREAM_REFERENCE_MEM_ALLOCATABLE, STREAM_REFERENCE_MEM_REQUESTED);

        MetricStreamJoinerConfig config = config(streamDef(ID1, METRIC1, INPUT_STREAMS1, MAX_TIME_DIFF1, JOIN_SCRIPT1));
        this.metricStreamer.configure(config);
        this.metricStreamer.start();
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STARTED));

        MetricStreamJoinerConfig newConfig = config(
                streamDef(ID2, METRIC2, INPUT_STREAMS2, MAX_TIME_DIFF2, JOIN_SCRIPT2));
        this.metricStreamer.configure(newConfig);
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STARTED));
    }

    /**
     * A reconfiguration should preserve the start-state.
     */
    @Test
    public void reconfigureWhenStopped() {
        setUpReferencableMetricStreams(STREAM_REFERENCE_CPU_SYSTEM, STREAM_REFERENCE_CPU_USER,
                STREAM_REFERENCE_MEM_ALLOCATABLE, STREAM_REFERENCE_MEM_REQUESTED);

        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));

        MetricStreamJoinerConfig config = config(streamDef(ID1, METRIC1, INPUT_STREAMS1, MAX_TIME_DIFF1, JOIN_SCRIPT1));
        this.metricStreamer.configure(config);
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));

        MetricStreamJoinerConfig newConfig = config(
                streamDef(ID2, METRIC2, INPUT_STREAMS2, MAX_TIME_DIFF2, JOIN_SCRIPT2));
        this.metricStreamer.configure(newConfig);
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));
    }

    /**
     * Verify that the {@link JoiningMetricStream}s are properly registered with
     * the {@link EventBus} on start and unregistered on stop.
     */
    @Test
    public void startAndStop() throws Exception {
        setUpReferencableMetricStreams(STREAM_REFERENCE_CPU_SYSTEM, STREAM_REFERENCE_CPU_USER);

        MetricStreamJoinerConfig config = config(streamDef(ID1, METRIC1, INPUT_STREAMS1, MAX_TIME_DIFF1, JOIN_SCRIPT1));
        this.metricStreamer.configure(config);
        assertThat(this.metricStreamer.getConfiguration(), is(config));
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));

        // start
        this.metricStreamer.start();
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STARTED));
        assertRegisteredWithEventBus(this.metricStreamer.getMetricStreams());

        // stop
        this.metricStreamer.stop();
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STOPPED));
        // stop should be idempotent
        this.metricStreamer.stop();
        assertUnregisteredFromEventBus(this.metricStreamer.getMetricStreams());

        // restart
        this.metricStreamer.start();
        assertThat(this.metricStreamer.getStatus().getState(), is(State.STARTED));
        // start should be idempotent
        this.metricStreamer.start();
        assertRegisteredWithEventBus(this.metricStreamer.getMetricStreams());
    }

    /**
     * Verify that all {@link JoiningMetricStream}s have registered with the
     * {@link EventBus}.
     *
     * @param metricStreams
     */
    private void assertRegisteredWithEventBus(List<MetricStream> metricStreams) {
        for (MetricStream metricStream : metricStreams) {
            verify(eventBus).register(metricStream);
        }
    }

    /**
     * Verify that all {@link JoiningMetricStream}s have unregistered with the
     * {@link EventBus}.
     *
     * @param metricStreams
     */
    private void assertUnregisteredFromEventBus(List<MetricStream> metricStreams) {
        for (MetricStream metricStream : metricStreams) {
            verify(eventBus).unregister(metricStream);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void startBeforeConfigured() {
        this.metricStreamer.start();
    }

    @Test(expected = IllegalStateException.class)
    public void getMetricStreamsBeforeConfigured() {
        this.metricStreamer.getMetricStreams();
    }

    @Test(expected = IllegalStateException.class)
    public void getMetricStreamBeforeConfigured() {
        this.metricStreamer.getMetricStream(ID1);
    }

    @Test(expected = IllegalStateException.class)
    public void fetchBeforeConfigured() {
        this.metricStreamer.fetch();
    }

    @Test(expected = IllegalStateException.class)
    public void fetchBeforeStarted() {
        // monitoring subsystem set with a MetricStreamer for referenced streams
        setUpReferencableMetricStreams(STREAM_REFERENCE_CPU_SYSTEM, STREAM_REFERENCE_CPU_USER);

        MetricStreamJoinerConfig config = config(streamDef(ID1, METRIC1, INPUT_STREAMS1, MAX_TIME_DIFF1, JOIN_SCRIPT1));
        this.metricStreamer.configure(config);

        this.metricStreamer.fetch();
    }

    @Test
    public void fetch() {
        // monitoring subsystem set with a MetricStreamer for referenced streams
        setUpReferencableMetricStreams(STREAM_REFERENCE_CPU_SYSTEM, STREAM_REFERENCE_CPU_USER);

        MetricStreamJoinerConfig config = config(streamDef(ID1, METRIC1, INPUT_STREAMS1, MAX_TIME_DIFF1, JOIN_SCRIPT1));
        this.metricStreamer.configure(config);
        this.metricStreamer.start();

        this.metricStreamer.fetch();
    }

    private MetricStreamJoinerConfig config(MetricStreamDefinition... streamDefs) {
        return new MetricStreamJoinerConfig(Arrays.asList(streamDefs));
    }

    private MetricStreamDefinition streamDef(String id, String metric, Map<String, String> inputStreams,
            TimeInterval maxTimeDiff, List<String> joinScript) {
        return new MetricStreamDefinition(id, metric, inputStreams, maxTimeDiff, joinScript);
    }

    /**
     * Prepares the mock {@link MetricStreamer} to publish {@link MetricStream}s
     * with the given identifiers making these the {@link MetricStream}s that
     * the {@link JoiningMetricStream} can reference in input streams.
     *
     * @param metricStreamIds
     */
    private MetricStreamer<?> setUpReferencableMetricStreams(String... metricStreamIds) {

        List<MetricStream> mockedMetricStreams = new ArrayList<>();
        for (String metricStreamId : metricStreamIds) {
            MetricStream mockStream = mock(MetricStream.class);
            when(mockStream.getId()).thenReturn(metricStreamId);
            mockedMetricStreams.add(mockStream);
        }
        when(this.mockMetricStreamer.getMetricStreams()).thenReturn(mockedMetricStreams);

        return this.mockMetricStreamer;
    }

}
