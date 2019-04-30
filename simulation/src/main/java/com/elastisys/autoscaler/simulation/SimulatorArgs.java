package com.elastisys.autoscaler.simulation;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import com.elastisys.scale.commons.cli.CommandLineOptions;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonObject;

/**
 * Command-line arguments for the {@link Simulator}.
 */
public class SimulatorArgs implements CommandLineOptions {

    private static final String VERSION = "1.0.0";

    @Option(name = "--autoscaler-config", metaVar = "PATH", usage = "autoscaler configuration. The must configure the monitoring subsystem to create an InfluxdbMetricStreamer and an InfluxdbSystemHistorian and configure these to operate against the InfluxDB server that is assumed to have been set up and to stream the metric that the InfluxDB server has been preloaded with.")
    public String autoscalerConfig = null;

    @Option(name = "--start-time", metaVar = "DATETIME", usage = "The start-time of the simulation. This should be set to the start-time in the InfluxDB database of the relevant part of the workload that is to be included in the simulation. The simulated {@link AutoScaler} will start streaming values from this point in time.")
    public String startTime = null;

    @Option(name = "--end-time", metaVar = "DATETIME", usage = "The end-time of the simulation. This should be set to the end-time in the InfluxDB database of the relevant part of the workload that is to be included in the simulation.")
    public String endTime = null;

    @Option(name = "--help", usage = "Display help text.")
    public boolean help = false;

    @Option(name = "--version", usage = "Displays the version.")
    public boolean version = false; // default

    /** Positional command-line arguments end up here. */
    @Argument
    public List<String> positionalArgs = new ArrayList<String>();

    @Override
    public boolean isHelpFlagSet() {
        return this.help;
    }

    @Override
    public boolean isVersionFlagSet() {
        return this.version;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public void validate() throws IllegalArgumentException {
        checkArgument(this.autoscalerConfig != null, "no autoscaler config set");
        checkArgument(this.startTime != null, "no start-time set");
        checkArgument(this.endTime != null, "no end-time set");

        if (!new File(this.autoscalerConfig).isFile()) {
            throw new IllegalArgumentException("autoscaler config does not exist: " + this.autoscalerConfig);
        }

        try {
            DateTime.parse(this.startTime);
        } catch (Exception e) {
            throw new IllegalArgumentException("illegal start-time given: " + e.getMessage());
        }

        try {
            DateTime.parse(this.endTime);
        } catch (Exception e) {
            throw new IllegalArgumentException("illegal end-time given: " + e.getMessage());
        }
    }

    public DateTime getStartTime() {
        return DateTime.parse(this.startTime);
    }

    public DateTime getEndTime() {
        return DateTime.parse(this.endTime);
    }

    public JsonObject getAutoscalerConfig() {
        return JsonUtils.parseJsonFile(new File(this.autoscalerConfig)).getAsJsonObject();
    }

}
