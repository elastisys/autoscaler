package com.elastisys.autoscaler.core.cloudpool.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import javax.servlet.Servlet;

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
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.elastisys.scale.commons.net.ssl.KeyStoreType;
import com.elastisys.scale.commons.server.ServletDefinition;
import com.elastisys.scale.commons.server.ServletServerBuilder;
import com.elastisys.scale.commons.server.SslKeyStoreType;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Verifies the correctness of the {@link StandardCloudPoolProxy}, to ensure
 * that it makes proper HTTPS call-outs when instructed to use client
 * certificate authentication.
 */
public class TestStandardCloudPoolProxyOperationWithClientCertAuthentication {
    static Logger logger = LoggerFactory
            .getLogger(TestStandardCloudPoolProxyOperationWithClientCertAuthentication.class);

    private static final String CLIENT_SECURITY = "src/test/resources/cloudpool/client-security";
    private static final String SERVER_SECURITY = "src/test/resources/cloudpool/server-security";
    private EventBus eventBus = new SynchronousEventBus(logger);

    /** The cloud pool under test */
    private StandardCloudPoolProxy cloudPool;

    /** The https port where the remote cloud pool endpoint is set up. */
    private int httpsPort;
    private Server server;

    @Before
    public void onSetup() throws Exception {
        // find a free port for test server
        List<Integer> freePorts = HostUtils.findFreePorts(2);
        this.httpsPort = freePorts.get(0);

        // configure cloud pool under test
        String poolUrl = String.format("https://localhost:%d/", this.httpsPort);
        StandardCloudPoolProxyConfig config = new StandardCloudPoolProxyConfig(poolUrl, null,
                new CertificateCredentials(KeyStoreType.PKCS12, CLIENT_SECURITY + "/client_keystore.p12",
                        "pkcs12password", null),
                null, null, new RetryConfig(3, TimeInterval.seconds(0)));
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
        MachinePool actualPool = pool();
        int desiredSize = actualPool.getAllocatedMachines().size();
        this.server = clientTrustingServer(new SuccessfulCloudPoolServlet(actualPool, desiredSize)).build();
        this.server.start();
        MachinePool retrievedPool = this.cloudPool.getMachinePool();
        assertThat(retrievedPool, is(actualPool));
    }

    /**
     * Verifies proper behavior when invoking a remote endpoint that won't
     * authenticate our client certificate.
     *
     * @throws Exception
     */
    @Test(expected = CloudPoolProxyException.class)
    public void testGetPoolWithAuthenticationFailure() throws Exception {
        MachinePool actualPool = pool();
        int desiredSize = actualPool.getAllocatedMachines().size();
        this.server = clientDistrustingServer(new SuccessfulCloudPoolServlet(actualPool, desiredSize)).build();
        this.server.start();
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
        MachinePool actualPool = pool();
        int desiredSize = actualPool.getAllocatedMachines().size() + 1;
        this.server = clientTrustingServer(new SuccessfulCloudPoolServlet(actualPool, desiredSize)).build();
        this.server.start();
        PoolSizeSummary poolSizeSummary = this.cloudPool.getPoolSize();
        PoolSizeSummary expectedPoolSizeSummary = new PoolSizeSummary(desiredSize,
                actualPool.getAllocatedMachines().size(), actualPool.getActiveMachines().size());
        assertThat(poolSizeSummary.getActive(), is(expectedPoolSizeSummary.getActive()));
        assertThat(poolSizeSummary.getDesiredSize(), is(expectedPoolSizeSummary.getDesiredSize()));
        assertThat(poolSizeSummary.getAllocated(), is(expectedPoolSizeSummary.getAllocated()));

    }

    /**
     * Verifies proper behavior when attempting a GET pool request on a remote
     * endpoint that fails.
     *
     * @throws Exception
     */
    @Test(expected = CloudPoolProxyException.class)
    public void testUnsuccessfulGetPool() throws Exception {
        this.server = clientTrustingServer(new UnsuccessfulCloudPoolServlet()).build();
        this.server.start();
        this.cloudPool.getMachinePool();
    }

    /**
     * Verifies proper behavior when invoking a remote endpoint that won't
     * authenticate our client certificate.
     *
     * @throws Exception
     */
    @Test(expected = CloudPoolProxyException.class)
    public void testGetPoolSizeWithAuthenticationFailure() throws Exception {
        MachinePool actualPool = pool();
        int desiredSize = actualPool.getAllocatedMachines().size();
        this.server = clientDistrustingServer(new SuccessfulCloudPoolServlet(actualPool, desiredSize)).build();
        this.server.start();
        this.cloudPool.getPoolSize();
    }

