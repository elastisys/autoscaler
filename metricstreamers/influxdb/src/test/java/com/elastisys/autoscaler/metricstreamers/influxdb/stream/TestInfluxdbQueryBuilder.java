package com.elastisys.autoscaler.metricstreamers.influxdb.stream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.joda.time.Interval;
import org.junit.Test;

import com.elastisys.autoscaler.metricstreamers.influxdb.config.MetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.influxdb.config.Query;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercise the {@link InfluxdbQueryBuilder}.
 */
public class TestInfluxdbQueryBuilder {

    /**
     * Without a where-clause, a where clause with the time interval should
     * still be produced.
     */
    @Test
    public void withoutWhereClause() {
        Interval interval = new Interval(UtcTime.parse("2016-09-15T12:00:00.000Z"),
                UtcTime.parse("2016-09-15T13:00:00.000Z"));
        Query query = Query.builder().select("\"system\"").from("\"cpu\"").build();

        assertThat(InfluxdbQueryBuilder.buildQuery(streamDef(query), interval), is(
                "SELECT \"system\" FROM \"cpu\" WHERE '2016-09-15T12:00:00.000Z' <= time AND time <= '2016-09-15T13:00:00.000Z'"));
    }

    /**
     * When a where-clause is specified it should be extended with the time
     * interval to be queried.
     */
    @Test
    public void withWhereClause() {
        Interval interval = new Interval(UtcTime.parse("2016-09-15T12:00:00.000Z"),
                UtcTime.parse("2016-09-15T13:00:00.000Z"));
        Query query = Query.builder().select("\"system\"").from("\"cpu\"").where("region = 'us-east-1'").build();

        assertThat(InfluxdbQueryBuilder.buildQuery(streamDef(query), interval), is(
                "SELECT \"system\" FROM \"cpu\" WHERE '2016-09-15T12:00:00.000Z' <= time AND time <= '2016-09-15T13:00:00.000Z' AND region = 'us-east-1'"));
    }

    /**
     * When specifying a groupBy clause the built query must contain a
     * corresponding {@code GROUP BY} clause.
     */
    @Test
    public void withGroupByClause() {
        Interval interval = new Interval(UtcTime.parse("2016-09-15T12:00:00.000Z"),
                UtcTime.parse("2016-09-15T13:00:00.000Z"));
        Query query = Query.builder().select("non_negative_derivative(max(requests),1s)").from("nginx")
                .groupBy("time(5m) fill(none)").build();

        assertThat(InfluxdbQueryBuilder.buildQuery(streamDef(query), interval), is(
                "SELECT non_negative_derivative(max(requests),1s) FROM nginx WHERE '2016-09-15T12:00:00.000Z' <= time AND time <= '2016-09-15T13:00:00.000Z' GROUP BY time(5m) fill(none)"));
    }

    /**
     * Must be possible to give both a where- and group by-clause.
     */
    @Test
    public void withGroupByAndWhereClause() {
        Interval interval = new Interval(UtcTime.parse("2016-09-15T12:00:00.000Z"),
                UtcTime.parse("2016-09-15T13:00:00.000Z"));
        Query query = Query.builder().select("non_negative_derivative(max(requests),1s)").from("nginx")
                .where("region =~ /us-*/").groupBy("time(5m) fill(none)").build();

        assertThat(InfluxdbQueryBuilder.buildQuery(streamDef(query), interval), is(
                "SELECT non_negative_derivative(max(requests),1s) FROM nginx WHERE '2016-09-15T12:00:00.000Z' <= time AND time <= '2016-09-15T13:00:00.000Z' AND region =~ /us-*/ GROUP BY time(5m) fill(none)"));
    }

    /**
     * A query time interval must be specified.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withNullInterval() {
        Interval interval = null;
        Query query = Query.builder().select("\"system\"").from("\"cpu\"").build();

        InfluxdbQueryBuilder.buildQuery(streamDef(query), interval);
    }

    /**
     * A metric stream must be specified.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withNullStream() {
        Interval interval = new Interval(UtcTime.parse("2016-09-15T12:00:00.000Z"),
                UtcTime.parse("2016-09-15T13:00:00.000Z"));
        MetricStreamDefinition stream = null;

        InfluxdbQueryBuilder.buildQuery(stream, interval);
    }

    /**
     * Query must be present.
     */
    @Test(expected = IllegalArgumentException.class)
    public void withNullQuery() {
        Interval interval = new Interval(UtcTime.parse("2016-09-15T12:00:00.000Z"),
                UtcTime.parse("2016-09-15T13:00:00.000Z"));
        Query query = null;

        InfluxdbQueryBuilder.buildQuery(streamDef(query), interval);
    }

    /**
     * Query must be valid.
     */
    @Test(expected = IllegalArgumentException.class)
    public void queryMissingSelectClause() {
        Interval interval = new Interval(UtcTime.parse("2016-09-15T12:00:00.000Z"),
                UtcTime.parse("2016-09-15T13:00:00.000Z"));
        Query query = Query.builder().select(null).from("cpu").build();

        InfluxdbQueryBuilder.buildQuery(streamDef(query), interval);
    }

    /**
     * Query must be valid.
     */
    @Test(expected = IllegalArgumentException.class)
    public void queryMissingFromClause() {
        Interval interval = new Interval(UtcTime.parse("2016-09-15T12:00:00.000Z"),
                UtcTime.parse("2016-09-15T13:00:00.000Z"));
        Query query = Query.builder().select("request_rate").from(null).build();

        InfluxdbQueryBuilder.buildQuery(streamDef(query), interval);
    }

    /**
     * Returns a stream definition with a given query.
     *
     * @return
     */
    private MetricStreamDefinition streamDef(Query query) {
        TimeInterval dataSettlingTime = null;
        TimeInterval queryChunkSize = null;
        MetricStreamDefinition stream = new MetricStreamDefinition("id", "metricName", "db", query, dataSettlingTime,
                queryChunkSize);
        return stream;
    }

}
