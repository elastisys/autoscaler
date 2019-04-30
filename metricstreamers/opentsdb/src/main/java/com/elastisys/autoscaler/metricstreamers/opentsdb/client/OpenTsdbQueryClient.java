package com.elastisys.autoscaler.metricstreamers.opentsdb.client;

import java.util.List;

import com.elastisys.autoscaler.core.api.types.MetricValue;

/**
 * A client capable of running queries against an OpenTSDB endpoint over the
 * <a href="http://opentsdb.net/http-api.html">OpenTSDB HTTP API</a>.
 * <p/>
 * Returned {@link MetricValue}s are always sorted in increasing order of time
 * stamp.
 * 
 * 
 */
public interface OpenTsdbQueryClient {

    /**
     * Executes a query against an OpenTSDB server.
     * <p/>
     * The returned {@link MetricValue}s are sorted in order of increasing time
     * stamp.
     * 
     * @param queryUrl
     *            A complete OpenTSDB query URL. The query URL must adhere to
     *            the <a href="http://opentsdb.net/http-api.html">OpenTSDB HTTP
     *            API</a>.
     * @return The list of metric values that the query generated, sorted in
     *         order of increasing time.
     * @throws Exception
     *             If the query execution failed.
     */
    List<MetricValue> query(String queryUrl) throws Exception;
}