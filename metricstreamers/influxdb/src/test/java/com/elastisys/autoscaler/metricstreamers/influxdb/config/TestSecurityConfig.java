package com.elastisys.autoscaler.metricstreamers.influxdb.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.metricstreamers.influxdb.config.SecurityConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;

/**
 * Exercise {@link SecurityConfig}.
 */
public class TestSecurityConfig {

    /**
     * Verify basic field access when specifying all fields.
     */
    @Test
    public void basicSanity() {
        Boolean https = true;
        BasicCredentials auth = new BasicCredentials("username", "password");
        Boolean verifyCert = true;
        Boolean verifyHost = true;
        SecurityConfig config = new SecurityConfig(https, auth, verifyCert, verifyHost);
        config.validate();

        assertThat(config.useHttps(), is(true));
        assertThat(config.getAuth(), is(new BasicCredentials("username", "password")));
        assertThat(config.shouldVerifyCert(), is(true));
        assertThat(config.shouldVerifyHost(), is(true));
    }

    /**
     * Verify defaults for all fields.
     */
    @Test
    public void defaults() {
        SecurityConfig config = new SecurityConfig(null, null, null, null);
        config.validate();

        assertThat(config.useHttps(), is(SecurityConfig.DEFAULT_USE_HTTPS));
        // no default is given for basic auth
        assertThat(config.getAuth(), is(nullValue()));
        assertThat(config.shouldVerifyCert(), is(SecurityConfig.DEFAULT_VERIFY_CERT));
        assertThat(config.shouldVerifyHost(), is(SecurityConfig.DEFAULT_VERIFY_HOST));
    }

    /**
     * Verify that validation also checks the auth credentials.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withAuthMissingUsername() {
        BasicCredentials creds = JsonUtils.toObject(JsonUtils.parseJsonString("{\"password\": \"bar\"}"),
                BasicCredentials.class);
        new SecurityConfig(null, creds, null, null).validate();
    }

    /**
     * Verify that validation also checks the auth credentials.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withAuthMissingPassword() {
        BasicCredentials creds = JsonUtils.toObject(JsonUtils.parseJsonString("{\"username\": \"foo\"}"),
                BasicCredentials.class);
        new SecurityConfig(null, creds, null, null).validate();
    }

}
