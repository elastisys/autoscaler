package com.elastisys.autoscaler.core.cloudpool.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.core.cloudpool.impl.RetryConfig;
import com.elastisys.autoscaler.core.cloudpool.impl.StandardCloudPoolProxyConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;
import com.elastisys.scale.commons.net.ssl.KeyStoreType;
import com.google.gson.JsonObject;

/**
 * Verifies proper parsing from JSON document to
 * {@link StandardCloudPoolProxyConfig}.
 */
public class TestStandardCloudPoolProxyConfigParsing {
    /**
     * Verifies proper translation of a {@link StandardCloudPoolProxyConfig}
     * JSON configuration to its java type.
     */
    @Test
    public void parseJsonConfiguration() throws Exception {
        JsonObject jsonConfig = JsonUtils.parseJsonResource("cloudpool/remote-cloudpool-config.json").getAsJsonObject();
        StandardCloudPoolProxyConfig config = JsonUtils.toObject(jsonConfig, StandardCloudPoolProxyConfig.class);

        assertThat(config.getRetries(), is(new RetryConfig(3, TimeInterval.seconds(2))));

        assertThat(config.getCloudPoolUrl(), is("https://some.host:443"));

        assertThat(config.getBasicCredentials().get(), is(new BasicCredentials("admin", "adminpassword")));

        // Should fall back to default key store type
        assertThat(config.getCertificateCredentials().get(), is(new CertificateCredentials(KeyStoreType.JKS,
                "src/test/resources/cloudpool/client-security/client_keystore.jks", "jkspassword", "jkspassword")));
    }
}
