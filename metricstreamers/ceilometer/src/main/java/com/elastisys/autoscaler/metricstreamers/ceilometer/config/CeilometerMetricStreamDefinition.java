package com.elastisys.autoscaler.metricstreamers.ceilometer.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamer;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Describes what query to run for this particular Ceilometer
 * {@link MetricStream}.
 *
 * @see CeilometerMetricStreamerConfig
 */
public class CeilometerMetricStreamDefinition {
    public static final Boolean DEFAULT_RATE_CONVERSION = false;
    public static final TimeInterval DEFAULT_DATA_SETTLING_TIME = new TimeInterval(0L, TimeUnit.SECONDS);
    public static final TimeInterval DEFAULT_QUERY_CHUNK_SIZE = new TimeInterval(14L, TimeUnit.DAYS);

    /**
     * The id of the metric stream. This is the id that will be used by clients
     * wishing to subscribe to {@link MetricValue}s for this metric stream.
     * Required.
     */
    private final String id;

    /**
     * The particular Ceilometer meter to query. Required.
     * <p/>
     * Metrics are in Ceilometer terms called "meters", read the documentation
     * about them <a href=
     * "http://docs.openstack.org/developer/ceilometer/webapi/v2.html#meters">
     * here</a>.
     */
    private final String meter;

    /**
     * A resource identifier which can be used narrow down the query to only
     * retrieve metric values associated with a given OpenStack resource. May be
     * <code>null</code>.
     */
    private final String resourceId;

    /**
     * A downsampling specification which can be set to query for statistics
     * rather than raw samples. May be <code>null</code> which means samples
     * will be collected for the given meter.
     */
    private final Downsampling downsampling;

    /**
     * When <code>true</code> the stream will feed the change rate of the metric
     * rather than the absolute values of the metric. This is useful if the
     * meter represents a cumulative metric. May be <code>null</code>. Default
     * is {@link #DEFAULT_RATE_CONVERSION}.
     */
    private final Boolean convertToRate;

    /**
     * The minimum age of requested data points. The {@link MetricStreamer} will
     * never request values newer than this from Ceilometer. This value can be
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
     * The maximum time period that a single query attempt to fetch in a single
     * call. A query with a longer time interval will be run incrementally, each
     * fetching a sub-interval of this duration. This type of incremental
     * retrieval of large {@link QueryResultSet}s limits the amount of (memory)
     * resources involved in processing large queries. May be <code>null</code>.
     * Default: {@value #DEFAULT_QUERY_CHUNK_SIZE}.
     */
    private final TimeInterval queryChunkSize;

