package com.elastisys.autoscaler.metricstreamers.opentsdb.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.metricstreamers.opentsdb.OpenTsdbMetricStreamer;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.DownsamplingSpecification;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.MetricAggregator;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.OpenTsdbQueryBuilder;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Represents a metric stream definition for a {@link OpenTsdbMetricStreamer}.
 * <p/>
 * For more details on the meaning of the different parameters, refer to the
 * <a href="http://opentsdb.net/http-api.html">OpenTSDB HTTP API
 * specification</a>, <a href="http://opentsdb.net/metrics.html">metrics and
 * tags</a> and <a href="http://opentsdb.net/query-execution.html">OpenTSDB
 * query exeution</a>.
 *
 * @see OpenTsdbMetricStreamerConfig
 * @see OpenTsdbMetricStreamer
 */
public class OpenTsdbMetricStreamDefinition {
    /** Default value for {@link #queryChunkSize}. */
    public static final TimeInterval DEFAULT_QUERY_CHUNK_SIZE = new TimeInterval(30L, TimeUnit.DAYS);

    /** Default for {@link #convertToRate}. */
    public static final Boolean DEFAULT_RATE_CONVERSION = false;

    /**
     * Regular expression describing a valid OpenTSDB metric/tag-key/tag-value
     * identifier.
     */
    public static final Pattern VALID_OPENTSDB_IDENTIFIER = Pattern.compile("[A-Za-z0-9\\-_\\./]+");

    /**
     * The id of the metric stream. This is the id that will be used by clients
     * wishing to subscribe to {@link MetricValue}s for this metric stream.
     * Required.
     */
    private final String id;
    /**
     * The OpenTSDB metric that the metric stream retrieves {@link MetricValue}s
     * for. Required.
     */
    private final String metric;
    /**
     * The aggregation function used to aggregate {@link MetricValue}s in the
     * metric stream. Required.
     */
    private final MetricAggregator aggregator;
    /**
     * When <code>true</code> the stream will feed the change rate of the
     * metric, rather than the absolute values of the metric. Optional. Default:
     * {@link #DEFAULT_RATE_CONVERSION}.
     */
    private final Boolean convertToRate;
    /**
     * The down-sampling to apply to {@link MetricValue}s in the metric stream.
     * May be <code>null</code>.
     */
    private final DownsamplingSpecification downsampling;
    /**
     * The collection of tags used to filter the {@link MetricValue}s returned
     * from the metric stream. May be <code>null</code>.
     */
    private final Map<String, List<String>> tags;

