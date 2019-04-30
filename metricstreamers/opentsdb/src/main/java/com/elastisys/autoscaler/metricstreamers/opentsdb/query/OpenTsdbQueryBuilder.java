package com.elastisys.autoscaler.metricstreamers.opentsdb.query;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * An OpenTSDB query builder that uses the builder pattern to construct a query
 * string that can be submitted to a OpenTSDB server endpoint.
 * <p/>
 * Note that the returned query is not a complete URL. More specifically, it
 * does not include the host/port-part of a full URL. A returned query string
 * may, for instance, look as follows:
 *
 * <pre>
 * /q?&m=sum:proc.stat.cpu&ascii&nocache
 * </pre>
 *
 * The client needs to turn the query string into a full URL by adding protocol,
 * host, and a port.
 * <p/>
 * The {@link OpenTsdbQueryBuilder} supports all functionality offered by the
 * <a href="http://opentsdb.net/http-api.html">OpenTSDB HTTP API
 * specification</a>.
 * <p/>
 * Read about <a href= "http://opentsdb.net/metrics.html">metrics and tags</a>
 * and <a href=" http://opentsdb.net/query-execution.html">OpenTSDB query
 * exeution</a> to learn more about OpenTSDB's operation.
 * <p/>
 */
public class OpenTsdbQueryBuilder {

    /**
     * {@link DateTimeFormatter} that converts timestamps to OpenTSDB format.
     * Thread-safe.
     */
    private final static DateTimeFormatter openTsdbDateFormat = DateTimeFormat.forPattern("yyyy/MM/dd-HH:mm:ss");

    /** The metric to query for. Required field. */
    private String metric;
    /**
     * The {@link MetricAggregator} function used to aggregate values from
     * different time-series of the metric. Required field.
     */
    private MetricAggregator metricAggregator;

    /** Query down-sampling. Optional. */
    private Optional<DownsamplingSpecification> downsamplingSpecification = Optional.empty();
    /**
     * Indicates whether the change rate of the metric should be returned,
     * rather than the absolute values of the metric.. Optional.
     */
    private Optional<Boolean> rate = Optional.empty();
    /** Time interval of query. Optional. */
    private Optional<Interval> interval = Optional.empty();
    /** Query filter tags. Optional. */
    private Optional<Map<String, List<String>>> tags = Optional.empty();

    /**
     * Constructs a new {@link OpenTsdbQueryBuilder}.
     */
    public OpenTsdbQueryBuilder() {
    }

    /**
     * Sets the metric for the query being built.
     *
     * @param metric
     *            The metric for which to retrieve values.
     * @return The builder itself, enabling chaining of build instructions.
     */
    public OpenTsdbQueryBuilder withMetric(String metric) {
        Objects.requireNonNull(metric, "metric cannot be null");
        this.metric = metric;
        return this;
    }

    /**
     * Sets the {@link MetricAggregator} function used to aggregate values from
     * different time-series of the metric for the query being built.
     *
     * @param aggregator
     *            The aggregation function to use for aggregating values.
     * @return The builder itself, enabling chaining of build instructions.
     */
    public OpenTsdbQueryBuilder withAggregator(MetricAggregator aggregator) {
        Objects.requireNonNull(aggregator, "metric aggregator cannot be null");
        this.metricAggregator = aggregator;
        return this;
    }

    /**
     * Adds a down-sampling specification to the query being built.
     *
     * @param downsamplingSpecification
     *            The down-sampling specification to add.
     * @return The builder itself, enabling chaining of build instructions.
     */
    public OpenTsdbQueryBuilder withDownsamplingSpecification(DownsamplingSpecification downsamplingSpecification) {
        Objects.requireNonNull(downsamplingSpecification, "Downsampling specification cannot be null");
        Objects.requireNonNull(downsamplingSpecification.getFunction(), "Downsampling function cannot be null");
        checkArgument(downsamplingSpecification.getInterval() != null, "Downsampling interval cannot be null");
        checkArgument(downsamplingSpecification.getInterval().getMillis() > 0,
                "Downsampling must be a positive duration");

        this.downsamplingSpecification = Optional.of(downsamplingSpecification);
        return this;
    }

    /**
     * Adds a flag indicating whether returned metric values are to be converted
     * to rate. When <code>true</code>, the built query will ask for the change
     * rate of the metric, rather than the absolute values of the metric.
     *
     * @param convertToRate
     *            A boolean flag that when <code>true</code> causes the built
     *            query to ask for the change rate of the metric, rather than
     *            the absolute values of the metric.
     * @return The builder itself, enabling chaining of build instructions.
     */
    public OpenTsdbQueryBuilder withRateConversion(boolean convertToRate) {
        if (convertToRate) {
            this.rate = Optional.of(convertToRate);
        }
        return this;
    }

    /**
     * Sets the interval of interest, limiting the built query to include only
     * {@link MetricValue}s reported within the specified points in time.
     *
     * @param interval
     *            The interval of interest.
     * @return The builder itself, enabling chaining of build instructions.
     */
    public OpenTsdbQueryBuilder withInterval(Interval interval) {
        Objects.requireNonNull(interval, "Interval cannot be null");
        Objects.requireNonNull(interval.getStart(), "Interval start cannot be null");
        Objects.requireNonNull(interval.getEnd(), "Interval end cannot be null");

        this.interval = Optional.of(interval);
        return this;
    }

