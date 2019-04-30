package com.elastisys.autoscaler.core.prediction.impl.standard.aggregator;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.Configurable;
import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.api.types.PredictionUnit;
import com.elastisys.autoscaler.core.prediction.impl.standard.StandardPredictionSubsystem;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.AggregatorConfig;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * An {@link Aggregator} takes care of producing a single <i>aggregate
 * prediction</i> from the set of {@link Prediction}s produced by the
 * {@link Predictor}s. All received {@link Prediction}s are assumed to be
 * expressed in {@link PredictionUnit#COMPUTE} units.
 * <p/>
 * The {@link Aggregator} applies an aggregation expression to the set of
 * {@link Prediction}s. This expression is expressed using JavaScript and is
 * passed to the {@link Aggregator} via configuration.
 * <p/>
 * The contract of the aggregator is as follows: <i/>the end result of executing
 * the last script statement/expression must be a single numerical value</i>.
 * The predictions are passed to the aggregator script in a {@code input}
 * variable which can be referenced from the script. It follows a structure
 * similar to this:
 *
 * <pre>
 * input: {
 *   timestamp: '2013-03-08T14:51:53.095+01:00',
 *   predictions: [
 *     {predictor: p1, prediction:1.0, confidence: Confidence{}},
 *     {predictor: p2, prediction:3.0, confidence: Confidence{}},
 *     {predictor: p3, prediction:3.0, confidence: Confidence{}}
 *   ]
 * }
 * </pre>
 *
 * Besides the {@code input} variable, each {@link Predictor}'s prediction can
 * be referenced directly with a variable of the same name as the
 * {@link Predictor}'s id. In the above case, the following three variables
 * would also be referenceable from the script:
 *
 * <pre>
 *     p1: {predictor: p1, prediction:1.0, confidence: Confidence{}}
 *     p2: {predictor: p2, prediction:3.0, confidence: Confidence{}}
 *     p3: {predictor: p3, prediction:3.0, confidence: Confidence{}}
 * </pre>
 *
 * As an example, an aggregator script could look as follows:
 *
 * <pre>
 * Math.max.apply(Math, input.predictions.map( function(p){return p.prediction;} ))
 * </pre>
 *
 * It returns the highest prediction.
 *
 * @see StandardPredictionSubsystem
 * @see AggregatorConfig
 *
 */
public class Aggregator implements Configurable<AggregatorConfig> {
    private final Logger logger;

    /** The {@link Aggregator} configuration. */
    private AggregatorConfig config;

    @Inject
    public Aggregator(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void validate(AggregatorConfig configuration) throws IllegalArgumentException {
        checkArgument(configuration != null, "aggregator: configuration cannot be null");
        configuration.validate();

        // checks that expression is a valid javascript
        compileJavaScript(configuration.getExpression());
    }

    @Override
    public void configure(AggregatorConfig configuration) throws IllegalArgumentException {
        validate(configuration);

        this.config = configuration;
    }

    @Override
    public AggregatorConfig getConfiguration() {
        return this.config;
    }

    @Override
    public Class<AggregatorConfig> getConfigurationClass() {
        return AggregatorConfig.class;
    }

    /**
     * Produces an <i>aggregate prediction</i> by applying the aggregation
     * script on a collection of {@link Predictor} predictions.
     *
     * @param predictions
     *            A collection of {@link Prediction}s, all assumed to be
     *            expressed in {@link PredictionUnit#COMPUTE} units.
     * @param predictionTime
     *            The time for which the predictions apply.
     * @return The aggregate prediction as produced by the aggregation
     *         JavaScript.
     * @throws AggregatorException
     */
    public Optional<Double> aggregate(Map<Predictor, Optional<Prediction>> predictions, DateTime predictionTime)
            throws AggregatorException {
        ensureConfigured();

        if (predictions.isEmpty()) {
            return Optional.empty();
        }

        // all predictors must have produced predictions or else we cancel the
        // aggregation
        if (predictions.values().stream().anyMatch(it -> !it.isPresent())) {
            return Optional.empty();
        }

        try {
            ScriptEngine scriptEngine = createScriptEngine();

            String initScript = aggregatorInputScript(predictions, predictionTime);
            this.logger.debug("executing aggregator init script:\n{}", initScript.toString());
            scriptEngine.eval(initScript);
            this.logger.debug(String.format("executing aggregator script:\n%s", this.config.getExpression()));
            Object returnValue = scriptEngine.eval(this.config.getExpression());
            Double result = validateOutput(returnValue);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            throw new AggregatorException(String.format("Prediction aggregation" + " script \"%s\" failed: %s",
                    this.config.getExpression(), e.getMessage()), e);
        }
    }

    /**
     * Validate that script return value is a number and convert to a double
     * value. Throws an {@link AggregatorException} on failure to validate
     * output.
     *
     * @param returnValue
     *            The script output.
     * @return The returned numerical value.
     * @throws AggregatorException
     *             thrown on failure to validate result.
     */
    private Double validateOutput(Object returnValue) throws AggregatorException {
        requireNonNull(returnValue, "aggregator script evaulated to null");
        if (!Number.class.isAssignableFrom(returnValue.getClass())) {
            throw new AggregatorException(
                    String.format("Output value type is not a number: %s", returnValue.getClass()));
        }
        Number number = Number.class.cast(returnValue);
        return number.doubleValue();
    }

    /**
     * Returns an initialization script that sets the input variables to be used
     * by for the aggregator script. See the class-level javadoc for a detailed
     * description of the script input parameters.
     *
     * @param predictionMap
     * @param predictionTime
     * @return
     */
    private String aggregatorInputScript(Map<Predictor, Optional<Prediction>> predictionMap, DateTime predictionTime) {
        StringWriter initScript = new StringWriter();

        List<PredictorOutput> predictions = new ArrayList<>();
        for (Entry<Predictor, Optional<Prediction>> predictionEntry : predictionMap.entrySet()) {
            String predictorId = predictionEntry.getKey().getConfiguration().getId();
            Prediction predictionResult = predictionEntry.getValue().get();
            double predictionValue = predictionResult.getValue();
            PredictorOutput prediction = new PredictorOutput(predictorId, predictionValue);
            predictions.add(prediction);
            // add variable for predictor to context
            initScript.write(String.format("var %s = %s;\n", predictorId, prediction.toJson()));
        }
        AggregatorInput aggregatorInput = new AggregatorInput(predictionTime, predictions);
        // add 'input' variable to context
        initScript.write(String.format("var input = %s;\n", aggregatorInput.toJson()));
        return initScript.toString();
    }

    /**
     * Compiles a java script. Throws a {@link IllegalArgumentException} on
     * failure to do so.
     *
     * @param javascript
     *            The JavaScipt to be compiled.
     * @return The {@link CompiledScript}.
     * @throws IllegalArgumentException
     */
    private CompiledScript compileJavaScript(String javascript) throws IllegalArgumentException {
        Compilable compiler = (Compilable) createScriptEngine();
        try {
            return compiler.compile(javascript);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    format("aggregator: failed to compile javascript expression: %s", javascript), e);
        }
    }

    /**
     * Creates a new JavaScript {@link ScriptEngine}.
     *
     * @return
     */
    private ScriptEngine createScriptEngine() {
        return new ScriptEngineManager().getEngineByName("JavaScript");
    }

    private void ensureConfigured() {
        checkState(this.config != null, "aggregator: cannot operate without an aggregator expression set");
    }

    /**
     * Represents a set of predictions produced by the {@link Predictor}s.
     * <p/>
     * An {@link AggregatorInput} instance is passed as input to the aggregator
     * (java)script.
     */
    public static final class AggregatorInput {
        private final DateTime timestamp;
        private final PredictorOutput[] predictions;

        public AggregatorInput(DateTime timestamp, List<PredictorOutput> predictions) {
            this.timestamp = timestamp;
            this.predictions = predictions.toArray(new PredictorOutput[0]);
        }

        public DateTime getTimestamp() {
            return this.timestamp;
        }

        public PredictorOutput[] getPredictions() {
            return this.predictions;
        }

        public String toJson() {
            return JsonUtils.toString(JsonUtils.toJson(this));
        }
    }

    /**
     * Represents a {@link Predictor}'s predicted value.
     */
    public static final class PredictorOutput {
        private final String predictor;
        private final double prediction;

        public PredictorOutput(String predictorId, double prediction) {
            this.predictor = predictorId;
            this.prediction = prediction;
        }

        public String toJson() {
            return JsonUtils.toString(JsonUtils.toJson(this));
        }

        public String getPredictor() {
            return this.predictor;
        }

        public double getPrediction() {
            return this.prediction;
        }
    }
}
