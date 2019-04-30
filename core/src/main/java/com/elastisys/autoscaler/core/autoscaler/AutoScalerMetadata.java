package com.elastisys.autoscaler.core.autoscaler;

import static com.elastisys.scale.commons.json.JsonUtils.toJson;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.util.collection.Maps;
import com.google.gson.JsonElement;

/**
 * Convenience class for producing standard {@link Alert} metadata tags to add
 * to {@link Alert}s sent from a given {@link AutoScaler} instance, to
 * discriminate it from {@link Alert}s sent by other {@link AutoScaler}
 * instances.
 */
public class AutoScalerMetadata {
    private static final Logger LOG = LoggerFactory.getLogger(AutoScalerMetadata.class);

    /** Autoscaler id/name tag. */
    public static final String AUTOSCALER_ID_TAG = "autoScalerId";
    /** Autoscaler UUID tag. */
    public static final String AUTOSCALER_UUID_TAG = "autoScalerUuid";

    /**
     * Produces a collection of metadata tags to be added to {@link Alert}s sent
     * from an {@link AutoScaler} with the given identifiers. These standard
     * metadata tags help distinguish {@link Alert}s produced by different
     * {@link AutoScaler} instances.
     *
     * @param autoScalerUuid
     *            The UUID of the {@link AutoScaler} instance.
     * @param autoScalerId
     *            The id/name of the {@link AutoScaler} instance.
     * @return
     */
    public static Map<String, JsonElement> alertTags(UUID autoScalerUuid, String autoScalerId) {
        Map<String, JsonElement> metadata = Maps.of(//
                AUTOSCALER_ID_TAG, toJson(autoScalerId), //
                AUTOSCALER_UUID_TAG, toJson(autoScalerUuid.toString()));
        return metadata;
    }

    /**
     * Produces a collection of metadata tags to be added to system metrics sent
     * from an {@link AutoScaler} with the given identifiers. These standard
     * metadata tags help distinguish metrics produced by different
     * {@link AutoScaler} instances.
     *
     * @param autoScalerUuid
     * @param autoScalerId
     * @return
     */
    public static Map<String, String> metricTags(UUID autoScalerUuid, String autoScalerId) {
        Map<String, String> metadata = Maps.of(//
                AUTOSCALER_ID_TAG, autoScalerId, //
                AUTOSCALER_UUID_TAG, autoScalerUuid.toString());
        return metadata;
    }
}
