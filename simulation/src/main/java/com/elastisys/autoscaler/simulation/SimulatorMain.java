package com.elastisys.autoscaler.simulation;

import com.elastisys.scale.commons.cli.CommandLineParser;

public class SimulatorMain {

    public static void main(String[] argv) throws Exception {
        CommandLineParser<SimulatorArgs> argParser = new CommandLineParser<>(SimulatorArgs.class);
        SimulatorArgs args = argParser.parseCommandLine(argv);

        Simulator simulator = new Simulator(args.getAutoscalerConfig(), args.getStartTime(), args.getEndTime());
        simulator.call();
    }
}
