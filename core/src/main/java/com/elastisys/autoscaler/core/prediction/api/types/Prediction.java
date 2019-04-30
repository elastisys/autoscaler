package com.elastisys.autoscaler.core.prediction.api.types;

import java.util.Objects;

import org.joda.time.DateTime;

import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Represents a capacity need prediction with respect to a given metric, as
 * predicted by a {@link Predictor}.
 * <p/>
 * The capacity prediction can either be expressed in "raw" metric form (as a
 * pure capacity need projection) or in terms of the number of compute units
 * that the predicted capacity need translates to. This unit of expression for
 * the prediction encoded in a {@link PredictionUnit} field.
 *
 * @see Predictor
 */
public class Prediction {

    /** The predicted capacity need. */
    private final double value;
    /** The unit that the predicted capacity need is expressed in. */
    private final PredictionUnit unit;
    /** The metric that the prediction is based on. */
    private final String metric;
    /** The time (in the future) for which the prediction applies. */
    private final DateTime timestamp;

    /**
     * Constructs a new capacity {@link Prediction}.
     *
     * @param value
     *            The predicted capacity need.
     * @param unit
     *            The unit that the predicted capacity need is expressed in.
     * @param metric
     *            The metric that the prediction is based on.
     * @param timestamp
     *            The time (in the future) for which the prediction applies.
     */
    public Prediction(double value, PredictionUnit unit, String metric, DateTime timestamp) {
        this.value = value;
        this.unit = unit;
        this.metric = metric;
        this.timestamp = timestamp;
    }

    /**
     * Returns the predicted capacity need.
     *
     * @return
     */
    public double getValue() {
        return this.value;
    }

    /**
     * The unit that the predicted capacity need is expressed in.
     *
     * @return
     */
    public PredictionUnit getUnit() {
        return this.unit;
    }

    /**
     * Returns the metric that the prediction is based on.
     *
     * @return
     */
    public String getMetric() {
        return this.metric;
    }

    /**
     * Returns the time (in the future) for which the prediction applies.
     *
     * @return
     */
    public DateTime getTimestamp() {
        return this.timestamp;
    }

    /**
     * Returns a copy of this {@link Prediction} with the predicted value
     * replaced.
     *
     * @param value
     *            The prediction value to use for the returned copy.
     * @return a copy of this {@link Prediction} with the specified field
     *         updated.
     */
    public Prediction withValue(double value) {
        return new Prediction(value, getUnit(), getMetric(), getTimestamp());
    }

    /**
     * Returns a copy of this {@link Prediction} with the predicted value's unit
     * replaced.
     *
     * @param unit
     *            The prediction value unit to use for the returned copy.
     * @return a copy of this {@link Prediction} with the specified field
     *         updated.
     */
    public Prediction withUnit(PredictionUnit unit) {
        return new Prediction(getValue(), unit, getMetric(), getTimestamp());
    }

    /**
     * Returns a copy of this {@link Prediction} with the metric field replaced.
     *
     * @param metric
     *            The metric field to use for the returned copy.
     * @return a copy of this {@link Prediction} with the specified field
     *         updated.
     */
    public Prediction withMetric(String metric) {
        return new Prediction(getValue(), getUnit(), metric, getTimestamp());
    }

    /**
     * Returns a copy of this {@link Prediction} with the timestamp field
     * replaced.
     *
     * @param timestamp
     *            The timestamp field to use for the returned copy.
     * @return a copy of this {@link Prediction} with the specified field
     *         updated.
     */
    public Prediction withTimestamp(DateTime timestamp) {
        return new Prediction(getValue(), getUnit(), getMetric(), timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Prediction) {
            Prediction that = (Prediction) obj;
            return Objects.equals(this.value, that.value) //
                    && Objects.equals(this.unit, that.unit) //
                    && Objects.equals(this.metric, that.metric) //
                    && Objects.equals(this.timestamp, that.timestamp);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
