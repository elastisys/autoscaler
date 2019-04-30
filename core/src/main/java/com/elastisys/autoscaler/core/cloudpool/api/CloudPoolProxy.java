package com.elastisys.autoscaler.core.cloudpool.api;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;

/**
 * A {@link CloudPoolProxy} is a local proxy that the {@link AutoScaler} uses to
 * send commands to a remote {@link CloudPool} endpoint, over the
 * <a href="http://cloudpoolrestapi.readthedocs.org/en/latest/">cloud pool REST
 * API</a>).
 * <p/>
 * Note: the {@link CloudPoolProxy} interface is a stripped down version of the
 * full {@link CloudPool} interface, containing only the subset of functionality
 * needed by the {@link AutoScaler}.
 *
 * @param <T>
 *            The type of configuration objects that this {@link CloudPoolProxy}
 *            accepts.
 */
public interface CloudPoolProxy<T> extends Service<T> {

    /**
     * Returns a list of the members of the cloud pool.
     * <p/>
     * Note, that the response may include machines in any {@link MachineState},
     * even machines that are in the process of terminating.
     * <p/>
     * The {@link MembershipStatus} of a machine in an allocated/started state
     * determines if it is to be considered an active member of the pool.The
     * <i>active size</i> of the machine pool should be interpreted as the
     * number of allocated machines (in any of the non-terminal machine states
     * {@code REQUESTED}, {@code PENDING} or {@code RUNNING} that have not been
     * marked with an inactive {@link MembershipStatus}. See
     * {@link Machine#isActiveMember()}.
     *
     * @return A list of cloud pool members.
     *
     * @throws CloudPoolProxyException
     *             If interactions with the remote cloud pool failed.
     */
    MachinePool getMachinePool() throws CloudPoolProxyException;

    /**
     * Returns the current size of the {@link MachinePool} -- both in terms of
     * the desired size and the actual size (as these may differ at any time).
     *
     * @return The current {@link PoolSizeSummary}.
     * @throws CloudPoolException
     *             If the operation could not be completed.
     */
    PoolSizeSummary getPoolSize() throws CloudPoolProxyException;

    /**
     * Sets the desired size of the cloud pool.
     *
     * @param desiredSize
     *            The desired number of machines in the pool.
     * @throws CloudPoolProxyException
     *             If interactions with the remote cloud pool failed.
     */
    void setDesiredSize(int desiredSize) throws CloudPoolProxyException;

}
