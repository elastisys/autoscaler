package com.elastisys.autoscaler.core.prediction.impl.standard.predictor;

import static com.elastisys.autoscaler.core.api.types.ServiceStatus.State.STARTED;
import static com.elastisys.autoscaler.core.api.types.ServiceStatus.State.STOPPED;
import static com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictionTestUtils.configs;
import static com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictionTestUtils.predictorConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictorRegistry.PredictorRegistryConfigDiff;
import com.elastisys.autoscaler.core.prediction.impl.standard.stubs.PredictorStub;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Verifies the behavior of the {@link PredictorRegistryConfigDiff} class.
 */
public class TestPredictorRegistryConfigDiff {

    private static final PredictorConfig p1 = predictorConfig("p1", PredictorStub.class, STARTED, "metric1.stream",
            new JsonObject());
    private static final PredictorConfig p2 = predictorConfig("p2", PredictorStub.class, STARTED, "metric2.stream",
            JsonUtils.parseJsonString("{\"option1\": \"value1\"}").getAsJsonObject());
    private static final PredictorConfig p3 = predictorConfig("p3", PredictorStub.class, STARTED, "metric3.stream",
            JsonUtils.parseJsonString("{\"option1\": \"value1\"}").getAsJsonObject());

    /** Slightly modified variant of p1 wiht a different state. */
    private static final PredictorConfig p1Modified = predictorConfig("p1", PredictorStub.class, STOPPED,
            "metric1.stream", new JsonObject());
    /** Slightly modified variant of p2 with a different config value. */
    private static final PredictorConfig p2Modified = predictorConfig("p2", PredictorStub.class, STARTED,
            "metric2.stream", JsonUtils.parseJsonString("{\"option1\": \"value2\"}").getAsJsonObject());
    /**
     * Slightly modified variant of p3 with a different predictor-specific
     * config.
     */
    private static final PredictorConfig p3Modified = predictorConfig("p3", PredictorStub.class, STARTED,
            "metric3.stream", JsonUtils.parseJsonString("{\"option2\": \"value1\"}").getAsJsonObject());

    @Test
    public void compareEmptyConfigurations() {
        assertFalse(diff(null, null).isDifferent());
        assertTrue(diff(null, null).added().isEmpty());
        assertTrue(diff(null, null).deleted().isEmpty());
        assertTrue(diff(null, null).modified().isEmpty());

        assertFalse(diff(configs(), null).isDifferent());
        assertTrue(diff(configs(), null).added().isEmpty());
        assertTrue(diff(configs(), null).deleted().isEmpty());
        assertTrue(diff(configs(), null).modified().isEmpty());

        assertFalse(diff(null, configs()).isDifferent());
        assertTrue(diff(null, configs()).added().isEmpty());
        assertTrue(diff(null, configs()).deleted().isEmpty());
        assertTrue(diff(null, configs()).modified().isEmpty());

        assertFalse(diff(configs(), configs()).isDifferent());
        assertTrue(diff(configs(), configs()).added().isEmpty());
        assertTrue(diff(configs(), configs()).deleted().isEmpty());
        assertTrue(diff(configs(), configs()).modified().isEmpty());
    }

    @Test
    public void compareConfigurationsWithNoDiff() {
        assertFalse(diff(configs(p1), configs(p1)).isDifferent());
        assertTrue(diff(configs(p1), configs(p1)).added().isEmpty());
        assertTrue(diff(configs(p1), configs(p1)).deleted().isEmpty());
        assertTrue(diff(configs(p1), configs(p1)).modified().isEmpty());

        assertFalse(diff(configs(p1, p2), configs(p1, p2)).isDifferent());
        assertTrue(diff(configs(p1, p2), configs(p1, p2)).added().isEmpty());
        assertTrue(diff(configs(p1, p2), configs(p1, p2)).deleted().isEmpty());
        assertTrue(diff(configs(p1, p2), configs(p1, p2)).modified().isEmpty());
    }

