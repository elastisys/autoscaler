package com.elastisys.autoscaler.metricstreamers.opentsdb.config;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.junit.Test;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.DownsampleFunction;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.DownsamplingSpecification;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.MetricAggregator;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link OpenTsdbMetricStreamDefinition} class.
 */
public class TestOpenTsdbMetricStreamDefinition {

    /** Sample stream id. */
    private static final String ID = "id";
    /** Sample metric name. */
    private static final String METRIC = "metric";
    /** Sample rate convertion. */
    private static final boolean RATE_CONVERSION = true;
    private static final TimeInterval DOWNSAMPLE_INTERVAL = new TimeInterval(10L, TimeUnit.SECONDS);
    /** Sample downsampling. */
    private static final DownsamplingSpecification DOWNSAMPLE = new DownsamplingSpecification(DOWNSAMPLE_INTERVAL,
            DownsampleFunction.MEAN);
    /** Sample query tags. */
    private static final Map<String, List<String>> TAGS = Maps.of("host", asList("*"));
    /** Sample data settling time. */
    private static final TimeInterval DATA_SETTLING_TIME = new TimeInterval(1L, TimeUnit.MINUTES);
    /** Sample query chunk size. */
    private static final TimeInterval QUERY_CHUNK_SIZE = new TimeInterval(7L, TimeUnit.DAYS);

    @Test
    public void validDefinition() {

        OpenTsdbMetricStreamDefinition streamDefinition = new OpenTsdbMetricStreamDefinition(ID, METRIC,
                MetricAggregator.MAX, RATE_CONVERSION, DOWNSAMPLE, TAGS, DATA_SETTLING_TIME, QUERY_CHUNK_SIZE);
        streamDefinition.validate();

        assertThat(streamDefinition.getId(), is(ID));
        assertThat(streamDefinition.getMetric(), is(METRIC));
        assertThat(streamDefinition.getAggregator(), is(MetricAggregator.MAX));
        assertThat(streamDefinition.isConvertToRate(), is(RATE_CONVERSION));
        assertThat(streamDefinition.getDownsampling(), is(DOWNSAMPLE));
        assertThat(streamDefinition.getTags(), is(TAGS));
        assertThat(streamDefinition.getDataSettlingTime(), is(DATA_SETTLING_TIME));
        assertThat(streamDefinition.getQueryChunkSize(), is(QUERY_CHUNK_SIZE));
    }

