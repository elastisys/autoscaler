package com.elastisys.autoscaler.server.restapi.types;

import java.util.Objects;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * REST API request/response type that represents a {@link Service} execution
 * {@link State}.
 */
public class StateType {

    /** The service {@link State}. */
    private State state;

    /**
     * Constructs a {@link StateType} that represents a certain {@link Service}
     * execution {@link State}.
     *
     * @param state
     *            The {@link Service} {@link State} to represent.
     */
    public StateType(State state) {
        this.state = state;
    }

    /**
     * Returns the {@link Service} {@link State}.
     *
     * @return
     */
    public State getState() {
        return this.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.state);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StateType) {
            StateType that = (StateType) obj;
            return Objects.equals(this.state, that.state);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
