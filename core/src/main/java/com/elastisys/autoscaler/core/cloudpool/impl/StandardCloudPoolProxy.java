package com.elastisys.autoscaler.core.cloudpool.impl;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;
import static java.lang.String.format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.alerter.api.types.AlertTopics;
import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxyException;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetric;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;
import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.AlertSeverity;
import com.elastisys.scale.commons.net.http.HttpRequestResponse;
import com.elastisys.scale.commons.net.http.client.AuthenticatedHttpClient;
import com.elastisys.scale.commons.net.retryable.Retryable;
import com.elastisys.scale.commons.net.retryable.Retryers;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.google.gson.JsonElement;

/**
 * A {@link CloudPoolProxy} that acts as a local proxy to a remotely located
 * {@link CloudPool}.
 * <p/>
 * The remote {@link CloudPool} is assumed to publish a REST API as described in
 * the <a href="http://cloudpoolrestapi.readthedocs.org/en/latest/">cloud pool
 * REST API</a>).
 * <p/>
 */
public class StandardCloudPoolProxy implements CloudPoolProxy<StandardCloudPoolProxyConfig> {

    /** {@link Logger} instance. */
    private final Logger logger;
    private final EventBus eventBus;

    /** Contains fault details of the latest failure. */
    private Optional<Throwable> lastFailure = Optional.empty();

    /** The current configuration set. */
    private StandardCloudPoolProxyConfig config;
    /** <code>true</code> if this {@link Service} has been started. */
    private boolean started;

    /** The currently desired cloud pool size. */
    private Integer lastSetDesiredSize;

    @Inject
    public StandardCloudPoolProxy(Logger logger, EventBus eventBus) {
        this.logger = logger;
        this.eventBus = eventBus;

        this.config = null;
        this.started = false;
        this.lastSetDesiredSize = null;
    }

    @Override
    public void validate(StandardCloudPoolProxyConfig configuration) throws IllegalArgumentException {
        checkArgument(configuration != null, "cloudPool: missing configuration");
        configuration.validate();
    }

    @Override
    public synchronized void configure(StandardCloudPoolProxyConfig configuration) throws IllegalArgumentException {
        validate(configuration);
        this.config = configuration;

        if (isStarted()) {
            stop();
            start();
        }
    }

    public boolean isStarted() {
        return this.started;
    }

    @Override
    public synchronized void start() throws IllegalStateException {
        checkState(getConfiguration() != null, "attempt to start cloud pool proxy before being configured");

        if (!isStarted()) {
            this.started = true;
            this.logger.info(getClass().getSimpleName() + " started.");
        }

    }

    @Override
    public synchronized void stop() {
        if (isStarted()) {
            this.started = false;
            this.logger.info(getClass().getSimpleName() + " stopped.");
        }
    }

    @Override
    public ServiceStatus getStatus() {
        return new ServiceStatus.Builder().started(isStarted()).lastFault(this.lastFailure).build();
    }

    @Override
    public StandardCloudPoolProxyConfig getConfiguration() {
        return this.config;
    }

    @Override
    public Class<StandardCloudPoolProxyConfig> getConfigurationClass() {
        return StandardCloudPoolProxyConfig.class;
    }

    @Override
    public MachinePool getMachinePool() throws CloudPoolProxyException {
        ensureStarted();
        // send GET request to remote cloud pool's REST API
        String url = format("%s%s", this.config.getCloudPoolUrl(), "/pool");

        AuthenticatedHttpClient client = httpClient(getConfiguration());
        HttpRequestResponse response = null;
        try {
            Retryable<HttpRequestResponse> retryer = Retryers.exponentialBackoffRetryer("getMachinePool",
                    () -> client.execute(new HttpGet(url)), retryDelay().getTime(), retryDelay().getUnit(),
                    maxRetries());
            response = retryer.call();
        } catch (Exception e) {
            throw new CloudPoolProxyException(
                    format("failed to retrieve machine pool from " + "cloud pool at %s: %s", url, e.getMessage()), e);
        }

        String responseBody = response.getResponseBody();
        try {
            return MachinePool.fromJson(responseBody);
        } catch (Exception e) {
            throw new CloudPoolProxyException(
                    format("failed to parse machine pool from " + "cloud pool at %s: %s", url, e.getMessage()), e);
        }
    }

