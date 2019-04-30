package com.elastisys.autoscaler.metricstreamers.streamjoiner.config;

import static java.util.stream.Collectors.joining;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.CompiledScript;

import org.junit.Test;

import com.elastisys.autoscaler.metricstreamers.streamjoiner.stream.JoiningMetricStream;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;

/**
 * Exercise the {@link MetricStreamDefinition}.
 */
public class TestMetricStreamDefinition {

    private static final String ID = "total.cpu.stream";
    private static final String METRIC = "cpu_busy";
    private static final Map<String, String> INPUT_STREAMS = Maps.of(//
            "cpu_system", "cpu.system.sum.stream", //
            "cpu_user", "cpu.user.sum.stream");
    private static final TimeInterval MAX_TIME_DIFF = TimeInterval.seconds(10);
    private static final List<String> JOIN_SCRIPT = Arrays.asList(//
            "cpu_system + cpu_user");

    /**
     * Verify basic sanity of a complete (and correct) configuration: it should
     * validate and return correct values for its constituents parts.
     */
    @Test
    public void correctCompleteConfig() {
        MetricStreamDefinition streamDef = new MetricStreamDefinition(ID, METRIC, INPUT_STREAMS, MAX_TIME_DIFF,
                JOIN_SCRIPT);
        streamDef.validate();

        assertThat(streamDef.getId(), is(ID));
        assertThat(streamDef.getMetric(), is(METRIC));
        assertThat(streamDef.getInputStreams(), is(INPUT_STREAMS));
        assertThat(streamDef.getMaxTimeDiff(), is(MAX_TIME_DIFF));
        assertThat(streamDef.getJoinScript(), is(JOIN_SCRIPT.stream().collect(joining("\n"))));
        assertThat(streamDef.getCompiledJoinScript(), is(instanceOf(CompiledScript.class)));
    }

    /**
     * {@code metric} and {@code maxTimeDiff} are optional.
     */
    @Test
    public void defaults() {
        String nullMetric = null;
        TimeInterval nullMaxTimeDiff = null;

        MetricStreamDefinition streamDef = new MetricStreamDefinition(ID, nullMetric, INPUT_STREAMS, nullMaxTimeDiff,
                JOIN_SCRIPT);
        streamDef.validate();

        // check defaults
        assertThat(streamDef.getMetric(), is(ID));
        assertThat(streamDef.getMaxTimeDiff(), is(MetricStreamDefinition.DEFAULT_MAX_TIME_DIFF));
    }

    /**
     * {@code id} is a required field.
     */
    @Test
    public void missingId() {
        String nullId = null;
        try {
            new MetricStreamDefinition(nullId, METRIC, INPUT_STREAMS, MAX_TIME_DIFF, JOIN_SCRIPT).validate();
            fail("expected to fail validation");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("no id given"));
        }
    }

    /**
     * {@code inputStreams} is a required field.
     */
    @Test
    public void missingInputStreams() {
        Map<String, String> nullInputStreams = null;
        try {
            new MetricStreamDefinition(ID, METRIC, nullInputStreams, MAX_TIME_DIFF, JOIN_SCRIPT).validate();
            fail("expected to fail validation");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("no inputStreams given"));
        }
    }

    /**
     * At least one input stream is requred.
     */
    @Test
    public void emptyInputStreams() {
        Map<String, String> emptyInputStreams = Collections.emptyMap();
        try {
            new MetricStreamDefinition(ID, METRIC, emptyInputStreams, MAX_TIME_DIFF, JOIN_SCRIPT).validate();
            fail("expected to fail validation");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("at least one inputStream must be given"));
        }
    }

    /**
     * Input stream aliases must be valid JavaScript identifiers (as they are to
     * be passed to the joinScript).
     */
    @Test
    public void illegalInputStreamAliases() {
        // the first letter may be an underscore, a letter, or a dollar sign.
        Map<String, String> inputStreams = Maps.of("1cpu", "cpu.sum.stream");

        try {
            new MetricStreamDefinition(ID, METRIC, inputStreams, MAX_TIME_DIFF, JOIN_SCRIPT).validate();
            fail("expected to fail validation");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("is not a valid JavaScript identifier"));
        }
    }

    /**
     * An inputStream must specify a metric stream id.
     */
    @Test
    public void missingInputStreamIds() {
        // no metric stream id specified
        Map<String, String> inputStreams = new HashMap<>();
        inputStreams.put("cpu", null);

        try {
            new MetricStreamDefinition(ID, METRIC, inputStreams, MAX_TIME_DIFF, JOIN_SCRIPT).validate();
            fail("expected to fail validation");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("a null value was given as a metricStream id"));
        }
    }

    /**
     * A {@link JoiningMetricStream} must not specify itself as an inputStream
     * (cyclical dependency).
     */
    @Test
    public void selfReferentialInputStream() {
        // no metric stream id specified
        Map<String, String> inputStreams = new HashMap<>();
        inputStreams.put("cpu", ID);

        try {
            new MetricStreamDefinition(ID, METRIC, inputStreams, MAX_TIME_DIFF, JOIN_SCRIPT).validate();
            fail("expected to fail validation");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("self-reference"));
        }
    }

    /**
     * {@code joinScript} is a required field.
     */
    @Test
    public void missingJsonScript() {
        List<String> nullJsonScript = null;
        try {
            new MetricStreamDefinition(ID, METRIC, INPUT_STREAMS, MAX_TIME_DIFF, nullJsonScript).validate();
            fail("expected to fail validation");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("no joinScript given"));
        }

    }

    /**
     * {@code joinScript} cannot be empty
     */
    @Test
    public void emptyJsonScript() {
        List<String> emptyJsonScript = Collections.emptyList();
        try {
            new MetricStreamDefinition(ID, METRIC, INPUT_STREAMS, MAX_TIME_DIFF, emptyJsonScript).validate();
            fail("expected to fail validation");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("joinScript: cannot be empty"));
        }

    }

    /**
     * JsonScript must be a valid JavaScript.
     */
    @Test
    public void illegalJavascriptSyntaxScript() {
        // missing paranthesis in conditional
        List<String> malformedJsonScript = Arrays.asList("if a > 10 { b }");
        try {
            new MetricStreamDefinition(ID, METRIC, INPUT_STREAMS, MAX_TIME_DIFF, malformedJsonScript).validate();
            fail("expected to fail validation");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("joinScript: failed to compile"));
        }
    }

    @Test
    public void validationOfJavaScriptIdentifiers() {
        List<String> validIdentifiers = Arrays.asList("myStream", "stream1", "my_stream", "$Stream", "$$");
        for (String validIdentifier : validIdentifiers) {
            try {
                MetricStreamDefinition.validJavaScriptIdentifier(validIdentifier);
            } catch (IllegalArgumentException e) {
                fail("unexpectedly failed to validate JavaScript identifier: " + e.getMessage());
            }
        }

        List<String> invalidIdentifiers = Arrays.asList("1stream", "*stream", " stream", "!stream", "p-1", "p+1");
        for (String invalidIdentifier : invalidIdentifiers) {
            try {
                MetricStreamDefinition.validJavaScriptIdentifier(invalidIdentifier);
                fail("expected validation of JavaScript identifier to fail: " + invalidIdentifier);
            } catch (IllegalArgumentException e) {
                // expected
            }
        }
    }
}
