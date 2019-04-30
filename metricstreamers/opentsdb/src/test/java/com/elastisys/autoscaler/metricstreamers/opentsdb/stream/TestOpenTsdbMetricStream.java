package com.elastisys.autoscaler.metricstreamers.opentsdb.stream;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.Downsample;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.DownsampleFunction;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.metricstreamers.opentsdb.client.OpenTsdbQueryClient;
import com.elastisys.autoscaler.metricstreamers.opentsdb.config.OpenTsdbMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.DownsamplingSpecification;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.MetricAggregator;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.time.FrozenTime;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link OpenTsdbMetricStream} with a mocked http client.
 */
public class TestOpenTsdbMetricStream {

    static final Logger LOG = LoggerFactory.getLogger(TestOpenTsdbMetricStream.class);

    /** Mocked client used by {@link OpenTsdbMetricStream} under test. */
    private final OpenTsdbQueryClient clientMock = mock(OpenTsdbQueryClient.class);

    @Before
    public void onSetup() {
        // freeze the current time prior to running test
        FrozenTime.setFixed(UtcTime.parse("2017-01-01T12:00:00.000Z"));
    }

    /**
     * Verifies that the {@link OpenTsdbMetricStream} sends proper query URLs
     * based on its stream definition.
     */
    @Test
    public void queryBuilding() throws Exception {
        OpenTsdbMetricStreamDefinition basicStream = new OpenTsdbMetricStreamDefinition("requests.stream", "requests",
                MetricAggregator.SUM, false, null, null, null, null);

        OpenTsdbMetricStream metricStream = new OpenTsdbMetricStream(LOG, this.clientMock,
                new MetricStreamConfig("opentsdb", 4242, basicStream));
        QueryResultSet resultSet = metricStream.query(lastFiveMins(), null);
        resultSet.fetchNext();
        verify(this.clientMock).query(
                "http://opentsdb:4242/q?tz=UTC&start=2017/01/01-11:55:00&end=2017/01/01-12:00:00&m=sum:requests&ascii&nocache");

        // with rate conversion
        metricStream = new OpenTsdbMetricStream(LOG, this.clientMock,
                new MetricStreamConfig("opentsdb", 4242, basicStream.withConverToRate(true)));
        resultSet = metricStream.query(lastFiveMins(), null);
        resultSet.fetchNext();
        verify(this.clientMock).query(
                "http://opentsdb:4242/q?tz=UTC&start=2017/01/01-11:55:00&end=2017/01/01-12:00:00&m=sum:rate:requests&ascii&nocache");

        // with downsampling
        metricStream = new OpenTsdbMetricStream(LOG, this.clientMock, new MetricStreamConfig("opentsdb", 4242,
                basicStream.withDownsampling(new DownsamplingSpecification(new TimeInterval(10L, TimeUnit.MINUTES),
                        DownsampleFunction.MEAN))));
        resultSet = metricStream.query(lastFiveMins(), null);
        resultSet.fetchNext();
        verify(this.clientMock).query(
                "http://opentsdb:4242/q?tz=UTC&start=2017/01/01-11:55:00&end=2017/01/01-12:00:00&m=sum:600s-avg:requests&ascii&nocache");

        // with tags
        Map<String, List<String>> tags = Maps.of("backend", asList("SOURCE"));
        metricStream = new OpenTsdbMetricStream(LOG, this.clientMock,
                new MetricStreamConfig("opentsdb", 4242, basicStream.withTags(tags)));
        resultSet = metricStream.query(lastFiveMins(), null);
        resultSet.fetchNext();
        verify(this.clientMock).query(
                "http://opentsdb:4242/q?tz=UTC&start=2017/01/01-11:55:00&end=2017/01/01-12:00:00&m=sum:requests{backend=SOURCE}&ascii&nocache");
    }

    /**
     * Verifies that {@link OpenTsdbQueryClient} responses are properly received
     * by the {@link OpenTsdbMetricStream}.
     */
    @Test
    public void responseHandling() throws Exception {
        OpenTsdbMetricStreamDefinition basicStream = new OpenTsdbMetricStreamDefinition("requests.stream", "requests",
                MetricAggregator.SUM, false, null, null, null, null);
        OpenTsdbMetricStream metricStream = new OpenTsdbMetricStream(LOG, this.clientMock,
                new MetricStreamConfig("opentsdb", 4242, basicStream));

        // should handle empty response
        List<MetricValue> emptyResponse = values();
        when(this.clientMock.query(anyString())).thenReturn(emptyResponse);
        assertThat(metricStream.query(lastFiveMins(), null).fetchNext().getMetricValues(), is(emptyResponse));

        // should handle non-empty response
        List<MetricValue> response = values(value(1, 20), value(2, 10));
        when(this.clientMock.query(anyString())).thenReturn(response);
        assertThat(metricStream.query(lastFiveMins(), null).fetchNext().getMetricValues(), is(response));
    }

