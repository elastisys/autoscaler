package com.elastisys.autoscaler.metricstreamers.influxdb.stream;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.io.StringWriter;

import org.joda.time.Interval;

import com.elastisys.autoscaler.metricstreamers.influxdb.config.MetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.Query;

/**
 * A builder of InfluxDB {@code SELECT} queries, used by an
 * {@link InfluxdbFetcher}.
 *
 * @see InfluxdbFetcher
 */
class InfluxdbQueryBuilder {

    /**
     * Builds an InfluxDB query language {@code SELECT} statement for a given
     * metric stream and time interval.
     *
     * @param stream
     *            The metric stream that defines how to query InfluxDB.
     * @param interval
     *            The time interval that the query should span.
     * @return A {@code SELECT} statement that can be used to query InfluxDB.
     */
    public static String buildQuery(MetricStreamDefinition stream, Interval interval) {
        checkArgument(stream != null, "buildQuery: stream cannot be null");
        checkArgument(interval != null, "buildQuery: interval cannot be null");
        checkArgument(stream.getQuery() != null, "buildQuery: stream query cannot be null");
        try {
            stream.getQuery().validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("buildQuery: " + e.getMessage(), e);
        }

        Query query = stream.getQuery();
        StringWriter q = new StringWriter();
        // SELECT clause
        q.append(String.format("SELECT %s", query.getSelect()));

        // FROM clause
        q.append(String.format(" FROM %s", query.getFrom()));

        // WHERE clause
        // add time constraints
        String whereClause = String.format("'%s' <= time AND time <= '%s'", interval.getStart(), interval.getEnd());
        if (query.getWhere() != null) {
            whereClause += " AND " + query.getWhere();
        }
        q.append(String.format(" WHERE %s", whereClause));

        if (query.getGroupBy() != null) {
            q.append(String.format(" GROUP BY %s", query.getGroupBy()));
        }

        return q.toString();
    }
}
