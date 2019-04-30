package com.elastisys.autoscaler.core.alerter.api;

import javax.inject.Inject;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.scale.commons.eventbus.AllowConcurrentEvents;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;
import com.elastisys.scale.commons.net.alerter.Alert;

/**
 * An {@link Alerter} is responsible for notifying the outside world of events
 * in an {@link AutoScaler} instance. Different {@link Alerter} implementations
 * may support different protocols (for example, SMTP or HTTP) to send system
 * {@link Alert}s to system administrators or other external monitoring systems.
 * <p/>
 * The {@link Alerter} waits for the {@link AutoScaler} to emit {@link Alert}s
 * on the {@link EventBus}, and when it does, the {@link Alerter} takes care of
 * sending those messages to all configured recipients.
 * <p/>
 * An {@link Alerter} implementation should, itself, take care of registering
 * with the {@link AutoScaler}'s {@link EventBus} in order to receive
 * {@link Alert}s. To this end, {@link Alerter} implementations should have the
 * {@link EventBus} {@link Inject}ed on construction (by Google Guice).
 * <p/>
 * Well-behaved {@link Alerter} implementations should take care to include meta
 * data tags (such as {@link AutoScaler} id, host) about the {@link AutoScaler}
 * that produced the {@link Alert}. Such meta data is useful to distinguish
 * {@link AutoScaler} messages from each other in scenarios where several
 * {@link AutoScaler} instances are employed (for example, to achieve high
 * availability or to monitor several application layers).
 *
 * @param <T>
 *            The configuration type of the implementation class.
 */
public interface Alerter<T> extends Service<T> {

    /**
     * Invoked by the {@link AutoScaler}'s {@link EventBus} whenever an
     * {@link Alert} has been posted on the bus.
     * <p/>
     * Note that {@link Alerter} implementations are responsible themselves for
     * registering with the {@link EventBus} at construction/start-time.
     * <p/>
     * Implementations should take necessary measures to ensure thread-safety,
     * as this method explicitly allows the {@link EventBus} to call it from
     * multiple threads concurrently (see the {@link AllowConcurrentEvents}
     * annotation).
     *
     * @param alert
     *            The {@link Alert} to submit.
     */
    @Subscriber
    @AllowConcurrentEvents
    public void onAlert(Alert alert);
}
