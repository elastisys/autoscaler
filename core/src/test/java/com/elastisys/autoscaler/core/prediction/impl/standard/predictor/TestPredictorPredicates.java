package com.elastisys.autoscaler.core.prediction.impl.standard.predictor;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictorPredicates;

/**
 * Verifies the behavior of the {@link PredictorPredicates}.
 */
public class TestPredictorPredicates {

    private static final Logger logger = LoggerFactory.getLogger(TestPredictorPredicates.class);

    private Predictor predictor = mock(Predictor.class);

    @Before
    public void onSetup() {
    }

    @Test
    public void testIsStarted() {
        when(this.predictor.getStatus()).thenReturn(stoppedState());
        assertThat(PredictorPredicates.isStarted().test(this.predictor), is(false));

        when(this.predictor.getStatus()).thenReturn(startedState());
        assertThat(PredictorPredicates.isStarted().test(this.predictor), is(true));
    }

    @Test
    public void testWithIdIn() {
        PredictorConfig config = configWithId("id1");
        assertThat(PredictorPredicates.withIdIn(set()).test(config), is(false));
        assertThat(PredictorPredicates.withIdIn(set("id2", "id3")).test(config), is(false));

        assertThat(PredictorPredicates.withIdIn(set("id1", "id2")).test(config), is(true));
        assertThat(PredictorPredicates.withIdIn(set("id1")).test(config), is(true));
    }

    @Test
    public void testWithId() {
        when(this.predictor.getConfiguration()).thenReturn(configWithId("id1"));

        assertThat(PredictorPredicates.withId("id1").test(this.predictor), is(true));

        assertThat(PredictorPredicates.withId("id2").test(this.predictor), is(false));
    }

    @Test
    public void testWithConfigIn() {
        when(this.predictor.getConfiguration()).thenReturn(configWithId("id1"));

        Set<PredictorConfig> configs = new HashSet<>(asList(configWithId("id1"), configWithId("id2")));
        assertThat(PredictorPredicates.withConfigIn(configs).test(this.predictor), is(true));

        configs = new HashSet<>(asList(configWithId("id2")));
        assertThat(PredictorPredicates.withConfigIn(configs).test(this.predictor), is(false));
        configs = new HashSet<>();
        assertThat(PredictorPredicates.withConfigIn(configs).test(this.predictor), is(false));
    }

    private HashSet<String> set(String... ids) {
        return new HashSet<>(asList(ids));
    }

    private PredictorConfig configWithId(String id) {
        return new PredictorConfig(id, "java.type", State.STOPPED, "metric", null);
    }

    private ServiceStatus stoppedState() {
        return new ServiceStatus.Builder().started(false).build();
    }

    private ServiceStatus startedState() {
        return new ServiceStatus.Builder().started(true).build();
    }
}
