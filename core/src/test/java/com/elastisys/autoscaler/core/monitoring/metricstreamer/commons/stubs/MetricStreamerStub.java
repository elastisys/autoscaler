package com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.stubs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamException;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.MetricStreamDriver;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * A {@link MetricStreamer} implementation intended for testing purposes.
 */
/**
 * @author peterg
 *
 */
public class MetricStreamerStub implements MetricStreamer<MetricStreamerStubConfig> {
    private final Logger logger;
    private final ScheduledExecutorService executor;
    private final EventBus eventBus;
    private final List<MetricStreamer<?>> priorDeclaredMetricStreamers;

    private MetricStreamerStubConfig config;
    private boolean started = false;
    private MetricStreamDriver streamDriver;

    @Inject
    public MetricStreamerStub(Logger logger, ScheduledExecutorService executor, EventBus eventBus,
            List<MetricStreamer<?>> priorDeclaredMetricStreamers) {
        this.logger = logger;
        this.executor = executor;
        this.eventBus = eventBus;
        this.priorDeclaredMetricStreamers = priorDeclaredMetricStreamers;
    }

    @Override
    public void start() throws IllegalStateException {

        TimeInterval pollInterval = new TimeInterval(15L, TimeUnit.SECONDS);
        TimeInterval firstQueryLookback = new TimeInterval(5L, TimeUnit.MINUTES);
        this.streamDriver = new MetricStreamDriver(this.logger, this.executor, this.eventBus, metricStreams(),
                pollInterval, firstQueryLookback);
        this.streamDriver.start();
        this.started = true;
    }

    @Override
    public void stop() {
        this.streamDriver.stop();
        this.streamDriver = null;
        this.started = false;
    }

    @Override
    public ServiceStatus getStatus() {
        return new ServiceStatus.Builder().started(this.started).build();
    }

    @Override
    public void validate(MetricStreamerStubConfig configuration) throws IllegalArgumentException {
        configuration.validate();
    }

    @Override
    public void configure(MetricStreamerStubConfig configuration) throws IllegalArgumentException {
        this.config = configuration;
    }

    @Override
    public MetricStreamerStubConfig getConfiguration() {
        return this.config;
    }

    @Override
    public Class<MetricStreamerStubConfig> getConfigurationClass() {
        return MetricStreamerStubConfig.class;
    }

    @Override
    public List<MetricStream> getMetricStreams() {
        return metricStreams();
    }

    @Override
    public MetricStream getMetricStream(String metricStreamId) throws IllegalArgumentException {
        for (MetricStream stream : metricStreams()) {
            if (stream.getId().equals(metricStreamId)) {
                return stream;
            }
        }
        throw new IllegalArgumentException("unrecognized metric stream: " + metricStreamId);
    }

    private List<MetricStream> metricStreams() {
        List<MetricStream> streams = new ArrayList<>();
        List<MetricStreamerStubStreamDefinition> metricStreams = this.config.getMetricStreams();
        for (MetricStreamerStubStreamDefinition streamDefinition : metricStreams) {
            streams.add(new MetricStreamStub(streamDefinition.getId(), streamDefinition.getMetric(),
                    Collections.emptyList()));
        }
        return streams;
    }

    /**
     * Returns the {@link MetricStreamer}s that were injected as declared prior
     * to this {@link MetricStreamer} (and which it can therefore reference).
     *
     * @return
     */
    public List<MetricStreamer<?>> getPriorDeclaredMetricStreamers() {
        return this.priorDeclaredMetricStreamers;
    }

    @Override
    public void fetch() throws MetricStreamException, IllegalStateException {
        this.streamDriver.fetch();
    }
}