    /**
     * Not all fields are mandatory.
     */
    @Test
    public void defaults() {

        DownsamplingSpecification downsample = null;
        Map<String, List<String>> tags = null;
        TimeInterval dataSettlingTime = null;
        TimeInterval queryChunkSize = null;

        OpenTsdbMetricStreamDefinition streamDefinition = new OpenTsdbMetricStreamDefinition(ID, METRIC,
                MetricAggregator.MAX, RATE_CONVERSION, downsample, tags, dataSettlingTime, queryChunkSize);
        streamDefinition.validate();

        assertThat(streamDefinition.getDownsampling(), is(nullValue()));
        assertThat(streamDefinition.getTags(), is(Collections.emptyMap()));
        assertThat(streamDefinition.getDataSettlingTime(), is(new TimeInterval(0L, TimeUnit.SECONDS)));
        assertThat(streamDefinition.getQueryChunkSize(), is(OpenTsdbMetricStreamDefinition.DEFAULT_QUERY_CHUNK_SIZE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithMissingId() {
        String id = null;
        new OpenTsdbMetricStreamDefinition(id, METRIC, MetricAggregator.MAX, RATE_CONVERSION, DOWNSAMPLE, TAGS,
                DATA_SETTLING_TIME, QUERY_CHUNK_SIZE).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithMissingMetric() {
        String metric = null;
        new OpenTsdbMetricStreamDefinition(ID, metric, MetricAggregator.MAX, RATE_CONVERSION, DOWNSAMPLE, TAGS,
                DATA_SETTLING_TIME, QUERY_CHUNK_SIZE).validate();
    }

    /**
     * Metric name must be a valid opentsdb identifier
     */
    @Test(expected = IllegalArgumentException.class)
    public void validateWithIllegalMetricName() {
        String metric = "my?metric";
        new OpenTsdbMetricStreamDefinition(ID, metric, MetricAggregator.MAX, RATE_CONVERSION, DOWNSAMPLE, TAGS,
                DATA_SETTLING_TIME, QUERY_CHUNK_SIZE).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithMissingAggregator() {
        MetricAggregator aggregator = null;
        new OpenTsdbMetricStreamDefinition(ID, METRIC, aggregator, RATE_CONVERSION, DOWNSAMPLE, TAGS,
                DATA_SETTLING_TIME, QUERY_CHUNK_SIZE).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithIllegalDataSettlingTime() throws Exception {
        TimeInterval illegalDataSettlingTime = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": -1 , \"unit\": \"seconds\"}"), TimeInterval.class);

        new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.MAX, RATE_CONVERSION, DOWNSAMPLE, TAGS,
                illegalDataSettlingTime, QUERY_CHUNK_SIZE).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithIllegalQueryChunkSize() {
        TimeInterval illegalQueryChunkSize = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": 0 , \"unit\": \"seconds\"}"), TimeInterval.class);
        new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.MAX, RATE_CONVERSION, DOWNSAMPLE, TAGS,
                DATA_SETTLING_TIME, illegalQueryChunkSize).validate();
    }

    /**
     * Tag keys must be valid opentsdb identifiers.
     */
    @Test(expected = IllegalArgumentException.class)
    public void validateWithIllegalTagKey1() {
        Map<String, List<String>> illegalTags = Maps.of("a:tag", asList("value"));
        new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.MAX, RATE_CONVERSION, DOWNSAMPLE, illegalTags,
                DATA_SETTLING_TIME, QUERY_CHUNK_SIZE).validate();
    }

    @Test
    public void testEqualsAndHashcode() {
        OpenTsdbMetricStreamDefinition stream = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.MAX,
                RATE_CONVERSION, DOWNSAMPLE, TAGS, DATA_SETTLING_TIME, QUERY_CHUNK_SIZE);

