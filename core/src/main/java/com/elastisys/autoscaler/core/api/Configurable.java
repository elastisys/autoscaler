package com.elastisys.autoscaler.core.api;

/**
 * A {@link Configurable} is capable of validating and applying a configuration
 * object of a given type (the type parameter {@code T}). It can also return its
 * current configuration.
 *
 * @param <T>
 *            The type of the configuration objects accepted by this
 *            {@link Configurable}.
 */
public interface Configurable<T> {

    /**
     * Performs basic validation of a configuration. That is, checks that all
     * mandatory arguments are specified and that values are syntactically
     * well-formed and within permissible ranges.
     * <p/>
     * If the method returns successfully, the configuration has passed
     * validation. If the configuration is found to be invalid, an
     * {@link IllegalArgumentException} will be thrown.
     *
     * @param configuration
     *            The configuration object or <code>null</code> if an attempt is
     *            made to set an empty configuration.
     * @throws IllegalArgumentException
     *             If the configuration is found to be invalid.
     */
    void validate(T configuration) throws IllegalArgumentException;

    /**
     * Applies a given configuration to this {@link Configurable}.
     *
     * @param configuration
     *            The configuration object or <code>null</code> if an attempt is
     *            made to set an empty configuration.
     * @throws IllegalArgumentException
     *             If the configuration could not be applied.
     */
    void configure(T configuration) throws IllegalArgumentException;

    /**
     * Returns the configuration currently applied to this {@link Configurable}.
     * <p/>
     * A <code>null</code> return value represents a missing configuration.
     *
     * @return A configuration object.
     */
    T getConfiguration();

    /**
     * Returns the particular configuration {@link Class} that this
     * {@link Configurable} accepts.
     * <p/>
     * This method is used when the configuration class of this
     * {@link Configurable} needs to be determined (via introspection) at
     * runtime.
     *
     * @return The {@link Class}, whose instances can be passed to this
     *         {@link Configurable}.
     */
    Class<T> getConfigurationClass();
}
