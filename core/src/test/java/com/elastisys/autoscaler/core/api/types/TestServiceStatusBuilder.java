package com.elastisys.autoscaler.core.api.types;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import org.junit.Test;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.Health;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;

/**
 * Verifies the behavior of the {@link ServiceStatus.Builder} class.
 */
public class TestServiceStatusBuilder {

    @Test
    public void buildStartedAndHealthyService() {
        ServiceStatus status = new ServiceStatus.Builder().started(true).build();
        assertThat(status, is(new ServiceStatus(State.STARTED, Health.OK)));
    }

    @Test
    public void buildStoppedAndHealthyService() {
        ServiceStatus status = new ServiceStatus.Builder().started(false).build();
        assertThat(status, is(new ServiceStatus(State.STOPPED, Health.OK)));
    }

    @Test
    public void buildStartedAndUnhealthyService() {
        String expectedErrorMessage = "last error";
        ServiceStatus status = new ServiceStatus.Builder().started(true).lastFault(new Exception(expectedErrorMessage))
                .build();

        assertThat(status, is(new ServiceStatus(State.STARTED, Health.NOT_OK, Optional.of(expectedErrorMessage))));
    }

    @Test
    public void buildStoppedAndUnhealthyService() {
        String expectedErrorMessage = "last error";
        Exception lastException = new Exception(expectedErrorMessage);
        ServiceStatus expectedStatus = new ServiceStatus(State.STOPPED, Health.NOT_OK,
                Optional.of(expectedErrorMessage));

        // pass last failure to builder as Exception
        ServiceStatus status = new ServiceStatus.Builder().started(false).lastFault(lastException).build();
        assertThat(status, is(expectedStatus));

        // pass last failure to builder as Optional<Throwable>
        Optional<Exception> lastFailure = Optional.of(lastException);
        status = new ServiceStatus.Builder().started(false).lastFault(lastFailure).build();
        assertThat(status, is(expectedStatus));
    }

    @Test(expected = NullPointerException.class)
    public void buildWithMissingStartedField() {
        new ServiceStatus.Builder().build();
    }
}
