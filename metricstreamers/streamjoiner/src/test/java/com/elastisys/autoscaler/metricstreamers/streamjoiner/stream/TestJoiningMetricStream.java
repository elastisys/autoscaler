package com.elastisys.autoscaler.metricstreamers.streamjoiner.stream;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.metronome.api.MetronomeEvent;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.autoscaler.metricstreamers.streamjoiner.TestMetricStreamJoiner;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link JoiningMetricStream}.
 */
public class TestJoiningMetricStream {
    private static final Logger LOG = LoggerFactory.getLogger(TestMetricStreamJoiner.class);

    private static final String ID1 = "total.cpu.stream";
    private static final String METRIC1 = "cpu_busy";

    private static final MetricStream CPU_SYSTEM_STREAM = mock(MetricStream.class);
    private static final MetricStream CPU_USER_STREAM = mock(MetricStream.class);
    static {
        when(CPU_SYSTEM_STREAM.getId()).thenReturn("cpu.system.sum.stream");
        when(CPU_SYSTEM_STREAM.getMetric()).thenReturn("cpu.system.sum");

        when(CPU_USER_STREAM.getId()).thenReturn("cpu.user.sum.stream");
        when(CPU_USER_STREAM.getMetric()).thenReturn("cpu.user.sum");
    }

    private static final Map<String, MetricStream> INPUT_STREAMS1 = Maps.of(//
            "cpu_system", CPU_SYSTEM_STREAM, //
            "cpu_user", CPU_USER_STREAM);
    private static final TimeInterval MAX_TIME_DIFF1 = TimeInterval.seconds(10);
    private static final List<String> JOIN_SCRIPT1 = Arrays.asList("cpu_system + cpu_user");
    private EventBus eventBus;
    private MetricListener eventListener;

    /** Object under test. */
    private JoiningMetricStream metricStream;

    @Before
    public void beforeTestMethod() throws ScriptException {
        this.eventBus = new SynchronousEventBus(LOG);
        this.eventListener = new MetricListener();
        this.eventBus.register(this.eventListener);

        FrozenTime.setFixed(UtcTime.parse("2017-01-01T12:00:00.000Z"));

        JoiningMetricStreamConfig config = new JoiningMetricStreamConfig(ID1, METRIC1, MAX_TIME_DIFF1, INPUT_STREAMS1,
                compileScript(JOIN_SCRIPT1));
        this.metricStream = new JoiningMetricStream(LOG, this.eventBus, config);
    }

    @Test
    public void basicSanity() {
        assertThat(this.metricStream.getId(), is(ID1));
        assertThat(this.metricStream.getMetric(), is(METRIC1));

        // should always respond with an empty query result
        Interval queryInterval = new Interval(new DateTime(0), UtcTime.now());
        assertThat(this.metricStream.query(queryInterval, null).hasNext(), is(false));
    }

    /**
     * Verify basic behavior of the {@link JoiningMetricStream} when it comes to
     * processing incoming metrics and producing new metric values.
     * <ul>
     * <li>no output until observations have been made for all input
     * streams</li>
     * <li>no output when input stream observations are too far apart</li>
     * </ul>
     */
    @Test
    public void basicOperation() {
        this.metricStream.start();

        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(0));

