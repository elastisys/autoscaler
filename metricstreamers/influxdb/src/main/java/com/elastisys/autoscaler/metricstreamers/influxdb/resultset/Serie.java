package com.elastisys.autoscaler.metricstreamers.influxdb.resultset;

import java.util.List;
import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Represents a single data point series for an InfluxDB query {@link Result}.
 * <p/>
 * If this {@link Serie}s was the result of a query that grouped by tags, the
 * {@link Serie} represents a single result group, with a common set of tags as
 * specified by {@link #tags}.
 * <p/>
 * If the {@link Serie} was the result of a query that did not group by tags,
 * the {@link Serie} is the single result group and no {@link #tags} field is
 * specified.
 *
 * @see Result
 */
public class Serie {

    /**
     * The name of the measurement that was requested in the {@code FROM}
     * clause.
     */
    private final String name;
    /**
     * The grouping tags shared by all data points in this {@link Serie}s. Only
     * set if the query grouped by tag, otherwise <code>null</code>.
     */
    private final List<String> tags;
    /**
     * Holds the column names selected in the SELECT clause. The first column is
     * always {@code time}.
     */
    private final List<String> columns;
    /**
     * The data points in the {@link Serie}s as a list of
     * {@code [<time>, <col1-value>, <col2-value>, ...]} tuples.
     */
    private final List<List<Object>> values;

    /**
     * Creates a {@link Serie}.
     *
     * @param name
     *            The name of the measurement that was requested in the
     *            {@code FROM} clause.
     * @param tags
     *            The grouping tags shared by all data points in this
     *            {@link Serie}s. Only set if the query grouped by tag,
     *            otherwise <code>null</code>.
     * @param columns
     *            Holds the column names selected in the SELECT clause. The
     *            first column is always {@code time}.
     * @param values
     *            The data points in the {@link Serie}s as a list of
     *            {@code [<time>, <col1-value>, <col2-value>, ...]} tuples.
     */
    public Serie(String name, List<String> tags, List<String> columns, List<List<Object>> values) {
        this.name = name;
        this.tags = tags;
        this.columns = columns;
        this.values = values;
    }

    /**
     * The name of the measurement that was requested in the {@code FROM}
     * clause.
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * The grouping tags shared by all data points in this {@link Serie}s. Only
     * set if the query grouped by tag, otherwise <code>null</code>.
     *
     * @return
     */
    public List<String> getTags() {
        return this.tags;
    }

    /**
     * Holds the column names selected in the SELECT clause. The first column is
     * always {@code time}.
     *
     * @return
     */
    public List<String> getColumns() {
        return this.columns;
    }

    /**
     * The data points in the {@link Serie}s as a list of
     * {@code [<time>, <col1-value>, <col2-value>, ...]} tuples.
     *
     * @return
     */
    public List<List<Object>> getValues() {
        return this.values;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.tags, this.columns, this.values);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Serie) {
            Serie that = (Serie) obj;
            return Objects.equals(this.name, that.name) && Objects.equals(this.tags, that.tags)
                    && Objects.equals(this.columns, that.columns) && Objects.equals(this.values, that.values);

        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