    /**
     * The minimum age (in seconds) of requested data points. The
     * {@link MetricStreamer} will never request values newer than this from
     * OpenTSDB. This value can be regarded as the expected "settling time" of
     * new data points.
     * <p/>
     * When requesting recent aggregate metric data points, there is always a
     * risk of seeing partial/incomplete results before metric values from all
     * instances have been registered. Typically, it takes a couple of minutes
     * for all instances to report their values to CloudWatch.
     * <p/>
     * The value to set for this field depends on the reporting frequency of
     * monitoring agents, but as a general rule-of-thumb, this value can be set
     * to be about {@code 1.5} times the length of the reporting-interval for
     * monitoring agents.
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
     * Constructs a new {@link OpenTsdbMetricStreamDefinition}.
     *
     * @param id
     *            The id of the metric stream. This is the id that will be used
     *            by clients wishing to subscribe to {@link MetricValue}s for
     *            this metric stream. Required.
     * @param metric
     *            The OpenTSDB metric that the metric stream retrieves
     *            {@link MetricValue}s for. Required.
     * @param aggregator
     *            The aggregation function used to aggregate {@link MetricValue}
     *            s in the metric stream. Required.
     * @param convertToRate
     *            When <code>true</code> the stream will feed the change rate of
     *            the metric, rather than the absolute values of the metric.
     *            Optional. Default: {@link #DEFAULT_RATE_CONVERSION}.
     * @param downsampling
     *            The down-sampling to apply to {@link MetricValue}s in the
     *            metric stream. May be <code>null</code>.
     * @param tags
     *            The collection of tags used to filter the {@link MetricValue}s
     *            returned from the metric stream. May be <code>null</code>.
     * @param dataSettlingTime
     *            The minimum age (in seconds) of requested data points. The
     *            {@link MetricStreamer} will never request values newer than
     *            this from OpenTSDB. This value can be regarded as the expected
     *            "settling time" of new data points.
     *            <p/>
     *            When requesting recent aggregate metric data points, there is
     *            always a risk of seeing partial/incomplete results before
     *            metric values from all instances have been registered.
     *            Typically, it takes a couple of minutes for all instances to
     *            report their values to CloudWatch.
     *            <p/>
     *            The value to set for this field depends on the reporting
     *            frequency of monitoring agents, but as a general
     *            rule-of-thumb, this value can be set to be about {@code 1.5}
     *            times the length of the reporting-interval for monitoring
     *            agents.
     *            <p/>
     *            If <code>null</code>, a settling time of zero is assumed.
     * @param queryChunkSize
     *            The maximum time period that a single query against the
     *            OpenTSDB server will attempt to fetch in a single call. A
     *            query with a longer time interval will be run incrementally,
     *            each fetching a sub-interval of this duration. This type of
     *            incremental retrieval of large {@link QueryResultSet}s limits
     *            the amount of (memory) resources involved in processing large
     *            queries. May be <code>null</code>. Default:
     *            {@value #DEFAULT_QUERY_CHUNK_SIZE}.
     */
    public OpenTsdbMetricStreamDefinition(String id, String metric, MetricAggregator aggregator, Boolean convertToRate,
            DownsamplingSpecification downsampling, Map<String, List<String>> tags, TimeInterval dataSettlingTime,
            TimeInterval queryChunkSize) {
        this.id = id;
        this.metric = metric;
        this.aggregator = aggregator;
        this.convertToRate = convertToRate;
        this.downsampling = downsampling;
        this.tags = tags;
        this.dataSettlingTime = dataSettlingTime;
        this.queryChunkSize = queryChunkSize;
    }

    /**
     * Returns the id of the metric stream. This is the id that will be used by
     * clients wishing to subscribe to {@link MetricValue}s for this metric
     * stream.
     *
     * @return
     */
    public String getId() {
        return this.id;
    }

    /**
     * Returns the OpenTSDB metric that the metric stream retrieves
     * {@link MetricValue}s for.
     *
     * @return
     */
    public String getMetric() {
        return this.metric;
    }

    /**
     * Returns the aggregation function used to aggregate {@link MetricValue}s
     * in the metric stream.
     *
     * @return
     */
    public MetricAggregator getAggregator() {
        return this.aggregator;
    }

    /**
     * Returns <code>true</code> if the stream will feed the change rate of the
     * metric, rather than the absolute values of the metric.
     *
     * @return
     */
    public boolean isConvertToRate() {
        return Optional.ofNullable(this.convertToRate).orElse(DEFAULT_RATE_CONVERSION);
    }

    /**
     * Returns the down-sampling to apply to {@link MetricValue}s in the metric
     * stream.
     *
     * @return
     */
    public DownsamplingSpecification getDownsampling() {
        return this.downsampling;
    }

    /**
     * Returns the collection of tags used to filter the {@link MetricValue}s
     * returned from the metric stream.
     *
     * @return
     */
    public Map<String, List<String>> getTags() {
        return Optional.ofNullable(this.tags).orElse(Collections.emptyMap());
    }

    /**
     * Returns the minimum age of requested data points. The
     * {@link MetricStreamer} will never request values newer than this from
     * OpenTSDB. This value can be regarded as the expected "settling time" of
     * new data points.
     * <p/>
     * When requesting recent aggregate metric data points, there is always a
     * risk of seeing partial/incomplete results before metric values from all
     * instances have been registered. Typically, it takes a couple of minutes
     * for all instances to report their values to CloudWatch.
     * <p/>
     * The value to set for this field depends on the reporting frequency of
     * monitoring agents, but as a general rule-of-thumb, this value can be set
     * to be about {@code 1.5} times the length of the reporting-interval for
     * monitoring agents.
     *
     * @return
     */
    public TimeInterval getDataSettlingTime() {
        return Optional.ofNullable(this.dataSettlingTime).orElse(new TimeInterval(0L, TimeUnit.SECONDS));
    }

