package com.elastisys.autoscaler.metricstreamers.influxdb.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.metricstreamers.influxdb.InfluxdbMetricStreamer;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Describes what query to run for this particular {@link MetricStream}.
 *
 * @see InfluxdbMetricStreamerConfig
 */
public class MetricStreamDefinition {
    /** Default value for {@link #queryChunkSize}. */
    public static final TimeInterval DEFAULT_QUERY_CHUNK_SIZE = new TimeInterval(30L, TimeUnit.DAYS);

    /**
     * The id of the metric stream. This is the id that will be used by clients
     * wishing to subscribe to this metric stream. Required.
     */
    private final String id;

    /**
     * The metric name that will be assigned to the {@link MetricValue}s
     * produced by this stream. Optional. Default: {@link #id}.
     */
    private final String metricName;

    /**
     * The InfluxDB database to query. An InfluxDB database acts as a container
     * for time series data. For example, {@code mydb}. Required.
     */
    private final String database;
    /**
     * The InfluxDB {@code SELECT} query to be used to fetch new metrics values.
     * Required. Note: a query should avoid filtering on {@code time} in the
     * {@code WHERE} clause, as selecting the approriate interval for which to
     * fetch metrics will be handled by the {@link InfluxdbMetricStreamer}.
     */
    private final Query query;

    /**
     * The minimum age of requested data points. The {@link MetricStreamer} will
     * never request values newer than this from InfluxDB. This value can be
     * regarded as the expected "settling time" of new data points.
     * <p/>
     * When requesting recent aggregate metric data points, there is always a
     * risk of seeing partial/incomplete results before metric values from all
     * sources have been registered. The data settling time is intended to give
     * all sources a chance to report before fetching recent values.
     * <p/>
     * The value to set for this field depends on the reporting frequency of
     * monitoring agents, but as a general rule-of-thumb, this value can be set
     * to be about {@code 1.5} times the length of the reporting-interval for
     * monitoring agents. May be <code>null</code>.
     */
    private final TimeInterval dataSettlingTime;

    /**
     * The maximum time period that a single query will attempt to fetch in a
     * single call. A query with a longer time interval will be run
     * incrementally, each fetching a sub-interval of this duration. This type
     * of incremental retrieval of large {@link QueryResultSet}s limits the
     * amount of (memory) resources involved in processing large queries. May be
     * <code>null</code>. Default: {@value #DEFAULT_QUERY_CHUNK_SIZE}.
     */
    private final TimeInterval queryChunkSize;

    /**
     * Creates an {@link MetricStreamDefinition}.
     *
     * @param id
     *            The id of the metric stream. This is the id that will be used
     *            by clients wishing to subscribe to this metric stream.
     *            Required.
     * @param metricName
     *            The metric name that will be assigned to the
     *            {@link MetricValue}s produced by this stream. Optional.
     *            Default: {@link #id}.
     * @param database
     *            The InfluxDB database to query. An InfluxDB database acts as a
     *            container for time series data. For example, {@code mydb}.
     *            Required.
     * @param query
     *            The InfluxDB {@code SELECT} query to be used to fetch new
     *            metrics values. Required. Note: a query should avoid filtering
     *            on {@code time} in the {@code WHERE} clause, as selecting the
     *            approriate interval for which to fetch metrics will be handled
     *            by the {@link InfluxdbMetricStreamer}.
     * @param dataSettlingTime
     *            The minimum age of requested data points. The
     *            {@link MetricStreamer} will never request values newer than
     *            this from InfluxDB. This value can be regarded as the expected
     *            "settling time" of new data points.
     *            <p/>
     *            When requesting recent aggregate metric data points, there is
     *            always a risk of seeing partial/incomplete results before
     *            metric values from all sources have been registered. The data
     *            settling time is intended to give all sources a chance to
     *            report before fetching recent values.
     *            <p/>
     *            The value to set for this field depends on the reporting
     *            frequency of monitoring agents, but as a general
     *            rule-of-thumb, this value can be set to be about {@code 1.5}
     *            times the length of the reporting-interval for monitoring
     *            agents. May be <code>null</code>.
     * @param queryChunkSize
     *            The maximum time period that a single query will attempt to
     *            fetch in a single call. A query with a longer time interval
     *            will be run incrementally, each fetching a sub-interval of
     *            this duration. This type of incremental retrieval of large
     *            {@link QueryResultSet}s limits the amount of (memory)
     *            resources involved in processing large queries. May be
     *            <code>null</code>. Default:
     *            {@value #DEFAULT_QUERY_CHUNK_SIZE}.
     */
    public MetricStreamDefinition(String id, String metricName, String database, Query query,
            TimeInterval dataSettlingTime, TimeInterval queryChunkSize) {
        this.id = id;
        this.metricName = metricName;
        this.database = database;
        this.query = query;
        this.dataSettlingTime = dataSettlingTime;
        this.queryChunkSize = queryChunkSize;
    }

    /**
     * The id of the metric stream. This is the id that will be used by clients
     * wishing to subscribe to this metric stream. Required.
     *
     * @return
     */
    public String getId() {
        return this.id;
    }

