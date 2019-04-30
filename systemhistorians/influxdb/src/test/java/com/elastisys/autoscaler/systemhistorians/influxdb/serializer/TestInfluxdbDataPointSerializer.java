package com.elastisys.autoscaler.systemhistorians.influxdb.serializer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Test;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link InfluxdbDataPointSerializer}.
 *
 */
public class TestInfluxdbDataPointSerializer {

    private final InfluxdbDataPointSerializer serializer = new InfluxdbDataPointSerializer();

    private static final DateTime TIME = UtcTime.parse("2016-01-01T12:00:00.000Z");
    private static final DateTime TIME2 = UtcTime.parse("2016-12-31T12:59:59.999Z");

    /**
     * Serializes {@link MetricValue}s without tags.
     */
    @Test
    public void withoutTags() {
        MetricValue value = new MetricValue("metric", 1.0, TIME);
        assertThat(this.serializer.apply(value), is("metric value=1.0 1451649600000000000"));

        value = new MetricValue("metric", 2.0, TIME);
        assertThat(this.serializer.apply(value), is("metric value=2.0 1451649600000000000"));

        value = new MetricValue("qualified.metric", 2.0, TIME);
        assertThat(this.serializer.apply(value), is("qualified.metric value=2.0 1451649600000000000"));

        value = new MetricValue("metric", 2.0, TIME2);
        assertThat(this.serializer.apply(value), is("metric value=2.0 1483189199999000000"));
    }

    /**
     * Serialize {@link MetricValue} with a single tag.
     */
    @Test
    public void withSingleTag() {
        Map<String, String> tags = Maps.of("region", "us-east-1");
        MetricValue value = new MetricValue("metric", 1.0, TIME, tags);
        assertThat(this.serializer.apply(value), is("metric,region=us-east-1 value=1.0 1451649600000000000"));

    }

    /**
     * Serialize {@link MetricValue} with several tags.
     */
    @Test
    public void withMultipleTags() {
        Map<String, String> tags = Maps.of("region", "us-east-1", "host", "srv1");
        MetricValue value = new MetricValue("metric", 1.0, TIME, tags);
        assertThat(this.serializer.apply(value), is("metric,region=us-east-1,host=srv1 value=1.0 1451649600000000000"));
    }

}
