package com.elastisys.autoscaler.metricstreamers.opentsdb.lab;

import static com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeriesPredicates.within;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.metricstreamers.opentsdb.client.impl.OpenTsdbHttpQueryClient;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.MetricAggregator;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.OpenTsdbQueryBuilder;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Lab program that simply runs a metric query against a certain OpenTSDB server
 * and prints all result values.
 */
public class PullMetricValuesFromOpenTsdb {
    static Logger logger = LoggerFactory.getLogger(PullMetricValuesFromOpenTsdb.class);

    public static void main(String[] args) throws Exception {
        String openTsdbServer = "<IP address>:4242";
        // String metric = "some.metric";
        String metric = "http.total.accesses";
        // String metric = "http.total.accesses";
        // String metric =
        // SystemMetric.SCALING_GROUP_SIZE_CHANGED.getMetricName();
        // DateTime start = new DateTime().minusHours(1);
        DateTime start = UtcTime.now().minusMinutes(1);
        DateTime end = UtcTime.now();
        Interval interval = new Interval(start, end);
        // host=* will retrieve values separately for each host (separate
        // time-series) rather than aggregating all host time-series to a single
        // time-series (with an aggregated value at each point in time t)
        // Multimap<String, String> filterTags = ImmutableMultimap.of("host",
        // "*");
        Map<String, List<String>> filterTags = Maps.of();
        String query = new OpenTsdbQueryBuilder().withMetric(metric).withAggregator(MetricAggregator.SUM)
                .withInterval(interval)
                // .withDownsamplingSpecification( new
                // DownsamplingSpecification(Function.SUM, 120))
                .withRateConversion(true).withTags(filterTags).build();
        String queryUrl = "http://" + openTsdbServer + query;
        OpenTsdbHttpQueryClient queryClient = new OpenTsdbHttpQueryClient(logger);
        List<MetricValue> values = queryClient.query(queryUrl);
        // OpenTSDB has a habit of returning too many data points

        List<MetricValue> filteredValues = values.stream().filter(within(interval)).collect(Collectors.toList());
        logger.info("Query returned {} values", values.size());
        for (MetricValue value : filteredValues) {
            System.out.println("Value " + value);
        }
    }
}