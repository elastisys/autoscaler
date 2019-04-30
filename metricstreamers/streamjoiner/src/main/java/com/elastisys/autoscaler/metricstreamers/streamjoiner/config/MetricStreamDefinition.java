package com.elastisys.autoscaler.metricstreamers.streamjoiner.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static java.lang.String.format;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.metricstreamers.streamjoiner.MetricStreamJoiner;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Describes a {@link MetricStream} published by the {@link MetricStreamJoiner}.
 * More specifically, it declares a set of input metric streams to be consumed
 * and a join-function used to produce output values for the joined metric
 * stream.
 *
 * @see MetricStreamJoinerConfig
 */
public class MetricStreamDefinition {
    public static final TimeInterval DEFAULT_MAX_TIME_DIFF = TimeInterval.seconds(0);

    /**
     * The first letter may be an underscore, a letter, or a dollar sign.
     * Subsequent characters may also contain digits.
     */
    private static final Pattern VALID_JS_ID = Pattern.compile("^[_a-zA-Z\\$][_a-zA-Z0-9\\\\$]*$");

    /**
     * The id of the metric stream. This is the id that will be used by clients
     * wishing to subscribe to this metric stream. Required.
     */
    private final String id;

    /**
     * The name of the metric produced by this metric stream. This is the metric
     * that will be set for produced {@link MetricValue}s. Optional. If left
     * out, {@link #id} is used.
     */
    private final String metric;
    /**
     * Declares the input metric streams that are to be joined by this metric
     * stream. Keys are <i>metric stream aliases</i> and values are metric
     * stream identifiers. The referenced metric streams must have been declared
     * prior to this metric stream (note that this can be another joined metric
     * stream). A <i>metric stream alias</i> needs to be a valid JavaScript
     * identifier name, as these will be passed as variables to the
     * {@link #joinScript}. Required.
     */
    private final Map<String, String> inputStreams;

    /**
     * The maximum difference in time between observed metric stream values for
     * the joined metric stream to apply its {@link #joinScript} and produce a
     * new value. If stream metrics are farther apart than this, no new metric
     * value is produced on the joined stream. This value should be set so that
     * values that are close enough to be considered relevant to each other can
     * be joined. The default is 0 s, assuming that the input metric streams are
     * scraped at the same instant. May be <code>null</code>.
     */
    private final TimeInterval maxTimeDiff;

    /**
     * An array of JavaScript code lines for the join function. <i>The final
     * statement/expression that the script executes must be a numerical
     * value</i>. The script can assume that, when executed, each of the metric
     * stream aliases defined in the {@link #inputStreams} section will be
     * assigned a {@code Number} value. Required.
     */
    private final List<String> joinScript;

    /**
     * Creates a {@link MetricStreamDefinition}.
     *
     * @param id
     *            The id of the metric stream. This is the id that will be used
     *            by clients wishing to subscribe to this metric stream.
     *            Required.
     * @param metric
     *            The name of the metric produced by this metric stream. This is
     *            the metric that will be set for produced {@link MetricValue}s.
     *            Optional. If left out, {@link #id} is used.
     * @param inputStreams
     *            Declares the input metric streams that are to be joined by
     *            this metric stream. Keys are <i>metric stream aliases</i> and
     *            values are metric stream identifiers. The referenced metric
     *            streams must have been declared prior to this metric stream
     *            (note that this can be another joined metric stream). A
     *            <i>metric stream alias</i> needs to be a valid JavaScript
     *            identifier name, as these will be passed as variables to the
     *            {@link #joinScript}. Required.
     * @param maxTimeDiff
     *            The maximum difference in time between observed metric stream
     *            values for the joined metric stream to apply its
     *            {@link #joinScript} and produce a new value. If stream metrics
     *            are farther apart than this, no new metric value is produced
     *            on the joined stream. This value should be set so that values
     *            that are close enough to be considered relevant to each other
     *            can be joined. The default is 0 s, assuming that the input
     *            metric streams are scraped at the same instant. May be
     *            <code>null</code>.
     * @param joinScript
     *            An array of JavaScript code lines for the join function.
     *            <i>The final statement/expression that the script executes
     *            must be a numerical value</i>. The script can assume that,
     *            when executed, each of the metric stream aliases defined in
     *            the {@link #inputStreams} section will be assigned a
     *            {@code Number} value. Required.
     */
    public MetricStreamDefinition(String id, String metric, Map<String, String> inputStreams, TimeInterval maxTimeDiff,
            List<String> joinScript) {
        this.id = id;
        this.metric = metric;
        this.inputStreams = inputStreams;
        this.maxTimeDiff = maxTimeDiff;
        this.joinScript = joinScript;
    }

