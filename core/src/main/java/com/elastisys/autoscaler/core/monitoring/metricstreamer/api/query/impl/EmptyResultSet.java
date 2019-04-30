package com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.impl;

import java.util.NoSuchElementException;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultPage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;

/**
 * Represents an empty {@link QueryResultSet}.
 */
public class EmptyResultSet implements QueryResultSet {

    public EmptyResultSet() {
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public QueryResultPage fetchNext() throws NoSuchElementException {
        throw new NoSuchElementException("response set is empty");
    }
}
