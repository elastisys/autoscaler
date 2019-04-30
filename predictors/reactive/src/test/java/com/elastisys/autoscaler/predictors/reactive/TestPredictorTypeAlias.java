package com.elastisys.autoscaler.predictors.reactive;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictorTypeAlias;
import com.elastisys.autoscaler.predictors.reactive.ReactivePredictor;

/**
 * Verify that the {@link PredictorTypeAlias} entry for the
 * {@link ReactivePredictor} maps to the correct class name.
 */
public class TestPredictorTypeAlias {

    @Test
    public void verifyAliasToClassMapping() {
        assertThat(PredictorTypeAlias.ReactivePredictor.getFullClassName(),
                is(ReactivePredictor.class.getName()));
    }
}
