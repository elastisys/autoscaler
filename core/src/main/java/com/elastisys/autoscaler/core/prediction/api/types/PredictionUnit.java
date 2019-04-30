package com.elastisys.autoscaler.core.prediction.api.types;

public enum PredictionUnit {
    /** Capacity prediction is expressed in terms of the (raw) metric. */
    METRIC,
    /** Capacity prediction is expressed as a number of compute units. */
    COMPUTE
}