    /**
     * Query responses that return data points out of order are to be sorted.
     */
    @Test
    public void unorderedResponse() throws Exception {
        OpenTsdbMetricStreamDefinition basicStream = new OpenTsdbMetricStreamDefinition("requests.stream", "requests",
                MetricAggregator.SUM, false, null, null, null, null);
        OpenTsdbMetricStream metricStream = new OpenTsdbMetricStream(LOG, this.clientMock,
                new MetricStreamConfig("opentsdb", 4242, basicStream));

        // values retrieved out of order
        MetricValue value1 = value(1, 60);
        MetricValue value2 = value(2, 40);
        MetricValue value3 = value(3, 20);
        List<MetricValue> response = values(value3, value1, value2);
        when(this.clientMock.query(anyString())).thenReturn(response);

        // make sure values are returned in order
        assertThat(metricStream.query(lastFiveMins(), null).fetchNext().getMetricValues(),
                is(values(value1, value2, value3)));

    }

    /**
     * Execute a fetch request and make sure that only data points within the
     * requested time interval are returned. OpenTSDB has a habit of returning
     * more data points than asked for.
     */
    @Test
    public void tooManyReturnedDataPoints() throws Exception {
        OpenTsdbMetricStreamDefinition basicStream = new OpenTsdbMetricStreamDefinition("requests.stream", "requests",
                MetricAggregator.SUM, false, null, null, null, null);
        OpenTsdbMetricStream metricStream = new OpenTsdbMetricStream(LOG, this.clientMock,
                new MetricStreamConfig("opentsdb", 4242, basicStream));

        // a couple of values that fall outside both the left and right end of
        // the query interval
        MetricValue tooOldValue1 = value(1, 350);
        MetricValue tooOldValue2 = value(2, 310);
        MetricValue value1 = value(3, 240);
        MetricValue value2 = value(4, 180);
        MetricValue value3 = value(5, 120);
        MetricValue tooNewValue1 = value(6, 20);
        MetricValue tooNewValue2 = value(7, 10);
        List<MetricValue> response = values(tooOldValue1, tooOldValue2, value1, value2, value3, tooNewValue1,
                tooNewValue2);
        when(this.clientMock.query(anyString())).thenReturn(response);

        // make sure only the data points within the requested interval are
        // returned.
        Interval interval = new Interval(UtcTime.now().minusSeconds(300), UtcTime.now().minusSeconds(30));
        assertThat(metricStream.query(interval, null).fetchNext().getMetricValues(),
                is(values(value1, value2, value3)));
    }

    /**
     * In case a data settling time was specified, the query time interval
     * should always be adjusted to not include too recent (unsettled) data.
     */
    @Test
    public void adjustQueryForDataSettlingTime() throws Exception {
        TimeInterval dataSettlingTime = new TimeInterval(2L, TimeUnit.MINUTES);

        OpenTsdbMetricStreamDefinition basicStream = new OpenTsdbMetricStreamDefinition("requests.stream", "requests",
                MetricAggregator.SUM, false, null, null, dataSettlingTime, null);

        OpenTsdbMetricStream metricStream = new OpenTsdbMetricStream(LOG, this.clientMock,
                new MetricStreamConfig("opentsdb", 4242, basicStream));
        QueryResultSet resultSet = metricStream.query(lastFiveMins(), null);
        resultSet.fetchNext();

        // verify that the query time interval end was adjusted to not include
        // present time 12:00:00 but be truncated at the data settling point
        // (now - 2min == 11:58:00)
        verify(this.clientMock).query(
                "http://opentsdb:4242/q?tz=UTC&start=2017/01/01-11:55:00&end=2017/01/01-11:58:00&m=sum:requests&ascii&nocache");
    }

    /**
     * When too recent data is requested (that is, the requested time interval
     * is after stream's data settling point) query should be ignored.
     */
    @Test
    public void tooRecentDataRequested() throws Exception {
        TimeInterval dataSettlingTime = new TimeInterval(2L, TimeUnit.MINUTES);
        OpenTsdbMetricStreamDefinition basicStream = new OpenTsdbMetricStreamDefinition("requests.stream", "requests",
                MetricAggregator.SUM, false, null, null, dataSettlingTime, null);

        OpenTsdbMetricStream metricStream = new OpenTsdbMetricStream(LOG, this.clientMock,
                new MetricStreamConfig("opentsdb", 4242, basicStream));

        // data is requested after the stream's data settling point
        Interval interval = new Interval(UtcTime.now().minusMinutes(1), UtcTime.now());

        QueryResultSet resultSet = metricStream.query(interval, null);
        assertFalse(resultSet.hasNext());
        // verify that no call was ever made to remote database
        verifyZeroInteractions(this.clientMock);
    }

