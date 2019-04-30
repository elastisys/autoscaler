package com.elastisys.autoscaler.core.monitoring.systemhistorian.api;

import javax.inject.Inject;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;

/**
 * A {@link SystemHistorian} is a part of the {@link MonitoringSubsystem} that
 * captures and stores time-based data from an {@link AutoScaler} instance.
 * <p/>
 * It is used to capture monitoring and performance data from the instance,
 * which can be used to track system operation over time, detect operational
 * trends or just keep a historical account of system actions for future
 * reference.
 * <p/>
 * The {@link SystemHistorian} waits for the {@link AutoScaler} to emit
 * {@link SystemMetricEvent}s on the {@link EventBus}, and when it does, the
 * {@link SystemHistorian} stores those events to its backing store (which, for
 * example, could be a time-series database).
 * <p/>
 * A {@link SystemHistorian} implementation should, itself, take care of
 * registering with the {@link AutoScaler}'s {@link EventBus} in order to
 * receive {@link SystemMetricEvent}s. To this end, {@link SystemHistorian}
 * implementations should have the {@link EventBus} {@link Inject}ed on
 * construction (by Google Guice).
 * <p/>
 * Implementations should take care to ensure that values reported by different
 * {@link AutoScaler} instances can be distinguished, for example, by tagging
 * each reported value with the identifier of the {@link AutoScaler} instance
 * that produced the value.
 *
 * @see MonitoringSubsystem
 * @see SystemMetricEvent
 * @see TimeSeriesDataPoint
 *
 * @param <T>
 *            The type used to configure the {@link SystemHistorian}.
 */
public interface SystemHistorian<T> extends Service<T> {
    /**
     * Invoked whenever the {@link AutoScaler} system produces a new
     * {@link SystemMetricEvent}. The {@link SystemHistorian} should take
     * measure to record the event in its backing store. To decrease traffic to
     * its storage backend, the {@link SystemHistorian} <i>may</i> choose to
     * buffer {@link SystemMetricEvent}s and periodically write all buffered
     * events to its storage backend.
     *
     * @param event
     *            The event that has occurred.
     */
    @Subscriber
    public void onEvent(SystemMetricEvent event);

    /**
     * Forces the {@link SystemHistorian} to write any buffered
     * {@link SystemMetricEvent}s to its backing store.
     *
     * @throws IllegalStateException
     *             If called before being started.
     * @throws SystemHistorianFlushException
     *             On failure to write to the backing store.
     */
    public void flush() throws IllegalStateException, SystemHistorianFlushException;
}
