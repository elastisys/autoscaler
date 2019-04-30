package com.elastisys.autoscaler.metricstreamers.influxdb.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Exercises {@link MetricStreamDefinition}.
 */
public class TestMetricStreamDefinition {

    /**
     * Test basic field access when all fields are specified.
     */
    @Test
    public void basicSanity() {
        TimeInterval dataSettlingTime = new TimeInterval(2L, TimeUnit.MINUTES);
        TimeInterval maxQueryChunkSize = new TimeInterval(14L, TimeUnit.DAYS);
        Query query = Query.builder().select("non_negative_derivative(max(requests),1s)").from("nginx")
                .where("region = 'us-east-1 AND machineState = 'running'").groupBy("time(1m) fill(none)").build();
        MetricStreamDefinition stream = new MetricStreamDefinition("id", "request_rate", "mydb", query,
                dataSettlingTime, maxQueryChunkSize);
        stream.validate();

        assertThat(stream.getId(), is("id"));
        assertThat(stream.getMetricName(), is("request_rate"));
        assertThat(stream.getDatabase(), is("mydb"));
        assertThat(stream.getQuery(), is(query));
        assertThat(stream.getDataSettlingTime(), is(dataSettlingTime));
        assertThat(stream.getQueryChunkSize(), is(maxQueryChunkSize));

        // explicity metric name given
        assertThat(stream.getMetric(), is("request_rate"));
    }

    /**
     * Only id, database, measurement and query are strictly required fields.
     */
    @Test
    public void withoutOptionalFields() {
        String metricName = null;
        TimeInterval dataSettlingTime = null;
        TimeInterval maxQueryChunkSize = null;
        Query query = Query.builder().select("requests").from("nginx").build();
        MetricStreamDefinition stream = new MetricStreamDefinition("id", metricName, "mydb", query, dataSettlingTime,
                maxQueryChunkSize);
        stream.validate();

        assertThat(stream.getId(), is("id"));
        assertThat(stream.getDatabase(), is("mydb"));
        assertThat(stream.getQuery(), is(query));
        assertThat(stream.getDataSettlingTime(), is(nullValue()));
        assertThat(stream.getQueryChunkSize(), is(MetricStreamDefinition.DEFAULT_QUERY_CHUNK_SIZE));

        // default metric name is id
        assertThat(stream.getMetricName(), is("id"));
        assertThat(stream.getMetric(), is("id"));
    }

    /**
     * {@code id} is a required field
     */
    @Test
    public void missingId() {
        try {
            Query query = Query.builder().select("requests").from("nginx").build();
            new MetricStreamDefinition(null, null, "mydb", query, null, null).validate();
            fail("config should not be allowed");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("no id specified"));
        }
    }

    /**
     * {@code database} is a required field
     */
    @Test
    public void missingDatabase() {
        try {
            Query query = Query.builder().select("requests").from("nginx").build();
            new MetricStreamDefinition("id", null, null, query, null, null).validate();
            fail("config should not be allowed");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("no database specified"));
        }
    }

    /**
     * {@code fieldKey} is a required field
     */
    @Test
    public void missingQuery() {
        try {
            Query nullQuery = null;
            new MetricStreamDefinition("id", "metricName", "mydb", nullQuery, null, null).validate();
            fail("config should not be allowed");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("no query specified"));
        }
    }

    /**
     * When specified, the dataSettlingTime should be validated.
     */
    @Test
    public void illegalDataSettlingTime() {
        Query query = Query.builder().select("requests").from("nginx").build();
        TimeInterval illegalDataSettlingTime = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": 1, \"unit\": \"weekday\"}"), TimeInterval.class);
        try {
            new MetricStreamDefinition("id", null, "mydb", query, illegalDataSettlingTime, null).validate();
            fail("config should not be allowed");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("dataSettlingTime"));
        }
    }

    @Test
    public void withEmptyMetricName() {
        String metricName = "";
        try {
            Query query = Query.builder().select("requests").from("nginx").build();
            new MetricStreamDefinition("id", metricName, "mydb", query, null, null).validate();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("metricName cannot be empty string"));
        }
    }
}
