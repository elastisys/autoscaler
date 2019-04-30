package com.elastisys.autoscaler.systemhistorians.opentsdb;

import com.elastisys.autoscaler.core.api.types.MetricValue;

/**
 * Inserts {@link MetricValue}s into an OpenTSDB server.
 * 
 * 
 */
public interface OpenTsdbInserter {

    /**
     * Inserts a single {@link MetricValue} into a backing OpenTSDB server.
     * 
     * @param value
     *            The data point to be inserted.
     * @throws OpenTsdbException
     *             Thrown if the insert operation fails.
     */
    void insert(MetricValue value) throws OpenTsdbException;
}