    /**
     * Verifies proper behavior when attempting a GET pool size request on a
     * remote endpoint that fails.
     *
     * @throws Exception
     */
    @Test(expected = CloudPoolProxyException.class)
    public void testUnsuccessfulGetPoolSize() throws Exception {
        this.server = clientTrustingServer(new UnsuccessfulCloudPoolServlet()).build();
        this.server.start();
        this.cloudPool.getPoolSize();
    }

    /**
     * Verifies proper behavior when attempting a successful POST /pool request.
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulPostPool() throws Exception {
        MachinePool actualPool = pool();
        int desiredSize = actualPool.getAllocatedMachines().size();
        this.server = clientTrustingServer(new SuccessfulCloudPoolServlet(actualPool, desiredSize)).build();
        this.server.start();
        this.cloudPool.setDesiredSize(10);
    }

    /**
     * Verifies proper behavior when invoking a remote endpoint that won't
     * authenticate our client certificate.
     *
     * @throws Exception
     */
    @Test(expected = CloudPoolProxyException.class)
    public void testPostPoolWithAuthenticationFailure() throws Exception {
        MachinePool actualPool = pool();
        int desiredSize = actualPool.getAllocatedMachines().size();
        this.server = clientDistrustingServer(new SuccessfulCloudPoolServlet(actualPool, desiredSize)).build();
        this.server.start();
        this.cloudPool.setDesiredSize(10);
    }

    /**
     * Verifies proper behavior when attempting a POST pool request on a remote
     * endpoint that fails.
     *
     * @throws Exception
     */
    @Test(expected = CloudPoolProxyException.class)
    public void testUnsuccessfulPostPool() throws Exception {
        this.server = clientTrustingServer(new UnsuccessfulCloudPoolServlet()).build();
        this.server.start();
        this.cloudPool.setDesiredSize(10);
    }

    /**
     * Sets up a {@link ServletServerBuilder} from which a {@link Server} can be
     * built that requires client certificate authentication. No trust store is
     * set up.
     *
     * @param cloudPoolServlet
     *
     * @return
     */
    private ServletServerBuilder basicCertAuthServer(Servlet cloudPoolServlet) {
        ServletDefinition servlet = new ServletDefinition.Builder().servlet(cloudPoolServlet).servletPath("/pool")
                .build();
        return ServletServerBuilder.create().httpsPort(this.httpsPort).sslKeyStoreType(SslKeyStoreType.PKCS12)
                .sslKeyStorePath(SERVER_SECURITY + "/server_keystore.p12").sslKeyStorePassword("pkcs12password")
                .sslTrustStoreType(SslKeyStoreType.JKS).sslRequireClientCert(true).addServlet(servlet);
    }

    /**
     * Sets up a {@link ServletServerBuilder} from which a {@link Server} can be
     * built that requires client certificate authentication and with a trust
     * store that includes our client certificate.
     *
     * @param cloudPoolServlet
     *
     * @return
     */
    private ServletServerBuilder clientTrustingServer(Servlet cloudPoolServlet) {
        return basicCertAuthServer(cloudPoolServlet).sslTrustStorePath(SERVER_SECURITY + "/server_truststore.jks")
                .sslTrustStorePassword("truststorepassword");
    }

    /**
     * Sets up a {@link ServletServerBuilder} from which a {@link Server} can be
     * built that requires client certificate authentication and with a trust
     * store that <b>doesn't</b> include our client certificate.
     *
     * @param cloudPoolServlet
     * @return
     */
    private ServletServerBuilder clientDistrustingServer(Servlet cloudPoolServlet) {
        return basicCertAuthServer(cloudPoolServlet).sslTrustStorePath(SERVER_SECURITY + "/server_empty_truststore.jks")
                .sslTrustStorePassword("truststorepassword");
    }

    /**
     * Creates a dummy {@link MachinePool} for the (test-simulated) remote cloud
     * pool.
     */
    private static MachinePool pool() {
        DateTime now = UtcTime.now();
        return new MachinePool(Arrays.asList(Machine.builder().id("i-123456").machineState(MachineState.RUNNING)
                .cloudProvider("EC2").region("us-east-1").machineSize("m1.small").launchTime(now)
                .publicIps(ips("1.2.3.4")).privateIps(ips("1.2.3.5")).build()), UtcTime.now());
    }

    public static List<String> ips(String... ipAddresses) {
        return Arrays.asList(ipAddresses);
    }

}
