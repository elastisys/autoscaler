package com.elastisys.autoscaler.core.alerter.impl.standard;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.elastisys.autoscaler.core.alerter.impl.standard.config.StandardAlerterConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.http.HttpAuthConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.elastisys.scale.commons.net.smtp.SmtpClientAuthentication;
import com.elastisys.scale.commons.net.smtp.SmtpClientConfig;

public class AlerterTestUtils {

    /**
     * Loads a configuration file assumed to contain a JSON object with a single
     * {@code alerter} element containing a {@link StandardAlerterConfig}.
     *
     * @param configResource
     *            Classpath resource containing a json-formatted
     *            {@link StandardAlerterConfig} file.
     * @return
     * @throws IOException
     */
    public static StandardAlerterConfig loadAlerterConfig(String configResource) throws IOException {
        return JsonUtils.toObject(JsonUtils.parseJsonResource(configResource).getAsJsonObject().get("alerter"),
                StandardAlerterConfig.class);
    }

    /**
     * Creates an {@link SmtpAlerterConfig} for a list of email recipients.
     *
     * @param recipients
     * @return
     */
    public static SmtpAlerterConfig smtpAlerter(String... recipients) {
        String sender = "test@foo.com";
        String subject = "alert!";
        String severityFilter = null;
        SmtpClientConfig smtpClientConfig = new SmtpClientConfig("smtp.foo.com", 485,
                new SmtpClientAuthentication("user", "secret"), true);
        return new SmtpAlerterConfig(Arrays.asList(recipients), sender, subject, severityFilter, smtpClientConfig);
    }

    /**
     * Creates a {@link HttpAlerterConfig} with a given set of destinations
     * using no authentication and the default severity filter (".*").
     *
     * @param urls
     * @return
     */
    public static HttpAlerterConfig httpAlerter(String... urls) {
        // default severity filter
        String severityFilter = null;
        return httpAlerter(Arrays.asList(urls), severityFilter);
    }

    /**
     * Creates a {@link HttpAlerterConfig} with a given set of destinations with
     * a given severity filter and using no authentication.
     *
     * @param destinationUrls
     * @return
     */
    public static HttpAlerterConfig httpAlerter(List<String> destinationUrls, String severityFilter) {
        HttpAuthConfig authConf = null;
        return new HttpAlerterConfig(destinationUrls, severityFilter, authConf);
    }
}
