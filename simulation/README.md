# Autoscaler simulations

The simulator is mainly intended to exercise scaling algorithms/predictors
against different workloads.

The simulator runs a simulation over a given time-interval on an autoscaler
instance set up to read metrics from (and write system metrics to) an InfluxDB
server, which is assumed to have been prepared with a sample workload for the
simulation time-interval. Since the simulation is mainly intended to serve as a
simple testbed for trying out different predictor algorithms on different
workloads, the simulated autoscaler does not operate against a real cloudpool,
but uses a `NoOpCloudPoolProxy`, which only logs the desired size it is told to
set.

*NOTE: The simulation jar file only includes the core autoscaler. As such, any
predictors that you want to try out that aren't in the core autoscaler module
need to be explicitly added to the classpath.*

Running a simulation involves the following steps:

1. Start an InfluxDB database server (preferably with a Grafana server running
   alongside to visualize results), for instance using
   the [influxdb scripts](../scripts/influxdb). Refer to
   the [README.md](../scripts/influxdb/README.md) for details.
2. Populate the InfluxDB database with metrics that you wish to run the
   autoscaler against. For this purpose, the `push-metrics.py` script from the
   [influxdb scripts](../scripts/influxdb) directory may be used. Refer to
   the [README.md](../scripts/influxdb/README.md) for details. Make note of the
   time-interval of interest that you want the simulation to capture.
3. Create an autoscaler config which:

   - sets up an `InfluxdbMetricstreamer` to read the metric of interest from
     InfluxDB
   - sets up an `InfluxdbSystemHistorian` that pushes system metrics to the
     InfluxDB server (this way, you can see predictor output in Grafana after
     the simulation finishes)
   - sets up the prediction algorithm(s) you want to exercise

   Sample configuration files can be found under [configs](configs).

4. Run the simulation

        java -cp target/autoscaler.simulation.jar:<your-predictor-jar> \
        com.elastisys.autoscaler.simulation.SimulatorMain \
        --autoscaler-config=<PATH> \
        --start-time=2017-01-01T12:00:00.000Z \
        --end-time=2017-01-01T22:00:00.000Z

5. Check the results in Grafana. Note, in some cases, one needs to specify the
   unit of the prediction to not get both `METRIC` and `COMPUTE` unit
   predictions in the same graph. For example:

        SELECT distinct("value") FROM "autoscaler.prediction" WHERE ("unit" = 'METRIC') AND $timeFilter GROUP BY time(1s) fill(none)

6. To view a detailed log of the actions taken by the autoscaler, look in the
   `simulation-trace.log` file.



## Re-run simulation
If you would like to re-run a simulation without clearing and re-populating the
InfluxDB database, you can remove only the system metrics produced by the
simulated autoscaler by running:

    curl -i -X POST http://localhost:8086/query?db=metricsdb --data-urlencode "q=DROP SERIES WHERE autoScalerId = 'simscaler'"
