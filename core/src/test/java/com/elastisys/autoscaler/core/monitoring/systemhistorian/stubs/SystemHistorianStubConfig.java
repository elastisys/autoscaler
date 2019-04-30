package com.elastisys.autoscaler.core.monitoring.systemhistorian.stubs;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

/**
 * A dummy configuration for a {@link SystemHistorianStub}.
 *
 * @see SystemHistorianStub
 */
public class SystemHistorianStubConfig {
    private final String host;
    private final int port;

    public SystemHistorianStubConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void validate() throws IllegalArgumentException {
        try {
            checkArgument(this.host != null, "missing host");
            checkArgument(this.port > 0, "port number must be positive");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("systemHistorian: " + e.getMessage(), e);
        }
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

}
