package com.elastisys.autoscaler.metricstreamers.ceilometer.converters;

import java.util.function.Function;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.openstack4j.model.telemetry.Sample;

import com.elastisys.autoscaler.core.api.types.MetricValue;

/**
 * Converts between OpenStack Ceilometer <a href=
 * "http://docs.openstack.org/developer/ceilometer/webapi/v2.html#Sample">Sample
 * </a> to {@link MetricValue}.
 */
public class SampleValueConverter implements Function<Sample, MetricValue> {

    private final String meter;

    /**
     * Creates a new instance bound to the given meter name.
     *
     * @param meter
     *            The name of the meter.
     */
    public SampleValueConverter(String meter) {
        this.meter = meter;
    }

    @Override
    public MetricValue apply(Sample sample) {
        DateTime timestamp = new DateTime(sample.getTimestamp(), DateTimeZone.UTC);
        return new MetricValue(this.meter, sample.getVolume(), timestamp);
    }
}
