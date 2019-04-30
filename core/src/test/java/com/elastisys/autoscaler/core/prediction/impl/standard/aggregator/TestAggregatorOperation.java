package com.elastisys.autoscaler.core.prediction.impl.standard.aggregator;

import static com.elastisys.autoscaler.core.prediction.impl.standard.aggregator.AggregatorTestUtils.config;
import static com.elastisys.autoscaler.core.prediction.impl.standard.aggregator.AggregatorTestUtils.maxAggregatorExpression;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.api.types.PredictionUnit;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.AggregatorConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.stubs.ConstantComputeUnitPredictor;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.precond.Preconditions;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Verifies the {@link Aggregator} logic.
 */
public class TestAggregatorOperation {
    static Logger logger = LoggerFactory.getLogger(TestAggregatorOperation.class);
    private final EventBus bus = mock(EventBus.class);

    /** Object under test. */
    private Aggregator aggregator;

    @Before
    public void onSetup() {
        this.aggregator = new Aggregator(logger);
    }

    @Test
    public void aggregatorMustAbortOnEmptyInput() throws Exception {
        AggregatorConfig config = config(maxAggregatorExpression());
        this.aggregator.validate(config);
        this.aggregator.configure(config);

        Map<Predictor, Optional<Prediction>> emptyInput = inputPredictions();
        Optional<Double> result = this.aggregator.aggregate(emptyInput, UtcTime.now());
        assertFalse(result.isPresent());
    }

    @Test
    public void executeAggregatorWithOnlyAbsentPredictions() throws Exception {
        AggregatorConfig config = config(maxAggregatorExpression());
        this.aggregator.validate(config);
        this.aggregator.configure(config);

        Optional<Prediction> missingPrediction = Optional.empty();
        Map<Predictor, Optional<Prediction>> onlyAbsentPredictions = zip(asList(predictor("p1"), predictor("p2")),
                asList(missingPrediction, missingPrediction));

        Optional<Double> result = this.aggregator.aggregate(onlyAbsentPredictions, UtcTime.now());
        assertFalse(result.isPresent());
    }

    /**
     * When there exists absent predictions the aggregator must not try to run
     * the aggregator script, as there is no guarantee that the script will work
     * (it is assumed to operate on all predictions).
     */
    @Test
    public void aggregatorMustAbortOnMissingPredictions() throws Exception {
        AggregatorConfig config = config(maxAggregatorExpression());
        this.aggregator.validate(config);
        this.aggregator.configure(config);

        // one prediction is absent
        Optional<Double> result = this.aggregator.aggregate(inputPredictions(1.2, null, 3.2), UtcTime.now());
        assertFalse(result.isPresent());
    }

    @Test
    public void executeWithValidAggregatorExpression() throws Exception {
        AggregatorConfig config = config(maxAggregatorExpression());
        this.aggregator.validate(config);
        this.aggregator.configure(config);

        Optional<Double> result = this.aggregator.aggregate(inputPredictions(1.2, 4.3, 3.2), UtcTime.now());
        assertTrue(result.isPresent());
        assertThat(result.get(), is(4.3));
    }

    @Test
    public void executeConstantExpression() throws Exception {
        AggregatorConfig config = config("1 + 1;");
        this.aggregator.validate(config);
        this.aggregator.configure(config);

        Optional<Double> result = this.aggregator.aggregate(inputPredictions(5.0), UtcTime.now());
        assertTrue(result.isPresent());
        assertThat(result.get(), is(1.0 + 1.0));
    }

    @Test
    public void executeMultiStatementExpression() throws Exception {
        String multiStatementScript = "" //
                + "var max = Number.MIN_VALUE;" + "\n" //
                + "for (var i = 0; i < input.predictions.length; i++) {" + "\n" //
                + "  if (input.predictions[i].prediction > max) {" + "\n"//
                + "    max = input.predictions[i].prediction;" + "\n" //
                + "  }" + "\n" //
                + "}" + "\n" //
                + "max;";
        AggregatorConfig config = config(multiStatementScript);
        this.aggregator.validate(config);
        this.aggregator.configure(config);

        Optional<Double> result = this.aggregator.aggregate(inputPredictions(5.1, 2.0, 4.0), UtcTime.now());
        assertTrue(result.isPresent());
        assertThat(result.get(), is(5.1));
    }

