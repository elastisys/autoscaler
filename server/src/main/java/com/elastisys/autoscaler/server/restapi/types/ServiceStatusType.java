package com.elastisys.autoscaler.server.restapi.types;

import java.util.Objects;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.Health;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * REST API request/response type that represents a {@link ServiceStatus}.
 */
public class ServiceStatusType {
    /** The execution state of the {@link Service}. */
    private State state;
    /** The health of the {@link Service}. */
    private Health health;
    /**
     * The (optional) message that provides a more detailed health message. This
     * may be of particular interest when the {@link Service} is found to be in
     * bad {@link Health}.
     */
    private String healthDetail;

    /**
     * Constructs a {@link ServiceStatusType} that represents a certain
     * {@link ServiceStatus}.
     *
     * @param state
     * @param health
     * @param healthDetail
     */
    public ServiceStatusType(State state, Health health, String healthDetail) {
        this.state = state;
        this.health = health;
        this.healthDetail = healthDetail;
    }

    /**
     * Constructs a {@link ServiceStatusType} that represents a certain
     * {@link ServiceStatus}.
     *
     * @param serviceStatus
     *            The {@link ServiceStatus} to represent.
     */
    public ServiceStatusType(ServiceStatus serviceStatus) {
        this.state = serviceStatus.getState();
        this.health = serviceStatus.getHealth();
        this.healthDetail = serviceStatus.getHealthDetail();
    }

    /**
     * Returns the execution state of the {@link Service}.
     *
     * @return
     */
    public State getState() {
        return this.state;
    }

    /**
     * Returns the health of the {@link Service}.
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
     *
     * @return
     */
    public String getHealthDetail() {
        return this.healthDetail;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.state, this.health, this.healthDetail);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServiceStatusType) {
            ServiceStatusType that = (ServiceStatusType) obj;
            return Objects.equals(this.state, that.state) && Objects.equals(this.health, that.health)
                    && Objects.equals(this.healthDetail, that.healthDetail);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
