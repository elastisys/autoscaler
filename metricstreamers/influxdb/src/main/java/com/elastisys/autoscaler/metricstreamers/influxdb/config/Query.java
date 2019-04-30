package com.elastisys.autoscaler.metricstreamers.influxdb.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static java.util.Arrays.asList;

import java.util.Objects;

import com.elastisys.autoscaler.metricstreamers.influxdb.InfluxdbMetricStreamer;

/**
 * Represents an InfluxDB {@code SELECT} query to be periodically executed by
 * the {@link InfluxdbMetricStreamer} to fetch new metrics values. A query
 * should avoid filtering on {@code time} in the {@code WHERE} clause, as
 * selecting the approriate interval for which to fetch metrics will be handled
 * by the {@link InfluxdbMetricStreamer}.
 * <p/>
 * The author of {@link InfluxdbMetricStreamer} queries need to be aware of the
 * following:
 * <ul>
 * <li>Only select a single field/column in the {@code SELECT} statement. Any
 * additional fields are ignored in the result processing.</li>
 * <li>The selected field/column must be a numeric value. If not, errors will be
 * raised during result processing.</li>
 * <li>Semi-colons are disallowed. This protects against injecting additional
 * modifying queries/statements.</li>
 * <li>It is entirely possible to put together queries that don't make sense.
 * These won't be caught until runtime (when an attmept is made to execute the
 * query against InfluxDB).</li>
 * </ul>
 *
 * @see MetricStreamDefinition
 */
public class Query {

    /**
     * {@code SELECT} clause. For example, {@code mean('request_rate')} or
     * {@code non_negative_derivative(max('value'), 1s)}. The select statement
     * should select one single field/column (only the first field is handled in
     * result processing). The selected field/column must be a numeric value. If
     * not, errors will be raised during result processing. Surround identifiers
     * with double quotes to support a richer character set for identifiers.
     */
    private final String select;
    /**
     * {@code FROM} clause. Specifies the measurement to query for. For example,
     * {@code cpu/system}. Surround identifiers with double quotes to support a
     * richer character set for identifiers.
     */
    private final String from;
    /**
     * {@code WHERE} clause. Used to filter data based on tags/field values.
     * Avoid filtering on {@code time} in the {@code WHERE} clause, as selecting
     * the approriate interval for which to fetch metrics will be handled by the
     * {@link InfluxdbMetricStreamer}. Behavior is undefined if {@code time}
     * filters are inclued. Surround identifiers with double quotes to support a
     * richer character set for identifiers.
     */
    private final String where;

    /**
     * {@code GROUP BY} clause. Can be used to downsample data (if combined with
     * an aggregation function in the {@code SELECT} clause), for example by
     * specifying something like {@code time(10s) fill(none)}.
     * <p/>
     * Note: do not group on tags, since that will create different series in
     * the result (one per group) and the result handler will only process the
     * first series (the order may not be deterministic). Use a {@code WHERE}
     * clause to query for a particular combination of tags.
     */
    private final String groupBy;

    /**
     * Creates a {@link Query}.
     *
     * @param select
     *            {@code SELECT} clause. For example,
     *            {@code mean('request_rate')} or
     *            {@code non_negative_derivative(max('value'), 1s)}. The select
     *            statement should select one single field/column (only the
     *            first field is handled in result processing). The selected
     *            field/column must be a numeric value. If not, errors will be
     *            raised during result processing. Surround identifiers with
     *            double quotes to support a richer character set for
     *            identifiers.
     * @param from
     *            {@code FROM} clause. Specifies the measurement to query for.
     *            For example, {@code cpu/system}. Surround identifiers with
     *            double quotes to support a richer character set for
     *            identifiers.
     * @param where
     *            {@code WHERE} clause. Used to filter data based on tags/field
     *            values. Avoid filtering on {@code time} in the {@code WHERE}
     *            clause, as selecting the approriate interval for which to
     *            fetch metrics will be handled by the
     *            {@link InfluxdbMetricStreamer}. Behavior is undefined if
     *            {@code time} filters are inclued. Surround identifiers with
     *            double quotes to support a richer character set for
     *            identifiers.
     * @param groupBy
     *            {@code GROUP BY} clause. Can be used to downsample data (if
     *            combined with an aggregation function in the {@code SELECT}
     *            clause), for example by specifying something like
     *            {@code time(10s) fill(none)}.
     *            <p/>
     *            Note: do not group on tags, since that will create different
     *            series in the result (one per group) and the result handler
     *            will only process the first series (the order may not be
     *            deterministic). Use a {@code WHERE} clause to query for a
     *            particular combination of tags.
     *
     */
    public Query(String select, String from, String where, String groupBy) {
        this.select = select;
        this.from = from;
        this.where = where;
        this.groupBy = groupBy;
    }

