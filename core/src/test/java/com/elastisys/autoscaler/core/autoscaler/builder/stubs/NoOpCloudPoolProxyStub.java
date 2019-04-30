package com.elastisys.autoscaler.core.autoscaler.builder.stubs;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxyException;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;

public class NoOpCloudPoolProxyStub implements CloudPoolProxy<Object> {

    @Override
    public void start() throws IllegalStateException {
    }

    @Override
    public void stop() {
    }

    @Override
    public ServiceStatus getStatus() {
        return null;
    }

    @Override
    public void validate(Object configuration) throws IllegalArgumentException {
    }

    @Override
    public void configure(Object configuration) throws IllegalArgumentException {
    }

    @Override
    public Object getConfiguration() {
        return null;
    }

    @Override
    public Class<Object> getConfigurationClass() {
        return null;
    }

    @Override
    public MachinePool getMachinePool() throws CloudPoolProxyException {
        return null;
    }

    @Override
    public PoolSizeSummary getPoolSize() throws CloudPoolProxyException {
        return null;
    }

    @Override
    public void setDesiredSize(int desiredSize) throws CloudPoolProxyException {
    }

}
