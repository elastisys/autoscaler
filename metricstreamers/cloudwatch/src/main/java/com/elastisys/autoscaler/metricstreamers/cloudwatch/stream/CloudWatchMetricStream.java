package com.elastisys.autoscaler.metricstreamers.cloudwatch.stream;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamException;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.Downsample;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.impl.EmptyResultSet;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchMetricStreamDefinition;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.time.TimeUtils;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * A {@link MetricStream} that retrieves values from AWS CloudWatch.
 */
public class CloudWatchMetricStream implements MetricStream {

    private final Logger logger;
    private final MetricStreamConfig config;

    public CloudWatchMetricStream(Logger logger, MetricStreamConfig config) {
        this.logger = logger;
        this.config = config;
    }

    @Override
    public String getId() {
        return stream().getId();
    }

    @Override
    public String getMetric() {
        return stream().getMetric();
    }

    @Override
    public QueryResultSet query(Interval interval, QueryOptions options) throws MetricStreamException {
        CloudWatchMetricStreamDefinition stream = stream();

        interval = adjustForDataSettlingTime(interval);
        this.logger.debug("retrieving CloudWatch datapoints for stream {}, metric {}, period {}, statistic {}",
                stream.getId(), stream.getMetric(), interval, stream.getStatistic());
        if (interval.toDurationMillis() == 0) {
            return new EmptyResultSet();
        }

        // check query hints to see if custom downsampling was requested
        if (options != null && options.getDownsample().isPresent()) {
            Downsample customDownsampling = options.getDownsample().get();
            this.logger.debug("overriding downsampling with query hint: {}", customDownsampling);

            stream = withCustomDownsampling(stream, customDownsampling);
        }

        // breaks query into chunks which are incrementally fetched in case of a
        // query spanning a long time-frame
        List<Interval> subQueryIntervals = TimeUtils.splitInterval(interval, queryChunkSize());
        List<QueryCall> subQueries = new ArrayList<>();
        for (Interval subQueryInterval : subQueryIntervals) {
            this.logger.debug("preparing (sub)query: {}", subQueryInterval);
            subQueries.add(new QueryCall(this.logger, this.config, subQueryInterval));
        }
        return new LazyCloudWatchResultSet(this.logger, subQueries);
    }

    /**
     * Adjusts a query interval to not include data points more recent than the
     * {@code dataSettlingTime} configured for the metric stream.
     *
     * @param queryInterval
     *            A query time interval.
     * @return The adjusted query interval (with an end time earlier than
     *         {@code dataSettlingTime}).
     */
    private Interval adjustForDataSettlingTime(Interval queryInterval) {
        TimeInterval dataSettlingTime = stream().getDataSettlingTime();
        DateTime now = UtcTime.now();

        DateTime minAge = now.minus(dataSettlingTime.getMillis());
        if (queryInterval.isBefore(minAge)) {
            // interval is ok
            return queryInterval;
        }

        if (queryInterval.getStart().isAfter(minAge)) {
            // interval is too short => return a zero interval
            return new Interval(minAge, minAge);
        }

        // interval end too late => adjust query end time
        return new Interval(queryInterval.getStart(), minAge);
    }

    /**
     * Returns a copy of the given stream definition with a different
     * downsampling.
     *
     * @param stream
     * @param customDownsampling
     * @return
     */
    private CloudWatchMetricStreamDefinition withCustomDownsampling(CloudWatchMetricStreamDefinition stream,
            Downsample customDownsampling) {
        return stream.withPeriod(new DownsamplingIntervalToCloudWatchPeriod().apply(customDownsampling.getInterval()))
                .withStatistic(new DownsamplingFunctionToCloudWatchStatistic().apply(customDownsampling.getFunction()));
    }

    private Duration queryChunkSize() {
        return Duration.millis(this.config.getMetricStreamDef().getQueryChunkSize().getMillis());
    }

    private CloudWatchMetricStreamDefinition stream() {
        return this.config.getMetricStreamDef();
    }

}
