package com.elastisys.autoscaler.core.prediction.impl.standard.predictor;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamException;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.impl.EmptyResultSet;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.stubs.MetricStreamerStub;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.impl.noop.NoOpSystemHistorian;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.predictor.PredictorRegistry;
import com.elastisys.autoscaler.core.prediction.impl.standard.stubs.MonitoringSubsystemStub;
import com.google.gson.JsonObject;

/**
 * Various test utility methods to be used from tests.
 */
public class PredictionTestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PredictionTestUtils.class);

    public static PredictorConfig predictorConfig(String id, Class<? extends Predictor> predictorType, State state,
            String metricStream, JsonObject parameters) {
        String predictorClass = predictorType == null ? null : predictorType.getName();
        return new PredictorConfig(id, predictorClass, state, metricStream, parameters);
    }

    public static PredictorConfig predictorConfig(String id, String predictorClass, State state, String metricStream,
            JsonObject config) {
        return new PredictorConfig(id, predictorClass, state, metricStream, config);
    }

    /**
     * Creates a configuration for a {@link PredictorRegistry} from a
     * {@link List} of {@link PredictorConfig} objects.
     *
     * @param configs
     * @return
     */
    public static List<PredictorConfig> configs(PredictorConfig... configs) {
        return Arrays.asList(configs);
    }

    /**
     * Constructs and configures a {@link MetricStreamer} mock with a collection
     * of configured {@link MetricStream}s.
     *
     * @see MetricStreamerStub
     *
     * @param supportedMetricStreamIds
     *            The metric stream ids that can be subscribed to.
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static MetricStreamer createMetricStreamerStub(String... supportedMetricStreamIds) {
        List<MetricStream> metricStreams = new ArrayList<>();
        for (String streamId : supportedMetricStreamIds) {
            metricStreams.add(new FakeMetricStream(streamId));
        }

        return new FakeMetricStreamer(metricStreams);
    }

    /**
     * Creates a {@link MonitoringSubsystemStub} configured with the given
     * {@link MetricStreamer} (no system.
     *
     * @param metricStreamer
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static MonitoringSubsystem createMonitoringSubsystemStub(MetricStreamer metricStreamer) {
        return new MonitoringSubsystemStub(asList(metricStreamer), new NoOpSystemHistorian(LOG));
    }

    /**
     * Fake {@link MetricStreamer} for testing purposes.
     *
     */
    private static class FakeMetricStreamer implements MetricStreamer<Object> {

        private final List<MetricStream> metricStreams;
        private boolean started = true;

        public FakeMetricStreamer(List<MetricStream> metricStreams) {
            this.metricStreams = metricStreams;
        }

        @Override
        public void start() throws IllegalStateException {
            this.started = true;
        }

        @Override
        public void stop() {
            this.started = false;
        }

        @Override
        public ServiceStatus getStatus() {
            return new ServiceStatus.Builder().started(this.started).build();
        }

        @Override
        public void validate(Object configuration) throws IllegalArgumentException {
        }

        @Override
        public void configure(Object configuration) throws IllegalArgumentException {
        }

        @Override
        public Object getConfiguration() {
            return new Object();
        }

        @Override
        public Class<Object> getConfigurationClass() {
            return Object.class;
        }

        @Override
        public List<MetricStream> getMetricStreams() {
            return this.metricStreams;
        }

        @Override
        public MetricStream getMetricStream(String metricStreamId) throws IllegalArgumentException {
            for (MetricStream metricStream : this.metricStreams) {
                if (metricStreamId.equals(metricStream.getId())) {
                    return metricStream;
                }
            }
            throw new IllegalArgumentException("unrecognized metric stream: " + metricStreamId);
        }

        @Override
        public void fetch() throws MetricStreamException, IllegalStateException {
        }
    }

    /**
     * Fake {@link MetricStream} for testing purposes.
     */
    private static class FakeMetricStream implements MetricStream {

        private final String id;

        public FakeMetricStream(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public String getMetric() {
            return this.id + ".metric";
        }

        @Override
        public QueryResultSet query(Interval timeInterval, QueryOptions options) {
            return new EmptyResultSet();
        }

    }
}
