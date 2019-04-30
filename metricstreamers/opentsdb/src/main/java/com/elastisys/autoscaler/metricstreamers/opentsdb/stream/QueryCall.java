package com.elastisys.autoscaler.metricstreamers.opentsdb.stream;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.joda.time.Interval;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.metricstreamers.opentsdb.client.OpenTsdbQueryClient;

/**
 * Executes a single remote query against a particular OpenTSDB server.
 */
public class QueryCall implements Callable<List<MetricValue>> {

    /** The client that will execute the query. */
    private final OpenTsdbQueryClient queryClient;
    /** The full OpenTSDB query URL. */
    private final String queryUrl;
    /**
     * The interval that the query is intended to cover. OpenTSDB sometimes
     * returns to many data points, so the {@link QueryCall} takes care of
     * filtering out any data points outside of this interval.
     */
    private final Interval queryInterval;

    /**
     * Creates a {@link QueryCall}.
     *
     * @param queryClient
     *            The client that will execute the query.
     * @param queryUrl
     *            The full OpenTSDB query URL.
     * @param queryInterval
     *            The interval that the query is intended to cover. OpenTSDB
     *            sometimes returns to many data points, so the
     *            {@link QueryCall} takes care of filtering out any data points
     *            outside of this interval.
     */
    public QueryCall(OpenTsdbQueryClient queryClient, String queryUrl, Interval queryInterval) {
        this.queryClient = queryClient;
        this.queryUrl = queryUrl;
        this.queryInterval = queryInterval;
    }

    @Override
    public List<MetricValue> call() throws Exception {
        List<MetricValue> unfiltered = this.queryClient.query(this.queryUrl);
        // OpenTSDB has a habit of returning too many data points.
        // Only include the ones within the query interval.
        List<MetricValue> filtered = unfiltered.stream()
                .filter(datapoint -> this.queryInterval.contains(datapoint.getTime())).collect(Collectors.toList());
        Collections.sort(filtered);
        return filtered;
    }

}
