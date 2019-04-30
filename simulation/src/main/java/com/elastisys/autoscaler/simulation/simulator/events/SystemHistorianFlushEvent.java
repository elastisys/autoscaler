package com.elastisys.autoscaler.simulation.simulator.events;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.autoscaler.simulation.simulator.driver.Event;
import com.elastisys.autoscaler.simulation.simulator.driver.EventAction;
import com.elastisys.autoscaler.simulation.simulator.driver.EventContext;
import com.elastisys.autoscaler.systemhistorians.influxdb.InfluxdbSystemHistorian;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * A simulator event that, when executed, causes the {@link SystemHistorian} to
 * flush any buffered {@link SystemMetricEvent}s to its backing store.
 * <p/>
 * This is a repeating task that re-schedules itself after every invocation (as
 * long as there are {@link RequestEvent}s left in the simulation's event
 * queue).
 */
public class SystemHistorianFlushEvent implements EventAction {
    private static Logger LOG = LoggerFactory.getLogger(SystemHistorianFlushEvent.class);

    private final InfluxdbSystemHistorian systemHistorian;

    public SystemHistorianFlushEvent(InfluxdbSystemHistorian systemHistorian) {
        this.systemHistorian = systemHistorian;
    }

    @Override
    public void execute(EventContext context, Object... parameters) throws Exception {
        LOG.debug("system historian flush at simulation time {}", context.getEventTime());
        this.systemHistorian.flush();

        scheduleNextFetchEvent(context);
    }

    private void scheduleNextFetchEvent(EventContext context) {
        TimeInterval interval = this.systemHistorian.getConfiguration().getReportingInterval();
        DateTime nextEventTime = context.getEventTime().plusSeconds((int) interval.getSeconds());
        context.getEventDriver().addEvent(new Event(nextEventTime, this));
    }
}