        postMetricValues(CPU_SYSTEM_STREAM, UtcTime.now(), 1.0);
        // only metrics received on one stream, nothing to join yet...
        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(0));

        postMetricValues(CPU_SYSTEM_STREAM, UtcTime.now(), 2.0);
        // only metrics received on one stream, nothing to join yet...
        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(0));

        postMetricValues(CPU_USER_STREAM, UtcTime.now(), 90.0);
        // we have observations on both streams => now apply join function and
        // produce output
        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(1));
        assertThat(this.eventListener.getLatestMetricByStreamId(ID1).getMetric(), is(METRIC1));
        assertThat(this.eventListener.getLatestMetricByStreamId(ID1).getTime(), is(UtcTime.now()));
        assertThat(this.eventListener.getLatestMetricByStreamId(ID1).getValue(), is(2.0 + 90.0));

        this.eventListener.clear();

        FrozenTime.tick((int) MAX_TIME_DIFF1.getSeconds() + 1);

        postMetricValues(CPU_SYSTEM_STREAM, UtcTime.now(), 10.0);
        // should not apply join function => metric values too far apart
        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(0));
        postMetricValues(CPU_USER_STREAM, UtcTime.now(), 20.0);
        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(1));
        assertThat(this.eventListener.getLatestMetricByStreamId(ID1).getValue(), is(10.0 + 20.0));

    }

    /**
     * The {@link JoiningMetricStream} should wait until started before
     * processing messages.
     */
    @Test
    public void shouldNotProcessMetricsUntilStarted() {
        // at this point the metric streamer is not started (and should not be
        // listening)
        postMetricValues(CPU_SYSTEM_STREAM, UtcTime.now(), 1.0);
        postMetricValues(CPU_USER_STREAM, UtcTime.now(), 1.0);

        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(0));

        // now metric processing should start ...
        this.metricStream.start();

        postMetricValues(CPU_SYSTEM_STREAM, UtcTime.now(), 2.0);
        postMetricValues(CPU_USER_STREAM, UtcTime.now(), 3.0);

        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(1));
    }

    /**
     * When stopped, no metrics values are to be processed.
     */
    @Test
    public void shouldNotProcessMetricsWhenStopped() {
        this.metricStream.start();
        this.metricStream.stop();

        postMetricValues(CPU_SYSTEM_STREAM, UtcTime.now(), 1.0);
        postMetricValues(CPU_USER_STREAM, UtcTime.now(), 1.0);

        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(0));
    }

    /**
     * The {@link JoiningMetricStream} should only ever take action when metrics
     * are received on subscribed to input streams.
     */
    @Test
    public void shouldNotReactOnUnknownMetricStreams() throws Exception {
        Map<String, MetricStream> inputStreams = Maps.of("cpu_user", CPU_USER_STREAM);
        CompiledScript joinScript = compileScript(asList("100 * cpu_user"));
        JoiningMetricStreamConfig config = new JoiningMetricStreamConfig(ID1, METRIC1, MAX_TIME_DIFF1, inputStreams,
                joinScript);
        this.metricStream = new JoiningMetricStream(LOG, this.eventBus, config);
        this.metricStream.start();

        // should not react to metrics that are not sent on one of the input
        // streams
        postMetricValues(CPU_SYSTEM_STREAM, UtcTime.now(), 1.0);
        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(0));
        postMetricValues(CPU_SYSTEM_STREAM, UtcTime.now(), 2.0);
        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(0));
        postMetricValues(CPU_SYSTEM_STREAM, UtcTime.now(), 3.0);
        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(0));

        postMetricValues(CPU_USER_STREAM, UtcTime.now(), 2.0);
        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(1));
        assertThat(this.eventListener.getLatestMetricByStreamId(ID1).getValue(), is(100 * 2.0));
    }

    /**
     * When a collection of {@link MetricValue}s are received in a
     * {@link MetricStreamMessage}, only the latest should be considered.
     */
    @Test
    public void shouldOnlyConsiderLatestInputStreamMetric() throws Exception {
        Map<String, MetricStream> inputStreams = Maps.of("cpu_user", CPU_USER_STREAM);
        CompiledScript joinScript = compileScript(asList("100 * cpu_user"));
        JoiningMetricStreamConfig config = new JoiningMetricStreamConfig(ID1, METRIC1, MAX_TIME_DIFF1, inputStreams,
                joinScript);
        this.metricStream = new JoiningMetricStream(LOG, this.eventBus, config);
        this.metricStream.start();

        // should only consider the most recent metric value in the batch
        DateTime now = UtcTime.now();
        postMetricValues(CPU_USER_STREAM, //
                metricValue(CPU_USER_STREAM, now.minus(2), 0.0), //
                metricValue(CPU_USER_STREAM, now.minus(1), 1.0), //
                metricValue(CPU_USER_STREAM, now.minus(0), 2.0));
        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(1));
        assertThat(this.eventListener.getLatestMetricByStreamId(ID1).getValue(), is(100 * 2.0));
    }

    /**
     * Metric values that are delivered out-of-order should be ignored. That is,
     * if a more recent {@link MetricValue} has been observed for an input
     * stream, a later delivered {@link MetricValue} (with an older timestamp)
     * should be ignored.
     */
    @Test
    public void ignoreMetricsDeliveredOutOfOrder() throws Exception {
        Map<String, MetricStream> inputStreams = Maps.of("cpu_user", CPU_USER_STREAM);
        CompiledScript joinScript = compileScript(asList("100 * cpu_user"));
        JoiningMetricStreamConfig config = new JoiningMetricStreamConfig(ID1, METRIC1, MAX_TIME_DIFF1, inputStreams,
                joinScript);
        this.metricStream = new JoiningMetricStream(LOG, this.eventBus, config);
        this.metricStream.start();

        DateTime now = UtcTime.now();
        postMetricValues(CPU_USER_STREAM, now.minus(10), 1.0);
        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(1));
        assertThat(this.eventListener.getLatestMetricByStreamId(ID1).getValue(), is(100 * 1.0));

        FrozenTime.tick();

        // a new metric with older timestamp is received => it should be ignored

        this.eventListener.clear();

        postMetricValues(CPU_USER_STREAM, now.minus(20), 2.0);
        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(0));
    }

    /**
     * Ignore any {@link MetricStreamMessage}s that do not contain any
     * {@link MetricValue}s.
     */
    @Test
    public void ignoreEmptyMetricMessages() throws Exception {
        Map<String, MetricStream> inputStreams = Maps.of("cpu_user", CPU_USER_STREAM);
        CompiledScript joinScript = compileScript(asList("100 * cpu_user"));
        JoiningMetricStreamConfig config = new JoiningMetricStreamConfig(ID1, METRIC1, MAX_TIME_DIFF1, inputStreams,
                joinScript);
        this.metricStream = new JoiningMetricStream(LOG, this.eventBus, config);
        this.metricStream.start();

        DateTime now = UtcTime.now();
        postMetricValues(CPU_USER_STREAM, now);
        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(0));
    }

    /**
     * On failure to execute the join script, an {@link Alert} must be raised on
     * the {@link EventBus}.
     */
    @Test
    public void alertOnScriptFailure() throws Exception {
        AlertListener alertListener = new AlertListener();
        this.eventBus.register(alertListener);

        // script does not produce a number and should therefore fail
        Map<String, MetricStream> inputStreams = Maps.of("cpu_user", CPU_USER_STREAM);
        CompiledScript joinScript = compileScript(asList("'a';"));
        JoiningMetricStreamConfig config = new JoiningMetricStreamConfig(ID1, METRIC1, MAX_TIME_DIFF1, inputStreams,
                joinScript);
        this.metricStream = new JoiningMetricStream(LOG, this.eventBus, config);
        this.metricStream.start();

        assertThat(alertListener.getAlerts().size(), is(0));

        postMetricValues(CPU_USER_STREAM, UtcTime.now(), 1.0);

        assertThat(alertListener.getAlerts().size(), is(1));
    }

    /**
     * Whenever the {@link JoiningMetricStream} produces a new value, it should
     * post a {@link MetronomeEvent#RESIZE_ITERATION} event on the
     * {@link EventBus} to trigger a new resize iteration.
     */
    @Test
    public void triggerResizeIterationOnOutput() throws Exception {
        ResizeTriggerListener resizeTriggerListener = new ResizeTriggerListener();
        this.eventBus.register(resizeTriggerListener);

        Map<String, MetricStream> inputStreams = Maps.of("cpu_user", CPU_USER_STREAM);
        CompiledScript joinScript = compileScript(asList("100 * cpu_user"));
        JoiningMetricStreamConfig config = new JoiningMetricStreamConfig(ID1, METRIC1, MAX_TIME_DIFF1, inputStreams,
                joinScript);
        this.metricStream = new JoiningMetricStream(LOG, this.eventBus, config);
        this.metricStream.start();

        DateTime now = UtcTime.now();
        // empty metric values => no joiner output => no resize iteration
        postMetricValues(CPU_USER_STREAM, now);
        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(0));
        assertThat(resizeTriggerListener.getResizeTriggers().size(), is(0));

        // joiner will produce output => should post resize iteration event
        postMetricValues(CPU_USER_STREAM, now, 1.0);
        assertThat(this.eventListener.getMetricsByStreamId(ID1).size(), is(1));
        assertThat(resizeTriggerListener.getResizeTriggers().size(), is(1));
    }

    private static CompiledScript compileScript(List<String> scriptLines) throws ScriptException {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
        CompiledScript script = ((Compilable) engine).compile(scriptLines.stream().collect(Collectors.joining("\n")));
        return script;
    }

    private void postMetricValues(MetricStream sourceStream, DateTime time, double... values) {
        List<MetricValue> metricValues = new ArrayList<>();
        for (double value : values) {
            metricValues.add(metricValue(sourceStream, time, value));
        }

        this.eventBus.post(new MetricStreamMessage(sourceStream.getId(), metricValues));
    }

    private void postMetricValues(MetricStream sourceStream, MetricValue... values) {
        this.eventBus.post(new MetricStreamMessage(sourceStream.getId(), asList(values)));
    }

    private MetricValue metricValue(MetricStream source, DateTime time, double value) {
        return new MetricValue(source.getMetric(), value, time);
    }

}
