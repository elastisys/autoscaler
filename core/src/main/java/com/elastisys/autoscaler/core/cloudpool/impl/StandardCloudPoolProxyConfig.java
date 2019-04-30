package com.elastisys.autoscaler.core.cloudpool.impl;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.net.ssl.CertificateCredentials;

/**
 * Configuration for a {@link StandardCloudPoolProxy}. Can be set up to work
 * with basic authentication credentials and/or client certificate credentials.
 */
public class StandardCloudPoolProxyConfig {

    /**
     * The default timeout in milliseconds until a connection is established. A
     * timeout value of zero is interpreted as an infinite timeout. A negative
     * value is interpreted as undefined (system default).
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 60000;
    /**
     * The default socket timeout ({@code SO_TIMEOUT}) in milliseconds, which is
     * the timeout for waiting for data or, put differently, a maximum period
     * inactivity between two consecutive data packets). A timeout value of zero
     * is interpreted as an infinite timeout. A negative value is interpreted as
     * undefined (system default).
     */
    public static final int DEFAULT_SOCKET_TIMEOUT = 180000;

    /** The default {@link #retries} configuration to use. */
    public static final RetryConfig DEFAULT_RETRY_CONFIG = new RetryConfig(5, TimeInterval.seconds(1));

    /**
     * The base URL of the cloud pool. All <a href=
     * "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html#operations">REST
     * API operations</a> will be invoked relative to this base URL.
     * <p/>
     * For example, a call to start a cloudpool with a base URL of
     * {@code http://host:80/mypool} would make a {@code POST} to
     * {@code http://host:80/mypool/start}.
     */
    private final String cloudPoolUrl;

    /**
     * Username/password credentials for basic authentication. May be
     * <code>null</code>.
     */
    private final BasicCredentials basicCredentials;

    /**
     * Certificate credentials for certificate-based client authentication. Only
     * relevant when the {@link #cloudPoolUrl} uses the {@code https} protocol.
     * May be <code>null</code>.
     */
    private final CertificateCredentials certificateCredentials;

    /**
     * The timeout in milliseconds until a connection is established. A timeout
     * value of zero is interpreted as an infinite timeout. A negative value is
     * interpreted as undefined (system default). May be <code>null</code>.
     * Default: {@link #DEFAULT_CONNECTION_TIMEOUT}.
     */
    private final Integer connectionTimeout;

    /**
     * The socket timeout ({@code SO_TIMEOUT}) in milliseconds, which is the
     * timeout for waiting for data or, put differently, a maximum period
     * inactivity between two consecutive data packets). A timeout value of zero
     * is interpreted as an infinite timeout. A negative value is interpreted as
     * undefined (system default). May be <code>null</code>. Default:
     * {@link #DEFAULT_SOCKET_TIMEOUT}.
     */
    private final Integer socketTimeout;

    /**
     * Describes the retry-behavior to use when communication with the remote
     * {@link CloudPool} fails.
     */
    private final RetryConfig retries;

    /**
     * Creates a {@link StandardCloudPoolProxyConfig} with default timeouts,
     * default retry behavior, and no credentials.
     *
     * @param cloudPoolUrl
     *            The base URL of the cloud pool. All <a href=
     *            "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html#operations">REST
     *            API operations</a> will be invoked relative to this base URL.
     *            <p/>
     *            For example, a call to start a cloudpool with a base URL of
     *            {@code http://host:80/mypool} would make a {@code POST} to
     *            {@code http://host:80/mypool/start}.
     */
    public StandardCloudPoolProxyConfig(String cloudPoolUrl) {
        this(cloudPoolUrl, null, null, null, null, null);
    }

    /**
     * Creates a {@link StandardCloudPoolProxyConfig} with given credentials
     * using default timeouts and default retry behavior,.
     *
     * @param cloudPoolUrl
     *            The base URL of the cloud pool. All <a href=
     *            "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html#operations">REST
     *            API operations</a> will be invoked relative to this base URL.
     *            <p/>
     *            For example, a call to start a cloudpool with a base URL of
     *            {@code http://host:80/mypool} would make a {@code POST} to
     *            {@code http://host:80/mypool/start}.
     * @param basicCredentials
     *            Username/password credentials for basic authentication. May be
     *            <code>null</code>.
     * @param certificateCredentials
     *            Certificate credentials for certificate-based client
     *            authentication. May be <code>null</code>.
     */
    public StandardCloudPoolProxyConfig(String cloudPoolUrl, BasicCredentials basicCredentials,
            CertificateCredentials certificateCredentials) {
        this(cloudPoolUrl, basicCredentials, certificateCredentials, null, null, null);
    }

