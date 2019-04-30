package com.elastisys.autoscaler.core.alerter.impl.standard;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.elastisys.autoscaler.core.autoscaler.AutoScalerMetadata;
import com.google.gson.JsonElement;

/**
 * Exercises the {@link AutoScalerMetadata} class.
 */
public class TestAlertMetadata {

    @Test
    public void autoScalerTags() {
        UUID autoScalerUuid = UUID.randomUUID();
        String autoScalerId = "autoScalerId";

        Map<String, JsonElement> tags = AutoScalerMetadata.alertTags(autoScalerUuid, autoScalerId);

        assertTrue(tags.containsKey(AutoScalerMetadata.AUTOSCALER_ID_TAG));
        assertThat(tags.get(AutoScalerMetadata.AUTOSCALER_ID_TAG).getAsString(), is(autoScalerId));

        assertTrue(tags.containsKey(AutoScalerMetadata.AUTOSCALER_UUID_TAG));
        assertThat(tags.get(AutoScalerMetadata.AUTOSCALER_UUID_TAG).getAsString(), is(autoScalerUuid.toString()));
    }
}
