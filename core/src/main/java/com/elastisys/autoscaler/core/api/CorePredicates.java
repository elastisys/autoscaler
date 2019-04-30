package com.elastisys.autoscaler.core.api;

import java.util.Optional;
import java.util.function.Predicate;

import com.elastisys.autoscaler.core.api.types.ServiceStatus.Health;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;

/**
 * A utility class that contains {@link Predicate}s for core API entities.
 */
public class CorePredicates {

    private CorePredicates() {
        throw new RuntimeException(CorePredicates.class.getName() + " is not instantiable.");
    }

    /**
     * Returns a {@link Predicate} that returns <code>true</code> for any
     * {@link Service} that is in {@link State#STARTED}.
     *
     * @return
     */
    public static Predicate<? super Service> hasStarted() {
        return service -> service.getStatus() != null && service.getStatus().getState() == State.STARTED;
    }

    /**
     * Returns a {@link Predicate} that returns <code>true</code> for any
     * {@link Service} that is in bad health: {@link Health#NOT_OK}.
     *
     * @return
     */
    public static Predicate<? super Service> isUnhealthy() {
        return service -> service.getStatus() != null && service.getStatus().getHealth() == Health.NOT_OK;
    }

    /**
     * Returns a {@link Predicate} that returns <code>true</code> for any
     * {@link Service} that has been configured (that is, its
     * {@link Service#getConfiguration()} method returns successfully with a
     * non-<code>null</code> result).
     *
     * @return
     */
    public static Predicate<? super Configurable> isConfigured() {
        return service -> {
            try {
                return service.getConfiguration() != null;
            } catch (Exception e) {
                return false;
            }
        };
    }

    /**
     * Returns a {@link Predicate} that returns <code>true</code> for any
     * {@link Optional} with a value set.
     *
     * @return
     */
    public static Predicate<? super Optional> isPresent() {
        return it -> it.isPresent();
    }
}