    /**
     * Creates a new {@link StandardCloudPoolProxyConfig}.
     *
     * @param cloudPoolUrl
     *            The base URL of the cloud pool. All <a href=
     *            "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html#operations">REST
     *            API operations</a> will be invoked relative to this base URL.
     *            <p/>
     *            For example, a call to start a cloudpool with a base URL of
     *            {@code http://host:80/mypool} would make a {@code POST} to
     *            {@code http://host:80/mypool/start}.
     * @param basicCredentials
     *            Username/password credentials for basic authentication. May be
     *            <code>null</code>.
     * @param certificateCredentials
     *            Certificate credentials for certificate-based client
     *            authentication. May be <code>null</code>.
     * @param connectionTimeout
     *            The timeout in milliseconds until a connection is established.
     *            A timeout value of zero is interpreted as an infinite timeout.
     *            A negative value is interpreted as undefined (system default).
     *            May be <code>null</code>. Default:
     *            {@link #DEFAULT_CONNECTION_TIMEOUT}.
     * @param socketTimeout
     *            The socket timeout ({@code SO_TIMEOUT}) in milliseconds, which
     *            is the timeout for waiting for data or, put differently, a
     *            maximum period inactivity between two consecutive data
     *            packets). A timeout value of zero is interpreted as an
     *            infinite timeout. A negative value is interpreted as undefined
     *            (system default). May be <code>null</code>. Default:
     *            {@link #DEFAULT_SOCKET_TIMEOUT}.
     * @param retries
     *            Describes the retry-behavior to use when communication with
     *            the remote {@link CloudPool} fails. Defaults to
     *            {@link #DEFAULT_RETRY_CONFIG}.
     */
    public StandardCloudPoolProxyConfig(String cloudPoolUrl, BasicCredentials basicCredentials,
            CertificateCredentials certificateCredentials, Integer connectionTimeout, Integer socketTimeout,
            RetryConfig retries) {
        this.cloudPoolUrl = cloudPoolUrl;

        this.basicCredentials = basicCredentials;
        this.certificateCredentials = certificateCredentials;
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
        this.retries = retries;
    }

    /**
     * The base URL of the cloud pool. All <a href=
     * "http://cloudpoolrestapi.readthedocs.io/en/latest/api.html#operations">REST
     * API operations</a> will be invoked relative to this base URL.
     * <p/>
     * For example, a call to start a cloudpool with a base URL of
     * {@code http://host:80/mypool} would make a {@code POST} to
     * {@code http://host:80/mypool/start}.
     *
     * @return
     */
    public String getCloudPoolUrl() {
        return stripTrailingSlashes(this.cloudPoolUrl);
    }

    private String stripTrailingSlashes(String url) {
        return url.replaceAll("/+$", "");
    }

    public Optional<BasicCredentials> getBasicCredentials() {
        return Optional.ofNullable(this.basicCredentials);
    }

    public Optional<CertificateCredentials> getCertificateCredentials() {
        return Optional.ofNullable(this.certificateCredentials);
    }

    /**
     * Returns the timeout in milliseconds until a connection is established. A
     * timeout value of zero is interpreted as an infinite timeout. A negative
     * value is interpreted as undefined (system default).
     *
     * @return
     *
     */
    public Integer getConnectionTimeout() {
        return Optional.ofNullable(this.connectionTimeout).orElse(DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * Returns the socket timeout ({@code SO_TIMEOUT}) in milliseconds, which is
     * the timeout for waiting for data or, put differently, a maximum period
     * inactivity between two consecutive data packets). A timeout value of zero
     * is interpreted as an infinite timeout. A negative value is interpreted as
     * undefined (system default).
     *
     * @return
     */
    public Integer getSocketTimeout() {
        return Optional.ofNullable(this.socketTimeout).orElse(DEFAULT_SOCKET_TIMEOUT);
    }

    /**
     * Describes the retry-behavior to use when communication with the remote
     * {@link CloudPool} fails.
     *
     * @return
     */
    public RetryConfig getRetries() {
        return Optional.ofNullable(this.retries).orElse(DEFAULT_RETRY_CONFIG);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.cloudPoolUrl, this.basicCredentials, this.certificateCredentials,
                this.connectionTimeout, this.socketTimeout, this.retries);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StandardCloudPoolProxyConfig) {
            StandardCloudPoolProxyConfig that = (StandardCloudPoolProxyConfig) obj;
            return Objects.equals(this.cloudPoolUrl, that.cloudPoolUrl) //
                    && Objects.equals(this.basicCredentials, that.basicCredentials) //
                    && Objects.equals(this.certificateCredentials, that.certificateCredentials) //
                    && Objects.equals(this.connectionTimeout, that.connectionTimeout) //
                    && Objects.equals(this.socketTimeout, that.socketTimeout)
                    && Objects.equals(this.retries, that.retries);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }

    /**
     * Makes a basic sanity check verifying that all values are non-
     * <code>null</code>. If a value is missing for any field a
     * {@link IllegalArgumentException} is thrown.
     *
     * @throws IllegalArgumentException
     *             If any configuration field is missing.
     */
    public void validate() throws IllegalArgumentException {
        // validate connection details
        checkArgument(this.cloudPoolUrl != null, "cloudPool: missing cloudPoolUrl");
        try {
            URL url = new URL(this.cloudPoolUrl);
            List<String> validProtocols = Arrays.asList("http", "https");
            checkArgument(validProtocols.contains(url.getProtocol()), "illegal protocol '%s': only %s allowed",
                    url.getProtocol(), validProtocols);
        } catch (Exception e) {
            throw new IllegalArgumentException("cloudPool: malformed cloudPoolUrl: " + e.getMessage(), e);
        }

        // validate client credentials
        if (getBasicCredentials().isPresent()) {
            try {
                getBasicCredentials().get().validate();
            } catch (Exception e) {
                throw new IllegalArgumentException("cloudPool: " + e.getMessage(), e);
            }
        }

        if (getCertificateCredentials().isPresent()) {
            try {
                getCertificateCredentials().get().validate();
            } catch (Exception e) {
                throw new IllegalArgumentException("cloudPool: " + e.getMessage(), e);
            }
        }

        checkArgument(getConnectionTimeout() >= 0, "cloudPool: connectionTimeout cannot be negative");
        checkArgument(getSocketTimeout() >= 0, "cloudPool: socketTimeout cannot be negative");

        try {
            getRetries().validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("cloudPool: " + e.getMessage(), e);
        }
    }

}
