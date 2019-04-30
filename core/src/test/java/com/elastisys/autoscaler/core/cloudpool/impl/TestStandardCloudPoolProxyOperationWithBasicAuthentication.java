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

import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxyException;
import com.elastisys.autoscaler.core.cloudpool.impl.stubs.SuccessfulCloudPoolServlet;
import com.elastisys.autoscaler.core.cloudpool.impl.stubs.UnsuccessfulCloudPoolServlet;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.server.ServletDefinition;
import com.elastisys.scale.commons.server.ServletServerBuilder;
import com.elastisys.scale.commons.server.SslKeyStoreType;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Verifies the correctness of the {@link StandardCloudPoolProxy}, to ensure
 * that it makes proper HTTPS call-outs when instructed to use Basic
 * authentication.
 */
public class TestStandardCloudPoolProxyOperationWithBasicAuthentication {
    static Logger logger = LoggerFactory.getLogger(TestStandardCloudPoolProxyOperationWithBasicAuthentication.class);

    private static final String SERVER_SECURITY = "src/test/resources/cloudpool/server-security";
    private EventBus eventBus = new SynchronousEventBus(logger);

    /** The cloud pool under test */
    private StandardCloudPoolProxy cloudPool;

    /** The port where the remote cloud pool endpoint is set up. */
    private int port;

    private Server server;

