package com.elastisys.autoscaler.core.cloudpool.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.core.cloudpool.impl.RetryConfig;
import com.elastisys.autoscaler.core.cloudpool.impl.StandardCloudPoolProxyConfig;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.elastisys.scale.commons.net.ssl.KeyStoreType;

/**
 * Exercises the {@link StandardCloudPoolProxyConfig} class.
 */
public class TestStandardCloudPoolProxyConfig {

    private static final String cloudPoolUrlHttp = "http://some.host:80/pool";
    private static final String cloudPoolUrlHttps = "https://some.host:8443/pool";
    private static final BasicCredentials basicCredentials = new BasicCredentials("user", "password");
    private static final CertificateCredentials certificateCredentials = new CertificateCredentials(KeyStoreType.PKCS12,
            "src/test/resources/cloudpool/client-security/client_keystore.p12", "keystorePassword", "keyPassword");

    private static final int connectionTimeout = 15000;
    private static final int socketTimeout = 5000;
    private static final RetryConfig retries = new RetryConfig(3, TimeInterval.seconds(2));

    /**
     * explicit values can be provided for each field.
     */
    @Test
    public void completeConfig() {
        StandardCloudPoolProxyConfig config = new StandardCloudPoolProxyConfig(cloudPoolUrlHttp, basicCredentials,
                certificateCredentials, connectionTimeout, socketTimeout, retries);
        config.validate();

        assertThat(config.getCloudPoolUrl(), is(cloudPoolUrlHttp));
        assertThat(config.getBasicCredentials().isPresent(), is(true));
        assertThat(config.getBasicCredentials().get(), is(basicCredentials));
        assertThat(config.getCertificateCredentials().isPresent(), is(true));
        assertThat(config.getCertificateCredentials().get(), is(certificateCredentials));
        assertThat(config.getConnectionTimeout(), is(connectionTimeout));
        assertThat(config.getSocketTimeout(), is(socketTimeout));
        assertThat(config.getRetries(), is(retries));
    }

    /**
     * Only cloudPoolUrl is mandatory.
     */
    @Test
    public void defaults() {
        BasicCredentials basiccredentials2 = null;
        CertificateCredentials certificatecredentials2 = null;
        Integer connectiontimeout2 = null;
        Integer sockettimeout2 = null;
        RetryConfig nullRetries = null;
        StandardCloudPoolProxyConfig config = new StandardCloudPoolProxyConfig(cloudPoolUrlHttp, basiccredentials2,
                certificatecredentials2, connectiontimeout2, sockettimeout2, nullRetries);
        config.validate();

        assertThat(config.getBasicCredentials().isPresent(), is(false));
        assertThat(config.getCertificateCredentials().isPresent(), is(false));
        assertThat(config.getConnectionTimeout(), is(StandardCloudPoolProxyConfig.DEFAULT_CONNECTION_TIMEOUT));
        assertThat(config.getSocketTimeout(), is(StandardCloudPoolProxyConfig.DEFAULT_SOCKET_TIMEOUT));
        assertThat(config.getRetries(), is(StandardCloudPoolProxyConfig.DEFAULT_RETRY_CONFIG));
    }

    /**
     * HTTPS protocol should be permitted.
     */
    @Test
    public void withHttpsProtocol() {
        StandardCloudPoolProxyConfig config = new StandardCloudPoolProxyConfig(cloudPoolUrlHttps);
        config.validate();

        assertThat(config.getCloudPoolUrl(), is(cloudPoolUrlHttps));
    }

    /**
     * Make sure trailing slashes are dropped.
     */
    @Test
    public void stripTrailingSlashes() {
        assertThat(new StandardCloudPoolProxyConfig("https://host:443/").getCloudPoolUrl(), is("https://host:443"));
        assertThat(new StandardCloudPoolProxyConfig("https://host:443/path/").getCloudPoolUrl(),
                is("https://host:443/path"));
        assertThat(new StandardCloudPoolProxyConfig("https://host:443/path//").getCloudPoolUrl(),
                is("https://host:443/path"));
    }

    /**
     * Only http and https are supported URL protocols.
     */
    @Test(expected = IllegalArgumentException.class)
    public void illegalProtocol() {
        new StandardCloudPoolProxyConfig("ftp://host:80/mypool").validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalCloudPoolUrl() {
        new StandardCloudPoolProxyConfig("http://host:80:/mypool").validate();
    }
}
