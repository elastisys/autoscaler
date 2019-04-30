package com.elastisys.autoscaler.core.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.function.Predicate;

import org.junit.Test;

import com.elastisys.autoscaler.core.api.CorePredicates;
import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;

/**
 * Verifies the behavior of the {@link Predicate}s in {@link CorePredicates}.
 */
public class TestCorePredicates {

    private final Service<Object> service = mock(Service.class);

    @Test
    public void testServiceStartedPredicate() {
        // state: null
        when(this.service.getStatus()).thenReturn(null);
        assertFalse(CorePredicates.hasStarted().test(this.service));

        // state: started
        when(this.service.getStatus()).thenReturn(stateStarted());
        assertTrue(CorePredicates.hasStarted().test(this.service));

        // state: stopped
        when(this.service.getStatus()).thenReturn(stateStopped());
        assertFalse(CorePredicates.hasStarted().test(this.service));
    }

    @Test
    public void testServiceUnhealthyPredicate() {
        // state: null
        when(this.service.getStatus()).thenReturn(null);
        assertFalse(CorePredicates.isUnhealthy().test(this.service));

        // state: healthy
        when(this.service.getStatus()).thenReturn(stateHealthy());
        assertFalse(CorePredicates.isUnhealthy().test(this.service));

        // state: unhealthy
        when(this.service.getStatus()).thenReturn(stateUnhealthy());
        assertTrue(CorePredicates.isUnhealthy().test(this.service));
    }

    @Test
    public void testServiceConfiguredPredicate() {
        // config: throw exception
        when(this.service.getConfiguration()).thenThrow(new IllegalStateException());
        assertFalse(CorePredicates.isConfigured().test(this.service));

        // config: null
        reset(this.service);
        when(this.service.getConfiguration()).thenReturn(null);
        assertFalse(CorePredicates.isConfigured().test(this.service));

        // config: not null
        when(this.service.getConfiguration()).thenReturn(new Object());
        assertTrue(CorePredicates.isConfigured().test(this.service));
    }

    private ServiceStatus stateStarted() {
        return new ServiceStatus.Builder().started(true).build();
    }

    private ServiceStatus stateStopped() {
        return new ServiceStatus.Builder().started(false).build();
    }

    private ServiceStatus stateHealthy() {
        return stateStarted();
    }

    private ServiceStatus stateUnhealthy() {
        return new ServiceStatus.Builder().started(true).lastFault(new RuntimeException()).build();
    }

}