    /**
     * Verify failure on an aggregation script that does not produce a number.
     *
     * @throws Exception
     */
    @Test(expected = AggregatorException.class)
    public void executeNonNumericalExpression() throws Exception {
        // expression returns a string rather than a number
        String expression = "'a' + 'b'";
        AggregatorConfig config = config(expression);
        this.aggregator.validate(config);
        this.aggregator.configure(config);

        this.aggregator.aggregate(inputPredictions(5.0, 2.0), UtcTime.now());
    }

    /**
     * Verify failure on an aggregation script whose last statement is not an
     * expression.
     *
     * @throws Exception
     */
    @Test(expected = AggregatorException.class)
    public void executeScriptWhereFinalStatementIsNotAnExpression() throws Exception {
        // last statement is not an expression
        String expression = "if (true) { println('hej'); };";
        AggregatorConfig config = config(expression);
        this.aggregator.validate(config);
        this.aggregator.configure(config);

        this.aggregator.aggregate(inputPredictions(5.0, 2.0), UtcTime.now());
    }

    /**
     * Verify failure on an aggregation script that references missing
     * variables.
     *
     * @throws Exception
     */
    @Test(expected = AggregatorException.class)
    public void executeScriptReferencingMissingVariables() throws Exception {
        // p3 variable won't be available at runtime
        String expression = "Math.max(p1,p2,p3);";
        AggregatorConfig config = config(expression);
        this.aggregator.validate(config);
        this.aggregator.configure(config);

        this.aggregator.aggregate(inputPredictions(5.0, 2.0), UtcTime.now());
    }

    /**
     * Verify failure on an aggregation script that references an undeclared
     * function.
     *
     * @throws Exception
     */
    @Test(expected = AggregatorException.class)
    public void executeScriptReferencingUndeclaredFunction() throws Exception {
        // Math.maxium doesn't exist
        String expression = "Math.maximum(p1,p2);";
        AggregatorConfig config = config(expression);
        this.aggregator.validate(config);
        this.aggregator.configure(config);

        this.aggregator.aggregate(inputPredictions(5.0, 2.0), UtcTime.now());
    }

    /**
     * Constructs a collection of dummy predictions that can be passed as input
     * to an {@link Aggregator}'s {@link Aggregator#aggregate(Map, DateTime)}
     * method.
     *
     * @param predictions
     *            Prediction values. A <code>null</code> value will be
     *            translated into a {@link Optional#empty()} prediction.
     * @return A map of predictions (with dummy {@link Predictor}s) produced
     *         from the input values.
     */
    private Map<Predictor, Optional<Prediction>> inputPredictions(Double... predictions) {
        Map<Predictor, Optional<Prediction>> predictionTable = new HashMap<>();
        for (int i = 0; i < predictions.length; i++) {
            predictionTable.put(predictor("p" + (i + 1)), prediction(predictions[i]));
        }
        return predictionTable;
    }

    private static Map<Predictor, Optional<Prediction>> zip(List<Predictor> predictors,
            List<Optional<Prediction>> predictions) {
        Preconditions.checkArgument(predictors.size() == predictions.size(),
                "number of predictors and predictions must be equal");
        Map<Predictor, Optional<Prediction>> map = new HashMap<>();
        for (int i = 0; i < predictors.size(); i++) {
            Predictor predictor = predictors.get(i);
            Optional<Prediction> prediction = predictions.get(i);
            map.put(predictor, prediction);
        }
        return map;
    }

    /**
     * Creates a dummy {@link Predictor} with a given name.
     *
     * @param name
     *            The name of the {@link Predictor}.
     * @return
     */
    private Predictor predictor(String name) {
        MonitoringSubsystem mockedMonitoringSubsystem = mock(MonitoringSubsystem.class);
        Predictor predictor = new ConstantComputeUnitPredictor(logger, this.bus, mockedMonitoringSubsystem);
        predictor.configure(new PredictorConfig(name, ConstantComputeUnitPredictor.class.getName(), State.STARTED,
                "metric", JsonUtils.parseJsonString("{\"constant.prediction\": 1}").getAsJsonObject()));
        return predictor;
    }

    private Optional<Prediction> prediction(Double value) {
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(new Prediction(value, PredictionUnit.COMPUTE, "metric", UtcTime.now()));
    }
}
