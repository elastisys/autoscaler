package com.elastisys.autoscaler.metricstreamers.ceilometer.stream;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamException;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.Downsample;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.DownsampleFunction;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.impl.EmptyResultSet;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.Downsampling;
import com.elastisys.autoscaler.metricstreamers.ceilometer.converters.DownsamplingFunctionToCeilometerFunction;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.openstack.OSClientFactory;
import com.elastisys.scale.commons.util.time.TimeUtils;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * A {@link MetricStream} that retrieves values from OpenStack Ceilometer.
 */
public class CeilometerMetricStream implements MetricStream {

    private final Logger logger;
    private final OSClientFactory clientFactory;
    private final CeilometerMetricStreamDefinition streamDefinition;

    public CeilometerMetricStream(Logger logger, OSClientFactory clientFactory,
            CeilometerMetricStreamDefinition streamDefinition) {
        checkArgument(logger != null, "logger cannot be null");
        checkArgument(clientFactory != null, "clientFactory cannot be null");
        checkArgument(streamDefinition != null, "streamDefinition cannot be null");
        this.logger = logger;
        this.clientFactory = clientFactory;
        this.streamDefinition = streamDefinition;
    }

    @Override
    public String getId() {
        return this.streamDefinition.getId();
    }

    @Override
    public String getMetric() {
        return this.streamDefinition.getMeter();
    }

    @Override
    public QueryResultSet query(Interval interval, QueryOptions options) throws MetricStreamException {
        CeilometerMetricStreamDefinition stream = this.streamDefinition;

        // make sure we don't request too recent (unsettled) data
        interval = adjustForDataSettlingTime(interval, stream);
        this.logger.debug("retrieving ceilometer values for stream {}, meter {}, period {}, downsampling {}",
                stream.getId(), stream.getMeter(), interval, stream.getDownsampling());
        if (interval.toDurationMillis() == 0) {
            return new EmptyResultSet();
        }

        // check query hints to see if custom downsampling was requested
        if (options != null && options.getDownsample().isPresent()) {
            Downsample customDownsampling = options.getDownsample().get();
            this.logger.debug("overriding downsampling with query hint: {}", customDownsampling);
            DownsampleFunction function = customDownsampling.getFunction();
            stream = stream.withDownsampling(new Downsampling(
                    new DownsamplingFunctionToCeilometerFunction().apply(function), customDownsampling.getInterval()));
        }

        // breaks query into chunks which are incrementally fetched in case of a
        // query spanning a long time-frame

        List<Interval> subQueryIntervals = TimeUtils.splitInterval(interval, queryChunkSize());
        List<QueryCall> subQueries = new ArrayList<>();
        for (Interval subQueryInterval : subQueryIntervals) {
            this.logger.debug("preparing (sub)query: {}", subQueryInterval);
            subQueries.add(new QueryCall(this.logger, this.clientFactory, stream, subQueryInterval));
        }
        return new LazyCeilometerResultSet(this.logger, subQueries);
    }

    /**
     * Adjusts a query interval to not include data points more recent than the
     * {@code dataSettlingTime} configured for the metric stream.
     *
     * @param queryInterval
     *            A query time interval.
     * @param stream
     *            The metric stream definition whose {@code dataSettlingTime} is
     *            to be applied.
     * @return The adjusted query interval (with an end time earlier than
     *         {@code dataSettlingTime}).
     */
    private Interval adjustForDataSettlingTime(Interval queryInterval, CeilometerMetricStreamDefinition stream) {
        TimeInterval dataSettlingTime = stream.getDataSettlingTime();
        DateTime now = UtcTime.now();

        DateTime minAge = now.minusSeconds((int) dataSettlingTime.getSeconds());
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

    private Duration queryChunkSize() {
        return Duration.millis(this.streamDefinition.getQueryChunkSize().getMillis());
    }

}