        OpenTsdbMetricStreamDefinition equal = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.MAX,
                RATE_CONVERSION, DOWNSAMPLE, TAGS, DATA_SETTLING_TIME, QUERY_CHUNK_SIZE);
        OpenTsdbMetricStreamDefinition differentId = new OpenTsdbMetricStreamDefinition("otherId", METRIC,
                MetricAggregator.MAX, RATE_CONVERSION, DOWNSAMPLE, TAGS, DATA_SETTLING_TIME, QUERY_CHUNK_SIZE);
        OpenTsdbMetricStreamDefinition differentMetric = new OpenTsdbMetricStreamDefinition(ID, "otherMetric",
                MetricAggregator.MAX, RATE_CONVERSION, DOWNSAMPLE, TAGS, DATA_SETTLING_TIME, QUERY_CHUNK_SIZE);
        OpenTsdbMetricStreamDefinition differentAggregator = new OpenTsdbMetricStreamDefinition(ID, METRIC,
                MetricAggregator.MIN, RATE_CONVERSION, DOWNSAMPLE, TAGS, DATA_SETTLING_TIME, QUERY_CHUNK_SIZE);
        OpenTsdbMetricStreamDefinition differentRate = new OpenTsdbMetricStreamDefinition(ID, METRIC,
                MetricAggregator.MAX, !RATE_CONVERSION, DOWNSAMPLE, TAGS, DATA_SETTLING_TIME, QUERY_CHUNK_SIZE);
        OpenTsdbMetricStreamDefinition differentDownsampling = new OpenTsdbMetricStreamDefinition(ID, METRIC,
                MetricAggregator.MAX, RATE_CONVERSION,
                new DownsamplingSpecification(new TimeInterval(10L, TimeUnit.MINUTES), DownsampleFunction.MIN), TAGS,
                DATA_SETTLING_TIME, QUERY_CHUNK_SIZE);
        Map<String, List<String>> otherTags = Maps.of("host", asList("h1", "h2"));
        OpenTsdbMetricStreamDefinition differentTags = new OpenTsdbMetricStreamDefinition(ID, METRIC,
                MetricAggregator.MAX, RATE_CONVERSION, DOWNSAMPLE, otherTags, DATA_SETTLING_TIME, QUERY_CHUNK_SIZE);
        OpenTsdbMetricStreamDefinition differentDataSettlingTime = new OpenTsdbMetricStreamDefinition(ID, METRIC,
                MetricAggregator.MAX, RATE_CONVERSION, DOWNSAMPLE, TAGS, new TimeInterval(5L, TimeUnit.MINUTES),
                QUERY_CHUNK_SIZE);
        OpenTsdbMetricStreamDefinition differentQueryChunkSize = new OpenTsdbMetricStreamDefinition(ID, METRIC,
                MetricAggregator.MAX, RATE_CONVERSION, DOWNSAMPLE, TAGS, DATA_SETTLING_TIME,
                new TimeInterval(5L, TimeUnit.DAYS));

        assertTrue(stream.equals(equal));
        assertFalse(stream.equals(differentId));
        assertFalse(stream.equals(differentMetric));
        assertFalse(stream.equals(differentAggregator));
        assertFalse(stream.equals(differentRate));
        assertFalse(stream.equals(differentDownsampling));
        assertFalse(stream.equals(differentTags));
        assertFalse(stream.equals(differentDataSettlingTime));
        assertFalse(stream.equals(differentQueryChunkSize));

        assertTrue(stream.hashCode() == equal.hashCode());
        assertFalse(stream.hashCode() == differentId.hashCode());
        assertFalse(stream.hashCode() == differentMetric.hashCode());
        assertFalse(stream.hashCode() == differentAggregator.hashCode());
        assertFalse(stream.hashCode() == differentRate.hashCode());
        assertFalse(stream.hashCode() == differentDownsampling.hashCode());
        assertFalse(stream.hashCode() == differentTags.hashCode());
        assertFalse(stream.hashCode() == differentDataSettlingTime.hashCode());
        assertFalse(stream.hashCode() == differentQueryChunkSize.hashCode());
    }

    /**
     * Exercises the {@link OpenTsdbMetricStreamDefinition#getQuery()} method.
     */
    @Test
    public void testGetQuery() {
        Map<String, List<String>> noTags = Maps.of();
        Map<String, List<String>> singleTag = Maps.of("host", asList("*"));
        Map<String, List<String>> multiTags = Maps.of("host", asList("*"), "type", asList("t1.micro", "m1.small"));

        // no tags
        String query = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.SUM, RATE_CONVERSION, DOWNSAMPLE,
                noTags, DATA_SETTLING_TIME, QUERY_CHUNK_SIZE).getQuery();
        assertThat(query, is("/q?m=sum:10s-avg:rate:metric{}&ascii&nocache"));

        // single tag
        query = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.SUM, RATE_CONVERSION, DOWNSAMPLE,
                singleTag, DATA_SETTLING_TIME, QUERY_CHUNK_SIZE).getQuery();
        assertThat(query, is("/q?m=sum:10s-avg:rate:metric{host=*}&ascii&nocache"));

        // multiple tags
        query = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.SUM, RATE_CONVERSION, DOWNSAMPLE,
                multiTags, DATA_SETTLING_TIME, QUERY_CHUNK_SIZE).getQuery();
        assertThat(query, is("/q?m=sum:10s-avg:rate:metric{host=*,type=t1.micro|m1.small}&ascii&nocache"));

        // different downsampling
        query = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.SUM, RATE_CONVERSION,
                new DownsamplingSpecification(new TimeInterval(1L, TimeUnit.MINUTES), DownsampleFunction.MEAN),
                singleTag, DATA_SETTLING_TIME, QUERY_CHUNK_SIZE).getQuery();
        assertThat(query, is("/q?m=sum:60s-avg:rate:metric{host=*}&ascii&nocache"));

        // different aggregator
        query = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.AVG, RATE_CONVERSION, DOWNSAMPLE,
                singleTag, DATA_SETTLING_TIME, QUERY_CHUNK_SIZE).getQuery();
        assertThat(query, is("/q?m=avg:10s-avg:rate:metric{host=*}&ascii&nocache"));

        // no downsampling
        query = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.AVG, RATE_CONVERSION, null, singleTag,
                DATA_SETTLING_TIME, QUERY_CHUNK_SIZE).getQuery();
        assertThat(query, is("/q?m=avg:rate:metric{host=*}&ascii&nocache"));

        // no rate conversion
        query = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.AVG, false, null, singleTag,
                DATA_SETTLING_TIME, QUERY_CHUNK_SIZE).getQuery();
        assertThat(query, is("/q?m=avg:metric{host=*}&ascii&nocache"));
    }

    /**
     * Exercises the
     * {@link OpenTsdbMetricStreamDefinition#makeQuery(org.joda.time.Interval)}
     * method.
     */
    @Test
    public void testGetTimeBoundedQueries() {
        Map<String, List<String>> singleTag = Maps.of("host", asList("*"));

        DateTime start = new DateTime("2012-04-01T10:00:00+02:00", DateTimeZone.UTC);
        DateTime end = new DateTime("2012-04-01T14:00:00+02:00", DateTimeZone.UTC);
        Interval interval = new Interval(start, end);
        String query = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.AVG, false, null, singleTag,
                null, QUERY_CHUNK_SIZE).makeQuery(interval);
        assertThat(query, is(
                "/q?tz=UTC&start=2012/04/01-08:00:00&end=2012/04/01-12:00:00" + "&m=avg:metric{host=*}&ascii&nocache"));

        // query with time interval right before midnight
        start = new DateTime("2013-03-18T23:59:10Z", DateTimeZone.UTC);
        end = new DateTime("2013-03-18T23:59:59Z", DateTimeZone.UTC);
        query = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.AVG, false, null, singleTag, null,
                QUERY_CHUNK_SIZE).makeQuery(new Interval(start, end));
        assertThat(query, is(
                "/q?tz=UTC&start=2013/03/18-23:59:10&end=2013/03/18-23:59:59" + "&m=avg:metric{host=*}&ascii&nocache"));

        // query with time interval crossing day border
        start = new DateTime("2013-03-18T23:58:00Z", DateTimeZone.UTC);
        end = new DateTime("2013-03-19T00:02:00Z", DateTimeZone.UTC);
        query = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.AVG, false, null, singleTag, null,
                QUERY_CHUNK_SIZE).makeQuery(new Interval(start, end));
        assertThat(query, is(
                "/q?tz=UTC&start=2013/03/18-23:58:00&end=2013/03/19-00:02:00" + "&m=avg:metric{host=*}&ascii&nocache"));

        // query with time interval right after midnight
        start = new DateTime("2013-03-19T00:00:10Z", DateTimeZone.UTC);
        end = new DateTime("2013-03-19T00:00:40Z", DateTimeZone.UTC);
        query = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.AVG, false, null, singleTag, null,
                QUERY_CHUNK_SIZE).makeQuery(new Interval(start, end));
        assertThat(query, is(
                "/q?tz=UTC&start=2013/03/19-00:00:10&end=2013/03/19-00:00:40" + "&m=avg:metric{host=*}&ascii&nocache"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getTimeBoundedQueryWithSameStartAndEndTime() {
        DateTime start = UtcTime.parse("2012-04-01T10:00:00Z");
        DateTime end = UtcTime.parse("2012-04-01T10:00:00Z");
        Interval interval = new Interval(start, end);
        new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.AVG, false, null, null, null, QUERY_CHUNK_SIZE)
                .makeQuery(interval);
    }

    @Test
    public void getDataSettlingPoint() {
        // freeze current time
        FrozenTime.setFixed(UtcTime.parse("2014-03-20T08:00:00.000Z"));
        DateTime now = UtcTime.now();
        try {
            // no data settling time => data settling point is now
            TimeInterval dataSettlingTime = new TimeInterval(0L, TimeUnit.SECONDS);
            OpenTsdbMetricStreamDefinition stream = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.AVG,
                    false, null, null, dataSettlingTime, QUERY_CHUNK_SIZE);
            assertThat(stream.getDataSettlingTime(), is(new TimeInterval(0L, TimeUnit.SECONDS)));
            assertThat(stream.getDataSettlingPoint(), is(now));

            // data settling time => data settling point is in the past
            dataSettlingTime = new TimeInterval(30L, TimeUnit.SECONDS);
            stream = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.AVG, false, null, null,
                    dataSettlingTime, QUERY_CHUNK_SIZE);
            assertThat(stream.getDataSettlingTime(), is(new TimeInterval(30L, TimeUnit.SECONDS)));
            assertThat(stream.getDataSettlingPoint(), is(now.minusSeconds(30)));
        } finally {
            FrozenTime.resumeSystemTime();
        }
    }

    /**
     * Query intervals are to be adjusted to not include too "hot" data (that
     * is, data points that are more recent than the data settling time).
     */
    @Test
    public void adjustForDataSettlingTime() {
        // freeze current time
        FrozenTime.setFixed(UtcTime.parse("2014-03-20T08:00:00.000Z"));
        DateTime now = UtcTime.now();

        try {

            // request data from the last 30 seconds
            Interval interval = new Interval(now.minusSeconds(30), now);
            // no data settling time => no truncation of end time
            TimeInterval dataSettlingTime = new TimeInterval(0L, TimeUnit.SECONDS);
            Interval adjustedInterval = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.AVG, false,
                    null, null, dataSettlingTime, QUERY_CHUNK_SIZE).adjustForDataSettlingTime(interval);
            assertThat(adjustedInterval, is(interval));

            // request data from the last 30 seconds
            interval = new Interval(now.minusSeconds(30), now);
            // data settling time 15s => should truncate end time by 15s
            dataSettlingTime = new TimeInterval(15L, TimeUnit.SECONDS);
            adjustedInterval = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.AVG, false, null, null,
                    dataSettlingTime, QUERY_CHUNK_SIZE).adjustForDataSettlingTime(interval);
            assertThat(adjustedInterval, is(new Interval(now.minusSeconds(30), now.minusSeconds(15))));

            // request data from the last 30 seconds
            interval = new Interval(now.minusSeconds(30), now);
            // data settling time 20s => should truncate end time by 20s
            dataSettlingTime = new TimeInterval(20L, TimeUnit.SECONDS);
            adjustedInterval = new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.AVG, false, null, null,
                    dataSettlingTime, QUERY_CHUNK_SIZE).adjustForDataSettlingTime(interval);
            assertThat(adjustedInterval, is(new Interval(now.minusSeconds(30), now.minusSeconds(20))));
        } finally {
            FrozenTime.resumeSystemTime();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyThatQueryIntervalAfterDataSettlingTimeIsDisallowed() {
        // freeze current time
        FrozenTime.setFixed(UtcTime.parse("2014-03-20T08:00:00.000Z"));

        try {
            // request data from the last 20 seconds
            Interval interval = new Interval(UtcTime.now().minusSeconds(20), UtcTime.now());
            // data settling time 30s => query interval is too recent, query
            // disallowed
            TimeInterval dataSettlingTime = new TimeInterval(30L, TimeUnit.SECONDS);
            new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.AVG, false, null, null, dataSettlingTime,
                    QUERY_CHUNK_SIZE).adjustForDataSettlingTime(interval);
        } finally {
            FrozenTime.resumeSystemTime();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyThatQueryIntervalStartingAtDataSettlingTimeIsDisallowed() {
        // freeze current time
        FrozenTime.setFixed(UtcTime.parse("2014-03-20T08:00:00.000Z"));

        try {
            // request data from the last 30 seconds
            Interval interval = new Interval(UtcTime.now().minusSeconds(30), UtcTime.now());
            // data settling time 30s => query interval is too recent, query
            // disallowed
            TimeInterval dataSettlingTime = new TimeInterval(30L, TimeUnit.SECONDS);
            new OpenTsdbMetricStreamDefinition(ID, METRIC, MetricAggregator.AVG, false, null, null, dataSettlingTime,
                    QUERY_CHUNK_SIZE).adjustForDataSettlingTime(interval);
        } finally {
            FrozenTime.resumeSystemTime();
        }
    }

}
