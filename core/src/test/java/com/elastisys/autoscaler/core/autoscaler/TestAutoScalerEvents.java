package com.elastisys.autoscaler.core.autoscaler;

import static com.elastisys.autoscaler.core.api.CorePredicates.hasStarted;
import static com.elastisys.autoscaler.core.autoscaler.AutoScalerTestUtils.assertDefaultComponents;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.core.api.types.ServiceStatus.Health;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactory;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactoryConfig;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.concurrent.Sleep;
import com.google.gson.JsonObject;

/**
 * Exercises the {@link AutoScaler} with respect to reacting to
 * {@link AutoScalerEvent}s, which can be sent on the {@link AutoScaler}
 * {@link EventBus} to trigger {@link AutoScaler} actions.
 */
public class TestAutoScalerEvents {
    /** Where autoscaler instance state will be stored. */
    private final static String storageDir = "target/autoscaler/instances";

    /** Object under test. */
    private AutoScaler autoScaler;

    @Before
    public void onSetup() throws Exception {
        // create
        JsonObject autoScalerBlueprint = parseJsonResource("autoscaler/autoscaler-blueprint.json");
        this.autoScaler = AutoScalerFactory.launch(new AutoScalerFactoryConfig(storageDir, null))
                .createAutoScaler(autoScalerBlueprint);
        assertDefaultComponents(this.autoScaler);
        // configure
        JsonObject config = parseJsonResource("autoscaler/autoscaler-config.json");
        this.autoScaler.validate(config);
        this.autoScaler.configure(config);
    }

    /**
     * A {@link AutoScalerEvent#STOP} event sent on the {@link EventBus} should
     * cause the {@link AutoScaler} to stop.
     */
    @Test
    public void stopEvent() {
        assertThat(this.autoScaler.getStatus().getState(), is(State.STOPPED));
        // start
        this.autoScaler.start();
        // check that all subsystems are go
        assertThat(this.autoScaler.getStatus().getState(), is(State.STARTED));
        assertTrue(this.autoScaler.getSubsystems().stream().allMatch(hasStarted()));
        assertThat(this.autoScaler.getStatus().getHealth(), is(Health.OK));
        assertThat(this.autoScaler.getStatus().getHealthDetail(), is(""));

        // send stop event
        this.autoScaler.getBus().post(AutoScalerEvent.STOP);
        // give event a chance to reach AutoScaler
        Sleep.forTime(150, TimeUnit.MILLISECONDS);

        // check that all subsystems are stopped
        assertThat(this.autoScaler.getStatus().getState(), is(State.STOPPED));
        assertTrue(this.autoScaler.getSubsystems().stream().allMatch(it -> !hasStarted().test(it)));
        assertThat(this.autoScaler.getStatus().getHealth(), is(Health.OK));
    }

    private JsonObject parseJsonResource(String resourceName) {
        return JsonUtils.parseJsonResource(resourceName).getAsJsonObject();
    }
}
