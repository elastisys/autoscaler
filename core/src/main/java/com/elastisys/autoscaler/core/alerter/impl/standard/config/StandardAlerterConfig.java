package com.elastisys.autoscaler.core.alerter.impl.standard.config;

import java.util.List;

import com.elastisys.autoscaler.core.alerter.impl.standard.StandardAlerter;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.Alerter;
import com.elastisys.scale.commons.net.alerter.filtering.FilteringAlerter;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.multiplexing.AlertersConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;

/**
 * Configuration for the {@link StandardAlerter} that tracks the collection of
 * configured SMTP and HTTP(S) {@link Alerter}s.
 *
 * @see StandardAlerter
 */
public class StandardAlerterConfig extends AlertersConfig {

    /**
     * Constructs a new {@link AlertersConfig} instance with default duplicate
     * suppression ({@link AlertersConfig#DEFAULT_DUPLICATE_SUPPRESSION}).
     *
     * @param smtpAlerters
     *            A list of configured SMTP email {@link Alerter}s. A
     *            <code>null</code> value is equivalent to an empty list.
     * @param httpAlerters
     *            A list of HTTP(S) webhook {@link Alerter}s. A
     *            <code>null</code> value is equivalent to an empty list.
     */
    public StandardAlerterConfig(List<SmtpAlerterConfig> smtpAlerters, List<HttpAlerterConfig> httpAlerters) {
        super(smtpAlerters, httpAlerters);
    }

    /**
     * Constructs a new {@link AlertersConfig} instance.
     *
     * @param smtpAlerters
     *            A list of configured SMTP email {@link Alerter}s. A
     *            <code>null</code> value is equivalent to an empty list.
     * @param httpAlerters
     *            A list of HTTP(S) webhook {@link Alerter}s. A
     *            <code>null</code> value is equivalent to an empty list.
     * @param duplicateSuppression
     *            Duration of time to suppress duplicate {@link Alert}s from
     *            being re-sent. Two {@link Alert}s are considered equal if they
     *            share topic, message and metadata tags (see
     *            {@link FilteringAlerter#DEFAULT_IDENTITY_FUNCTION}). May be
     *            <code>null</code>. Default:
     *            {@link AlertersConfig#DEFAULT_DUPLICATE_SUPPRESSION}.
     */
    public StandardAlerterConfig(List<SmtpAlerterConfig> smtpAlerters, List<HttpAlerterConfig> httpAlerters,
            TimeInterval duplicateSuppression) {
        super(smtpAlerters, httpAlerters, duplicateSuppression);
    }
}