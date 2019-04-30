package com.elastisys.autoscaler.server.restapi.stubs;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;

import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxyException;
import com.elastisys.autoscaler.server.restapi.stubs.CloudPoolProxyStub.CloudPoolProxyConfig;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Stub implementation of the {@link CloudPoolProxy} interface.
 * <p/>
 * Intended for use in tests.
 */
public class CloudPoolProxyStub implements CloudPoolProxy<CloudPoolProxyConfig> {

    private final Logger logger;

    private CloudPoolProxyConfig config;
    private boolean started;

    @Inject
    public CloudPoolProxyStub(Logger logger) {
        this.logger = logger;

        this.config = null;
        this.started = false;
    }

    @Override
    public void validate(CloudPoolProxyConfig configuration) throws IllegalArgumentException {
        checkArgument(configuration != null, "cloud pool configuration missing");
        configuration.validate();
    }

    @Override
    public void configure(CloudPoolProxyConfig configuration) throws IllegalArgumentException {
        this.config = configuration;
    }

    @Override
    public CloudPoolProxyConfig getConfiguration() {
        return this.config;
    }

    @Override
    public Class<CloudPoolProxyConfig> getConfigurationClass() {
        return CloudPoolProxyConfig.class;
    }

    @Override
    public void start() {
        checkState(this.config != null, "attempt to start before being configured");
        this.started = true;
    }

    @Override
    public void stop() {
        this.started = false;
    }

    @Override
    public ServiceStatus getStatus() {
        return new ServiceStatus.Builder().started(this.started).build();
    }

    @Override
    public MachinePool getMachinePool() {
        MachinePool emptyPool = MachinePool.emptyPool(UtcTime.now());
        this.logger.debug("Returning machine pool: {}", emptyPool);
        return emptyPool;
    }

    @Override
    public void setDesiredSize(int computeUnitNeed) {
        this.logger.debug("Ignoring request to resize machine pool to {}", computeUnitNeed);
    }

    @Override
    public PoolSizeSummary getPoolSize() throws CloudPoolProxyException {
        PoolSizeSummary poolSizeSummary = new PoolSizeSummary(0, 0, 0);
        this.logger.debug("Returning empty pool size summary: {}", poolSizeSummary);
        return poolSizeSummary;
    }

    /**
     * Configuration class for the {@link CloudPoolProxyStub}.
     * <p/>
     * Intended for use in tests.
     */
    public static class CloudPoolProxyConfig {
        private final String cloudPoolHost;
        private final Integer cloudPoolPort;

        public CloudPoolProxyConfig(String cloudPoolHost, int cloudPoolPort) {
            this.cloudPoolHost = cloudPoolHost;
            this.cloudPoolPort = cloudPoolPort;
        }

        public void validate() throws IllegalArgumentException {
            checkArgument(this.cloudPoolHost != null, "cloudPool: missing cloudPoolHost");
            checkArgument(this.cloudPoolPort != null, "cloudPool: missing cloudPoolPort");
        }

        public String getCloudPoolHost() {
            return this.cloudPoolHost;
        }

        public int getCloudPoolPort() {
            return this.cloudPoolPort;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.cloudPoolHost, this.cloudPoolPort);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CloudPoolProxyConfig) {
                CloudPoolProxyConfig that = (CloudPoolProxyConfig) obj;
                return Objects.equals(this.cloudPoolHost, that.cloudPoolHost)
                        && Objects.equals(this.cloudPoolPort, that.cloudPoolPort);
            }
            return false;
        }
    }

}
