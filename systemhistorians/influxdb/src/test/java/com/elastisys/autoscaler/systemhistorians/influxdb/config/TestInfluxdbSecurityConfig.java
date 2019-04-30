package com.elastisys.autoscaler.systemhistorians.influxdb.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.systemhistorians.influxdb.config.InfluxdbSecurityConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;

/**
 * Exercise {@link InfluxdbSecurityConfig}.
 */
public class TestInfluxdbSecurityConfig {

    /**
     * Verify basic field access when specifying all fields.
     */
    @Test
    public void basicSanity() {
        Boolean https = true;
        BasicCredentials auth = new BasicCredentials("username", "password");
        Boolean verifyCert = true;
        Boolean verifyHost = true;
        InfluxdbSecurityConfig config = new InfluxdbSecurityConfig(https, auth, verifyCert, verifyHost);
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
        InfluxdbSecurityConfig config = new InfluxdbSecurityConfig(null, null, null, null);
        config.validate();

        assertThat(config.useHttps(), is(InfluxdbSecurityConfig.DEFAULT_USE_HTTPS));
        // no default is given for basic auth
        assertThat(config.getAuth(), is(nullValue()));
        assertThat(config.shouldVerifyCert(), is(InfluxdbSecurityConfig.DEFAULT_VERIFY_CERT));
        assertThat(config.shouldVerifyHost(), is(InfluxdbSecurityConfig.DEFAULT_VERIFY_HOST));
    }

    /**
     * Verify that validation also checks the auth credentials.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withAuthMissingUsername() {
        BasicCredentials creds = JsonUtils.toObject(JsonUtils.parseJsonString("{\"password\": \"bar\"}"),
                BasicCredentials.class);
        new InfluxdbSecurityConfig(null, creds, null, null).validate();
    }

    /**
     * Verify that validation also checks the auth credentials.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withAuthMissingPassword() {
        BasicCredentials creds = JsonUtils.toObject(JsonUtils.parseJsonString("{\"username\": \"foo\"}"),
                BasicCredentials.class);
        new InfluxdbSecurityConfig(null, creds, null, null).validate();
    }

}
