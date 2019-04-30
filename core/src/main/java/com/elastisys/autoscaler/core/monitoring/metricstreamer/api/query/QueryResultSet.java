package com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query;

import java.util.List;
import java.util.NoSuchElementException;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;

/**
 * Represents a paged result of a {@link MetricStream} query. Implementations
 * are recommended to fetch each response page lazily (not until requested) to
 * reduce memory load when large result sets are to be processed.
 * <p/>
 * Each {@link QueryResultPage} is a {@link List} of {@link MetricValue}s.
 *
 * @see MetricStream#query(org.joda.time.Interval, QueryOptions)
 */
public interface QueryResultSet {

    /**
     * Returns <code>true</code> if there are more {@link QueryResultPage}s to
     * fetch. If <code>true</code>, the next {@link QueryResultPage} can be
     * retrieved via a call to {@link #fetchNext()}.
     *
     * @return
     */
    boolean hasNext();

    /**
     * Returns the next {@link QueryResultPage}.
     * <p/>
     * If no more pages are available in the {@link QueryResultSet}, a
     * {@link NoSuchElementException} is thrown.
     * <p/>
     * Note that, depending on the implementation, this call may result in a
     * query being sent to a remote metric source to fetch the next result page,
     * which may block the calling thread for some time.
     *
     * @return
     * @throws NoSuchElementException
     *             If the {@link QueryResultSet} does not contain any more
     *             {@link QueryResultPage}s.
     * @throws PageFetchException
     *             If the next {@link QueryResultPage} could not be retrieved.
     */
    QueryResultPage fetchNext() throws NoSuchElementException, PageFetchException;
}