    /**
     * The maximum time period that a single query against the OpenTSDB server
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
     * Builds an OpenTSDB query string from this
     * {@link OpenTsdbMetricStreamDefinition}, or throws an exception in case
     * this fails. The created query will not be bounded by a certain time
     * interval.
     *
     * @return The query string. Note that the returned query is not a complete
     *         URL. More specifically, it does not include the host/port-part of
     *         a full URL.
     * @throws RuntimeException
     *             if a OpenTSDB query could not be built (for example, due to
     *             missing required fields).
     */
    public String getQuery() throws RuntimeException {
        // start with required fields.
        OpenTsdbQueryBuilder builder = new OpenTsdbQueryBuilder().withMetric(this.metric)
                .withAggregator(this.aggregator);
        builder.withRateConversion(isConvertToRate());
        if (this.downsampling != null) {
            builder.withDownsamplingSpecification(this.downsampling);
        }
        if (this.tags != null) {
            builder.withTags(this.tags);
        }
        return builder.build();
    }

    /**
     * Builds a time-bounded OpenTSDB query string from a time interval and this
     * {@link OpenTsdbMetricStreamDefinition}, or throws an exception in case
     * this fails.
     *
     * @param interval
     *            The time interval used to restrict the query.
     * @return The query string. Note that the returned query is not a complete
     *         URL. More specifically, it does not include the host/port-part of
     *         a full URL.
     * @throws RuntimeException
     *             if a OpenTSDB query could not be built (for example, due to
     *             missing required fields).
     * @throws IllegalArgumentException
     *             If an illegal query interval was specified (for example, the
     *             start of the query interval is later than the data settling
     *             time).
     */
    public String makeQuery(Interval interval) throws RuntimeException, IllegalArgumentException {
        checkArgument(interval != null, "query interval cannot be null");
        checkArgument(!interval.getStart().equals(interval.getEnd()), "query interval start time is same as end time");

        // start with required fields.
        OpenTsdbQueryBuilder builder = new OpenTsdbQueryBuilder().withMetric(this.metric)
                .withAggregator(this.aggregator);
        builder.withRateConversion(this.convertToRate);
        builder.withInterval(interval);
        if (this.downsampling != null) {
            this.downsampling.validate();
            builder.withDownsamplingSpecification(this.downsampling);
        }
        if (this.tags != null) {
            builder.withTags(this.tags);
        }
        return builder.build();
    }

    /**
     * Returns the current point in time before which data points can be
     * regarded as "settled" (according to the {@link #dataSettlingTime} of the
     * stream), and that hence are considered to be safe to query without
     * risking to see incomplete data.
     * <p/>
     * Queries should not ask for more recent data points than this.
     *
     * @return
     */
    public DateTime getDataSettlingPoint() {
        return UtcTime.now().minus(getDataSettlingTime().getMillis());
    }

    /**
     * Adjusts a query time interval to not include data points more recent than
     * the {@link #dataSettlingTime} configured for the metric stream.
     * <p/>
     * Should the {@link Interval} be too recent (its start time being later
     * than the data settling time), the query is disallowed and an
     * {@link IllegalArgumentException} is thrown.
     *
     * @param queryInterval
     *            A query time interval.
     * @return The adjusted query interval (with an end time earlier than
     *         {@code dataSettlingTime}).
     * @throws IllegalArgumentException
     *             If the start of the query interval is later than the data
     *             settling time.
     */
    public Interval adjustForDataSettlingTime(Interval queryInterval) throws IllegalArgumentException {
        DateTime dataSettlingPoint = getDataSettlingPoint();
        if (queryInterval.isBefore(dataSettlingPoint)) {
            // interval is ok
            return queryInterval;
        }

        if (!queryInterval.getStart().isBefore(dataSettlingPoint)) {
            // interval is too late (after data settling time) => raise error
            throw new IllegalArgumentException(
                    String.format("illegal query interval %s: " + "data settling time does not "
                            + "allow queries for data points after %s", queryInterval, dataSettlingPoint));
        }

        // interval end too late => adjust query end time
        return new Interval(queryInterval.getStart(), dataSettlingPoint);
    }

