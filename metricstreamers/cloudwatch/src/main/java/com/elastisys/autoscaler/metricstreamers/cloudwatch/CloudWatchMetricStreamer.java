package com.elastisys.autoscaler.metricstreamers.cloudwatch;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamException;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.MetricStreamDriver;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchMetricStreamerConfig;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.stream.CloudWatchMetricStream;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.stream.MetricStreamConfig;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Implementation of {@link MetricStreamer} that uses AWS CloudWatch as its
 * back-end metric source.
 */
public class CloudWatchMetricStreamer implements MetricStreamer<CloudWatchMetricStreamerConfig> {
    private final Logger logger;
    private final ScheduledExecutorService executor;
    private final EventBus eventBus;

    /** The currently set configuration. */
    private CloudWatchMetricStreamerConfig config;
    /**
     * The {@link MetricStreamDriver} which collects metrics for each of the
     * configured {@link MetricStream}s.
     */
    private MetricStreamDriver metricStreamDriver;

    /**
     * Creates a new instance. The new instance will be in an unconfigured and
     * stopped state.
     *
     * @param logger
     *            {@link Logger} to use.
     * @param executor
     *            Task execution service for performing work in separate
     *            threads.
     * @param eventBus
     *            The {@link EventBus} on which to send out collected
     *            {@link MetricValue}s.
     */
    @Inject
    public CloudWatchMetricStreamer(Logger logger, ScheduledExecutorService executor, EventBus eventBus) {
        this.logger = logger;
        this.executor = executor;
        this.eventBus = eventBus;
    }

    @Override
    public void validate(CloudWatchMetricStreamerConfig configuration) throws IllegalArgumentException {
        checkArgument(configuration != null, "metricStreamer: missing configuration");
        configuration.validate();
    }

    @Override
    public void configure(CloudWatchMetricStreamerConfig configuration) throws IllegalArgumentException {
        checkArgument(configuration != null, "missing configuration");
        if (configuration.equals(this.config)) {
            this.logger.debug("no configuration changes. ignoring new config ...");
            return;
        }

        boolean needsRestart = isStarted();
        if (needsRestart) {
            stop();
        }

        this.config = configuration;
        TimeInterval firstQueryLookback = null;
        this.metricStreamDriver = new MetricStreamDriver(this.logger, this.executor, this.eventBus, buildStreams(),
                configuration.getPollInterval(), firstQueryLookback);

        if (needsRestart) {
            start();
        }

    }

    private List<MetricStream> buildStreams() {
        List<MetricStream> metricStreams = new ArrayList<>();
        for (CloudWatchMetricStreamDefinition streamDefinition : this.config.getMetricStreams()) {
            MetricStreamConfig metricSteamConf = new MetricStreamConfig(this.config.getAccessKeyId(),
                    this.config.getSecretAccessKey(), this.config.getRegion(), streamDefinition);
            metricStreams.add(new CloudWatchMetricStream(this.logger, metricSteamConf));
        }
        return metricStreams;
    }

    @Override
    public CloudWatchMetricStreamerConfig getConfiguration() {
        return this.config;
    }

    @Override
    public void start() throws IllegalStateException {
        ensureConfigured();

        if (isStarted()) {
            return;
        }
        this.metricStreamDriver.start();
    }

    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }
        this.metricStreamDriver.stop();
    }

    @Override
    public ServiceStatus getStatus() {
        return new ServiceStatus.Builder().started(isStarted()).build();
    }

    @Override
    public Class<CloudWatchMetricStreamerConfig> getConfigurationClass() {
        return CloudWatchMetricStreamerConfig.class;
    }

    @Override
    public List<MetricStream> getMetricStreams() {
        ensureConfigured();
        return this.metricStreamDriver.getMetricStreams();
    }

    @Override
    public void fetch() throws MetricStreamException, IllegalStateException {
        ensureStarted();
        this.metricStreamDriver.fetch();
    }

    @Override
    public MetricStream getMetricStream(String metricStreamId) throws IllegalArgumentException {
        ensureConfigured();
        for (MetricStream stream : getMetricStreams()) {
            if (stream.getId().equals(metricStreamId)) {
                return stream;
            }
        }
        throw new IllegalArgumentException("unrecognized metric stream: " + metricStreamId);
    }

    private void ensureConfigured() {
        checkState(isConfigured(), "attempt to use metric streamer before being configured");
    }

    private void ensureStarted() {
        ensureConfigured();
        checkState(isStarted(), "attempt to use metric streamer before being started");
    }

    private boolean isStarted() {
        return this.metricStreamDriver != null && this.metricStreamDriver.isStarted();
    }

    private boolean isConfigured() {
        return this.config != null;
    }

}
