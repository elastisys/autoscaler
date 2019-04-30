package com.elastisys.autoscaler.core.metronome.api;

import javax.inject.Inject;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;
import com.elastisys.scale.commons.eventbus.EventBus;

/**
 * A {@link Metronome} drives the execution of the {@link AutoScaler}. It is
 * responsible for periodically carrying out resize iterations during which a
 * new round of pool size predictions is carried out and the machine pool is
 * resized to the expected load.
 * <p/>
 * Besides periodical execution of resize iterations, it should be possible to
 * trigger a resize iteration by posting a
 * {@link MetronomeEvent#RESIZE_ITERATION} over the {@link AutoScaler}'s
 * {@link EventBus}. It can be used by other components to trigger a new resize
 * iteration. For example, by a {@link MetricStreamer} after delivering a new
 * batch of {@link MetricValue}s. This reduces delays and allows predictors to
 * quickly start working on new metric values.
 * <p/>
 * An implementation should set itself up for periodical execution. Furthermore,
 * implementations are likely to require access to the {@link EventBus},
 * {@link PredictionSubsystem} and {@link CloudPoolProxy} objects. References to
 * these objects can be acquired via an {@link Inject}-annotated constructor.
 *
 * @param <T>
 *            The type of the configuration object for this {@link Metronome}.
 */
public interface Metronome<T> extends Service<T> {

}
