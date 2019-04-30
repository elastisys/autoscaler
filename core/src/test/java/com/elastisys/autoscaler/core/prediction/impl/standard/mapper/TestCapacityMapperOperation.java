package com.elastisys.autoscaler.core.prediction.impl.standard.mapper;

import static com.elastisys.autoscaler.core.prediction.api.types.PredictionUnit.COMPUTE;
import static com.elastisys.autoscaler.core.prediction.api.types.PredictionUnit.METRIC;
import static com.elastisys.autoscaler.core.prediction.impl.standard.mapper.CapacityMapperTestUtils.mapping;
import static com.elastisys.autoscaler.core.prediction.impl.standard.mapper.CapacityMapperTestUtils.mappings;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.api.types.PredictionUnit;
import com.elastisys.autoscaler.core.prediction.impl.standard.mapper.CapacityMapper;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Tests the operation logic of the {@link CapacityMapper}.
 */
public class TestCapacityMapperOperation {

    static Logger logger = LoggerFactory.getLogger(TestCapacityMapperOperation.class);

    /** Object under test. */
    private CapacityMapper mapper;

    @Before
    public void onSetup() {
        this.mapper = new CapacityMapper(logger);
    }

    @Test
    public void convertAbsentPrediction() {
        this.mapper.configure(mappings(mapping("metric1", 10.0)));

        Optional<Prediction> prediction = Optional.empty();
        Optional<Prediction> computeUnitPrediction = this.mapper.toComputeUnits(prediction);
        assertThat(computeUnitPrediction.isPresent(), is(false));
    }

    @Test
    public void basicMapping() {
        this.mapper.configure(mappings(mapping("metric1", 20.0)));

        Optional<Prediction> metricPrediction = prediction(100.0, METRIC, "metric1");
        Optional<Prediction> machinePrediction = this.mapper.toComputeUnits(metricPrediction);
        assertThat(machinePrediction.isPresent(), is(true));
        assertThat(machinePrediction.get(), is(metricPrediction.get().withValue(5.0).withUnit(COMPUTE)));
    }

    @Test
    public void multipleMappings() {
        this.mapper.configure(mappings(mapping("metric1", 20.0), mapping("metric2", 40.0)));

        Optional<Prediction> metricPrediction = prediction(100.0, METRIC, "metric1");
        Optional<Prediction> machinePrediction = this.mapper.toComputeUnits(metricPrediction);
        assertThat(machinePrediction.isPresent(), is(true));
        assertThat(machinePrediction.get(), is(metricPrediction.get().withValue(5.0).withUnit(COMPUTE)));

        metricPrediction = prediction(100.0, METRIC, "metric2");
        machinePrediction = this.mapper.toComputeUnits(metricPrediction);
        assertThat(machinePrediction.isPresent(), is(true));
        assertThat(machinePrediction.get(), is(metricPrediction.get().withValue(2.5).withUnit(COMPUTE)));
    }

    @Test
    public void mapComputeUnitPrediction() {
        this.mapper.configure(mappings(mapping("metric1", 20.0)));

        Optional<Prediction> computeUnitPrediction = prediction(10.0, COMPUTE, "metric1");
        Optional<Prediction> machinePrediction = this.mapper.toComputeUnits(computeUnitPrediction);
        assertThat(machinePrediction.isPresent(), is(true));
        assertThat(machinePrediction.get(), is(computeUnitPrediction.get()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapUnrecognizedMetric() {
        this.mapper.configure(mappings(mapping("metric1", 20.0)));

        Optional<Prediction> metricPrediction = prediction(100.0, METRIC, "unrecognizedMetric");
        this.mapper.toComputeUnits(metricPrediction);
    }

    private Optional<Prediction> prediction(double value, PredictionUnit unit, String metric) {
        return Optional.of(new Prediction(value, unit, metric, UtcTime.now()));
    }

}
