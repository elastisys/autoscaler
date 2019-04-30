package com.elastisys.autoscaler.metricstreamers.ceilometer.converters;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.DownsampleFunction;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerFunction;
import com.elastisys.autoscaler.metricstreamers.ceilometer.converters.DownsamplingFunctionToCeilometerFunction;

/**
 * Exercises {@link DownsamplingFunctionToCeilometerFunction}.
 */
public class TestDownsamplingFunctionToCeilometerFunction {

    @Test
    public void convert() {
        assertThat(convert(DownsampleFunction.MAX), is(CeilometerFunction.Maximum));
        assertThat(convert(DownsampleFunction.MIN), is(CeilometerFunction.Minimum));
        assertThat(convert(DownsampleFunction.SUM), is(CeilometerFunction.Sum));
        assertThat(convert(DownsampleFunction.MEAN), is(CeilometerFunction.Average));
    }

    public CeilometerFunction convert(DownsampleFunction function) {
        return new DownsamplingFunctionToCeilometerFunction().apply(function);
    }
}