    /**
     * Adds a collection of filter tags to the query being built.
     * <p/>
     * Tags can, for example, be used to filter query results specifying that
     * only {@link MetricValue}s with a given tag be returned or to specify that
     * {@link MetricValue}s are to be returned on a per-host basis rather than
     * in aggregated form.
     * <p/>
     * Read about <a href= "http://opentsdb.net/metrics.html>metrics and
     * tags</a> and <a href=" http://opentsdb.net/query-execution.html">OpenTSDB
     * query exeution</a> for more details on how to use filter tags.
     * <p/>
     * As an example, if {@link MetricValue}s are tagged with a {@code host}
     * tag, a {@code host} filter tag can be set to the list of hosts for which
     * to return {@link MetricValue}s. It is also possible to use a wild-card (
     * {@code *}) as value for a given tag, which causes the OpenTSDB system to
     * report all metrics that have the given tag, regardless of value.
     *
     * @param tags
     *            A mapping between tag keys and the list of requested values
     *            for each tag key.
     * @return The builder itself, enabling chaining of build instructions.
     */
    public OpenTsdbQueryBuilder withTags(Map<String, List<String>> tags) {
        Objects.requireNonNull(tags, "tags cannot be null");
        for (String key : tags.keySet()) {
            Objects.requireNonNull(tags.get(key), "list of tags for a key cannot be null");
            checkArgument(!tags.get(key).isEmpty(), "list of tag values for a key cannot be empty");
            List<String> values = tags.get(key);
            for (String value : values) {
                Objects.requireNonNull(value, "null tag values not allowed");
                checkArgument(!value.trim().equals(""), "tag cannot be empty");
            }
        }
        this.tags = Optional.of(tags);
        return this;
    }

    /**
     * Builds the query string that has been constructed with the
     * {@link OpenTsdbQueryBuilder}'s methods. The returned query can be sent
     * (by HTTP GET) to a OpenTSDB server.
     *
     * @return The query string produced by the builder. Note that the returned
     *         query is not a complete URL. More specifically, it does not
     *         include the host/port-part of a full URL. The client needs to
     *         turn the query string into a full URL by adding protocol, host,
     *         and a port.
     * @throws RuntimeException
     *             If the query could not be built.
     */
    public String build() throws RuntimeException {
        Objects.requireNonNull(this.metric, "metric cannot be null");
        Objects.requireNonNull(this.metricAggregator, "metric aggregator cannot be null");

        StringBuilder sb = new StringBuilder();
        sb.append("/q?");

        /*
         * add interval part first with trailing ampersand, so that the
         * resulting string works fine even if the interval specification is
         * missing
         */
        if (this.interval.isPresent()) {
            // make sure all query timestamps are expressed in UTC time
            sb.append("tz=UTC&");
            DateTime startTime = this.interval.get().getStart();
            DateTime startTimeUtc = startTime.withZone(DateTimeZone.UTC);
            sb.append("start=").append(startTimeUtc.toString(openTsdbDateFormat)).append('&');
            DateTime endTime = this.interval.get().getEnd();
            DateTime endTimeUtc = endTime.withZone(DateTimeZone.UTC);
            sb.append("end=").append(endTimeUtc.toString(openTsdbDateFormat)).append('&');
        }

        /*
         * metric part of the string starts with required aggregation function
         */
        sb.append("m=");
        switch (this.metricAggregator) {
        case AVG:
            sb.append("avg:");
            break;
        case MIN:
            sb.append("min:");
            break;
        case MAX:
            sb.append("max:");
            break;
        case SUM:
            sb.append("sum:");
            break;
        default:
            throw new IllegalArgumentException(
                    String.format("Unrecognized metric aggregator '%s'", this.metricAggregator));
        }

        /*
         * down-sampling specification is optional
         */
        if (this.downsamplingSpecification.isPresent()) {
            TimeInterval dowsampleInterval = this.downsamplingSpecification.get().getInterval();
            long intervalSeconds = TimeUnit.SECONDS.convert(dowsampleInterval.getTime(), dowsampleInterval.getUnit());
            sb.append(intervalSeconds).append("s-");

            switch (this.downsamplingSpecification.get().getFunction()) {
            case MEAN:
                sb.append("avg:");
                break;
            case MIN:
                sb.append("min:");
                break;
            case MAX:
                sb.append("max:");
                break;
            case SUM:
                sb.append("sum:");
                break;
            default:
                throw new IllegalArgumentException(
                        "Programmer error, buildQueryString not updated to include all possible downsampling functions!");
            }
        }

        /*
         * if the metric should be interpreted as a rate
         */
        if (this.rate.isPresent() && this.rate.get().booleanValue()) {
            sb.append("rate:");
        }

        // finally the metric itself
        sb.append(this.metric);

        /*
         * All the tags, if any, need to be enclosed in curly braces and a
         * single tag can have multiple values associated to it (i.e. the query
         * asks for values for a single metric but of two given types at the
         * same time)
         */
        if (this.tags.isPresent()) {
            sb.append('{');
            sb.append(String.join(",", buildTagRecords(this.tags.get())));
            sb.append('}');
        }

        // output format should always be ASCII
        sb.append("&ascii");

        /*
         * FIXME Investigate OpenTSDB caching policies
         *
         * With the current default policy, points are cached too aggressively.
         * We need to disable caching to get results at all, and that is quite
         * bad.
         */
        sb.append("&nocache");
        return sb.toString();
    }

    private List<String> buildTagRecords(Map<String, List<String>> tags) {
        Objects.requireNonNull(tags, "Tags cannot be null");

        List<String> tagRecords = new ArrayList<>();
        for (String key : tags.keySet()) {
            tagRecords.add(buildTagRecord(key, tags.get(key)));
        }

        return tagRecords;
    }

    private String buildTagRecord(String key, List<String> values) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(values, "Values cannot be null");
        checkArgument(!values.isEmpty(), "Values cannot be empty");

        return key + "=" + String.join("|", values);
    }

}
