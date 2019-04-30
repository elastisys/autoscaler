package com.elastisys.autoscaler.core.autoscaler.factory;

import static java.lang.String.format;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.autoscaler.builder.AutoScalerBuilder;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * An {@link AutoScalerFactory} configuration.
 *
 * @see AutoScalerFactory
 */
public class AutoScalerFactoryConfig {
    /**
     * The default directory where the {@link AutoScalerFactory} will store its
     * runtime state.
     */
    public final static String DEFAULT_STORAGE_DIR = "/var/lib/elastisys/autoscaler/instances";

    /**
     * File system path to the directory where the {@link AutoScalerFactory}
     * will persist instance state. Optional. Default:
     * {@link #DEFAULT_STORAGE_DIR}.
     */
    private final String storageDir;

    /**
     * The collection of add-on subsystems that will be added to all
     * {@link AutoScaler} instances the {@link AutoScalerFactory} creates. These
     * add-on subsystems are not strictly necessary for the {@link AutoScaler}
     * to operate, but may extend it with additional functionality. Accounting
     * and high-availability are two examples of what such add-on subsystems
     * could achieve. Keys are names, such as {@code accountingSubsystem}, and
     * values are class names, such as
     * {@code com.elastisys.AccountingSubsystemImpl}. Optional. Default: no
     * addon subsystems.
     */
    private final Map<String, String> addonSubsystems;

    /**
     * Constructs a new {@link AutoScalerFactoryConfig} with a given storage
     * directory.
     *
     * @param storageDir
     *            File system path to the directory where the
     *            {@link AutoScalerFactory} will persist instance state.
     *            Optional. Default: {@link #DEFAULT_STORAGE_DIR}.
     * @param addonSubsystems
     *            The collection of add-on subsystems that will be added to all
     *            {@link AutoScaler} instances the {@link AutoScalerFactory}
     *            creates. These add-on subsystems are not strictly necessary
     *            for the {@link AutoScaler} to operate, but may extend it with
     *            additional functionality. Accounting and high-availability are
     *            two examples of what such add-on subsystems could achieve. May
     *            be <code>null</code>. Keys are names, such as
     *            {@code accountingSubsystem}, and values are class names, such
     *            as {@code com.elastisys.AccountingSubsystemImpl}. Optional.
     *            Default: no addon subsystems.
     */
    public AutoScalerFactoryConfig(String storageDir, Map<String, String> addonSubsystems) {
        Objects.requireNonNull(storageDir, "storageDir cannot be null");
        this.storageDir = storageDir;
        this.addonSubsystems = addonSubsystems;
    }

    /**
     * Returns the file system path to the directory where the
     * {@link AutoScalerFactory} will persist instance state.
     *
     * @return
     */
    public File getStorageDir() {
        return new File(this.storageDir);
    }

    /**
     * The collection of add-on subsystems that will be added to all
     * {@link AutoScaler} instances the {@link AutoScalerFactory} creates. These
     * add-on subsystems are not strictly necessary for the {@link AutoScaler}
     * to operate, but may extend it with additional functionality. Accounting
     * and high-availability are two examples of what such add-on subsystems
     * could achieve. Keys are names, such as {@code accountingSubsystem}, and
     * values are class names, such as
     * {@code com.elastisys.AccountingSubsystemImpl}.
     *
     * @return
     */
    public Map<String, String> getAddonSubsytems() {
        return Optional.ofNullable(this.addonSubsystems).orElse(Collections.emptyMap());
    }

    /**
     * Validates this {@link AutoScalerFactoryConfig}, verifying that the
     * storage directory is a valid directory path and that any add-on
     * subsystems can be instantiated. If validation fails, an
     * {@link IllegalArgumentException} is thrown.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        File file = new File(this.storageDir);
        if (file.exists()) {
            if (!file.isDirectory()) {
                throw new IllegalArgumentException(
                        format("autoScalerFactory: storageDir: '%s' does not refer to a directory", this.storageDir));
            }
        }

        // validate add-on subsystems
        for (Entry<String, String> addon : getAddonSubsytems().entrySet()) {
            String addonSubsystemName = addon.getKey();
            String addonSubsystemClassName = addon.getValue();
            try {
                AutoScalerBuilder.loadClass(addonSubsystemClassName, Service.class);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        format("autoScalerFactory: addonSubsystems: cannot load add-on subsystem %s: %s",
                                addonSubsystemName, e.getMessage()),
                        e);
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.storageDir, this.addonSubsystems);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AutoScalerFactoryConfig) {
            AutoScalerFactoryConfig that = (AutoScalerFactoryConfig) obj;
            return Objects.equals(this.storageDir, that.storageDir)
                    && Objects.equals(this.addonSubsystems, that.addonSubsystems);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    /**
     * Returns a default {@link AutoScalerFactoryConfig}.
     *
     * @return The default configuration.
     */
    public static AutoScalerFactoryConfig defaultConfig() {
        return new AutoScalerFactoryConfig(DEFAULT_STORAGE_DIR, null);
    }
}