    @Override
    public PoolSizeSummary getPoolSize() throws CloudPoolProxyException {
        ensureStarted();
        // send GET request to remote cloud pool's REST API
        String url = format("%s%s", this.config.getCloudPoolUrl(), "/pool/size");

        AuthenticatedHttpClient client = httpClient(getConfiguration());
        HttpRequestResponse response = null;
        try {
            Retryable<HttpRequestResponse> retryingGet = Retryers.exponentialBackoffRetryer("getPoolSize",
                    () -> client.execute(new HttpGet(url)), retryDelay().getTime(), retryDelay().getUnit(),
                    maxRetries());
            response = retryingGet.call();
        } catch (Exception e) {
            throw new CloudPoolProxyException(
                    format("failed to retrieve machine pool size from " + "cloud pool at %s: %s", url, e.getMessage()),
                    e);
        }

        String responseBody = response.getResponseBody();
        try {
            return JsonUtils.toObject(JsonUtils.parseJsonString(responseBody), PoolSizeSummary.class);
        } catch (Exception e) {
            throw new CloudPoolProxyException(
                    format("failed to parse pool size summary from " + "cloud pool at %s: %s", url, e.getMessage()), e);
        }
    }

    @Override
    public void setDesiredSize(int desiredSize) throws CloudPoolProxyException {
        ensureStarted();
        if (this.lastSetDesiredSize == null) {
            // if not yet known, initialize last known desired group size
            this.lastSetDesiredSize = getPoolSize().getDesiredSize();
        }

        // send an alert if desired size has changed
        if (this.lastSetDesiredSize != desiredSize) {
            reportGroupSizeChanged(this.lastSetDesiredSize, desiredSize);
            this.lastSetDesiredSize = desiredSize;
        }

        // send POST request to remote cloud pool's REST API
        String url = format("%s%s", this.config.getCloudPoolUrl(), "/pool/size");
        AuthenticatedHttpClient client = httpClient(getConfiguration());
        try {
            HttpPost post = new HttpPost(url);
            String message = format("{ \"desiredSize\": %d }", desiredSize);
            StringEntity entity = new StringEntity(message, ContentType.APPLICATION_JSON);
            post.setEntity(entity);

            this.logger.info("requesting desired size {} at {}", desiredSize, url);
            Retryable<HttpRequestResponse> retryingPost = Retryers.exponentialBackoffRetryer("setDesiredSizePool",
                    () -> client.execute(post), retryDelay().getTime(), retryDelay().getUnit(), maxRetries());
            retryingPost.call();
        } catch (Exception e) {
            throw new CloudPoolProxyException(
                    format("failed to set desired size on remote " + "cloud pool at %s: %s", url, e.getMessage()), e);
        }

        this.logger.info("done requesting desired size {} at {}.", desiredSize, url);
    }

    private void ensureStarted() {
        checkState(this.config != null, "attempt to use cloudPoolProxy before being started");
    }

    /**
     * Push a cloud pool size changed event onto the {@link AutoScaler} event
     * bus to notify {@link SystemHistorian} of the change.
     *
     * @param oldSize
     * @param newSize
     */
    private void reportGroupSizeChanged(Integer oldSize, Integer newSize) {
        String message = String.format("desired cloud pool size changed from %d to %d", oldSize, newSize);
        this.logger.info(message);

        String metric = SystemMetric.CLOUDPOOL_SIZE_CHANGED.getMetricName();
        MetricValue dataPoint = new MetricValue(metric, newSize, UtcTime.now());
        this.eventBus.post(new SystemMetricEvent(dataPoint));

        // send an INFO alert to any registered alert message subscribers
        Map<String, JsonElement> tags = createTags();
        this.eventBus.post(new Alert(AlertTopics.POOL_SIZE_CHANGED.getTopicPath(), AlertSeverity.INFO, UtcTime.now(),
                message, null, tags));
    }

    /**
     * Creates tags for a {@link SystemMetric#CLOUDPOOL_SIZE_CHANGED} event.
     * Just logs the error if it fails.
     *
     * @return
     */
    private Map<String, JsonElement> createTags() {
        Map<String, JsonElement> tags = new HashMap<>();

        try {
            List<Machine> activeMachines = getMachinePool().getActiveMachines();
            List<String> memberIds = activeMachines.stream().map(Machine::getId).collect(Collectors.toList());
            tags.put("cloudPoolMembers", JsonUtils.toJson(memberIds));
        } catch (Exception e) {
            this.logger.error(format("failed to create tags on " + "cloud pool size changed event: %s", e.getMessage()),
                    e);
        }
        return tags;
    }

    /**
     * Returns a HTTP client configured according to a certain
     * {@link StandardCloudPoolProxyConfig}.
     *
     * @param config
     * @return
     */
    private AuthenticatedHttpClient httpClient(StandardCloudPoolProxyConfig config) {
        return new AuthenticatedHttpClient(this.logger, config.getBasicCredentials(),
                config.getCertificateCredentials(), config.getConnectionTimeout(), config.getSocketTimeout());
    }

    private Integer maxRetries() {
        return this.config.getRetries().getMaxAttempts();
    }

    private TimeInterval retryDelay() {
        return this.config.getRetries().getInitialDelay();
    }
}
