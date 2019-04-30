package com.elastisys.autoscaler.core.monitoring.streammonitor;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.inject.Inject;

import org.joda.time.Duration;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.impl.standard.StandardMonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.impl.standard.config.MetricStreamMonitorConfig;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;

/**
 * A component of the {@link StandardMonitoringSubsystem} that monitors the
 * activity of all {@link MetricStream}s published by the {@link MetricStreamer}
 * and alerts on suspiciously low activity.
 * <p/>
 * Whenever too long periods of metric stream inactivity is detected, an
 * {@link Alert} is sent on the {@link AutoScaler} 's {@link EventBus}.
 */
public class MetricStreamMonitor implements Service<MetricStreamMonitorConfig> {

    private final Logger logger;
    private final ScheduledExecutorService executor;

    /** The currently set configuration. */
    private MetricStreamMonitorConfig config;
    /** Periodical task that checks metric stream activity. */
    private ScheduledFuture<?> activityCheckerTask;

    /** The tracker of stream activity. */
    private final StreamActivityChecker streamActivityChecker;

    @Inject
    public MetricStreamMonitor(Logger logger, EventBus eventBus, ScheduledExecutorService executor,
            MonitoringSubsystem<?> monitoringSubsystem) {
        this.logger = logger;
        this.executor = executor;

        this.streamActivityChecker = new StreamActivityChecker(logger, monitoringSubsystem, eventBus);

        this.config = null;
    }

    @Override
    public void validate(MetricStreamMonitorConfig configuration) throws IllegalArgumentException {
        checkArgument(configuration != null, "metricStreamMonitor: null configuration received");
        configuration.validate();
    }

    @Override
    public synchronized void configure(MetricStreamMonitorConfig configuration) throws IllegalArgumentException {
        validate(configuration);

        boolean needsRestart = isStarted();
        if (needsRestart) {
            stop();
        }

        this.config = configuration;

        if (needsRestart) {
            start();
        }
    }

    @Override
    public synchronized void start() throws IllegalStateException {
        checkState(isConfigured(), "attempt to start before configuring");

        if (isStarted()) {
            return;
        }

        Duration maxTolerableInactivity = Duration.millis(config().getMaxTolerableInactivity().getMillis());
        TimeInterval checkInterval = config().getCheckInterval();

        this.streamActivityChecker.start();
        Runnable activityCheckTask = () -> this.streamActivityChecker.alertOnActivityChange(maxTolerableInactivity);

        this.activityCheckerTask = this.executor.scheduleAtFixedRate(activityCheckTask, checkInterval.getTime(),
                checkInterval.getTime(), checkInterval.getUnit());

        this.logger.info(getClass().getSimpleName() + " started");
    }

    @Override
    public synchronized void stop() {
        if (!isStarted()) {
            return;
        }

        this.streamActivityChecker.stop();
        if (this.activityCheckerTask != null) {
            this.activityCheckerTask.cancel(true);
            this.activityCheckerTask = null;

        }
        this.logger.info(getClass().getSimpleName() + " stopped");
    }

    @Override
    public ServiceStatus getStatus() {
        return new ServiceStatus.Builder().started(isStarted()).build();
    }

    public boolean isStarted() {
        return this.activityCheckerTask != null;
    }

    public boolean isConfigured() {
        return config() != null;
    }

    public MetricStreamMonitorConfig config() {
        return this.config;
    }

    @Override
    public MetricStreamMonitorConfig getConfiguration() {
        return this.config;
    }

    @Override
    public Class<MetricStreamMonitorConfig> getConfigurationClass() {
        return MetricStreamMonitorConfig.class;
    }

}
