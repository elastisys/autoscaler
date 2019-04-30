package com.elastisys.autoscaler.metricstreamers.influxdb.resultset;

import java.util.List;
import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Represents the results of a collection of InfluxDB {@code SELECT} queries.
 */
public class ResultSet {

    /** The list of results, one for each query sent to the server. */
    private final List<Result> results;

    /**
     * Creates a {@link ResultSet}.
     *
     * @param results
     *            The list of results, one for each query sent to the server.
     */
    public ResultSet(List<Result> results) {
        this.results = results;
    }

    /**
     * The list of results, one for each query sent to the server.
     *
     * @return
     */
    public List<Result> getResults() {
        return this.results;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.results);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ResultSet) {
            ResultSet that = (ResultSet) obj;
            return Objects.equals(this.results, that.results);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    /**
     * Parser that takes an InfluxDB query response as JSON and produces a
     * corresponding {@link ResultSet}.
     */
    public static class Parser {
        /**
         * Parses an InfluxDB query response as JSON and produces a
         * corresponding {@link ResultSet}.
         *
         * @param json
         *            InfluxDB query response as JSON.
         * @return
         */
        public static ResultSet parse(String json) {
            return JsonUtils.toObject(JsonUtils.parseJsonString(json), ResultSet.class);
        }
    }
}
