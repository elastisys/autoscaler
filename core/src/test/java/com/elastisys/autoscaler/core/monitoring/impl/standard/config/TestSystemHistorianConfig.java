package com.elastisys.autoscaler.core.monitoring.impl.standard.config;

import static com.elastisys.autoscaler.core.monitoring.impl.standard.config.MonitoringSystemTestUtils.systemHistorianConfigDocument;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.core.monitoring.impl.standard.config.SystemHistorianConfig;
import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;

/**
 * Verifies the behavior of {@link SystemHistorianConfig}.
 */
public class TestSystemHistorianConfig {

    @Test
    public void basicSanity() {
        SystemHistorianConfig config = new SystemHistorianConfig("some.SystemHistorianImpl",
                systemHistorianConfigDocument());
        config.validate();

        assertThat(config.getType(), is("some.SystemHistorianImpl"));
        assertThat(config.getConfig(), is(systemHistorianConfigDocument()));
    }

    /**
     * {@link SystemHistorian} type is a mandatory field.
     */
    @Test(expected = IllegalArgumentException.class)
    public void missingType() {
        new SystemHistorianConfig(null, systemHistorianConfigDocument()).validate();
    }

    /**
     * {@link SystemHistorian} configuration is a mandatory field.
     */
    @Test(expected = IllegalArgumentException.class)
    public void missingConfig() {
        new SystemHistorianConfig("SystemHistorianImpl", null).validate();
    }

}
