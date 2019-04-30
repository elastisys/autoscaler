package com.elastisys.autoscaler.simulation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.autoscaler.builder.AutoScalerBuilder;
import com.elastisys.autoscaler.core.metronome.impl.standard.StandardMetronome;
import com.elastisys.autoscaler.core.monitoring.api.MonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.impl.standard.StandardMonitoringSubsystem;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.prediction.impl.standard.StandardPredictionSubsystem;
import com.elastisys.autoscaler.metricstreamers.influxdb.InfluxdbMetricStreamer;
import com.elastisys.autoscaler.simulation.simulator.driver.DiscreteEventDriver;
import com.elastisys.autoscaler.simulation.simulator.driver.Event;
import com.elastisys.autoscaler.simulation.simulator.driver.SimulatorContext;
import com.elastisys.autoscaler.simulation.simulator.driver.StandardEventDriver;
import com.elastisys.autoscaler.simulation.simulator.events.MetricStreamerFetchEvent;
import com.elastisys.autoscaler.simulation.simulator.events.MetronomeEvent;
import com.elastisys.autoscaler.simulation.simulator.events.SystemHistorianFlushEvent;
import com.elastisys.autoscaler.simulation.stubs.NoOpAlerter;
import com.elastisys.autoscaler.simulation.stubs.NoOpCloudPoolProxy;
import com.elastisys.autoscaler.systemhistorians.influxdb.InfluxdbSystemHistorian;
import com.google.gson.JsonObject;

/**
 * Runs a simulation over a given time-interval on an {@link AutoScaler} set up
 * to read metrics from (and write system metrics to) an InfluxDB server, which
 * is assumed to have been prepared with a sample workload for the simulation
 * time-interval. The simulation is mainly intended to serve as a simple testbed
 * for trying out different predictor algorithms on different workloads. As
 * such, the simulated {@link AutoScaler} does not operate against a real
 * cloudpool, but uses a {@link NoOpCloudPoolProxy}, which only logs the desired
 * size it is told to set.
 * <p/>
 * The simulation is started when the {@link #call()} method is executed.
 */
