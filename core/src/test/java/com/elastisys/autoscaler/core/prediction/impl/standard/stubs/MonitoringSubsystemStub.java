package com.elastisys.autoscaler.core.prediction.impl.standard.stubs;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.List;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;

/**
 * Stubbed {@link MonitoringSubsystem} that is set up with a
 * {@link MetricStreamer} and (optionally) a {@link SystemHistorian} (may be
 * left out). Intended for testing of the {@link PredictionSubsystem} only.
 */
@SuppressWarnings("rawtypes")
public class MonitoringSubsystemStub implements MonitoringSubsystem<Object> {

    private final List<MetricStreamer<?>> metricStreamers;
    private final SystemHistorian systemHistorian;

    private Object config;
    private boolean started;

    /**
     * Creates a {@link MonitoringSubsystemStub}.
     *
     * @param metricStreamers
     *            {@link MetricStreamer}s to use.
     * @param systemHistorian
     *            {@link SystemHistorian} to use. May be <code>null</code>.
     */
    public MonitoringSubsystemStub(List<MetricStreamer<?>> metricStreamers, SystemHistorian systemHistorian) {
        checkArgument(metricStreamers != null, "metricStreamers cannot be null");
        checkArgument(!metricStreamers.isEmpty(), "at least one metricStreamer must be specified");
        this.metricStreamers = metricStreamers;
        this.systemHistorian = systemHistorian;
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
        this.config = configuration;
    }

    @Override
    public Object getConfiguration() {
        return this.config;
    }

    @Override
    public Class<Object> getConfigurationClass() {
        return Object.class;
    }

    @Override
    public List<MetricStreamer<?>> getMetricStreamers() {
        return this.metricStreamers;
    }

    @Override
    public SystemHistorian<?> getSystemHistorian() {
        return this.systemHistorian;
    }

}