    /**
     * As long as the query interval is shorter than the max query chunk size,
     * query won't be split into sub-queries.
     */
    @Test
    public void singleChunkResponseWhenQueryIntervalShorterThanMaxChunkSize() throws Exception {
        TimeInterval queryChunkSize = new TimeInterval(5L, TimeUnit.MINUTES);

        OpenTsdbMetricStreamDefinition basicStream = new OpenTsdbMetricStreamDefinition("requests.stream", "requests",
                MetricAggregator.SUM, false, null, null, null, queryChunkSize);

        OpenTsdbMetricStream metricStream = new OpenTsdbMetricStream(LOG, this.clientMock,
                new MetricStreamConfig("opentsdb", 4242, basicStream));
        QueryResultSet resultSet = metricStream.query(lastFiveMins(), null);

        assertTrue(resultSet.hasNext());
        resultSet.fetchNext();
        verify(this.clientMock).query(
                "http://opentsdb:4242/q?tz=UTC&start=2017/01/01-11:55:00&end=2017/01/01-12:00:00&m=sum:requests&ascii&nocache");

        assertFalse(resultSet.hasNext());
    }

    /**
     * A query that exceed queryChunkSize should be split into several
     * sub-queries which are fetched incrementally.
     */
    @Test
    public void chunkedQueryFetchingOnLargeQueries() throws Exception {
        TimeInterval queryChunkSize = new TimeInterval(1L, TimeUnit.HOURS);
        OpenTsdbMetricStreamDefinition basicStream = new OpenTsdbMetricStreamDefinition("requests.stream", "requests",
                MetricAggregator.SUM, false, null, null, null, queryChunkSize);
        OpenTsdbMetricStream metricStream = new OpenTsdbMetricStream(LOG, this.clientMock,
                new MetricStreamConfig("opentsdb", 4242, basicStream));

        Interval interval = new Interval(UtcTime.parse("2017-01-01T09:00:00.000Z"),
                UtcTime.parse("2017-01-01T11:30:00.000Z"));
        QueryResultSet resultSet = metricStream.query(interval, null);

        // first sub-query
        assertTrue(resultSet.hasNext());
        resultSet.fetchNext();
        verify(this.clientMock).query(
                "http://opentsdb:4242/q?tz=UTC&start=2017/01/01-09:00:00&end=2017/01/01-10:00:00&m=sum:requests&ascii&nocache");
        reset(this.clientMock);

        // second sub-query
        assertTrue(resultSet.hasNext());
        resultSet.fetchNext();
        verify(this.clientMock).query(
                "http://opentsdb:4242/q?tz=UTC&start=2017/01/01-10:00:00&end=2017/01/01-11:00:00&m=sum:requests&ascii&nocache");
        reset(this.clientMock);

        // third sub-query
        assertTrue(resultSet.hasNext());
        resultSet.fetchNext();
        verify(this.clientMock).query(
                "http://opentsdb:4242/q?tz=UTC&start=2017/01/01-11:00:00&end=2017/01/01-11:30:00&m=sum:requests&ascii&nocache");
        reset(this.clientMock);

        assertFalse(resultSet.hasNext());
    }

    /**
     * When query specifies a downsampling parameter, that should take
     * precedence over the one configured for the stream.
     */
    @Test
    public void queryHints() throws Exception {
        OpenTsdbMetricStreamDefinition basicStream = new OpenTsdbMetricStreamDefinition("requests.stream", "requests",
                MetricAggregator.SUM, false, null, null, null, null);
        DownsamplingSpecification downsampling = new DownsamplingSpecification(new TimeInterval(10L, TimeUnit.MINUTES),
                DownsampleFunction.MEAN);
        OpenTsdbMetricStream metricStream = new OpenTsdbMetricStream(LOG, this.clientMock,
                new MetricStreamConfig("opentsdb", 4242, basicStream.withDownsampling(downsampling)));

        // give downsampling as a query hint => should override stream config
        QueryOptions queryOptions = new QueryOptions(
                new Downsample(new TimeInterval(5L, TimeUnit.MINUTES), DownsampleFunction.MAX));
        QueryResultSet resultSet = metricStream.query(lastFiveMins(), queryOptions);
        resultSet.fetchNext();
        verify(this.clientMock).query(
                "http://opentsdb:4242/q?tz=UTC&start=2017/01/01-11:55:00&end=2017/01/01-12:00:00&m=sum:300s-max:requests&ascii&nocache");
    }

    /**
     * Creates a {@link MetricValue} with a given value and an age in seconds
     * (relative to current time as provided by the mocked {@link TimeSource}).
     *
     * @param value
     *            The value.
     * @param secondsAgo
     *            Age in seconds (relative to current time).
     * @return
     */
    private MetricValue value(double value, int secondsAgo) {
        Map<String, String> tags = Maps.of();
        return new MetricValue("requests", value, UtcTime.now().minusSeconds(secondsAgo), tags);
    }

    /**
     * Creates a {@link List} of {@link MetricValue}s.
     *
     * @param metricValues
     * @return
     */
    private List<MetricValue> values(MetricValue... metricValues) {
        List<MetricValue> values = Arrays.asList(metricValues);
        Collections.sort(values);
        return values;
    }

    private Interval lastFiveMins() {
        DateTime now = UtcTime.now();
        return new Interval(now.minusMinutes(5), now);
    }

}
