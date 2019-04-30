package com.elastisys.autoscaler.core.prediction.impl.standard.api;

import java.util.Map;
import java.util.Set;

/**
 * A {@link CapacityTranslationTable} is capable of converting a capacity value,
 * for a given metric, into the corresponding number of compute units needed to
 * supply that capacity.
 * <p/>
 * As such, it can be regarded as a translation table that, for a set of
 * supported metrics, tracks the capacity of each metric that a compute unit is
 * able to supply (see {@link #computeUnitCapacity(String)}). This is the
 * conversion rate used to convert between "raw" metric capacity and compute
 * units (machines).
 * 
 * 
 */
public interface CapacityTranslationTable {

    /**
     * Translates a given metric capacity into the corresponding number of
     * compute units (machines) needed to supply that amount of capacity.
     * 
     * @param metric
     *            The metric of interest.
     * @param capacity
     *            The amount of capacity to be converted.
     * @return The number of compute units (machines) needed to supply
     *         {@code capacity} amount of metric.
     */
    double toComputeUnits(String metric, double capacity);

    /**
     * Returns the capacity of a single compute unit (machine) with respect to a
     * given metric.
     * 
     * @param metric
     *            The metric of interest.
     * @return the capacity of a single compute unit (machine) with respect to
     *         {@code metric}.
     */
    double computeUnitCapacity(String metric);

    /**
     * Returns <code>true</code> if this {@link CapacityTranslationTable} has a
     * mapping for a given metric, <code>false</code> if not.
     * 
     * @param metric
     *            The metric of interest.
     * @return <code>true</code> if this {@link CapacityTranslationTable} has a
     *         mapping for a given metric.
     */
    boolean containsMapping(String metric);

    /**
     * Returns the {@link Set} of metrics for which this
     * {@link CapacityTranslationTable} provides mappings.
     * 
     * @return the {@link Set} of metrics for which this
     *         {@link CapacityTranslationTable} provides mappings.
     */
    Set<String> metrics();

    /**
     * Sets a new collection of capacity translation mappings for this
     * {@link CapacityTranslationTable}.
     * <p/>
     * Any existing mappings are cleared before setting the new mappings.
     * 
     * @param capacityMappings
     *            A {@link Map}-representation of the translation table holding
     *            metric conversion rates. Each key is a metric and the value is
     *            the metric capacity of a single compute unit.
     * @throws IllegalArgumentException
     *             if the passed capacity mappings are invalid
     */
    void setMappings(Map<String, Double> capacityMappings);

    /**
     * Adds a conversion entry to the {@link CapacityTranslationTable} for a
     * given metric. Along with the metric, a conversion rate is included,
     * specifying the capacity that a single compute unit can supply.
     * 
     * @param metric
     *            A metric.
     * @param computeUnitCapacity
     *            The metric capacity of a single compute unit (machine).
     * @throws IllegalArgumentException
     *             if a mapping for the given metric already exists
     */
    void addMapping(String metric, double computeUnitCapacity);

    /**
     * Clears this {@link CapacityTranslationTable}, removing all conversion
     * entries.
     */
    void clear();

}