    /**
     * Test comparing configurations that only differ in the newer configuration
     * having additional elements.
     */
    @Test
    public void compareConfigurationsWithAdditionalElements() {
        List<PredictorConfig> oldConfig = configs();
        List<PredictorConfig> newConfig = configs(p1, p2, p3);
        PredictorRegistryConfigDiff diff = diff(oldConfig, newConfig);
        assertThat(diff.isDifferent(), is(true));
        assertThat(diff.added(), is(configs(p1, p2, p3)));
        assertThat(diff.deleted(), is(configs()));
        assertThat(diff.modified(), is(configs()));

        oldConfig = configs(p2);
        newConfig = configs(p1, p2, p3);
        diff = diff(oldConfig, newConfig);
        assertThat(diff.isDifferent(), is(true));
        assertThat(diff.added(), is(configs(p1, p3)));
        assertThat(diff.deleted(), is(configs()));
        assertThat(diff.modified(), is(configs()));

        oldConfig = configs(p1, p2);
        newConfig = configs(p1, p2, p3);
        diff = diff(oldConfig, newConfig);
        assertThat(diff.isDifferent(), is(true));
        assertThat(diff.added(), is(configs(p3)));
        assertThat(diff.deleted(), is(configs()));
        assertThat(diff.modified(), is(configs()));
    }

    /**
     * Test comparing configurations that only differ in the newer configuration
     * having fewer elements.
     */
    @Test
    public void compareConfigurationsWithDeletedElements() {
        List<PredictorConfig> oldConfig = configs(p1, p2, p3);
        List<PredictorConfig> newConfig = configs(p1, p3);

        PredictorRegistryConfigDiff diff = diff(oldConfig, newConfig);
        assertThat(diff.isDifferent(), is(true));
        assertThat(diff.added(), is(configs()));
        assertThat(diff.deleted(), is(configs(p2)));
        assertThat(diff.modified(), is(configs()));
    }

    /**
     * Test comparing configurations that only differ in the newer configuration
     * having some elements in the old configuration modified.
     */
    @Test
    public void compareConfigurationsWithModifiedConfigs() {
        List<PredictorConfig> oldConfig = configs(p1, p2, p3);
        List<PredictorConfig> newConfig = configs(p1, p2Modified, p3);
        PredictorRegistryConfigDiff diff = diff(oldConfig, newConfig);
        assertThat(diff.isDifferent(), is(true));
        assertThat(diff.added(), is(configs()));
        assertThat(diff.deleted(), is(configs()));
        assertThat(diff.modified(), is(configs(p2Modified)));

        oldConfig = configs(p1, p2, p3);
        newConfig = configs(p1, p2Modified, p3Modified);
        diff = diff(oldConfig, newConfig);
        assertThat(diff.isDifferent(), is(true));
        assertThat(diff.added(), is(configs()));
        assertThat(diff.deleted(), is(configs()));
        assertThat(diff.modified(), is(configs(p2Modified, p3Modified)));

        oldConfig = configs(p1, p2, p3);
        newConfig = configs(p1Modified, p2Modified, p3Modified);
        diff = diff(oldConfig, newConfig);
        assertThat(diff.isDifferent(), is(true));
        assertThat(diff.added(), is(configs()));
        assertThat(diff.deleted(), is(configs()));
        assertThat(diff.modified(), is(configs(p1Modified, p2Modified, p3Modified)));
    }

    /**
     * Test comparing configurations that only differ in several aspects (added,
     * deleted and modified elements)
     */
    @Test
    public void compareConfigurationsDifferingInSeveralAspects() {
        List<PredictorConfig> oldConfig = configs(p1, p2);
        List<PredictorConfig> newConfig = configs(p2Modified, p3);
        PredictorRegistryConfigDiff diff = diff(oldConfig, newConfig);
        assertThat(diff.isDifferent(), is(true));
        assertThat(diff.added(), is(configs(p3)));
        assertThat(diff.deleted(), is(configs(p1)));
        assertThat(diff.modified(), is(configs(p2Modified)));
    }

    private PredictorRegistryConfigDiff diff(List<PredictorConfig> oldconf, List<PredictorConfig> newconf) {
        return new PredictorRegistryConfigDiff(oldconf, newconf);
    }

}