    /**
     * {@code SELECT} clause. For example, {@code mean('request_rate')} or
     * {@code non_negative_derivative(max('value'), 1s)}. The select statement
     * should select one single field/column (only the first field is handled in
     * result processing). The selected field/column must be a numeric value. If
     * not, errors will be raised during result processing.
     *
     * @return
     */
    public String getSelect() {
        return this.select;
    }

    /**
     * {@code FROM} clause. Specifies the measurement to query for. For example,
     * {@code cpu/system}.
     *
     * @return
     */
    public String getFrom() {
        return this.from;
    }

    /**
     * {@code WHERE} clause. Used to filter data based on tags/field values.
     * Avoid filtering on {@code time} in the {@code WHERE} clause, as selecting
     * the approriate interval for which to fetch metrics will be handled by the
     * {@link InfluxdbMetricStreamer}. Behavior is undefined if {@code time}
     * filters are inclued.
     *
     * @return
     */
    public String getWhere() {
        return this.where;
    }

    /**
     * {@code GROUP BY} clause. Can be used to downsample data (if combined with
     * an aggregation function in the {@code SELECT} clause), for example by
     * specifying something like {@code time(10s) fill(none)}.
     * <p/>
     * Note: do not group on tags, since that will create different series in
     * the result (one per group) and the result handler will only process the
     * first series (the order may not be deterministic). Use a {@code WHERE}
     * clause to query for a particular combination of tags.
     *
     * @return
     */
    public String getGroupBy() {
        return this.groupBy;
    }

    /**
     * Creates a field-by-field copy of this {@link Query} with the groupBy
     * clause replaced in the returned copy.
     *
     * @param groupBy
     * @return
     */
    public Query withGroupBy(String groupBy) {
        return new Query(this.select, this.from, this.where, groupBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.select, this.from, this.where, this.groupBy);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Query) {
            Query that = (Query) obj;
            return Objects.equals(this.select, that.select) //
                    && Objects.equals(this.from, that.from) //
                    && Objects.equals(this.where, that.where) //
                    && Objects.equals(this.groupBy, that.groupBy);
        }
        return false;
    }

    /**
     * * Checks the validity of field values. Throws an
     * {@link IllegalArgumentException} if necessary conditions are not
     * satisfied.
     *
     * @throws IllegalArgumentException
     * @return
     */
    public Query validate() throws IllegalArgumentException {
        checkArgument(this.select != null, "no select clause given");
        checkArgument(this.from != null, "no from clause given");

        asList(this.select, this.from, this.where, this.groupBy).stream().forEach(clause -> {
            if (clause != null && clause.contains(";")) {
                throw new IllegalArgumentException("no part of the query is allowed to contain semi-colon");
            }
        });

        return this;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String select;
        private String from;
        private String where;
        private String groupBy;

        private Builder() {

        }

        public Query build() {
            return new Query(this.select, this.from, this.where, this.groupBy);
        }

        /**
         * {@code SELECT} clause. For example, {@code mean('request_rate')} or
         * {@code non_negative_derivative(max('value'), 1s)}. The select
         * statement should select one single field/column (only the first field
         * is handled in result processing). The selected field/column must be a
         * numeric value. If not, errors will be raised during result
         * processing.
         *
         * @param select
         * @return
         */
        public Builder select(String select) {
            this.select = select;
            return this;
        }

        /**
         * {@code FROM} clause. Specifies the measurement to query for. For
         * example, {@code cpu/system}.
         *
         * @param from
         * @return
         */
        public Builder from(String from) {
            this.from = from;
            return this;
        }

        /**
         * {@code WHERE} clause. Used to filter data based on tags/field values.
         * Avoid filtering on {@code time} in the {@code WHERE} clause, as
         * selecting the approriate interval for which to fetch metrics will be
         * handled by the {@link InfluxdbMetricStreamer}. Behavior is undefined
         * if {@code time} filters are inclued.
         *
         * @param where
         * @return
         */
        public Builder where(String where) {
            this.where = where;
            return this;
        }

        /**
         * {@code GROUP BY} clause. Can be used to downsample data (if combined
         * with an aggregation function in the {@code SELECT} clause), for
         * example by specifying something like {@code time(10s) fill(none)}.
         * <p/>
         * Note: do not group on tags, since that will create different series
         * in the result (one per group) and the result handler will only
         * process the first series (the order may not be deterministic). Use a
         * {@code WHERE} clause to query for a particular combination of tags.
         *
         * @param groupBy
         * @return
         */
        public Builder groupBy(String groupBy) {
            this.groupBy = groupBy;
            return this;
        }
    }
}
