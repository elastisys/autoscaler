package com.elastisys.autoscaler.core.autoscaler;

import com.elastisys.scale.commons.eventbus.EventBus;

/**
 * A collection of events that, when sent over the {@link AutoScaler}'s
 * {@link EventBus}, trigger different {@link AutoScaler} actions.
 */
public enum AutoScalerEvent {
    /**
     * When this event is sent over the {@link EventBus}, the {@link AutoScaler}
     * will stop. It is semantically equivalent to calling
     * {@link AutoScaler#stop()}.
     */
    STOP;
}
