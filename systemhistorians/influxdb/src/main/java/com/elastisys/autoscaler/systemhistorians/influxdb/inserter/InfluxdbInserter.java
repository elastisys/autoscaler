package com.elastisys.autoscaler.systemhistorians.influxdb.inserter;

import java.util.Collection;

import com.elastisys.autoscaler.core.api.types.MetricValue;

/**
 * An {@link InfluxdbInserter} can pass {@link MetricValue}s to an InfluxDB
 * server according to some protocol.
 */
public interface InfluxdbInserter {

    /**
     * Inserts a collection of {@link MetricValue}s into InfluxDB. On failure,
     * an {@link InfluxdbInserterException} is thrown.
     *
     * @param values
     *            The values to be inserted.
     * @throws InfluxdbInserterException
     *             if the insert failed
     */
    void insert(Collection<MetricValue> values) throws InfluxdbInserterException;
}
