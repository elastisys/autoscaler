package com.elastisys.autoscaler.core.prediction.impl.standard.predictor;

import static com.elastisys.autoscaler.core.api.types.ServiceStatus.State.STARTED;
import static com.elastisys.autoscaler.core.api.types.ServiceStatus.State.STOPPED;
import static com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictionTestUtils.predictorConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.Health;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.stubs.PredictorStub;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.google.gson.JsonObject;

/**
 * Verifies the behavior of the {@link PredictorRegistry} operation logic.
 */
public class TestPredictorRegistryOperation {
    static Logger logger = LoggerFactory.getLogger(TestPredictorRegistryOperation.class);

    private static final PredictorConfig p1Stopped = predictorConfig("p1", PredictorStub.class, STOPPED,
            "metric1.stream", new JsonObject());
    private static final PredictorConfig p2Stopped = predictorConfig("p2", PredictorStub.class, STOPPED,
            "metric2.stream", new JsonObject());
    /** Slightly modified variant of p2 config with state started. */
    private static final PredictorConfig p1Started = predictorConfig("p1", PredictorStub.class, STARTED,
            "metric1.stream", new JsonObject());

    /** Object under test. */
    private PredictorRegistry predictorRegistry;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final EventBus eventBus = mock(EventBus.class);

    @Before
    public void onSetup() {
        MetricStreamer metricStreamer = PredictionTestUtils.createMetricStreamerStub("metric1.stream",
                "metric2.stream");
        MonitoringSubsystem monitoringSubsystem = PredictionTestUtils.createMonitoringSubsystemStub(metricStreamer);
        this.predictorRegistry = new PredictorRegistry(logger, this.eventBus, this.executor, monitoringSubsystem,
                FileUtils.cwd());
    }

    @Test
    public void startAndStopEmptyConfig() throws Exception {
        List<PredictorConfig> emptyConfig = PredictionTestUtils.configs();

        // configure
        this.predictorRegistry.validate(emptyConfig);
        this.predictorRegistry.configure(emptyConfig);
        assertThat(this.predictorRegistry.getStatus(), is(stopped()));

        // start
        this.predictorRegistry.start();
        assertThat(this.predictorRegistry.getStatus(), is(started()));
        assertThat(this.predictorRegistry.getPredictors().size(), is(0));
        assertThat(this.predictorRegistry.getStartedPredictors().size(), is(0));

        // stop
        this.predictorRegistry.stop();
        assertThat(this.predictorRegistry.getStatus(), is(stopped()));
        assertThat(this.predictorRegistry.getPredictors().size(), is(0));
        assertThat(this.predictorRegistry.getStartedPredictors().size(), is(0));
    }

    @Test
    public void startAndStopWithSingleStoppedPredictor() throws Exception {
        List<PredictorConfig> config = PredictionTestUtils.configs(p1Stopped);

        // configure
        this.predictorRegistry.validate(config);
        this.predictorRegistry.configure(config);
        assertThat(this.predictorRegistry.getStatus(), is(stopped()));

        // start
        this.predictorRegistry.start();
        assertThat(this.predictorRegistry.getStatus(), is(started()));
        assertThat(this.predictorRegistry.getPredictors().size(), is(1));
        assertThat(this.predictorRegistry.getStartedPredictors().size(), is(0));

        // stop
        this.predictorRegistry.stop();
        assertThat(this.predictorRegistry.getStatus(), is(stopped()));
        assertThat(this.predictorRegistry.getPredictors().size(), is(1));
        assertThat(this.predictorRegistry.getStartedPredictors().size(), is(0));
    }

    @Test
    public void startAndStopWithSingleStartedPredictor() throws Exception {
        List<PredictorConfig> config = PredictionTestUtils.configs(p1Started);

        // configure
        this.predictorRegistry.validate(config);
        this.predictorRegistry.configure(config);
        assertThat(this.predictorRegistry.getStatus(), is(stopped()));

        // start
        this.predictorRegistry.start();
        assertThat(this.predictorRegistry.getStatus(), is(started()));
        assertThat(this.predictorRegistry.getPredictors().size(), is(1));
        assertThat(this.predictorRegistry.getStartedPredictors().size(), is(1));

        // stop
        this.predictorRegistry.stop();
        assertThat(this.predictorRegistry.getStatus(), is(stopped()));
        assertThat(this.predictorRegistry.getPredictors().size(), is(1));
        assertThat(this.predictorRegistry.getStartedPredictors().size(), is(0));
    }

    @Test
    public void startAndStopWithOneStartedAndOneStoppedPredictor() throws Exception {
        List<PredictorConfig> config = PredictionTestUtils.configs(p1Started, p2Stopped);

        // configure
        this.predictorRegistry.validate(config);
        this.predictorRegistry.configure(config);
        assertThat(this.predictorRegistry.getStatus(), is(stopped()));

        // start
        this.predictorRegistry.start();
        assertThat(this.predictorRegistry.getStatus(), is(started()));
        assertThat(this.predictorRegistry.getPredictors().size(), is(2));
        assertThat(this.predictorRegistry.getStartedPredictors().size(), is(1));

        // stop
        this.predictorRegistry.stop();
        assertThat(this.predictorRegistry.getStatus(), is(stopped()));
        assertThat(this.predictorRegistry.getPredictors().size(), is(2));
        assertThat(this.predictorRegistry.getStartedPredictors().size(), is(0));
    }

    @Test
    public void restart() throws Exception {
        List<PredictorConfig> config = PredictionTestUtils.configs(p1Started, p2Stopped);

        // configure
        this.predictorRegistry.validate(config);
        this.predictorRegistry.configure(config);
        assertThat(this.predictorRegistry.getStatus(), is(stopped()));

        // start
        this.predictorRegistry.start();
        assertThat(this.predictorRegistry.getStatus(), is(started()));
        assertThat(this.predictorRegistry.getPredictors().size(), is(2));
        assertThat(this.predictorRegistry.getStartedPredictors().size(), is(1));

        // stop
        this.predictorRegistry.stop();
        assertThat(this.predictorRegistry.getStatus(), is(stopped()));
        assertThat(this.predictorRegistry.getPredictors().size(), is(2));
        assertThat(this.predictorRegistry.getStartedPredictors().size(), is(0));

        // restart
        this.predictorRegistry.start();
        assertThat(this.predictorRegistry.getStatus(), is(started()));
        assertThat(this.predictorRegistry.getPredictors().size(), is(2));
        assertThat(this.predictorRegistry.getStartedPredictors().size(), is(1));
    }

    private ServiceStatus started() {
        return new ServiceStatus(State.STARTED, Health.OK);
    }

    private ServiceStatus stopped() {
        return new ServiceStatus(State.STOPPED, Health.OK);
    }

}
