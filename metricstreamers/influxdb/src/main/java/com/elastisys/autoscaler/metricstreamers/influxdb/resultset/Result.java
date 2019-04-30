package com.elastisys.autoscaler.metricstreamers.influxdb.resultset;

import java.util.List;
import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Represents the results of a particular InfluxDB {@code SELECT} query.
 */
public class Result {

    /**
     * Contains an error message <i>if</if> the query failed to execute and did
     * not produce any result.
     */
    private final String error;

    /**
     * The collection of series that resulted from the query. Normally, the
     * result only a contains a single {@link Serie}. However, if the result was
     * produced for a query that grouped by tag ({@code GROUP BY <tag>}), this
     * list will contain one {@link Serie} for each set of data points that
     * share a commons set of tags (i.e. one {@link Serie} for each result
     * group).
     */
    private final List<Serie> series;

    /**
     * Creates a {@link Result}.
     *
     * @param error
     *            Contains an error message <i>if</if> the query failed to
     *            execute and did not produce any result.
     * @param series
     *            The collection of series that resulted from the query.
     *            Normally, the result only a contains a single {@link Serie}.
     *            However, if the result was produced for a query that grouped
     *            by tag ({@code GROUP BY <tag>}), this list will contain one
     *            {@link Serie} for each set of data points that share a commons
     *            set of tags (i.e. one {@link Serie} for each result group).
     */
    public Result(String error, List<Serie> series) {
        this.error = error;
        this.series = series;
    }

    /**
     * Returns <code>true</code> if the query failed to execute. The error
     * message can be retrieved from {@link #getError()}.
     *
     * @return
     */
    public boolean isError() {
        return this.error != null;
    }

    /**
     * Contains an error message <i>if</if> the query failed to execute and did
     * not produce any result.
     *
     * @return
     */
    public String getError() {
        return this.error;
    }

    /**
     * The collection of series that resulted from the query. Normally, the
     * result only a contains a single {@link Serie}. However, if the result was
     * produced for a query that grouped by tag ({@code GROUP BY <tag>}), this
     * list will contain one {@link Serie} for each set of data points that
     * share a commons set of tags (i.e. one {@link Serie} for each result
     * group).
     *
     * @return
     */
    public List<Serie> getSeries() {
        return this.series;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.error, this.series);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Result) {
            Result that = (Result) obj;
            return Objects.equals(this.error, that.error) && Objects.equals(this.series, that.series);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }
}
