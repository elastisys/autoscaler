package com.elastisys.autoscaler.metricstreamers.opentsdb.stream;

import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.PageFetchException;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultPage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;

/**
 * A {@link QueryResultSet} that is comprised of a number of (sub)queries which
 * are fetched one-at-a-time when {@link #fetchNext()} is called.
 */
public class LazyOpenTsdbResultSet implements QueryResultSet {

    private final Logger logger;
    private final List<QueryCall> subQueries;
    private int nextQuery = 0;

    public LazyOpenTsdbResultSet(Logger logger, List<QueryCall> subQueries) {
        this.logger = logger;
        this.subQueries = subQueries;
    }

    @Override
    public boolean hasNext() {
        return this.nextQuery < this.subQueries.size();
    }

    @Override
    public QueryResultPage fetchNext() throws NoSuchElementException, PageFetchException {
        if (!hasNext()) {
            throw new NoSuchElementException("result set has been exhausted");
        }
        int queryIndex = this.nextQuery++;

        try {
            this.logger.debug("running subquery {} out of {}", queryIndex + 1, this.subQueries.size());
            return new QueryResultPage(this.subQueries.get(queryIndex).call());
        } catch (Exception e) {
            throw new PageFetchException("failed to fetch result page: " + e.getMessage(), e);
        }
    }

}
