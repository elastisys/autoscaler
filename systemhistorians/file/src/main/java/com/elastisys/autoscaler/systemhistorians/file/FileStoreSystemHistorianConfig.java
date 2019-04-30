package com.elastisys.autoscaler.systemhistorians.file;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.io.File;

import com.elastisys.autoscaler.core.monitoring.systemhistorian.api.types.SystemMetricEvent;

/**
 * Represents a configuration for the {@link FileStoreSystemHistorian}.
 *
 * @see FileStoreSystemHistorian
 *
 */
public class FileStoreSystemHistorianConfig {

    /**
     * The file system path that {@link SystemMetricEvent}s will be written to.
     */
    private final String log;

    /**
     * Constructs a new {@link FileStoreSystemHistorianConfig}.
     *
     * @param log
     *            The file system path that {@link SystemMetricEvent}s will be
     *            written to.
     */
    public FileStoreSystemHistorianConfig(String log) {
        this.log = log;
    }

    /**
     * Validates the {@link FileStoreSystemHistorianConfig} and throws a
     * {@link IllegalArgumentException} if any check fails.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        try {
            checkArgument(this.log != null, "missing log");
            if (!getLog().exists()) {
                checkArgument(getLog().createNewFile(), "failed to create file %s", getLog().getAbsolutePath());
            } else {
                checkArgument(getLog().isFile(), "not a file: %s", getLog().getAbsolutePath());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("systemHistorian: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the {@link File} that {@link SystemMetricEvent}s will be written
     * to.
     *
     * @return
     */
    public File getLog() {
        return new File(this.log);
    }
}
