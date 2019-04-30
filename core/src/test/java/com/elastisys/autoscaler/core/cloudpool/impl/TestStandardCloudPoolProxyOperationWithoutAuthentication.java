package com.elastisys.autoscaler.core.cloudpool.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jetty.server.Server;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.cloudpool.impl.stubs.SuccessfulCloudPoolServlet;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.server.ServletDefinition;
import com.elastisys.scale.commons.server.ServletServerBuilder;
import com.elastisys.scale.commons.server.SslKeyStoreType;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Verifies the correctness of the {@link StandardCloudPoolProxy}, to ensure
 * that it makes proper HTTPS call-outs when instructed to use Basic
 * authentication.
 */
public class TestStandardCloudPoolProxyOperationWithoutAuthentication {
    static Logger logger = LoggerFactory.getLogger(TestStandardCloudPoolProxyOperationWithoutAuthentication.class);

    private static final String SERVER_SECURITY = "src/test/resources/cloudpool/server-security";
    private EventBus eventBus = new SynchronousEventBus(logger);

    /** The http/https port where the remote cloud pool endpoint is set up. */
    private int port;

    private Server server;

    @Before
    public void onSetup() throws Exception {
        // find a free port for test server
        List<Integer> freePorts = HostUtils.findFreePorts(2);
        this.port = freePorts.get(0);

        // server instances are created by each individual test method
        this.server = null;
    }

    /**
     * Tears down the {@link Server} instance (if any) created by the test.
     */
    @After
    public void onTeardown() throws Exception {
        if (this.server != null) {
            this.server.stop();
            this.server.join();
        }
    }

    /**
     * The {@link StandardCloudPoolProxy} should be capable of communicating
     * with a remote cloudpool over HTTP.
     */
    @Test
    public void getPoolOverHttp() throws Exception {
        MachinePool pool = actualPool();
        this.server = createNoAuthHttpServer(pool);
        this.server.start();

        StandardCloudPoolProxy cloudPoolProxy = httpCloudPoolProxy();
        MachinePool retrievedPool = cloudPoolProxy.getMachinePool();
        assertThat(retrievedPool, is(pool));
    }

    /**
     * The {@link StandardCloudPoolProxy} should be capable of communicating
     * with a remote cloudpool over HTTPS.
     */
    @Test
    public void getPoolOverHttps() throws Exception {
        MachinePool pool = actualPool();
        this.server = createNoAuthHttpsServer(pool);
        this.server.start();

        StandardCloudPoolProxy cloudPoolProxy = httpsCloudPoolProxy();
        MachinePool retrievedPool = cloudPoolProxy.getMachinePool();
        assertThat(retrievedPool, is(pool));
    }

    /**
     * Creates a dummy {@link MachinePool} for the (test-simulated) remote cloud
     * pool.
     */
    private static MachinePool actualPool() {
        DateTime now = UtcTime.now();
        return new MachinePool(Arrays.asList(Machine.builder().id("i-123456").machineState(MachineState.RUNNING)
                .cloudProvider("EC2").region("us-east-1").machineSize("m1.small").launchTime(now)
                .publicIps(ips("1.2.3.4")).privateIps(ips("1.2.3.5")).build()), UtcTime.now());
    }

    /**
     * Create a cloud pool {@link Server} that listens on HTTPS, doesn't require
     * client authentication and always returns the same machine pool.
     *
     * @param machinePool
     *            The machine pool returned by the cloud pool.
     * @param desiredSize
     *            The desired size of the cloud pool.
     *
     * @return
     */
    private Server createNoAuthHttpsServer(MachinePool machinePool) {
        SuccessfulCloudPoolServlet cloudpoolServlet = new SuccessfulCloudPoolServlet(machinePool,
                machinePool.getAllocatedMachines().size());
        ServletDefinition servlet = new ServletDefinition.Builder().servlet(cloudpoolServlet).servletPath("/pool")
                .build();
        return ServletServerBuilder.create().httpsPort(this.port).sslKeyStoreType(SslKeyStoreType.PKCS12)
                .sslKeyStorePath(SERVER_SECURITY + "/server_keystore.p12").sslKeyStorePassword("pkcs12password")
                .sslRequireClientCert(false).addServlet(servlet).build();
    }

    /**
     * Create a cloud pool {@link Server} that listens on HTTP, doesn't require
     * client authentication and always returns the same machine pool.
     *
     * @param machinePool
     *            The machine pool returned by the cloud pool.
     * @param desiredSize
     *            The desired size of the cloud pool.
     *
     * @return
     */
    private Server createNoAuthHttpServer(MachinePool machinePool) {
        SuccessfulCloudPoolServlet cloudpoolServlet = new SuccessfulCloudPoolServlet(machinePool,
                machinePool.getAllocatedMachines().size());
        ServletDefinition servlet = new ServletDefinition.Builder().servlet(cloudpoolServlet).servletPath("/pool")
                .build();
        return ServletServerBuilder.create().httpPort(this.port).addServlet(servlet).build();
    }

    /**
     * Creates a {@link CloudPoolProxy} configured to communicate over HTTP.
     *
     * @return
     */
    private StandardCloudPoolProxy httpCloudPoolProxy() {
        StandardCloudPoolProxy cloudPool = new StandardCloudPoolProxy(logger, this.eventBus);
        String poolUrl = String.format("http://localhost:%d", this.port);
        StandardCloudPoolProxyConfig config = new StandardCloudPoolProxyConfig(poolUrl, null, null);
        cloudPool.validate(config);
        cloudPool.configure(config);
        return cloudPool;
    }

    /**
     * Creates a {@link CloudPoolProxy} configured to communicate over HTTPS.
     *
     * @return
     */
    private StandardCloudPoolProxy httpsCloudPoolProxy() {
        StandardCloudPoolProxy cloudPool = new StandardCloudPoolProxy(logger, this.eventBus);
        String poolUrl = String.format("https://localhost:%d", this.port);
        StandardCloudPoolProxyConfig config = new StandardCloudPoolProxyConfig(poolUrl, null, null);
        cloudPool.validate(config);
        cloudPool.configure(config);
        return cloudPool;
    }

    public static List<String> ips(String... ipAddresses) {
        return Arrays.asList(ipAddresses);
    }
}
