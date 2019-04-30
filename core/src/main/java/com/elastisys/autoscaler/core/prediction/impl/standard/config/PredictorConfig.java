package com.elastisys.autoscaler.core.prediction.impl.standard.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.prediction.impl.standard.StandardPredictionSubsystem;
import com.elastisys.autoscaler.core.prediction.impl.standard.aggregator.Aggregator;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictorRegistry;
import com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictorTypeAlias;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonElement;

/**
 * Specifies the behavior of a {@link Predictor} instance in a
 * {@link StandardPredictionSubsystem}. Implementation-specific behavior can be
 * passed through {@link #parameters}.
 *
 * @see PredictorRegistry
 * @see StandardPredictionSubsystem
 */
public class PredictorConfig {
    /** Default for {@link #state}. */
    public static final State DEFAULT_STATE = State.STARTED;

    /**
     * The first letter may be an underscore, a letter, or a dollar sign.
     * Subsequent characters may also contain digits.
     */
    private static final Pattern VALID_JS_ID = Pattern.compile("^[_a-zA-Z\\$][_a-zA-Z0-9\\$]*$");

    /**
     * The identifier of the {@link Predictor} instance. Must be a valid
     * JavaScript identifier (since it is to be referenced in an
     * {@link Aggregator} script).
     */
    private final String id;
    /**
     * The type of the {@link Predictor}. This may either be a fully qualified
     * Java class name or one of the shorthand aliases in
     * {@link PredictorTypeAlias}.
     */
    private final String type;
    /**
     * The {@link State} of the {@link Predictor} instance. May be
     * <code>null</code>. Default: {@link #DEFAULT_STATE}.
     */
    private final State state;
    /**
     * The identifier of the metric stream (published by the
     * {@link MetricStreamer}) that this {@link Predictor} subscribes to.
     */
    private final String metricStream;
    /**
     * Carries implementation-specific parameters (as json) for the
     * {@link Predictor}. May be <code>null</code>, in which case the
     * {@link Predictor} needs to supply a default behavior. A {@link Predictor}
     * may also choose to reject <code>null</code> parameters in case it cannot
     * supply default ones.
     */
    private final JsonElement parameters;

    /**
     * @param id
     *            The identifier of the {@link Predictor} instance.
     * @param type
     *            The type of the {@link Predictor}. This may either be a fully
     *            qualified Java class name or one of the shorthand aliases in
     *            {@link PredictorTypeAlias}.
     * @param state
     *            The {@link State} of the {@link Predictor} instance. May be
     *            <code>null</code>. Default: {@link #DEFAULT_STATE}.
     * @param metricStream
     *            The identifier of the metric stream (published by the
     *            {@link MetricStreamer}) that this {@link Predictor} subscribes
     *            to. After creation, the {@link Predictor} will have a
     *            {@link MetricStream} injected through which
     *            {@link MetricValue}s from the subscribed to metric stream will
     *            flow.
     * @param parameters
     *            Carries implementation-specific parameters (as json) for the
     *            {@link Predictor}. May be <code>null</code>, in which case the
     *            {@link Predictor} needs to supply a default behavior. A
     *            {@link Predictor} may also choose to reject <code>null</code>
     *            parameters in case it cannot supply default ones.
     */
    public PredictorConfig(String id, String type, State state, String metricStream, JsonElement parameters) {
        this.id = id;
        this.type = type;
        this.state = state;
        this.metricStream = metricStream;
        this.parameters = parameters;
    }

    /**
     * Returns the identifier of the {@link Predictor} instance.
     *
     * @return
     */
    public String getId() {
        return this.id;
    }

    /**
     * The type of the {@link Predictor}. This may either be a fully qualified
     * Java class name or one of the shorthand aliases in
     * {@link PredictorTypeAlias}.
     *
     * @return
     */
    public String getType() {
        return this.type;
    }

    /**
     * Returns the {@link State} of the {@link Predictor} instance.
     *
     * @return
     */
    public State getState() {
        return Optional.ofNullable(this.state).orElse(DEFAULT_STATE);
    }

    /**
     * Returns the identifier of the metric stream (published by the
     * {@link MetricStreamer}) that this {@link Predictor} subscribes to. After
     * creation, the {@link Predictor} will have a {@link MetricStream} injected
     * through which {@link MetricValue}s from the subscribed to metric stream
     * will flow.
     *
     * @return
     */
    public String getMetricStream() {
        return this.metricStream;
    }

    /**
     * Carries implementation-specific parameters (as json) for the
     * {@link Predictor}. May be <code>null</code>, in which case the
     * {@link Predictor} needs to supply a default behavior. A {@link Predictor}
     * may also choose to reject <code>null</code> parameters in case it cannot
     * supply default ones.
     *
     * @return
     */
    public JsonElement getParameters() {
        return this.parameters;
    }

    /**
     * Validates the {@link PredictorConfig}, in the sense that all fields are
     * verified to be non-<code>null</code>. If any field is missing, an
     * {@link IllegalArgumentException} is raised.
     *
     * @throws IllegalArgumentException
     *             If configuration fields are missing.
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(this.id != null, "predictor: missing id");
        checkArgument(this.type != null, "predictor: missing type");
        checkArgument(this.metricStream != null, "predictor: missing metricStream");
        try {
            validJavaScriptIdentifier(this.id);
        } catch (Exception e) {
            throw new IllegalArgumentException("predictor: id: " + e.getMessage());
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
    public boolean equals(Object obj) {
        if (obj instanceof PredictorConfig) {
            PredictorConfig that = (PredictorConfig) obj;
            return Objects.equals(this.id, that.id) //
                    && Objects.equals(this.type, that.type) //
                    && Objects.equals(this.state, that.state) //
                    && Objects.equals(this.metricStream, that.metricStream) //
                    && Objects.equals(this.parameters, that.parameters);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.type, this.state, this.metricStream, this.parameters);
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
