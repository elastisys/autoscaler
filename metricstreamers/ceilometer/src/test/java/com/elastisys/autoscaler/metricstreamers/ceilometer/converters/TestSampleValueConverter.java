package com.elastisys.autoscaler.metricstreamers.ceilometer.converters;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.openstack4j.openstack.telemetry.domain.CeiloMeterSample;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.metricstreamers.ceilometer.converters.SampleValueConverter;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Exercises the {@link SampleValueConverter} class.
 */
public class TestSampleValueConverter {

    @Test(expected = NullPointerException.class)
    public void applyOnNull() {
        SampleValueConverter converter = new SampleValueConverter("foo");
        converter.apply(null);
    }

    private CeiloMeterSample parse(String resourceFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(getClass().getClassLoader().getResource(resourceFile).getFile()),
                CeiloMeterSample.class);
    }

    @Test
    public void fromDocumentationPage() throws IOException {
        final CeiloMeterSample parsed = parse("examples/sample.json");

        SampleValueConverter converter = new SampleValueConverter("instance");
        assertThat(converter.apply(parsed),
                equalTo(new MetricValue("instance", 1.0, new DateTime("2015-01-01T12:00:00", DateTimeZone.UTC))));
    }

}
