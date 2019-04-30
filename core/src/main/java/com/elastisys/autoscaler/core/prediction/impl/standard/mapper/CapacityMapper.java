package com.elastisys.autoscaler.core.prediction.impl.standard.mapper;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.Configurable;
import com.elastisys.autoscaler.core.prediction.api.types.Prediction;
import com.elastisys.autoscaler.core.prediction.api.types.PredictionUnit;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.CapacityTranslationTable;
import com.elastisys.autoscaler.core.prediction.impl.standard.api.Predictor;
import com.elastisys.autoscaler.core.prediction.impl.standard.config.CapacityMappingConfig;
import com.google.inject.TypeLiteral;

/**
 * The {@link CapacityMapper} converts raw metric capacity predictions produced
 * by {@link Predictor}s to predictions expressed in compute units.
 * <p/>
 * It makes use of (and implements itself) a {@link CapacityTranslationTable},
 * provided via configuration, that holds the conversion rate for each metric.
 * Each key is a metric and the value is the metric capacity of a single compute
 * unit.
 */
public class CapacityMapper implements Configurable<List<CapacityMappingConfig>>, CapacityTranslationTable {
    private final Logger logger;

    /**
     * A translation table capable of converting metric values into compute
     * units.
     */
    private final CapacityTranslationTable mappingTable;

    /** The current configuration of the {@link CapacityMapper}. */
    private List<CapacityMappingConfig> config = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new {@link CapacityMapper}
     *
     * @param logger
     */
    @Inject
    public CapacityMapper(Logger logger) {
        this.logger = logger;
        this.mappingTable = new StandardCapacityTranslationTable();
    }

    @Override
    public void validate(List<CapacityMappingConfig> configuration) throws IllegalArgumentException {
        checkArgument(configuration != null, "capacityMappings: configuration cannot be null");
        try {
            for (CapacityMappingConfig mappingConfig : configuration) {
                mappingConfig.validate();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("capacityMappings: " + e.getMessage(), e);
        }
    }

    @Override
    public void configure(List<CapacityMappingConfig> configuration) throws IllegalArgumentException {
        validate(configuration);

        setMappings(asMap(configuration));
        this.config = new CopyOnWriteArrayList<>(configuration);
    }

    private Map<String, Double> asMap(List<CapacityMappingConfig> capacityMappings) {
        Map<String, Double> map = new HashMap<>();
        for (CapacityMappingConfig capacityMapping : capacityMappings) {
            map.put(capacityMapping.getMetric(), capacityMapping.getAmountPerComputeUnit());
        }
        return map;
    }

    @Override
    public List<CapacityMappingConfig> getConfiguration() {
        return this.config;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<List<CapacityMappingConfig>> getConfigurationClass() {
        TypeLiteral<List<CapacityMappingConfig>> configType = new TypeLiteral<List<CapacityMappingConfig>>() {
        };
        return (Class<List<CapacityMappingConfig>>) configType.getRawType();
    }

    /**
     * Translates a capacity {@link Prediction} (in raw metric form) to a
     * corresponding capacity prediction measured in compute units. The
     * conversion is carried out using the {@link CapacityMappingConfig}s that
     * are currently configured for this {@link CapacityMapper}.
     * <p/>
     * If the {@link Prediction} is already in compute units, no conversion is
     * carried out.
     * <p/>
     * The method will raise an exception if an attempt is made to convert a
     * prediction in a metric for which the {@link CapacityMapper} doesn't have
     * a mapping configured.
     *
     * @param prediction
     *            The prediction, which may either be expressed in
     *            {@link PredictionUnit#METRIC} unit or in
     *            {@link PredictionUnit#COMPUTE} unit. In the latter case, no
     *            conversion is carried out.
     * @return The converted {@link Prediction} expressed in
     *         {@link PredictionUnit#COMPUTE} unit.
     * @throws IllegalArgumentException
     *             If an attempt is made to convert a prediction in a metric for
     *             which the {@link CapacityMapper} doesn't have a mapping
     *             configured.
     */
    public Optional<Prediction> toComputeUnits(Optional<Prediction> prediction) throws IllegalArgumentException {
        if (!prediction.isPresent()) {
            return Optional.empty();
        }

        Prediction pred = prediction.get();
        if (pred.getUnit() == PredictionUnit.COMPUTE) {
            // no conversion necessary
            return prediction;
        }

        String metric = pred.getMetric();
        checkArgument(containsMapping(metric),
                "capacity mapping table does not contain any " + "mapping for metric '%s'", metric);

        Double computeUnits = toComputeUnits(metric, pred.getValue());
        Prediction computeUnitPrediction = pred.withUnit(PredictionUnit.COMPUTE).withValue(computeUnits);
        return Optional.of(computeUnitPrediction);
    }

    @Override
    public double toComputeUnits(String metric, double capacity) {
        return this.mappingTable.toComputeUnits(metric, capacity);
    }

    @Override
    public double computeUnitCapacity(String metric) {
        return this.mappingTable.computeUnitCapacity(metric);
    }

    @Override
    public boolean containsMapping(String metric) {
        return this.mappingTable.containsMapping(metric);
    }

    @Override
    public Set<String> metrics() {
        return this.mappingTable.metrics();
    }

    @Override
    public void setMappings(Map<String, Double> capacityMappings) {
        this.mappingTable.setMappings(capacityMappings);
    }

    @Override
    public void addMapping(String metric, double computeUnitCapacity) {
        this.mappingTable.addMapping(metric, computeUnitCapacity);
    }

    @Override
    public void clear() {
        this.mappingTable.clear();
    }

}
