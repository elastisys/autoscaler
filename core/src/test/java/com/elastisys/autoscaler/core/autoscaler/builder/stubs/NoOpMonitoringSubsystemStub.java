package com.elastisys.autoscaler.core.autoscaler.builder.stubs;

import java.util.Collections;
import java.util.List;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;

public class NoOpMonitoringSubsystemStub implements MonitoringSubsystem<Object> {

    @Override
    public void start() throws IllegalStateException {

    }

    @Override
    public void stop() {

    }

    @Override
    public ServiceStatus getStatus() {
        return null;
    }

    @Override
    public void validate(Object configuration) throws IllegalArgumentException {

    }

    @Override
    public void configure(Object configuration) throws IllegalArgumentException {

    }

    @Override
    public Object getConfiguration() {
        return null;
    }

    @Override
    public Class<Object> getConfigurationClass() {
        return null;
    }

    @Override
    public List<MetricStreamer<?>> getMetricStreamers() {
        return Collections.emptyList();
    }

    @Override
    public SystemHistorian<?> getSystemHistorian() {
        return null;
    }

}