    /**
     * The id of the metric stream. This is the id that will be used by clients
     * wishing to subscribe to this metric stream.
     *
     * @return
     */
    public String getId() {
        return this.id;
    }

    /**
     * The name of the metric produced by this metric stream. This is the metric
     * that will be set for produced {@link MetricValue}s.
     *
     * @return
     */
    public String getMetric() {
        return Optional.ofNullable(this.metric).orElse(this.id);
    }

    /**
     * Declares the input metric streams that are to be joined by this metric
     * stream. Keys are <i>metric stream aliases</i> and values are metric
     * stream identifiers. The referenced metric streams must have been declared
     * prior to this metric stream (note that this can be another joined metric
     * stream). A <i>metric stream alias</i> needs to be a valid JavaScript
     * identifier name, as these will be passed as variables to the
     * {@link #joinScript}.
     *
     * @return
     */
    public Map<String, String> getInputStreams() {
        return this.inputStreams;
    }

    /**
     * The maximum difference in time between observed metric stream values for
     * the joined metric stream to apply its {@link #joinScript} and produce a
     * new value. If stream metrics are farther apart than this, no new metric
     * value is produced on the joined stream. This value should be set so that
     * values that are close enough to be considered relevant to each other can
     * be joined.
     *
     * @return
     */
    public TimeInterval getMaxTimeDiff() {
        return Optional.ofNullable(this.maxTimeDiff).orElse(DEFAULT_MAX_TIME_DIFF);
    }

    /**
     * A JavaScript for the join function. <i>The final statement/expression
     * that the script executes must be a numerical value</i>. The script can
     * assume that, when executed, each of the metric stream aliases defined in
     * the {@link #inputStreams} section will be assigned a {@code Number}
     * value.
     *
     * @return
     */
    public String getJoinScript() {
        return this.joinScript.stream().collect(Collectors.joining("\n"));
    }

    /**
     * Returns the {@link #joinScript} as a compiled a JavaScript. If invalid,
     * an {@link IllegalArgumentException} is thrown.
     *
     * @param javascript
     */
    public CompiledScript getCompiledJoinScript() throws IllegalArgumentException {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
        Compilable compiler = (Compilable) engine;
        try {
            return compiler.compile(getJoinScript());
        } catch (Exception e) {
            throw new IllegalArgumentException(format("failed to compile javascript expression: %s", e.getMessage()),
                    e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.inputStreams, this.maxTimeDiff, this.joinScript);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetricStreamDefinition) {
            MetricStreamDefinition that = (MetricStreamDefinition) obj;
            return Objects.equals(this.id, that.id) //
                    && Objects.equals(this.inputStreams, that.inputStreams) //
                    && Objects.equals(this.maxTimeDiff, that.maxTimeDiff) //
                    && Objects.equals(this.joinScript, that.joinScript);
        }
        return false;
    }

    public void validate() throws IllegalArgumentException {

        try {
            checkArgument(this.id != null, "no id given");
            checkArgument(this.inputStreams != null, "no inputStreams given");
            checkArgument(!this.inputStreams.isEmpty(), "at least one inputStream must be given");

            try {
                this.inputStreams.keySet().forEach(metricStreamAlias -> validJavaScriptIdentifier(metricStreamAlias));
            } catch (Exception e) {
                throw new IllegalArgumentException("inputStreams: " + e.getMessage(), e);
            }
            this.inputStreams.values().forEach(metricStreamId -> {
                checkArgument(metricStreamId != null, "inputStreams: a null value was given as a metricStream id");
                checkArgument(metricStreamId != this.id, "inputStreams: input stream with id %s is a self-reference",
                        this.id);
            });

            try {
                getMaxTimeDiff().validate();
            } catch (Exception e) {
                throw new IllegalArgumentException("maxTimeDiff: " + e.getMessage(), e);
            }

            checkArgument(this.joinScript != null, "no joinScript given");
            checkArgument(!getJoinScript().trim().isEmpty(), "joinScript: cannot be empty");
            try {
                getCompiledJoinScript();
            } catch (Exception e) {
                throw new IllegalArgumentException("joinScript: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("metricStream: " + e.getMessage(), e);
        }
    }

    /**
     * Checks that an identifier is a valid JavaScript identifier. Throws an
     * {@link IllegalArgumentException} if it isn't.
     *
     * @param identifier
     * @throws IllegalArgumentException
     */
    public static void validJavaScriptIdentifier(String identifier) throws IllegalArgumentException {
        if (!VALID_JS_ID.matcher(identifier).matches()) {
            throw new IllegalArgumentException(String.format("%s is not a valid JavaScript identifier", identifier));
        }
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