    /**
     * Constructs a new {@link CeilometerMetricStreamDefinition}.
     *
     * @param id
     *            The id of the metric stream. This is the id that will be used
     *            by clients wishing to subscribe to {@link MetricValue}s for
     *            this metric stream. Required.
     * @param meter
     *            The particular Ceilometer meter to query. Required.
     * @param resourceId
     *            A resource identifier which can be used narrow down the query
     *            to only retrieve metric values associated with a given
     *            OpenStack resource. May be <code>null</code>.
     * @param downsampling
     *            A downsampling specification which can be set to query for
     *            statistics rather than raw samples. May be <code>null</code>
     *            which means samples will be collected for the given meter.
     * @param convertToRate
     *            When <code>true</code> the stream will feed the change rate of
     *            the metric rather than the absolute values of the metric. This
     *            is useful if the meter represents a cumulative metric. May be
     *            <code>null</code>. Default is
     *            {@link #DEFAULT_RATE_CONVERSION}.
     * @param dataSettlingTime
     *            The minimum age of requested data points. The
     *            {@link MetricStreamer} will never request values newer than
     *            this from Ceilometer. This value can be regarded as the
     *            expected "settling time" of new data points.
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
     *
     */
    public CeilometerMetricStreamDefinition(String id, String meter, String resourceId, Downsampling downsampling,
            Boolean convertToRate, TimeInterval dataSettlingTime, TimeInterval queryChunkSize) {
        this.id = id;
        this.meter = meter;
        this.resourceId = resourceId;
        this.downsampling = downsampling;
        this.convertToRate = convertToRate;
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
     * The particular Ceilometer meter to query.
     *
     * @return
     */
    public String getMeter() {
        return this.meter;
    }

    /**
     * A resource identifier which can be used narrow down the query to only
     * retrieve metric values associated with a given OpenStack resource. May be
     * <code>null</code>.
     *
     * @return
     */
    public Optional<String> getResourceId() {
        return Optional.ofNullable(this.resourceId);
    }

    /**
     * A downsampling specification which can be set to query for statistics
     * rather than raw samples. An absent value means that samples will be
     * collected for the given meter rather than "statistics".
     *
     * @return
     */
    public Optional<Downsampling> getDownsampling() {
        return Optional.ofNullable(this.downsampling);
    }

    /**
     * When <code>true</code> the stream will feed the change rate of the metric
     * rather than the absolute values of the metric.
     *
     * @return
     */
    public boolean isConvertToRate() {
        return Optional.ofNullable(this.convertToRate).orElse(DEFAULT_RATE_CONVERSION);
    }

    /**
     * The minimum age of requested data points. The {@link MetricStreamer} will
     * never request values newer than this from Ceilometer. This value can be
     * regarded as the expected "settling time" of new data points.
     * <p/>
     * When requesting recent aggregate metric data points, there is always a
     * risk of seeing partial/incomplete results before metric values from all
     * sources have been registered. The data settling time is intended to give
     * all sources a chance to report before fetching recent values.
     *
     * @return
     */
    public TimeInterval getDataSettlingTime() {
        return Optional.ofNullable(this.dataSettlingTime).orElse(DEFAULT_DATA_SETTLING_TIME);
    }

    /**
     * The maximum time period that a single query will attempt to fetch in a
     * single call. A query with a longer time interval will be run
     * incrementally, each fetching a sub-interval of this duration. This type
     * of incremental retrieval of large {@link QueryResultSet}s limits the
     * amount of (memory) resources involved in processing large queries.
     *
     * @return
     */
    public TimeInterval getQueryChunkSize() {
        return Optional.ofNullable(this.queryChunkSize).orElse(DEFAULT_QUERY_CHUNK_SIZE);
    }

    /**
     * Validates that this {@link CeilometerMetricStreamDefinition} contains
     * sufficient information to allow a valid Ceilometer meter query to be
     * built. If it does, the method will just return. If it is found to be
     * invalid, an {@link IllegalArgumentException} is thrown.
     *
     * @throws IllegalArgumentException
     *
     */
    public void validate() throws IllegalArgumentException {
        checkArgument(this.id != null, "missing id");
        checkArgument(this.meter != null, "missing meter");
        try {
            getDataSettlingTime().validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("dataSettlingTime: " + e.getMessage(), e);
        }
        checkArgument(getDataSettlingTime().getSeconds() >= 0, "dataSettlingTime must be a positive duration");

        if (getDownsampling().isPresent()) {
            try {
                getDownsampling().get().validate();
            } catch (Exception e) {
                throw new IllegalArgumentException("downsampling: " + e.getMessage(), e);
            }
        }

        try {
            getQueryChunkSize().validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("queryChunkSize: " + e.getMessage(), e);
        }
        checkArgument(getQueryChunkSize().getSeconds() > 0, "queryChunkSize must be a positive duration");

    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.meter, this.downsampling, this.convertToRate, this.dataSettlingTime,
                this.queryChunkSize);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CeilometerMetricStreamDefinition) {
            CeilometerMetricStreamDefinition that = (CeilometerMetricStreamDefinition) obj;
            return Objects.equals(this.id, that.id) && Objects.equals(this.meter, that.meter)
                    && Objects.equals(this.downsampling, that.downsampling)
                    && Objects.equals(this.convertToRate, that.convertToRate)
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
     * Returns a copy of this object with the {@link #downsampling} field
     * replaced by the given value.
     *
     * @param downsampling
     * @return
     */
    public CeilometerMetricStreamDefinition withDownsampling(Downsampling downsampling) {
        return new CeilometerMetricStreamDefinition(this.id, this.meter, this.resourceId, downsampling,
                this.convertToRate, this.dataSettlingTime, this.queryChunkSize);
    }
}
