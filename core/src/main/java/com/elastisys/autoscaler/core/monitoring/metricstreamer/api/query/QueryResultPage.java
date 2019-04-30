package com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query;

import java.util.List;
import java.util.Objects;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Represents a single query result page in a {@link QueryResultSet}.
 *
 * @see QueryResultSet
 */
public class QueryResultPage {

    /** The collection of query results. */
    private final List<MetricValue> metricValues;

    /**
     * Creates a {@link QueryResultPage}.
     *
     * @param metricValues
     *            A collection of query results.
     */
    public QueryResultPage(List<MetricValue> metricValues) {
        this.metricValues = metricValues;
    }

    /**
     * The collection of query results.
     *
     * @return
     */
    public List<MetricValue> getMetricValues() {
        return this.metricValues;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.metricValues);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof QueryResultPage) {
            QueryResultPage that = (QueryResultPage) obj;
            return Objects.equals(this.metricValues, that.metricValues);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
