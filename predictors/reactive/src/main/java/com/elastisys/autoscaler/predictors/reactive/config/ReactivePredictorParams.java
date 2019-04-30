package com.elastisys.autoscaler.predictors.reactive.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.autoscaler.core.prediction.impl.standard.config.PredictorConfig;
import com.elastisys.autoscaler.predictors.reactive.ReactivePredictor;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Class that represents the {@link ReactivePredictor}-specific parameters of
 * the {@link PredictorConfig}.
 * <p/>
 * That is, {@link ReactivePredictorParams} is the Java representation of the
 * {@link PredictorConfig#getParameters()} {@link JsonObject} for the
 * {@link ReactivePredictor}.
 */
public class ReactivePredictorParams {

    /**
     * A default {@link ReactivePredictorParams} to use when none is explicitly
     * set.
     */
    public static final ReactivePredictorParams DEFAULT = new ReactivePredictorParams(0.0);

    /**
     * The safety margin (in percent) to, optionally, add to predictions. This
     * adds some extra padding to every prediction to keep some margin to the
     * load curve.
     */
    private Double safetyMargin = null;

    /**
     * Creates a new {@link ReactivePredictorParams}.
     *
     * @param safetyMargin
     *            The safety margin (in percent) to add to predictions. This
     *            adds some extra padding to every prediction to keep some
     *            margin to the load curve. Can be <code>null</code>, which sets
     *            the margin to {@code 0.0}.
     */
    public ReactivePredictorParams(Double safetyMargin) {
        this.safetyMargin = safetyMargin;
    }

    /**
     * Returns the safety margin (in percent) to add to predictions which, if
     * specified, keeps some extra margin to the load curve.
     *
     * @return
     */
    public double getSafetyMargin() {
        return Optional.ofNullable(this.safetyMargin).orElse(0.0);
    }

    /**
     * Factory method that parses out an {@link ReactivePredictorParams} from a
     * JSON representation, or fails with a {@link JsonSyntaxException}.
     *
     * @param jsonConfig
     *            The JSON representation of the
     *            {@link ReactivePredictorParams}.
     * @return The parsed {@link ReactivePredictorParams}.
     */
    public static ReactivePredictorParams parse(JsonElement jsonConfig) {
        return JsonUtils.toObject(jsonConfig, ReactivePredictorParams.class);
    }

    /**
     * Performs basic validation of this {@link ReactivePredictorParams} and in
     * case verification fails, an {@link IllegalArgumentException} is thrown.
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(getSafetyMargin() >= 0.0, "safetyMargin must be a non-negative value");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ReactivePredictorParams) {
            ReactivePredictorParams that = (ReactivePredictorParams) obj;
            return Objects.equals(this.safetyMargin, that.safetyMargin);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.safetyMargin);
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
