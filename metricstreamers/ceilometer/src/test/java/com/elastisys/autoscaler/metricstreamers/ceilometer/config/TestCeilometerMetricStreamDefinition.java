package com.elastisys.autoscaler.metricstreamers.ceilometer.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerFunction;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.Downsampling;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Verifies the behavior of the {@link CeilometerMetricStreamDefinition} class.
 */
public class TestCeilometerMetricStreamDefinition {

    private static final String ID = "id";
    private static final String METER = "meter";
    private static final String RESOURCE = "resource";
    private static final Downsampling DOWNSAMPLING = new Downsampling(CeilometerFunction.Average,
            TimeInterval.seconds(60));
    private static final boolean RATE_CONVERSION = false;
    private static final TimeInterval DATA_SETTLING_TIME = TimeInterval.seconds(60);
    private static final TimeInterval QUERY_CHUNK_SIZE = new TimeInterval(14L, TimeUnit.DAYS);

    /**
     * Should be possible to give explicit values for all fields.
     */
    @Test
    public void completeConfig() {
        CeilometerMetricStreamDefinition def = new CeilometerMetricStreamDefinition(ID, METER, RESOURCE, DOWNSAMPLING,
                RATE_CONVERSION, DATA_SETTLING_TIME, QUERY_CHUNK_SIZE);
        def.validate();

        assertThat(def.getId(), is(ID));
        assertThat(def.getMeter(), is(METER));
        assertThat(def.getResourceId().isPresent(), is(true));
        assertThat(def.getResourceId().get(), is(RESOURCE));
        assertThat(def.getDownsampling().isPresent(), is(true));
        assertThat(def.getDownsampling().get(), is(DOWNSAMPLING));
        assertThat(def.isConvertToRate(), is(RATE_CONVERSION));
        assertThat(def.getDataSettlingTime(), is(DATA_SETTLING_TIME));
        assertThat(def.getQueryChunkSize(), is(QUERY_CHUNK_SIZE));
    }

    /**
     * Not all fields are mandatory.
     */
    @Test
    public void defaults() {
        String resource = null;
        Downsampling downsampling = null;
        Boolean rateConversion = null;
        TimeInterval dataSettlingTime = null;
        TimeInterval queryChunkSize = null;

        CeilometerMetricStreamDefinition def = new CeilometerMetricStreamDefinition(ID, METER, resource, downsampling,
                rateConversion, dataSettlingTime, queryChunkSize);
        def.validate();

        assertThat(def.getResourceId().isPresent(), is(false));
        assertThat(def.getDownsampling().isPresent(), is(false));
        assertThat(def.isConvertToRate(), is(CeilometerMetricStreamDefinition.DEFAULT_RATE_CONVERSION));
        assertThat(def.getDataSettlingTime(), is(CeilometerMetricStreamDefinition.DEFAULT_DATA_SETTLING_TIME));
        assertThat(def.getQueryChunkSize(), is(CeilometerMetricStreamDefinition.DEFAULT_QUERY_CHUNK_SIZE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingId() {
        String id = null;
        new CeilometerMetricStreamDefinition(id, METER, RESOURCE, DOWNSAMPLING, RATE_CONVERSION, DATA_SETTLING_TIME,
                QUERY_CHUNK_SIZE).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingMeter() {
        String meter = null;
        new CeilometerMetricStreamDefinition(ID, meter, RESOURCE, DOWNSAMPLING, RATE_CONVERSION, DATA_SETTLING_TIME,
                QUERY_CHUNK_SIZE).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalDownsampling() {
        Downsampling downsampling = new Downsampling(CeilometerFunction.Maximum, TimeInterval.seconds(0));
        new CeilometerMetricStreamDefinition(ID, METER, RESOURCE, downsampling, RATE_CONVERSION, DATA_SETTLING_TIME,
                QUERY_CHUNK_SIZE).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeDataSettlingTime() {
        TimeInterval dataSettlingTime = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": -1, \"unit\": \"seconds\"}"), TimeInterval.class);
        new CeilometerMetricStreamDefinition(ID, METER, RESOURCE, DOWNSAMPLING, RATE_CONVERSION, dataSettlingTime,
                QUERY_CHUNK_SIZE).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeQueryChunkSize() {
        TimeInterval queryChunkSize = JsonUtils
                .toObject(JsonUtils.parseJsonString("{\"time\": -1, \"unit\": \"seconds\"}"), TimeInterval.class);
        new CeilometerMetricStreamDefinition(ID, METER, RESOURCE, DOWNSAMPLING, RATE_CONVERSION, DATA_SETTLING_TIME,
                queryChunkSize).validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroQueryChunkSize() {
        TimeInterval queryChunkSize = TimeInterval.seconds(0);
        new CeilometerMetricStreamDefinition(ID, METER, RESOURCE, DOWNSAMPLING, RATE_CONVERSION, DATA_SETTLING_TIME,
                queryChunkSize).validate();
    }
}
