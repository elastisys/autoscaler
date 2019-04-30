package com.elastisys.autoscaler.metricstreamers.ceilometer.converters;

import java.util.function.Function;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.DownsampleFunction;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerFunction;

/**
 * Converts from {@link DownsampleFunction} to {@link CeilometerFunction}.
 */
public class DownsamplingFunctionToCeilometerFunction implements Function<DownsampleFunction, CeilometerFunction> {

    @Override
    public CeilometerFunction apply(DownsampleFunction function) {
        switch (function) {
        case MAX:
            return CeilometerFunction.Maximum;
        case MIN:
            return CeilometerFunction.Minimum;
        case MEAN:
            return CeilometerFunction.Average;
        case SUM:
            return CeilometerFunction.Sum;
        default:
            throw new IllegalArgumentException("unrecognized DownsampleFunction: " + function.name());
        }
    }
}
