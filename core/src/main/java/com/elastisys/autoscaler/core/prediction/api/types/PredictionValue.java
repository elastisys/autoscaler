package com.elastisys.autoscaler.core.prediction.api.types;

import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Returns a predicted value. A prediction may, as indicated by its
 * {@link PredictionUnit}, represent a "raw" metric prediction (
 * {@link PredictionUnit#METRIC}) or a prediction of needed compute units (
 * {@link PredictionUnit#COMPUTE}).
 */
public class PredictionValue {

    /** The predicted value. */
    private final double value;
    /** The unit of the predicted value. */
    private final PredictionUnit unit;

    /**
     * Constructs a new {@link PredictionValue}.
     *
     * @param value
     *            The predicted value.
     * @param unit
     *            The unit of the predicted value.
     */
    public PredictionValue(double value, PredictionUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    public double getValue() {
        return this.value;
    }

    public PredictionUnit getUnit() {
        return this.unit;
    }

    /**
     * Returns a copy of this {@link PredictionValue} with the value field
     * replaced.
     *
     * @param value
     *            The value to use for the returned copy.
     * @return a copy of this {@link Prediction} with the specified field
     *         updated.
     */
    public PredictionValue withValue(double value) {
        return new PredictionValue(value, getUnit());
    }

    /**
     * Returns a copy of this {@link PredictionValue} with the unit field
     * replaced.
     *
     * @param unit
     *            The unit to use for the returned copy.
     * @return a copy of this {@link Prediction} with the specified field
     *         updated.
     */
    public PredictionValue withUnit(PredictionUnit unit) {
        return new PredictionValue(getValue(), unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value, this.unit);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PredictionValue) {
            PredictionValue that = (PredictionValue) obj;
            return Objects.equals(this.value, that.value) && Objects.equals(this.unit, that.unit);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
