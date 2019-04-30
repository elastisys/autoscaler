package com.elastisys.autoscaler.simulation.simulator.events;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.metronome.impl.standard.StandardMetronome;
import com.elastisys.autoscaler.simulation.simulator.driver.Event;
import com.elastisys.autoscaler.simulation.simulator.driver.EventAction;
import com.elastisys.autoscaler.simulation.simulator.driver.EventContext;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * A simulator event that, when executed, executes a resize iteration (a new
 * round of predictions and a cloudpool resize).
 * <p/>
 * This is a repeating task that re-schedules itself after every invocation (as
 * long as there are {@link RequestEvent}s left in the simulation's event
 * queue).
 */
public class MetronomeEvent implements EventAction {
    private static Logger LOG = LoggerFactory.getLogger(MetronomeEvent.class);

    private final StandardMetronome metronome;

    public MetronomeEvent(StandardMetronome metronome) {
        this.metronome = metronome;
    }

    @Override
    public void execute(EventContext context, Object... parameters) throws Exception {
        LOG.debug("metronome running resize iteration at simulation time {}", context.getEventTime());
        this.metronome.doResizeIteration();

        scheduleNextFetchEvent(context);
    }

    private void scheduleNextFetchEvent(EventContext context) {
        TimeInterval interval = this.metronome.getConfiguration().getInterval();
        DateTime nextEventTime = context.getEventTime().plusSeconds((int) interval.getSeconds());
        context.getEventDriver().addEvent(new Event(nextEventTime, this));
    }
}