public class Simulator implements Callable<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(Simulator.class);

    private static final String AUTOSCALER_ID = "simscaler";
    private static final String AUTOSCALER_UUID = "00000000-0000-0000-0000-000000000000";
    private static final File STORAGE_DIR = new File("autoscaler-data");

    /**
     * {@link AutoScaler} configuration. Must configure the monitoring subsystem
     * to create an {@link InfluxdbMetricStreamer} and an
     * {@link InfluxdbSystemHistorian} and configure these to operate against
     * the InfluxDB server that is assumed to have been set up and to stream the
     * metric that the InfluxDB server has been preloaded with.
     */
    private final JsonObject autoscalerConfig;

    /**
     * The start-time of the simulation. This should be set to the start-time in
     * the InfluxDB database of the relevant part of the workload that is to be
     * included in the simulation. The simulated {@link AutoScaler} will start
     * streaming values from this point in time.
     */
    private final DateTime simulationStart;
    /**
     * The end-time of the simulation. This should be set to the end-time in the
     * InfluxDB database of the relevant part of the workload that is to be
     * included in the simulation.
     */
    private final DateTime simulationEnd;

    /**
     * Creates a new {@link Simulator}.
     *
     * @param autoscalerConfig
     *            {@link AutoScaler} configuration. Must configure the
     *            monitoring subsystem to create an
     *            {@link InfluxdbMetricStreamer} and an
     *            {@link InfluxdbSystemHistorian} and configure these to operate
     *            against the InfluxDB server that is assumed to have been set
     *            up and to stream the metric that the InfluxDB server has been
     *            preloaded with.
     * @param simulationStart
     *            The start-time of the simulation. This should be set to the
     *            start-time in the InfluxDB database of the relevant part of
     *            the workload that is to be included in the simulation. The
     *            simulated {@link AutoScaler} will start streaming values from
     *            this point in time.
     * @param simulationEnd
     *            The end-time of the simulation. This should be set to the
     *            end-time in the InfluxDB database of the relevant part of the
     *            workload that is to be included in the simulation.
     */
    public Simulator(JsonObject autoscalerConfig, DateTime simulationStart, DateTime simulationEnd) {
        this.autoscalerConfig = autoscalerConfig;
        this.simulationStart = simulationStart;
        this.simulationEnd = simulationEnd;
    }

    @Override
    public Void call() throws Exception {
        LOG.info("creating autoscaler ...");
        AutoScaler autoscaler = createAutoScaler();
        LOG.info("configuring autoscaler ...");
        autoscaler.configure(this.autoscalerConfig);
        autoscaler.start();

        DiscreteEventDriver eventDriver = setUpEventDriver(autoscaler);

        eventDriver.run();

        return null;
    }

    /**
     * Creates and prepares a {@link DiscreteEventDriver} that will drive the
     * simulation.
     *
     * @param autoscaler
     *            The {@link AutoScaler} instance that the simulation will
     *            exercise.
     * @return
     */
    private DiscreteEventDriver setUpEventDriver(AutoScaler autoscaler) {
        DiscreteEventDriver eventDriver = createEventDriver(this.simulationStart, this.simulationEnd);
        eventDriver.initialize(this.simulationStart, Optional.of(this.simulationEnd));

        // schedule first metric streamer fetch event (will reschedule itself
        // after each execution)
        InfluxdbMetricStreamer metricStreamer = getInfluxdbMetricStreamer(autoscaler);
        MetricStreamerFetchEvent metricFetchEvent = new MetricStreamerFetchEvent(metricStreamer);
        eventDriver.addEvent(new Event(this.simulationStart, metricFetchEvent));

        // schedule first metronome resize iteration
        StandardMetronome metronome = StandardMetronome.class.cast(autoscaler.getMetronome());
        MetronomeEvent metronomeEvent = new MetronomeEvent(metronome);
        eventDriver.addEvent(new Event(this.simulationStart, metronomeEvent));

        // schedule first system historian report event
        InfluxdbSystemHistorian systemHistorian = InfluxdbSystemHistorian.class
                .cast(autoscaler.getMonitoringSubsystem().getSystemHistorian());
        SystemHistorianFlushEvent systemMetricFlushEvent = new SystemHistorianFlushEvent(systemHistorian);
        eventDriver.addEvent(new Event(this.simulationStart, systemMetricFlushEvent));
        return eventDriver;
    }

    /**
     * Returns the {@link InfluxdbMetricStreamer} by which scaling metrics are
     * to be read. It is assumed to be defined as the first
     * {@link MetricStreamer} defined for the {@link MonitoringSubsystem}.
     *
     * @param autoscaler
     * @return
     */
    private InfluxdbMetricStreamer getInfluxdbMetricStreamer(AutoScaler autoscaler) {
        List<MetricStreamer<?>> metricStreamers = autoscaler.getMonitoringSubsystem().getMetricStreamers();
        MetricStreamer<?> primaryMetricStreamer = metricStreamers.get(0);
        if (!InfluxdbMetricStreamer.class.isAssignableFrom(primaryMetricStreamer.getClass())) {
            throw new IllegalArgumentException(
                    "expected first metricStreamer to be of type " + InfluxdbMetricStreamer.class.getName());
        }
        return InfluxdbMetricStreamer.class.cast(metricStreamers.get(0));
    }

    /**
     * Creates an {@link AutoScaler} instance intended for simulation purposes.
     *
     * @return
     * @throws IOException
     * @throws Exception
     */
    private AutoScaler createAutoScaler() throws IOException {
        AutoScalerBuilder builder = AutoScalerBuilder.newBuilder();
        builder.withUuid(UUID.fromString(AUTOSCALER_UUID));
        builder.withId(AUTOSCALER_ID);
        builder.withMetronome(StandardMetronome.class);
        builder.withMonitoringSubsystem(StandardMonitoringSubsystem.class);
        builder.withPredictionSubsystem(StandardPredictionSubsystem.class);
        builder.withAlerter(NoOpAlerter.class);
        builder.withCloudPoolProxy(NoOpCloudPoolProxy.class);
        builder.withStorageDir(STORAGE_DIR);

        return builder.build();
    }

    /**
     * Creates a {@link DiscreteEventDriver} that will drive the simulation.
     *
     * @param simulationStart
     * @param simulationEnd
     * @return
     */
    private static DiscreteEventDriver createEventDriver(DateTime simulationStart, DateTime simulationEnd) {
        final DiscreteEventDriver eventDriver = new StandardEventDriver(false);
        eventDriver.initialize(simulationStart, Optional.of(simulationEnd));
        // associate event driver with simulation thread.
        SimulatorContext.set(new SimulatorContext(eventDriver));
        return eventDriver;
    }
}