    /**
     * Validates that this {@link OpenTsdbMetricStreamDefinition} contains
     * sufficient information to allow a valid OpenTSDB query to be built. If it
     * does, the method will just return. If it is found to be invalid, a
     * {@link IllegalArgumentException} is thrown.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        try {
            checkArgument(this.id != null, "missing id");
            checkArgument(this.metric != null, "missing metric");
            ensureValidIdentifier(this.metric);
            checkArgument(this.aggregator != null, "missing aggregator");
            checkArgument(getDataSettlingTime().getMillis() >= 0, "dataSettlingTime must be non-negative");
            checkArgument(getQueryChunkSize().getMillis() > 0, "queryChunkSize must be a non-zero duration");

            getTags().forEach((key, value) -> ensureValidIdentifier(key));

            getQuery();
        } catch (Exception e) {
            throw new IllegalArgumentException("metricStream: " + e.getMessage(), e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.metric, this.aggregator, this.convertToRate, this.downsampling, this.tags,
                this.dataSettlingTime, this.queryChunkSize);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OpenTsdbMetricStreamDefinition) {
            OpenTsdbMetricStreamDefinition that = (OpenTsdbMetricStreamDefinition) obj;
            return Objects.equals(this.id, that.id) && Objects.equals(this.metric, that.metric)
                    && Objects.equals(this.aggregator, that.aggregator)
                    && Objects.equals(this.convertToRate, that.convertToRate)
                    && Objects.equals(this.downsampling, that.downsampling) && Objects.equals(this.tags, that.tags)
                    && Objects.equals(this.dataSettlingTime, that.dataSettlingTime)
                    && Objects.equals(this.queryChunkSize, that.queryChunkSize);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    /**
     * Validates that a given metric name/tag name/tag value only contains
     * allowed characters.
     * <p/>
     * Metric names, tag names and tag values have to be made of alpha numeric
     * characters, dash "-", underscore "_", period ".", and forward slash "/".
     *
     * @param identifier
     *            A metric name, tag name or tag value to validate.
     */
    private void ensureValidIdentifier(String identifier) {
        checkArgument(VALID_OPENTSDB_IDENTIFIER.matcher(identifier).matches(), "illegal OpenTSDB identifier: %s",
                identifier);
    }

    /**
     * Creates a field-by-field copy of this object but sets a different
     * downsampling in the returned copy.
     *
     * @param function
     * @return
     */
    public OpenTsdbMetricStreamDefinition withDownsampling(DownsamplingSpecification downsampling) {
        return new OpenTsdbMetricStreamDefinition(this.id, this.metric, this.aggregator, this.convertToRate,
                downsampling, this.tags, this.dataSettlingTime, this.queryChunkSize);
    }

    /**
     * Creates a field-by-field copy of this object but sets a different rate
     * conversion in the returned copy.
     *
     * @param convertToRate
     * @return
     */
    public OpenTsdbMetricStreamDefinition withConverToRate(boolean convertToRate) {
        return new OpenTsdbMetricStreamDefinition(this.id, this.metric, this.aggregator, convertToRate,
                this.downsampling, this.tags, this.dataSettlingTime, this.queryChunkSize);
    }

    /**
     * Creates a field-by-field copy of this object but sets a different tags in
     * the returned copy.
     *
     * @param tags
     * @return
     */
    public OpenTsdbMetricStreamDefinition withTags(Map<String, List<String>> tags) {
        return new OpenTsdbMetricStreamDefinition(this.id, this.metric, this.aggregator, this.convertToRate,
                this.downsampling, tags, this.dataSettlingTime, this.queryChunkSize);
    }

}
