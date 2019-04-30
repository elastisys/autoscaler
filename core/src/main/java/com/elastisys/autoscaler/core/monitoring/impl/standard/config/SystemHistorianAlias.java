package com.elastisys.autoscaler.core.monitoring.impl.standard.config;

import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.SystemHistorian;

public enum SystemHistorianAlias {
    /** Alias for the OpenTSDB {@link SystemHistorian} implementation class. */
    OpenTsdbSystemHistorian("com.elastisys.autoscaler.systemhistorians.opentsdb.OpenTsdbSystemHistorian"),
    /** Alias for the file {@link SystemHistorian} implementation class. */
    FileStoreSystemHistorian("com.elastisys.autoscaler.systemhistorians.file.FileStoreSystemHistorian"),
    /** Alias for the InfluxDB {@link SystemHistorian} implementation class. */
    InfluxdbSystemHistorian("com.elastisys.autoscaler.systemhistorians.influxdb.InfluxdbSystemHistorian");

    /**
     * The fully (package-)qualified class name of the subsystem implementation
     * class that this alias refers to.
     */
    private String qualifiedClassName;

    private SystemHistorianAlias(String qualifiedClassName) {
        this.qualifiedClassName = qualifiedClassName;
    }

    /**
     * Returns the fully (package-)qualified class name of the subsystem
     * implementation class that this alias refers to.
     *
     * @return
     */
    public String getQualifiedClassName() {
        return this.qualifiedClassName;
    }
}
