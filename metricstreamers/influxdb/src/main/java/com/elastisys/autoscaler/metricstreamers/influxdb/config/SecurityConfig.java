package com.elastisys.autoscaler.metricstreamers.influxdb.config;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;

/**
 * Describes security settings needed on the client to connect with the InfluxDB
 * server.
 *
 * @see InfluxdbMetricStreamerConfig
 */
public class SecurityConfig {

    /** Default setting for server use of HTTPS. */
    static final boolean DEFAULT_USE_HTTPS = false;
    /** Default setting for verifying host certificate during TLS handshake. */
    static final boolean DEFAULT_VERIFY_CERT = false;
    /** Default setting for verifying host certificate during TLS handshake. */
    static final boolean DEFAULT_VERIFY_HOST = false;

    /**
     * If the InfluxDB server runs with HTTPS enabled, this option is
     * <code>true</code>. A value of <code>false</code> or <code>null</code>
     * means that the server runs plain HTTP.
     */
    private final Boolean https;
    /**
     * Basic (username/password) credentials to use if the InfluxDB server
     * requires user authentication. May be <code>null</code>, indicating no
     * client authentication.
     */
    private final BasicCredentials auth;
    /**
     * Set to <code>true</code> to enable server certificate verification on SSL
     * connections. If disabled, the server peer will not be verified, which is
     * similar to using the {@code --insecure} flag in {@code curl}.
     * <p/>
     * This option is only relevant when the server runs HTTPS.
     */
    private final Boolean verifyCert;

    /**
     * Enables/disables hostname verification during SSL handshakes. If
     * verification is enabled, the SSL handshake will only succeed if the URL's
     * hostname and the server's identification hostname match.
     * <p/>
     * This option is only relevant when the server runs HTTPS.
     */
    private final Boolean verifyHost;

    /**
     * Creates a {@link SecurityConfig}.
     *
     * @param https
     *            If the InfluxDB server runs with HTTPS enabled, this option is
     *            <code>true</code>. A value of <code>false</code> or
     *            <code>null</code> means that the server runs plain HTTP.
     * @param auth
     *            Basic (username/password) credentials to use if the InfluxDB
     *            server requires user authentication. May be <code>null</code>,
     *            indicating no client authentication.
     * @param verifyCert
     *            Set to <code>true</code> to enable server certificate
     *            verification on SSL connections. If disabled, the server peer
     *            will not be verified, which is similar to using the
     *            {@code --insecure} flag in {@code curl}.
     *            <p/>
     *            This option is only relevant when the server runs HTTPS.
     * @param verifyHost
     *            Enables/disables hostname verification during SSL handshakes.
     *            If verification is enabled, the SSL handshake will only
     *            succeed if the URL's hostname and the server's identification
     *            hostname match.
     *            <p/>
     *            This option is only relevant when the server runs HTTPS.
     */
    public SecurityConfig(Boolean https, BasicCredentials auth, Boolean verifyCert, Boolean verifyHost) {
        this.https = https;
        this.auth = auth;
        this.verifyCert = verifyCert;
        this.verifyHost = verifyHost;
    }

    /**
     * If the InfluxDB server runs with HTTPS enabled, this option is
     * <code>true</code>. A value of <code>false</code> means that the server
     * runs plain HTTP.
     *
     * @return
     */
    public boolean useHttps() {
        return Optional.ofNullable(this.https).orElse(DEFAULT_USE_HTTPS);
    }

    /**
     * Basic (username/password) credentials to use if the InfluxDB server
     * requires user authentication. May be <code>null</code>, indicating no
     * client authentication.
     *
     * @return
     */
    public BasicCredentials getAuth() {
        return this.auth;
    }

    /**
     * Set to <code>true</code> to enable server certificate verification on SSL
     * connections. If disabled, the server peer will not be verified, which is
     * similar to using the {@code --insecure} flag in {@code curl}.
     * <p/>
     * This option is only relevant when the server runs HTTPS.
     *
     * @return
     */
    public boolean shouldVerifyCert() {
        return Optional.ofNullable(this.verifyCert).orElse(DEFAULT_VERIFY_CERT);
    }

    /**
     * Enables/disables hostname verification during SSL handshakes. If
     * verification is enabled, the SSL handshaeake will only succeed if the
     * URL's hostname and the server's identification hostname match.
     * <p/>
     * This option is only relevant when the server runs HTTPS.
     *
     * @return
     */
    public boolean shouldVerifyHost() {
        return Optional.ofNullable(this.verifyHost).orElse(DEFAULT_VERIFY_HOST);
    }

    /**
     * Validates the configuration. Throws an {@link IllegalArgumentException}
     * if validation fails.
     */
    public void validate() throws IllegalArgumentException {
        try {
            if (this.auth != null) {
                this.auth.validate();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("security: " + e.getMessage(), e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(useHttps(), this.auth, shouldVerifyCert(), shouldVerifyHost());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SecurityConfig) {
            SecurityConfig that = (SecurityConfig) obj;
            return Objects.equals(useHttps(), that.useHttps()) && Objects.equals(this.auth, that.auth)
                    && Objects.equals(shouldVerifyCert(), that.shouldVerifyCert())
                    && Objects.equals(shouldVerifyHost(), that.shouldVerifyHost());
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
