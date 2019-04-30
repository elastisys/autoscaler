package com.elastisys.autoscaler.metricstreamers.cloudwatch.stream;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Converts a given downsampling {@link TimeInterval} to its corresponding
 * CloudWatch period counterpart. Since CloudWatch periods must be a multiple of
 * 60 seconds, the {@link TimeInterval} will be rounded up to the nearest
 * minute.
 */
public class DownsamplingIntervalToCloudWatchPeriod implements Function<TimeInterval, TimeInterval> {

    /**
     * Converts a given downsampling {@link TimeInterval} to its corresponding
     * CloudWatch period counterpart. Since CloudWatch periods must be a
     * multiple of 60 seconds, the {@link TimeInterval} will be rounded up to
     * the nearest minute.
     *
     * @param downsamplingInterval
     * @return
     */
    @Override
    public TimeInterval apply(TimeInterval downsamplingInterval) {
        long minutes = TimeUnit.MINUTES.convert(downsamplingInterval.getTime(), downsamplingInterval.getUnit());

        boolean intervalIsEvenMinutes = downsamplingInterval.getMillis() == minutes * 60 * 1000;
        if (!intervalIsEvenMinutes) {
            // not an even minute downsampling interval. round up.
            minutes++;
        }
        return TimeInterval.seconds(minutes * 60);
    }

}
