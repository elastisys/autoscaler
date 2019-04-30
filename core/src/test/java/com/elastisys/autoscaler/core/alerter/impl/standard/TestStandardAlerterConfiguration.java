package com.elastisys.autoscaler.core.alerter.impl.standard;

import static com.elastisys.autoscaler.core.alerter.impl.standard.AlerterTestUtils.loadAlerterConfig;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.alerter.impl.standard.config.StandardAlerterConfig;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.eventbus.impl.SynchronousEventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.multiplexing.AlertersConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.elastisys.scale.commons.net.smtp.SmtpClientConfig;

/**
 * Verifies the {@link StandardAlerter} behavior when fed different
 * configurations.
 * <p/>
 * Verifies that parsing of a {@link StandardAlerterConfig} from JSON works and
 * that the validation properly identifies any cases of invalid configuration.
 */
public class TestStandardAlerterConfiguration {
    private static final UUID AUTOSCALER_UUID = UUID.randomUUID();
    private static final String AUTOSCALER_ID = "autoScalerId";
    private static final String NO_SUBSCRIPTIONS = "alerter/alerter-no-subscriptions.json";
    private static final String HTTP_SUBSCRIPTION = "alerter/alerter_http.json";
    private static final String SMTP_SUBSCRIPTION = "alerter/alerter_smtp.json";
    private static final String SMTP_SUBSCRIPTION_WITH_DEFAULTS = "alerter/alerter_smtp_relying_on_defaults.json";
    private static final String HTTP_AND_SMTP_SUBSCRIPTION = "alerter/alerter_http_and_smtp.json";

    private static final String INVALID_SEVERITYPATTERN_SUBSCRIPTION = "alerter/alerter_invalid_severity.json";

    private static final String MISSING_HTTP_SETTINGS = "alerter/alerter_http_invalid.json";
    private static final String MISSING_SMTP_SETTINGS = "alerter/alerter_smtp_invalid.json";

    private static final Logger logger = LoggerFactory.getLogger(TestStandardAlerterConfiguration.class);
    private static final EventBus eventBus = new SynchronousEventBus(logger);

    /** Object under test. */
    private StandardAlerter alerter;

    @Before
    public void onSetup() {
        this.alerter = new StandardAlerter(AUTOSCALER_UUID, AUTOSCALER_ID, logger, eventBus);
    }

    /**
     * A <code>null</code> configuration can be passed, resulting in a default
     * {@link StandardAlerterConfig}.
     */
    @Test
    public void defaultConfig() {
        this.alerter.configure(null);
        assertThat(this.alerter.getConfiguration(), is(nullValue()));

        assertThat(this.alerter.effectiveConfig(), is(StandardAlerter.DEFAULT_ALERTER_CONFIG));
    }

    @Test
    public void parseAndConfigureWithNoAlerters() throws Exception {
        StandardAlerterConfig config = loadAlerterConfig(NO_SUBSCRIPTIONS);
        this.alerter.validate(config);
        this.alerter.configure(config);
        assertThat(this.alerter.getConfiguration().getSmtpAlerters().size(), is(0));
        assertThat(this.alerter.getConfiguration().getHttpAlerters().size(), is(0));
        assertThat(this.alerter.getConfiguration().getDuplicateSuppression(),
                is(AlertersConfig.DEFAULT_DUPLICATE_SUPPRESSION));
    }

    @Test
    public void parseAndConfigureWithSingleHttpAlerter() throws Exception {
        StandardAlerterConfig config = loadAlerterConfig(HTTP_SUBSCRIPTION);
        this.alerter.validate(config);
        this.alerter.configure(config);
        StandardAlerterConfig alerterConfig = this.alerter.getConfiguration();
        assertThat(alerterConfig.getSmtpAlerters().size(), is(0));
        assertThat(alerterConfig.getHttpAlerters().size(), is(1));
        assertThat(alerterConfig.getHttpAlerters().get(0).getDestinationUrls(),
                is(asList("http://localhost:8000/endpoint")));
    }

