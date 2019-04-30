package com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.impl;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultPage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;

/**
 * Represents a {@link QueryResultSet} with only a single result page.
 */
public class SinglePageResultSet implements QueryResultSet {

    private final Iterator<List<MetricValue>> resultPages;

    public SinglePageResultSet(List<MetricValue> queryResults) {
        checkArgument(queryResults != null, "queryResults cannot be null");
        this.resultPages = Arrays.asList(queryResults).iterator();
    }

    @Override
    public boolean hasNext() {
        return this.resultPages.hasNext();
    }

    @Override
    public QueryResultPage fetchNext() throws NoSuchElementException {
        return new QueryResultPage(this.resultPages.next());
    }

}
