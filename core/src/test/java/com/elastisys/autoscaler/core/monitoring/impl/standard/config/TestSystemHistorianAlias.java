package com.elastisys.autoscaler.core.monitoring.impl.standard.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.core.monitoring.impl.standard.config.SystemHistorianAlias;

/**
 * Verify that {@link SystemHistorianAlias} refer to the correct implementation
 * classes.
 */
public class TestSystemHistorianAlias {

    @Test
    public void testAliases() {
        assertThat(SystemHistorianAlias.OpenTsdbSystemHistorian.getQualifiedClassName(),
                is("com.elastisys.autoscaler.systemhistorians.opentsdb.OpenTsdbSystemHistorian"));
        assertThat(SystemHistorianAlias.FileStoreSystemHistorian.getQualifiedClassName(),
                is("com.elastisys.autoscaler.systemhistorians.file.FileStoreSystemHistorian"));
    }
}
