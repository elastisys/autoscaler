package com.elastisys.autoscaler.metricstreamers.cloudwatch.stream;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.joda.time.Interval;
import org.slf4j.Logger;

import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.RateConverter;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.converters.MetricValueConverter;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.tasks.GetMetricStatisticsTask;

/**
 * Executes a single remote query against the AWS CloudWatch API.
 */
public class QueryCall implements Callable<List<MetricValue>> {

    private final Logger logger;
    private final MetricStreamConfig config;
    /**
     * The interval that the query is intended to cover. OpenTSDB sometimes
     * returns to many data points, so the {@link QueryCall} takes care of
     * filtering out any data points outside of this interval.
     */
    private final Interval queryInterval;

    /**
     * Creates a new {@link QueryCall}.
     *
     * @param logger
     * @param config
     * @param queryInterval
     */
    public QueryCall(Logger logger, MetricStreamConfig config, Interval queryInterval) {
        this.logger = logger;
        this.config = config;
        this.queryInterval = queryInterval;
    }

    @Override
    public List<MetricValue> call() throws Exception {
        CloudWatchMetricStreamDefinition stream = this.config.getMetricStreamDef();

        // collect AWS CloudWatch data points
        Callable<GetMetricStatisticsResult> request = new GetMetricStatisticsTask(this.config.getAccessKeyId(),
                this.config.getSecretAccessKey(), this.config.getRegion(), stream.getNamespace(), stream.getMetric(),
                Arrays.asList(stream.getStatistic()), stream.getPeriod(), stream.getDimensions(), this.queryInterval);
        GetMetricStatisticsResult result = request.call();

        // convert data points to MetricValues
        List<MetricValue> metricValues = new MetricValueConverter(stream.getStatistic()).apply(result);

        this.logger.debug("retrieved {} values from CloudWatch for stream {}, metric {}, period {}",
                metricValues.size(), stream.getId(), stream.getMetric(), this.queryInterval);

        // (optionally) apply rate conversion
        if (stream.isConvertToRate()) {
            if (metricValues.size() < 2) {
                // too few values to calculate a rate: return empty list and
                // await more metric values in coming iterations
                this.logger.warn("not enough metric values to calculate rate.");
                return Collections.emptyList();
            }
            metricValues = new RateConverter().apply(metricValues);
            this.logger.debug("stream {} values after rate conversion: {}", stream.getId(), metricValues);
        }
        return metricValues;
    }

}
