package com.elastisys.autoscaler.metricstreamers.opentsdb.query;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.junit.Test;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.DownsampleFunction;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;

/**
 * Verifies the {@link OpenTsdbQueryBuilder} class, primarily using the queries
 * listed in the <a href="http://opentsdb.net/http-api.html">OpenTSDB HTTP
 * API</a> description page.
 *
 *
 */
public class TestOpenTsdbQueryBuilder {

    private final static String METRIC = "proc.stat.cpu";

    @Test
    public void simpleCpuQuery() {
        // m=sum:proc.stat.cpu
        final String correctQuery = correctQueryString(Optional.empty(), "m=sum:proc.stat.cpu");

        String actualQuery = new OpenTsdbQueryBuilder().withMetric(METRIC).withAggregator(MetricAggregator.SUM).build();
        assertThat(actualQuery, equalTo(correctQuery));
    }

    @Test
    public void cpuRateQuery() {
        // m=sum:rate:proc.stat.cpu
        final String correctQuery = correctQueryString(Optional.empty(), "m=sum:rate:proc.stat.cpu");

        String actualQuery = new OpenTsdbQueryBuilder().withMetric(METRIC).withAggregator(MetricAggregator.SUM)
                .withRateConversion(true).build();
        assertThat(actualQuery, equalTo(correctQuery));
    }

    @Test
    public void cpuRateForParticularHost() {
        // m=sum:rate:proc.stat.cpu{host=foo}
        final String correctQuery = correctQueryString(Optional.empty(), "m=sum:rate:proc.stat.cpu{host=foo}");

        Map<String, List<String>> tags = Maps.of("host", Arrays.asList("foo"));

        String actualQuery = new OpenTsdbQueryBuilder().withMetric(METRIC).withAggregator(MetricAggregator.SUM)
                .withRateConversion(true).withTags(tags).build();
        assertThat(actualQuery, equalTo(correctQuery));
    }

    @Test
    public void cpuRateForParticularHostIdleTime() {
        // m=sum:rate:proc.stat.cpu{host=foo,type=idle}
        final String correctQuery = correctQueryString(Optional.empty(),
                "m=sum:rate:proc.stat.cpu{host=foo,type=idle}");

        Map<String, List<String>> tags = Maps.of("host", asList("foo"), //
                "type", asList("idle"));

        String actualQuery = new OpenTsdbQueryBuilder().withMetric(METRIC).withAggregator(MetricAggregator.SUM)
                .withRateConversion(true).withTags(tags).build();
        assertThat(actualQuery, equalTo(correctQuery));
    }

    @Test
    public void cpuRateForParticularHostUserAndSystemTime() {
        // m=sum:rate:proc.stat.cpu{host=foo,type=user|system}
        final String correctQuery = correctQueryString(Optional.empty(),
                "m=sum:rate:proc.stat.cpu{host=foo,type=user|system}");

        Map<String, List<String>> tags = Maps.of();
        tags.put("host", asList("foo"));
        tags.put("type", asList("user", "system"));

        String actualQuery = new OpenTsdbQueryBuilder().withMetric(METRIC).withAggregator(MetricAggregator.SUM)
                .withRateConversion(true).withTags(tags).build();
        assertThat(actualQuery, equalTo(correctQuery));
    }

    @Test
    public void cpuRateForParticularAllTypes() {
        // m=sum:rate:proc.stat.cpu{host=foo,type=*}

        final String correctQuery = correctQueryString(Optional.empty(), "m=sum:rate:proc.stat.cpu{host=foo,type=*}");

        Map<String, List<String>> tags = Maps.of();
        tags.put("host", asList("foo"));
        tags.put("type", asList("*"));

        String actualQuery = new OpenTsdbQueryBuilder().withMetric(METRIC).withAggregator(MetricAggregator.SUM)
                .withRateConversion(true).withTags(tags).build();
        assertThat(actualQuery, equalTo(correctQuery));
    }

    @Test
    public void averageCpuRateForHostsIdleTime() {
        // m=avg:rate:proc.stat.cpu{type=idle}
        final String correctQuery = correctQueryString(Optional.empty(), "m=avg:rate:proc.stat.cpu{type=idle}");

        Map<String, List<String>> tags = Maps.of();
        tags.put("type", asList("idle"));

        String actualQuery = new OpenTsdbQueryBuilder().withMetric(METRIC).withAggregator(MetricAggregator.AVG)
                .withRateConversion(true).withTags(tags).build();
        assertThat(actualQuery, equalTo(correctQuery));
    }

