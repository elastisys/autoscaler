package com.elastisys.autoscaler.simulation.simulator.events;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.metricstreamers.influxdb.InfluxdbMetricStreamer;
import com.elastisys.autoscaler.simulation.simulator.driver.Event;
import com.elastisys.autoscaler.simulation.simulator.driver.EventAction;
import com.elastisys.autoscaler.simulation.simulator.driver.EventContext;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * A simulator event that, when executed, causes the {@link MetricStreamer} to
 * fetch metrics for its {@link MetricStream}s, post
 * {@link MetricStreamMessage}s on the {@link EventBus}, and trigger a new
 * resize iteration.
 * <p/>
 * This is a repeating task that re-schedules itself after every invocation (as
 * long as there are events left in the simulation's event queue).
 */
public class MetricStreamerFetchEvent implements EventAction {
    private static Logger LOG = LoggerFactory.getLogger(MetricStreamerFetchEvent.class);

    private final InfluxdbMetricStreamer metricStreamer;

    public MetricStreamerFetchEvent(InfluxdbMetricStreamer metricStreamer) {
        this.metricStreamer = metricStreamer;
    }

    @Override
    public void execute(EventContext context, Object... parameters) throws Exception {
        LOG.debug("metric fetch at simulation time {}", context.getEventTime());
        this.metricStreamer.fetch();

        scheduleNextFetchEvent(context);
    }

    private void scheduleNextFetchEvent(EventContext context) {
        TimeInterval interval = this.metricStreamer.getConfiguration().getPollInterval();
        DateTime nextEventTime = context.getEventTime().plusSeconds((int) interval.getSeconds());
        context.getEventDriver().addEvent(new Event(nextEventTime, this));
    }
}