    @Test
    public void parseAndConfigureWithSingleSmtpAlerter() throws Exception {
        StandardAlerterConfig config = loadAlerterConfig(SMTP_SUBSCRIPTION);
        this.alerter.validate(config);
        this.alerter.configure(config);

        StandardAlerterConfig alerterConfig = this.alerter.getConfiguration();
        assertThat(alerterConfig.getSmtpAlerters().size(), is(1));
        assertThat(alerterConfig.getHttpAlerters().size(), is(0));
        assertThat(alerterConfig.getSmtpAlerters().get(0).getRecipients(), is(asList("receiver@destination.com")));
    }

    @Test
    public void parseAndConfigureWithSingleSmtpAlerterRelyingOnDefaults() throws Exception {
        StandardAlerterConfig config = loadAlerterConfig(SMTP_SUBSCRIPTION_WITH_DEFAULTS);
        this.alerter.validate(config);
        this.alerter.configure(config);

        StandardAlerterConfig alerterConfig = this.alerter.getConfiguration();
        assertThat(alerterConfig.getSmtpAlerters().size(), is(1));
        assertThat(alerterConfig.getHttpAlerters().size(), is(0));
        SmtpAlerterConfig smtpAlerter = alerterConfig.getSmtpAlerters().get(0);
        assertThat(smtpAlerter.getSeverityFilter().getFilterExpression(), is(".*"));
        assertThat(smtpAlerter.getSmtpClientConfig().getSmtpPort(), is(25));
        assertThat(smtpAlerter.getSmtpClientConfig().getConnectionTimeout(),
                is(SmtpClientConfig.DEFAULT_CONNECTION_TIMEOUT));
        assertThat(smtpAlerter.getSmtpClientConfig().getSocketTimeout(), is(SmtpClientConfig.DEFAULT_SOCKET_TIMEOUT));
        assertThat(smtpAlerter.getSmtpClientConfig().getAuthentication(), is(nullValue()));
    }

    @Test
    public void reconfigure() throws Exception {
        // configure
        StandardAlerterConfig config = loadAlerterConfig(HTTP_SUBSCRIPTION);
        this.alerter.validate(config);
        this.alerter.configure(config);
        assertThat(this.alerter.getConfiguration(), is(loadAlerterConfig(HTTP_SUBSCRIPTION)));

        // re-configure
        config = loadAlerterConfig(SMTP_SUBSCRIPTION);
        this.alerter.validate(config);
        this.alerter.configure(config);
        assertThat(this.alerter.getConfiguration(), is(loadAlerterConfig(SMTP_SUBSCRIPTION)));
    }

    @Test
    public void parseAndConfigureWithHttpAndSmtpAlerter() throws Exception {

        StandardAlerterConfig config = loadAlerterConfig(HTTP_AND_SMTP_SUBSCRIPTION);
        this.alerter.validate(config);
        this.alerter.configure(config);

        StandardAlerterConfig alerterConfig = this.alerter.getConfiguration();
        assertThat(alerterConfig.getSmtpAlerters().size(), is(1));
        assertThat(alerterConfig.getHttpAlerters().size(), is(1));
        assertThat(alerterConfig.getSmtpAlerters().get(0).getRecipients(), is(asList("receiver@destination.com")));
        assertThat(alerterConfig.getHttpAlerters().get(0).getDestinationUrls(),
                is(asList("http://localhost:8000/endpoint")));
        assertThat(alerterConfig.getDuplicateSuppression(), is(new TimeInterval(15L, TimeUnit.MINUTES)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseAndConfigureWithInvalidSeverityPattern() throws Exception {
        StandardAlerterConfig config = loadAlerterConfig(INVALID_SEVERITYPATTERN_SUBSCRIPTION);
        this.alerter.validate(config);
        this.alerter.configure(config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseAndConfigureWithInvalidHttpAlerter() throws Exception {
        StandardAlerterConfig config = loadAlerterConfig(MISSING_HTTP_SETTINGS);
        this.alerter.validate(config);
        this.alerter.configure(config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseAndConfigureWithInvalidSmtpAlerter() throws Exception {
        StandardAlerterConfig config = loadAlerterConfig(MISSING_SMTP_SETTINGS);
        this.alerter.validate(config);
        this.alerter.configure(config);
    }

}
