package com.elastisys.autoscaler.server;

import java.io.IOException;

/**
 * Main class for starting the REST API server for the
 * {@link AutoScalerFactoryServer}.
 */
public class Main {

    public static void main(String[] args) throws IllegalArgumentException, IOException, Exception {
        AutoScalerFactoryServer.main(args);
    }
}