    /**
     * The metric name that will be assigned to the {@link MetricValue}s
     * produced by this stream.
     *
     * @return
     */
    public String getMetricName() {
        return Optional.ofNullable(this.metricName).orElse(this.id);
    }

    public String getMetric() {
        return getMetricName();
    }

    /**
     * The InfluxDB database to query. An InfluxDB database acts as a container
     * for time series data. For example, {@code mydb}.
     *
     * @return
     */
    public String getDatabase() {
        return this.database;
    }

    /**
     * The InfluxDB {@code SELECT} query to be used to fetch new metrics values.
     * Note: a query should avoid filtering on {@code time} in the {@code WHERE}
     * clause, as selecting the approriate interval for which to fetch metrics
     * will be handled by the {@link InfluxdbMetricStreamer}.
     *
     * @return
     */
    public Query getQuery() {
        return this.query;
    }

    /**
     * The minimum age of requested data points. The {@link MetricStreamer} will
     * never request values newer than this from InfluxDB. This value can be
     * regarded as the expected "settling time" of new data points.
     * <p/>
     * When requesting recent aggregate metric data points, there is always a
     * risk of seeing partial/incomplete results before metric values from all
     * sources have been registered. The data settling time is intended to give
     * all sources a chance to report before fetching recent values.
     * <p/>
     * The value to set for this field depends on the reporting frequency of
     * monitoring agents, but as a general rule-of-thumb, this value can be set
     * to be about {@code 1.5} times the length of the reporting-interval for
     * monitoring agents. May be <code>null</code>.
     *
     * @return
     */
    public TimeInterval getDataSettlingTime() {
        return this.dataSettlingTime;
    }

    /**
     * The maximum time period that a single query against the InfluxDB server
     * will attempt to fetch in a single call. A query with a longer time
     * interval will be run incrementally, each fetching a sub-interval of this
     * duration. This type of incremental retrieval of large
     * {@link QueryResultSet}s limits the amount of (memory) resources involved
     * in processing large queries.
     *
     * @return
     */
    public TimeInterval getQueryChunkSize() {
        return Optional.ofNullable(this.queryChunkSize).orElse(DEFAULT_QUERY_CHUNK_SIZE);
    }

    /**
     * Checks the validity of field values. Throws an
     * {@link IllegalArgumentException} if necessary conditions are not
     * satisfied.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        try {
            checkArgument(this.id != null, "no id specified");
            checkArgument(this.database != null, "no database specified");
            checkArgument(this.query != null, "no query specified");

            if (this.metricName != null) {
                checkArgument(!this.metricName.isEmpty(), "metricName cannot be empty string");
            }

            try {
                this.query.validate();
            } catch (Exception e) {
                throw new IllegalArgumentException("query: " + e.getMessage(), e);
            }

            if (this.dataSettlingTime != null) {
                try {
                    this.dataSettlingTime.validate();
                } catch (Exception e) {
                    throw new IllegalArgumentException("dataSettlingTime: " + e.getMessage(), e);
                }
            }

            checkArgument(getQueryChunkSize().getMillis() > 0, "queryChunkSize must be a non-zero duration");
        } catch (Exception e) {
            throw new IllegalArgumentException("metricStream: " + e.getMessage(), e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.metricName, this.database, this.query, this.dataSettlingTime,
                this.queryChunkSize);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetricStreamDefinition) {
            MetricStreamDefinition that = (MetricStreamDefinition) obj;
            return Objects.equals(this.id, that.id) //
                    && Objects.equals(this.metricName, that.metricName) //
                    && Objects.equals(this.database, that.database) //
                    && Objects.equals(this.query, that.query) //
                    && Objects.equals(this.dataSettlingTime, that.dataSettlingTime) //
                    && Objects.equals(this.queryChunkSize, that.queryChunkSize);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    /**
     * Creates a field-by-field copy of this object but sets a different query
     * in the returned copy.
     *
     * @param query
     * @return
     */
    public MetricStreamDefinition withQuery(Query query) {
        return new MetricStreamDefinition(this.id, this.metricName, this.database, query, this.dataSettlingTime,
                this.queryChunkSize);
    }

    /**
     * Creates a field-by-field copy of this object but sets a different data
     * settling time in the returned copy.
     *
     * @param dataSettlingTime
     * @return
     */
    public MetricStreamDefinition withDataSettlingTime(TimeInterval dataSettlingTime) {
        return new MetricStreamDefinition(this.id, this.metricName, this.database, this.query, dataSettlingTime,
                this.queryChunkSize);
    }

    /**
     * Creates a field-by-field copy of this object but sets a different query
     * chunk size in the returned copy.
     *
     * @param queryChunkSize
     * @return
     */
    public MetricStreamDefinition withQueryChunkSize(TimeInterval queryChunkSize) {
        return new MetricStreamDefinition(this.id, this.metricName, this.database, this.query, this.dataSettlingTime,
                queryChunkSize);
    }

}
