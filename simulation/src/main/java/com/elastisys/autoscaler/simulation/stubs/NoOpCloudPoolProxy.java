package com.elastisys.autoscaler.simulation.stubs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxyException;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.util.time.UtcTime;

public class NoOpCloudPoolProxy implements CloudPoolProxy<Object> {
    private static final Logger LOG = LoggerFactory.getLogger(NoOpCloudPoolProxy.class);
    private boolean started = false;
    private Object config;

    @Override
    public void start() throws IllegalStateException {
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
    public void validate(Object configuration) throws IllegalArgumentException {
    }

    @Override
    public void configure(Object configuration) throws IllegalArgumentException {
        this.config = configuration;
    }

    @Override
    public Object getConfiguration() {
        return this.config;
    }

    @Override
    public Class<Object> getConfigurationClass() {
        return Object.class;
    }

    @Override
    public MachinePool getMachinePool() throws CloudPoolProxyException {
        return MachinePool.emptyPool(UtcTime.now());
    }

    @Override
    public PoolSizeSummary getPoolSize() throws CloudPoolProxyException {
        return new PoolSizeSummary(0, 0, 0);
    }

    @Override
    public void setDesiredSize(int desiredSize) throws CloudPoolProxyException {
        LOG.debug("cloudpool proxy asked to set desiredSize to {}", desiredSize);
    }

}
