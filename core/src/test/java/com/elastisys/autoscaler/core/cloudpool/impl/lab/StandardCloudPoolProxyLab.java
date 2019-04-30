package com.elastisys.autoscaler.core.cloudpool.impl.lab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.cloudpool.impl.StandardCloudPoolProxy;
import com.elastisys.autoscaler.core.cloudpool.impl.StandardCloudPoolProxyConfig;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.elastisys.scale.commons.net.ssl.KeyStoreType;

/**
 * A simple lab program that exercises the {@link StandardCloudPoolProxy}
 * against a cloud pool REST endpoint set up at https://localhost:8443.
 */
public class StandardCloudPoolProxyLab {

    static Logger logger = LoggerFactory.getLogger(StandardCloudPoolProxy.class);
    static EventBus eventBus = new SynchronousEventBus(logger);

    public static void main(String[] args) throws Exception {
        CloudPoolProxy<StandardCloudPoolProxyConfig> cloudPool = new StandardCloudPoolProxy(logger, eventBus);
        BasicCredentials basicCredentials = new BasicCredentials("admin", "adminpassword");
        CertificateCredentials certificateCredentials = new CertificateCredentials(KeyStoreType.PKCS12,
                "src/test/resources/cloudpool/client-security/client_keystore.p12", "pkcs12password", null);
        StandardCloudPoolProxyConfig config = new StandardCloudPoolProxyConfig("https://localhost:8443",
                basicCredentials, certificateCredentials);
        cloudPool.validate(config);
        cloudPool.configure(config);

        logger.info("getting machine pool ...");
        logger.info("machine pool: " + cloudPool.getMachinePool());

    }
}