    @Before
    public void onSetup() throws Exception {
        // find a free port for test server
        List<Integer> freePorts = HostUtils.findFreePorts(2);
        this.port = freePorts.get(0);

        // create the cloud pool under test
        this.cloudPool = new StandardCloudPoolProxy(logger, this.eventBus);
        String poolUrl = String.format("https://localhost:%d/", this.port);
        StandardCloudPoolProxyConfig config = new StandardCloudPoolProxyConfig(poolUrl,
                new BasicCredentials("admin", "adminpassword"), null, null, null,
                new RetryConfig(2, TimeInterval.seconds(0)));
        this.cloudPool = new StandardCloudPoolProxy(logger, this.eventBus);
        this.cloudPool.validate(config);
        this.cloudPool.configure(config);

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
     * Verifies proper behavior when attempting a successful GET /pool request.
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulGetPool() throws Exception {
        MachinePool actualPool = actualPool();
        this.server = functionalHttpsBasicAuthCloudPool(actualPool);
        this.server.start();
        MachinePool retrievedPool = this.cloudPool.getMachinePool();
        assertThat(retrievedPool, is(actualPool));
    }

    /**
     * It should be possible to use basic auth over plain HTTP.
     */
    @Test
    public void basicAuthOverHttp() throws Exception {
        MachinePool actualPool = actualPool();
        this.server = functionalHttpBasicAuthCloudPool(actualPool);
        this.server.start();

        String poolUrl = String.format("http://localhost:%d/", this.port);
        StandardCloudPoolProxyConfig config = new StandardCloudPoolProxyConfig(poolUrl,
                new BasicCredentials("admin", "adminpassword"), null);
        StandardCloudPoolProxy httpCloudPoolProxy = this.cloudPool;
        httpCloudPoolProxy = new StandardCloudPoolProxy(logger, this.eventBus);
        httpCloudPoolProxy.validate(config);
        httpCloudPoolProxy.configure(config);

        MachinePool retrievedPool = httpCloudPoolProxy.getMachinePool();
        assertThat(retrievedPool, is(actualPool));
    }

    /**
     * Verifies proper behavior when invoking a remote endpoint that won't
     * authenticate us.
     *
     * @throws Exception
     */
    @Test(expected = CloudPoolProxyException.class)
    public void testGetPoolWithAuthenticationFailure() throws Exception {
        MachinePool actualPool = actualPool();
        this.server = functionalHttpsBasicAuthCloudPool(actualPool);
        this.server.start();
        // make sure cloud pool uses wrong credentials
        BasicCredentials badCredentials = new BasicCredentials("admin", "wrongpassword");
        String poolUrl = String.format("https://localhost:%d/", this.port);
        this.cloudPool.configure(new StandardCloudPoolProxyConfig(poolUrl, badCredentials, null, null, null,
                new RetryConfig(2, TimeInterval.seconds(0))));
        this.cloudPool.getMachinePool();
    }

    /**
     * Verifies proper behavior when attempting a successful GET /pool/size
     * request.
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulGetPoolSize() throws Exception {
        MachinePool actualPool = actualPool();
        int desired = actualPool.getAllocatedMachines().size();
        this.server = functionalHttpsBasicAuthCloudPool(actualPool);
        this.server.start();
        PoolSizeSummary poolSizeSummary = this.cloudPool.getPoolSize();
        PoolSizeSummary expectedPoolSizeSummary = new PoolSizeSummary(desired, actualPool.getAllocatedMachines().size(),
                actualPool.getActiveMachines().size());
        assertThat(poolSizeSummary.getActive(), is(expectedPoolSizeSummary.getActive()));
        assertThat(poolSizeSummary.getDesiredSize(), is(expectedPoolSizeSummary.getDesiredSize()));
        assertThat(poolSizeSummary.getAllocated(), is(expectedPoolSizeSummary.getAllocated()));
    }

    /**
     * Verifies proper behavior when invoking a remote endpoint that won't
     * authenticate us.
     *
     * @throws Exception
     */
    @Test(expected = CloudPoolProxyException.class)
    public void testGetPoolSizeWithAuthenticationFailure() throws Exception {
        MachinePool actualPool = actualPool();
        this.server = functionalHttpsBasicAuthCloudPool(actualPool);
        this.server.start();
        // make sure cloud pool uses wrong credentials
        BasicCredentials badCredentials = new BasicCredentials("admin", "wrongpassword");
        String poolUrl = String.format("https://localhost:%d/", this.port);
        this.cloudPool.configure(new StandardCloudPoolProxyConfig(poolUrl, badCredentials, null, null, null,
                new RetryConfig(2, TimeInterval.seconds(0))));
        this.cloudPool.getPoolSize();
    }

    /**
     * Verifies proper behavior when attempting a successful POST /pool request.
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulPostPool() throws Exception {
        MachinePool actualPool = actualPool();
        this.server = functionalHttpsBasicAuthCloudPool(actualPool);
        this.server.start();
        this.cloudPool.setDesiredSize(10);
    }

    /**
     * Verifies proper behavior when attempting a GET pool request on a remote
     * endpoint that fails.
     *
     * @throws Exception
     */
    @Test(expected = CloudPoolProxyException.class)
    public void testUnsuccessfulGetPool() throws Exception {
        this.server = brokenHttpsBasicAuthCloudPool();
        this.server.start();
        this.cloudPool.getMachinePool();
    }

    /**
     * Verifies proper behavior when attempting a GET pool size request on a
     * remote endpoint that fails.
     *
     * @throws Exception
     */
    @Test(expected = CloudPoolProxyException.class)
    public void testUnsuccessfulGetPoolSize() throws Exception {
        this.server = brokenHttpsBasicAuthCloudPool();
        this.server.start();
        this.cloudPool.getPoolSize();
    }

    /**
     * Verifies proper behavior when attempting a POST pool request on a remote
     * endpoint that fails.
     *
     * @throws Exception
     */
    @Test(expected = CloudPoolProxyException.class)
    public void testUnsuccessfulPostPool() throws Exception {
        this.server = brokenHttpsBasicAuthCloudPool();
        this.server.start();
        this.cloudPool.setDesiredSize(10);
    }

    /**
     * Verifies proper behavior when invoking a remote endpoint that won't
     * authenticate us.
     *
     * @throws Exception
     */
    @Test(expected = CloudPoolProxyException.class)
    public void testPostPoolWithAuthenticationFailure() throws Exception {
        MachinePool actualPool = actualPool();
        this.server = functionalHttpsBasicAuthCloudPool(actualPool);
        this.server.start();
        // make sure cloud pool uses wrong credentials
        BasicCredentials badCredentials = new BasicCredentials("admin", "wrongpassword");
        String poolUrl = String.format("https://localhost:%d/", this.port);

        this.cloudPool.configure(new StandardCloudPoolProxyConfig(poolUrl, badCredentials, null, null, null,
                new RetryConfig(2, TimeInterval.seconds(0))));
        this.cloudPool.setDesiredSize(10);
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
     * Create a cloud pool {@link Server} that requires HTTPS and basic auth,
     * and which always succeeds to respond to client requests.
     *
     * @param machinePool
     *            The machine pool returned by the cloud pool.
     * @param desiredSize
     *            The desired size of the cloud pool.
     *
     * @return
     */
    private Server functionalHttpsBasicAuthCloudPool(MachinePool machinePool) {
        SuccessfulCloudPoolServlet successfulCloudpool = new SuccessfulCloudPoolServlet(machinePool,
                machinePool.getAllocatedMachines().size());
        ServletDefinition servlet = new ServletDefinition.Builder().servlet(successfulCloudpool).servletPath("/pool")
                .requireBasicAuth(true).realmFile(SERVER_SECURITY + "/security-realm.properties").requireRole("USER")
                .build();
        return ServletServerBuilder.create().httpsPort(this.port).sslKeyStoreType(SslKeyStoreType.PKCS12)
                .sslKeyStorePath(SERVER_SECURITY + "/server_keystore.p12").sslKeyStorePassword("pkcs12password")
                .sslRequireClientCert(false).addServlet(servlet).build();
    }

    /**
     * Create a cloud pool {@link Server} that requires HTTP and basic auth, and
     * which always succeeds to respond to client requests.
     *
     * @param machinePool
     *            The machine pool returned by the cloud pool.
     * @param desiredSize
     *            The desired size of the cloud pool.
     *
     * @return
     */
    private Server functionalHttpBasicAuthCloudPool(MachinePool machinePool) {
        SuccessfulCloudPoolServlet successfulCloudpool = new SuccessfulCloudPoolServlet(machinePool,
                machinePool.getAllocatedMachines().size());
        ServletDefinition servlet = new ServletDefinition.Builder().servlet(successfulCloudpool).servletPath("/pool")
                .requireBasicAuth(true).realmFile(SERVER_SECURITY + "/security-realm.properties").requireRole("USER")
                .build();
        return ServletServerBuilder.create().httpPort(this.port).addServlet(servlet).build();
    }

    /**
     * Create a cloud pool {@link Server} that requires HTTPS and basic auth,
     * and which always fails to respond to client requests.
     *
     * @return
     */
    private Server brokenHttpsBasicAuthCloudPool() {
        ServletDefinition servlet = new ServletDefinition.Builder().servlet(new UnsuccessfulCloudPoolServlet())
                .servletPath("/pool").requireBasicAuth(true).realmFile(SERVER_SECURITY + "/security-realm.properties")
                .requireRole("USER").build();
        return ServletServerBuilder.create().httpsPort(this.port).sslKeyStoreType(SslKeyStoreType.PKCS12)
                .sslKeyStorePath(SERVER_SECURITY + "/server_keystore.p12").sslKeyStorePassword("pkcs12password")
                .sslRequireClientCert(false).addServlet(servlet).build();
    }

    public static List<String> ips(String... ipAddresses) {
        return Arrays.asList(ipAddresses);
    }
}
