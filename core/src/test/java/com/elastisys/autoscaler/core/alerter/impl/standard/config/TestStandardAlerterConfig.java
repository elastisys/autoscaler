package com.elastisys.autoscaler.core.alerter.impl.standard.config;

import static com.elastisys.autoscaler.core.alerter.impl.standard.AlerterTestUtils.httpAlerter;
import static com.elastisys.autoscaler.core.alerter.impl.standard.AlerterTestUtils.smtpAlerter;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.elastisys.autoscaler.core.alerter.impl.standard.config.StandardAlerterConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.alerter.Alerter;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.google.gson.JsonElement;

/**
 * Verifies basic properties and behavior of the {@link StandardAlerterConfig}
 * class.
 */
public class TestStandardAlerterConfig {

    @Test
    public void basicSanity() {
        // empty alerter set
        List<SmtpAlerterConfig> smtpAlerters = Collections.emptyList();
        List<HttpAlerterConfig> httpAlerters = Collections.emptyList();
        StandardAlerterConfig config = new StandardAlerterConfig(smtpAlerters, httpAlerters);
        config.validate();
        assertThat(config.getHttpAlerters(), is(httpAlerters));
        assertThat(config.getSmtpAlerters(), is(smtpAlerters));

        // with smtp alerter
        smtpAlerters = Arrays.asList(smtpAlerter("receiver@gmail.com"));
        httpAlerters = Arrays.asList();
        config = new StandardAlerterConfig(smtpAlerters, httpAlerters);
        config.validate();
        assertThat(config.getHttpAlerters(), is(httpAlerters));
        assertThat(config.getSmtpAlerters(), is(smtpAlerters));

        // with http alerter
        smtpAlerters = Arrays.asList();
        httpAlerters = Arrays.asList(httpAlerter("https://my.web.hook"));
        config = new StandardAlerterConfig(smtpAlerters, httpAlerters);
        config.validate();
        assertThat(config.getHttpAlerters(), is(httpAlerters));
        assertThat(config.getSmtpAlerters(), is(smtpAlerters));

        // with smtp and http alerter
        smtpAlerters = Arrays.asList(smtpAlerter("receiver@gmail.com"));
        httpAlerters = Arrays.asList(httpAlerter("https://my.web.hook"));
        config = new StandardAlerterConfig(smtpAlerters, httpAlerters);
        config.validate();
        assertThat(config.getHttpAlerters(), is(httpAlerters));
        assertThat(config.getSmtpAlerters(), is(smtpAlerters));
    }

    /**
     * Verify that when {@link StandardAlerterConfig#validate()} is called, any
     * configured {@link SmtpAlerterConfig} or {@link HttpAlerterConfig} gets
     * validated as well.
     */
    @Test(expected = IllegalArgumentException.class)
    public void verifyThatValidationAlsoValidatesChildAlerters() {
        JsonElement invalidAlerterConfig = JsonUtils.parseJsonResource("alerter/alerter_http_invalid.json")
                .getAsJsonObject().get("alerter");
        StandardAlerterConfig invalidConfig = JsonUtils.toObject(invalidAlerterConfig, StandardAlerterConfig.class);
        invalidConfig.validate();
    }

    /**
     * A <code>null</code> value should be equivalent to an empty list of
     * {@link Alerter}s.
     */
    @Test
    public void configureWithNulls() {
        List<SmtpAlerterConfig> emptySmtp = Collections.emptyList();
        List<HttpAlerterConfig> emptyHttp = Collections.emptyList();
        StandardAlerterConfig config = new StandardAlerterConfig(null, null);
        config.validate();
        assertThat(config.getHttpAlerters(), is(emptyHttp));
        assertThat(config.getSmtpAlerters(), is(emptySmtp));
    }

}
