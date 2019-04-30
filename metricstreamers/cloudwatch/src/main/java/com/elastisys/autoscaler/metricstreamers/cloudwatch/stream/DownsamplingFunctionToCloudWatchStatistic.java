package com.elastisys.autoscaler.metricstreamers.cloudwatch.stream;

import java.util.function.Function;

import com.amazonaws.services.cloudwatch.model.Statistic;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.DownsampleFunction;
import com.elastisys.autoscaler.metricstreamers.cloudwatch.config.CloudWatchStatistic;

/**
 * Converts a given {@link DownsampleFunction} to its CloudWatch counterpart
 * {@link Statistic}.
 */
public class DownsamplingFunctionToCloudWatchStatistic implements Function<DownsampleFunction, CloudWatchStatistic> {

    @Override
    public CloudWatchStatistic apply(DownsampleFunction downsamplingFunction) {
        switch (downsamplingFunction) {
        case MIN:
            return CloudWatchStatistic.Minimum;
        case MAX:
            return CloudWatchStatistic.Maximum;
        case SUM:
            return CloudWatchStatistic.Sum;
        case MEAN:
            return CloudWatchStatistic.Average;
        default:
            throw new IllegalArgumentException("unrecognized downsampling function: " + downsamplingFunction.name());
        }
    }

}
