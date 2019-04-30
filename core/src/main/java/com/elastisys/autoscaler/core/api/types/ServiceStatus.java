package com.elastisys.autoscaler.core.api.types;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Represents the status of a system {@link Service}.
 * <p/>
 * A distinction is made between the execution {@link State} of the service (if
 * it is started/stopped) and the {@link Health} of the service (if the service
 * is experiencing problems).
 * <p/>
 * The service {@link Health}, which may change over the lifetime of a service,
 * is only applicable for a running service. If the service {@link Health} is
 * poor {@link Health#NOT_OK}, additional details may (optionally) be provided
 * through {@link #getHealthDetail()}.
 *
 * @see Service
 *
 *
 */
public class ServiceStatus {
    /** The execution state of the {@link Service}. */
    private final State state;
    /** The health of the {@link Service}. */
    private final Health health;
    /**
     * The (optional) message that provides a more detailed health message. This
     * may be of particular interest when the {@link Service} is found to be in
     * bad {@link Health}. An empty {@link String} is used to represent that no
     * additional health detail is available.
     */
    private final String healthDetail;

    /** The allowed values for the execution state of a {@link Service}. */
    public static enum State {
        STARTED, STOPPED
    }

    /** The allowed values for the health of a {@link Service}. */
    public static enum Health {
        OK, NOT_OK
    }

    /**
     * Constructs a new {@link ServiceStatus}.
     *
     * @param state
     *            The execution state of the {@link Service}.
     * @param health
     *            The health of the {@link Service}.
     */
    public ServiceStatus(State state, Health health) {
        this.state = state;
        this.health = health;
        this.healthDetail = "";
    }

    /**
     * Constructs a new {@link ServiceStatus}.
     *
     * @param state
     *            The execution state of the {@link Service}.
     * @param health
     *            The health of the {@link Service}.
     * @param healthDetail
     *            The (optional) message that provides a more detailed health
     *            message. This may be of particular interest when the
     *            {@link Service} is found to be in bad {@link Health}.
     */
    public ServiceStatus(State state, Health health, Optional<String> healthDetail) {
        this.state = state;
        this.health = health;
        this.healthDetail = healthDetail.orElse("");
    }

    /**
     * Returns the current execution state of the {@link Service}.
     *
     * @return
     */
    public State getState() {
        return this.state;
    }

    /**
     * Returns the current health of the {@link Service}.
     *
     * @return
     */
    public Health getHealth() {
        return this.health;
    }

    /**
     * Returns the (optional) message that provides a more detailed health
     * message. This may be of particular interest when the {@link Service} is
     * found to be in bad {@link Health}.
     * <p/>
     * In case no additional health detail is available, an empty {@link String}
     * is returned.
     *
     * @return
     */
    public String getHealthDetail() {
        return this.healthDetail;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServiceStatus) {
            ServiceStatus that = (ServiceStatus) obj;
            return Objects.equals(this.state, that.state) && Objects.equals(this.health, that.health)
                    && Objects.equals(this.healthDetail, that.healthDetail);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.state, this.health, this.healthDetail);
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }

    /**
     * A builder class for constructing {@link ServiceStatus} instances.
     *
     *
     *
     */
    public static class Builder {
        /**
         * <code>true</code> if the {@link Service} is started,
         * <code>false</code> otherwise.
         */
        private Boolean started = null;
        private Optional<? extends Throwable> lastFailure = Optional.empty();

        public Builder() {
        }

        /**
         * Constructs a {@link ServiceStatus} instance from the parameters set
         * in the {@link Builder}.
         *
         * @return A new {@link ServiceStatus} instance.
         */
        public ServiceStatus build() {
            requireNonNull(this.started, "ServiceStatus missing started value");

            State state = this.started ? State.STARTED : State.STOPPED;
            Health health = Health.OK;
            Optional<String> healthDetail = Optional.empty();
            if (this.lastFailure.isPresent()) {
                health = Health.NOT_OK;
                String errorMessage = Optional.ofNullable(this.lastFailure.get().getMessage()).orElse("");
                healthDetail = Optional.of(errorMessage);
            }
            return new ServiceStatus(state, health, healthDetail);
        }

        /**
         * Set the {@link State} of the {@link Service}.
         *
         * @param isStarted
         *            <code>true</code> if the {@link Service} is started,
         *            <code>false</code> otherwise.
         * @return The {@link Builder} instance.
         */
        public Builder started(boolean isStarted) {
            this.started = isStarted;
            return this;
        }

        /**
         * Set the last failure of the {@link Service}.
         * <p/>
         * Note that if a last failure is present, the {@link Builder} will
         * create a {@link ServiceStatus} instance with a {@link Health} that is
         * {@link Health#NOT_OK}.
         *
         * @param lastFault
         *            The last failure of the {@link Service}.
         * @return
         */
        public Builder lastFault(Throwable lastFault) {
            requireNonNull(lastFault, "null not allowed");
            this.lastFailure = Optional.of(lastFault);
            return this;
        }

        /**
         * Set the last failure of the {@link Service} as an {@link Optional}.
         * <p/>
         * Note that if a last failure is present, the {@link Builder} will
         * create a {@link ServiceStatus} instance with a {@link Health} that is
         * {@link Health#NOT_OK}.
         *
         * @param lastFault
         *            The last failure of the {@link Service}.
         * @return
         */
        public Builder lastFault(Optional<? extends Throwable> lastFault) {
            requireNonNull(lastFault, "null not allowed");
            this.lastFailure = lastFault;
            return this;
        }
    }
}
