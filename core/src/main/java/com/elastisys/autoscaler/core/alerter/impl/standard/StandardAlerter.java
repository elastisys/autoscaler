package com.elastisys.autoscaler.core.alerter.impl.standard;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Named;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.alerter.api.Alerter;
import com.elastisys.autoscaler.core.alerter.impl.standard.config.StandardAlerterConfig;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.autoscaler.AutoScalerMetadata;
import com.elastisys.scale.commons.eventbus.AllowConcurrentEvents;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.Subscriber;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.filtering.FilteringAlerter;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerter;
import com.elastisys.scale.commons.net.alerter.multiplexing.MultiplexingAlerter;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerter;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

/**
 * Standard implementation of {@link Alerter}, which supports SMTP (email) and
 * HTTP/S (webhook) alerting. See {@link StandardAlerterConfig}.
 * <p/>
 * The {@link StandardAlerter} can manage a collection of {@link SmtpAlerter}s
 * and {@link HttpAlerter}s, to which it dispatches all incoming {@link Alert}s
 * it receives on {@link #onAlert(AlertMessage)}.
 *
 * @see Alerter
 * @see StandardAlerterConfig
 */
public class StandardAlerter implements Alerter<StandardAlerterConfig> {
    /**
     * Default if a config element is missing for alerter is to set up an empty
     * alerter.
     */
    public static final StandardAlerterConfig DEFAULT_ALERTER_CONFIG = new StandardAlerterConfig(null, null);

    private final Logger logger;

    private final EventBus eventBus;

    /**
     * The currently set configuration (if any). A <code>null</code>
     * configuration is allowed, in which case {@link #DEFAULT_ALERTER_CONFIG}
     * is used.
     */
    private Optional<StandardAlerterConfig> config;

    /**
     * Dispatches {@link Alert}s sent on the {@link EventBus} to configured
     * {@link Alerter}s.
     */
    private final MultiplexingAlerter alerter;

    /**
     * Universally unique id of the {@link AutoScaler} instance to which we
     * belong.
     */
    private final UUID autoScalerUuid;
    /** Id/name of the {@link AutoScaler} instance to which we belong. */
    private final String autoScalerId;
    /** <code>true</code> if the service has been started. */
    private boolean started;
    /** Contains fault details of the latest prediction run failure. */
    private Optional<Throwable> lastFailure = Optional.empty();

    @Inject
    public StandardAlerter(@Named("Uuid") UUID autoScalerUuid, @Named("AutoScalerId") String autoScalerId,
            Logger logger, EventBus eventBus) {
        requireNonNull(logger, "logger cannot be null");
        requireNonNull(eventBus, "event bus cannot be null");

        this.logger = logger;
        this.autoScalerUuid = autoScalerUuid;
        this.autoScalerId = autoScalerId;
        this.eventBus = eventBus;

        this.config = null;
        this.started = false;
        this.alerter = new MultiplexingAlerter(FilteringAlerter.DEFAULT_IDENTITY_FUNCTION);
    }

    @Override
    public void validate(StandardAlerterConfig configuration) throws IllegalArgumentException {
        if (configuration == null) {
            // null config is accepted
            return;
        }

        configuration.validate();
    }

    @Override
    public synchronized void configure(StandardAlerterConfig configuration) throws IllegalArgumentException {
        validate(configuration);

        // if already running: restart
        boolean needsRestart = isStarted();
        if (needsRestart) {
            stop();
        }

        apply(configuration);

        if (needsRestart) {
            start();
        }
    }

    /**
     * Registers a new set of child
     * {@link com.elastisys.scale.commons.net.alerter.Alerter}s.
     *
     * @param configuration
     */
    private void apply(StandardAlerterConfig configuration) {
        this.logger.debug("applying configuration ...");
        this.config = Optional.ofNullable(configuration);
    }

    private boolean isStarted() {
        return this.started;
    }

    @Override
    public StandardAlerterConfig getConfiguration() {
        return this.config == null ? null : this.config.orElse(null);
    }

    @Override
    public Class<StandardAlerterConfig> getConfigurationClass() {
        return StandardAlerterConfig.class;
    }

    @Override
    public synchronized void start() {
        ensureConfigured();

        if (isStarted()) {
            return;
        }

        // register with event bus to start listening for Alerts
        this.eventBus.register(this);

        // start forwarding alerts sent on event bus
        Map<String, JsonElement> autoscalerTags = AutoScalerMetadata.alertTags(this.autoScalerUuid, this.autoScalerId);
        this.alerter.registerAlerters(effectiveConfig(), autoscalerTags);
        this.started = true;
        this.logger.info(getClass().getSimpleName() + " started.");

    }

    @Override
    public synchronized void stop() {
        if (!isStarted()) {
            return;
        }

        // unregister from event bus to stop listening for Alerts
        this.eventBus.register(this);

        // stop forwarding alerts sent on event bus
        this.alerter.unregisterAlerters();
        this.started = false;
        this.logger.info(getClass().getSimpleName() + " stopped.");
    }

    @Override
    public ServiceStatus getStatus() {
        return new ServiceStatus.Builder().started(isStarted()).lastFault(this.lastFailure).build();
    }

    @Override
    @Subscriber
    @AllowConcurrentEvents
    public void onAlert(Alert alert) {
        requireNonNull(alert, "alert message cannot be null");

        if (!isStarted()) {
            this.logger.debug("suppressing alert dispatch, since {} is stopped", getClass().getSimpleName());
            return;
        }

        // dispatch alert through all child alerters
        try {
            this.alerter.handleAlert(alert);
        } catch (Exception e) {
            this.logger.warn("failed to send alert: {}:\n{}", alert, e.getMessage(), e);
        }
    }

    private void ensureConfigured() {
        checkState(this.config != null, "attempt to start before being configured");
    }

    /**
     * Returns the effective config which is the currently set configuration, if
     * one has been set. Otherwise {@link #DEFAULT_ALERTER_CONFIG} is returned.
     *
     * @return
     */
    StandardAlerterConfig effectiveConfig() {
        if (this.config == null) {
            return DEFAULT_ALERTER_CONFIG;
        }
        return this.config.orElse(DEFAULT_ALERTER_CONFIG);
    }

}
