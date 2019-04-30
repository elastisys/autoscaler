package com.elastisys.autoscaler.core.metronome.api;

import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.scale.commons.eventbus.EventBus;

/**
 * Events that, when received over the {@link AutoScaler} {@link EventBus},
 * trigger different {@link Metronome} actions.
 */
public enum MetronomeEvent {
    /**
     * When this event is received over the {@link EventBus}, the
     * {@link Metronome} should start a new resize iteration.
     */
    RESIZE_ITERATION;
}