package com.elastisys.autoscaler.metricstreamers.streamjoiner.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;

/**
 * Exercise {@link MetricStreamJoinerConfig}.
 */
public class TestMetricStreamJoinerConfig {
    private static final String ID1 = "total.cpu.stream";
    private static final String METRIC1 = "cpu_busy";
    private static final Map<String, String> INPUT_STREAMS1 = Maps.of(//
            "cpu_system", "cpu.system.sum.stream", //
            "cpu_user", "cpu.user.sum.stream");
    private static final TimeInterval MAX_TIME_DIFF1 = TimeInterval.seconds(10);
    private static final List<String> JOIN_SCRIPT1 = Arrays.asList(//
            "cpu_system + cpu_user");

    private static final String ID2 = "mem.utilization.stream";
    private static final String METRIC2 = "mem.utilization";
    private static final Map<String, String> INPUT_STREAMS2 = Maps.of(//
            "memRequested", "mem.requsted.sum.stream", //
            "memAllocatable", "mem.allocatable.sum.stream");
    private static final TimeInterval MAX_TIME_DIFF2 = TimeInterval.seconds(0);
    private static final List<String> JOIN_SCRIPT2 = Arrays.asList(//
            "memRequested / memAllocatable");

    /**
     * It is okay to not define any metric streams.
     */
    @Test
    public void noStreamDefinitions() {
        MetricStreamJoinerConfig config = new MetricStreamJoinerConfig(Collections.emptyList());
        config.validate();

        assertThat(config.getMetricStreams(), is(Collections.emptyList()));
    }

    @Test
    public void singleStreamDefinition() {
        MetricStreamDefinition streamDef1 = new MetricStreamDefinition(ID1, METRIC1, INPUT_STREAMS1, MAX_TIME_DIFF1,
                JOIN_SCRIPT1);
        MetricStreamJoinerConfig config = new MetricStreamJoinerConfig(Arrays.asList(streamDef1));
        config.validate();

        assertThat(config.getMetricStreams(), is(Arrays.asList(streamDef1)));
    }

    @Test
    public void multipleStreamDefinitions() {
        MetricStreamDefinition streamDef1 = new MetricStreamDefinition(ID1, METRIC1, INPUT_STREAMS1, MAX_TIME_DIFF1,
                JOIN_SCRIPT1);
        MetricStreamDefinition streamDef2 = new MetricStreamDefinition(ID2, METRIC2, INPUT_STREAMS2, MAX_TIME_DIFF2,
                JOIN_SCRIPT2);
        MetricStreamJoinerConfig config = new MetricStreamJoinerConfig(Arrays.asList(streamDef1, streamDef2));
        config.validate();

        assertThat(config.getMetricStreams(), is(Arrays.asList(streamDef1, streamDef2)));
    }

    /**
     * Each stream must have a unique id.
     */
    @Test(expected = IllegalArgumentException.class)
    public void duplicateStreamIds() {
        String duplicateId = ID1;
        MetricStreamDefinition streamDef1 = new MetricStreamDefinition(duplicateId, METRIC1, INPUT_STREAMS1,
                MAX_TIME_DIFF1, JOIN_SCRIPT1);
        MetricStreamDefinition streamDef2 = new MetricStreamDefinition(duplicateId, METRIC2, INPUT_STREAMS2,
                MAX_TIME_DIFF2, JOIN_SCRIPT2);
        new MetricStreamJoinerConfig(Arrays.asList(streamDef1, streamDef2)).validate();
    }

    /**
     * verify that validation propagates to stream definitions
     */
    @Test
    public void validationOfStreamDefinitions() {
        List<String> nullJoinScript = null;
        MetricStreamDefinition illegalStreamDef = new MetricStreamDefinition(ID1, METRIC1, INPUT_STREAMS1,
                MAX_TIME_DIFF1, nullJoinScript);
        MetricStreamDefinition streamDef2 = new MetricStreamDefinition(ID2, METRIC2, INPUT_STREAMS2, MAX_TIME_DIFF2,
                JOIN_SCRIPT2);
        try {
            new MetricStreamJoinerConfig(Arrays.asList(streamDef2, illegalStreamDef)).validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(ID1 + ": metricStream: no joinScript given"));
        }

    }
}