    @Test
    public void tenMinuteAverageRageForHostsIdleTime() {
        // m=avg:10m-avg:rate:proc.stat.cpu{type=idle}

        /*
         * NOTE: This query has to be translated into 600 seconds rather than 10
         * minutes, since we always specify seconds as the time unit
         */

        final String correctQuery = correctQueryString(Optional.empty(),
                "m=avg:600s-avg:rate:proc.stat.cpu{type=idle}");

        DownsamplingSpecification downsamplingSpecification = new DownsamplingSpecification(
                new TimeInterval(600L, TimeUnit.SECONDS), DownsampleFunction.MEAN);

        Map<String, List<String>> tags = Maps.of("type", asList("idle"));

        String actualQuery = new OpenTsdbQueryBuilder().withMetric(METRIC).withAggregator(MetricAggregator.AVG)
                .withDownsamplingSpecification(downsamplingSpecification).withRateConversion(true).withTags(tags)
                .build();
        assertThat(actualQuery, equalTo(correctQuery));
    }

    @Test
    public void intervalBoundedCpuRateForParticularHostUserAndSystemTime() {
        final DateTime startTime = new DateTime("2011-02-12T13:42:12.000+02:00");
        final DateTime endTime = new DateTime("2011-02-12T19:42:51.000+02:00");

        // Local query time instants will be converted to UTC time
        final String correctQuery = correctQueryString(
                Optional.of("tz=UTC&start=2011/02/12-11:42:12&end=2011/02/12-17:42:51"),
                "m=sum:rate:proc.stat.cpu{host=foo,type=user|system}");

        Map<String, List<String>> tags = Maps.of();
        tags.put("host", asList("foo"));
        tags.put("type", asList("user", "system"));

        String actualQuery = new OpenTsdbQueryBuilder().withMetric(METRIC).withAggregator(MetricAggregator.SUM)
                .withInterval(new Interval(startTime, endTime)).withRateConversion(true).withTags(tags).build();
        assertThat(actualQuery, equalTo(correctQuery));
    }

    @Test
    public void intervalBoundedQueries() {
        // Local query time instants will be converted to UTC time
        DateTime start = new DateTime("2011-02-12T13:42:12.000+02:00");
        DateTime end = new DateTime("2011-02-12T19:42:51.000+02:00");
        String expectedQuery = correctQueryString(
                Optional.of("tz=UTC&start=2011/02/12-11:42:12&end=2011/02/12-17:42:51"), "m=sum:rate:proc.stat.cpu");
        String actualQuery = new OpenTsdbQueryBuilder().withMetric(METRIC).withAggregator(MetricAggregator.SUM)
                .withInterval(new Interval(start, end)).withRateConversion(true).build();
        assertThat(actualQuery, equalTo(expectedQuery));

        // query with time interval right before midnight
        start = new DateTime("2013-03-18T23:59:10Z", DateTimeZone.UTC);
        end = new DateTime("2013-03-18T23:59:59Z", DateTimeZone.UTC);
        expectedQuery = correctQueryString(Optional.of("tz=UTC&start=2013/03/18-23:59:10&end=2013/03/18-23:59:59"),
                "m=sum:rate:proc.stat.cpu");
        actualQuery = new OpenTsdbQueryBuilder().withMetric(METRIC).withAggregator(MetricAggregator.SUM)
                .withInterval(new Interval(start, end)).withRateConversion(true).build();
        assertThat(actualQuery, equalTo(expectedQuery));

        // query with time interval crossing day border
        start = new DateTime("2013-03-18T23:58:00Z", DateTimeZone.UTC);
        end = new DateTime("2013-03-19T00:02:00Z", DateTimeZone.UTC);
        expectedQuery = correctQueryString(Optional.of("tz=UTC&start=2013/03/18-23:58:00&end=2013/03/19-00:02:00"),
                "m=sum:rate:proc.stat.cpu");
        actualQuery = new OpenTsdbQueryBuilder().withMetric(METRIC).withAggregator(MetricAggregator.SUM)
                .withInterval(new Interval(start, end)).withRateConversion(true).build();
        assertThat(actualQuery, equalTo(expectedQuery));

        // query with time interval right after midnight
        start = new DateTime("2013-03-19T00:00:10Z", DateTimeZone.UTC);
        end = new DateTime("2013-03-19T00:00:40Z", DateTimeZone.UTC);
        expectedQuery = correctQueryString(Optional.of("tz=UTC&start=2013/03/19-00:00:10&end=2013/03/19-00:00:40"),
                "m=sum:rate:proc.stat.cpu");
        actualQuery = new OpenTsdbQueryBuilder().withMetric(METRIC).withAggregator(MetricAggregator.SUM)
                .withInterval(new Interval(start, end)).withRateConversion(true).build();
        assertThat(actualQuery, equalTo(expectedQuery));
    }

    private String correctQueryString(final Optional<String> intervalSpecification, final String metricSpecification) {
        if (intervalSpecification.isPresent()) {
            return "/q?" + intervalSpecification.get() + "&" + metricSpecification + "&ascii&nocache";
        } else {
            return "/q?" + metricSpecification + "&ascii&nocache";
        }
    }
}
