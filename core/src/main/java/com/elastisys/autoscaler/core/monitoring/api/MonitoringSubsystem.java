package com.elastisys.autoscaler.core.monitoring.api;

import java.util.List;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;

/**
 * A {@link MonitoringSubsystem} is an {@link AutoScaler} subsystem responsible
 * for (1) collecting metric data from the outside world to feed into the
 * {@link PredictionSubsystem} and (2) for reporting internal service metrics
 * for the {@link AutoScaler} itself.
 * <p/>
 * (1) is accomplished by a set of {@link MetricStreamer}s and (2) is carried
 * out by a {@link SystemHistorian} that the {@link MonitoringSubsystem} has
 * been configured to use.
 *
 * @param <T>
 *            The configuration type of the implementation class.
 */
public interface MonitoringSubsystem<T> extends Service<T> {

    /**
     * Returns the {@link MetricStreamer}s that the {@link MonitoringSubsystem}
     * has been configured to use, or an empty list if no configuration has been
     * set yet.
     *
     * @return
     */
    List<MetricStreamer<?>> getMetricStreamers();

    /**
     * Returns the {@link SystemHistorian} that the {@link MonitoringSubsystem}
     * has been configured to use, or <code>null</code> if no configuration has
     * been set yet.
     *
     * @return
     */
    SystemHistorian<?> getSystemHistorian();
}
