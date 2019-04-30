package com.elastisys.autoscaler.core.prediction.impl.standard.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;

import com.elastisys.autoscaler.core.prediction.impl.standard.mapper.CapacityMapper;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * A single mapping to be used in the capacity prediction translation table to,
 * for a given metric, convert raw metric capacity into a number of compute
 * units.
 *
 * @see CapacityMapper
 */
public class CapacityMappingConfig {

    /** The metric that this capacity mapping applies to. */
    private final String metric;
    /**
     * The metric capacity of a single compute unit. This is the conversion rate
     * when converting from raw metric to compute unit.
     */
    private final Double amountPerComputeUnit;

    /**
     * Constructs a {@link CapacityMappingConfig}.
     *
     * @param metric
     *            The metric that this capacity mapping applies to.
     * @param amountPerComputeUnit
     *            The metric capacity of a single compute unit. This is the
     *            conversion rate when converting from raw metric to compute
     *            unit.
     */
    public CapacityMappingConfig(String metric, double amountPerComputeUnit) {
        this.metric = metric;
        this.amountPerComputeUnit = amountPerComputeUnit;
    }

    /**
     * Returns the metric that this capacity mapping applies to.
     *
     * @return
     */
    public String getMetric() {
        return this.metric;
    }

    /**
     * Returns rhe metric capacity of a single compute unit. This is the
     * conversion rate when converting from raw metric to compute unit.
     *
     * @return
     */
    public double getAmountPerComputeUnit() {
        return this.amountPerComputeUnit;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CapacityMappingConfig) {
            CapacityMappingConfig that = (CapacityMappingConfig) obj;
            return Objects.equals(this.metric, that.metric)
                    && Objects.equals(this.amountPerComputeUnit, that.amountPerComputeUnit);

        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.metric, this.amountPerComputeUnit);
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.metric != null, "capacityMapping: missing metric");
        checkArgument(this.amountPerComputeUnit != null, "capacityMapping: missing amountPerComputeUnit");
        checkArgument(this.amountPerComputeUnit > 0, "capacityMapping: amountPerComputeUnit must be > 0");
    }
}
