package com.elastisys.autoscaler.core.prediction.impl.standard.mapper;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.elastisys.autoscaler.core.prediction.impl.standard.api.CapacityTranslationTable;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.CapacityMappingConfig;

/**
 * A basic, {@link Map}-based implementation of a
 * {@link CapacityTranslationTable}.
 * <p/>
 * The class is thread safe and can be used concurrently by different
 * {@link Thread}s.
 */
class StandardCapacityTranslationTable implements CapacityTranslationTable {

    /**
     * The translation table holding metric conversion rates. Each key is a
     * metric and the value is the metric capacity of a single compute unit.
     */
    private final Map<String, Double> metricCapacityPerMachine;

    /**
     * Constructs a new {@link StandardCapacityTranslationTable} with an empty
     * set of initial capacity mappings.
     */
    public StandardCapacityTranslationTable() {
        this(new ArrayList<CapacityMappingConfig>());
    }

    /**
     * Constructs a new {@link StandardCapacityTranslationTable} with a given
     * set of initial capacity mappings.
     *
     * @param capacityMappings
     *            The initial set of capacity mappings.
     */
    public StandardCapacityTranslationTable(List<CapacityMappingConfig> capacityMappings) {
        requireNonNull(capacityMappings, "capacityMappings cannot be null");

        this.metricCapacityPerMachine = new ConcurrentHashMap<>();
        for (CapacityMappingConfig capacityMapping : capacityMappings) {
            requireNonNull(capacityMapping.getMetric(), "null metric for capacity mapping");
            checkArgument(capacityMapping.getAmountPerComputeUnit() > 0,
                    "non-positive machine capacity for capacity mapping '%s'", capacityMapping.getMetric());
            this.metricCapacityPerMachine.put(capacityMapping.getMetric(), capacityMapping.getAmountPerComputeUnit());
        }
    }

    @Override
    public double toComputeUnits(String metric, double capacity) {
        validateMetric(metric);
        checkArgument(capacity >= 0, "capacity must be >= 0");

        return capacity / this.metricCapacityPerMachine.get(metric);
    }

    @Override
    public double computeUnitCapacity(String metric) {
        validateMetric(metric);
        return this.metricCapacityPerMachine.get(metric);
    }

    /**
     * Checks the existence of a capacity mapping for a given metric. Throws a
     * {@link RuntimeException} if no mapping exists.
     *
     * @param metric
     */
    private void validateMetric(String metric) {
        checkArgument(containsMapping(metric), "unrecognized metric: '%s'", metric);
    }

    @Override
    public boolean containsMapping(String metric) {
        return this.metricCapacityPerMachine.containsKey(metric);
    }

    @Override
    public Set<String> metrics() {
        return Collections.unmodifiableSet(this.metricCapacityPerMachine.keySet());
    }

    @Override
    public void addMapping(String metric, double computeUnitCapacity) {
        validateMapping(metric, computeUnitCapacity);
        checkArgument(!this.metricCapacityPerMachine.containsKey(metric), "a mapping already exists for metric '%s'",
                metric);
        this.metricCapacityPerMachine.put(metric, computeUnitCapacity);
    }

    @Override
    public void setMappings(Map<String, Double> capacityMappings) {
        validateMappings(capacityMappings);
        this.metricCapacityPerMachine.clear();
        for (Entry<String, Double> mapping : capacityMappings.entrySet()) {
            String metric = mapping.getKey();
            Double computeUnitCapacity = mapping.getValue();
            this.metricCapacityPerMachine.put(metric, computeUnitCapacity);
        }
    }

    private void validateMappings(Map<String, Double> capacityMappings) {
        requireNonNull(capacityMappings, "capacityMappings cannot be null");
        for (Entry<String, Double> mapping : capacityMappings.entrySet()) {
            String metric = mapping.getKey();
            Double computeUnitCapacity = mapping.getValue();
            validateMapping(metric, computeUnitCapacity);
        }
    }

    private void validateMapping(String metric, Double computeUnitCapacity) {
        requireNonNull(metric, "null metric for capacity mapping");
        Objects.requireNonNull(computeUnitCapacity,
                String.format("null machine capacity for capacity mapping '%s'", metric));
        checkArgument(computeUnitCapacity > 0, "non-positive machine capacity for capacity mapping '%s'", metric);
    }

    @Override
    public void clear() {
        this.metricCapacityPerMachine.clear();
    }
}
