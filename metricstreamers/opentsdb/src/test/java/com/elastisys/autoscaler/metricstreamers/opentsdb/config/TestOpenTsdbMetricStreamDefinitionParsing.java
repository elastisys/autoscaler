package com.elastisys.autoscaler.metricstreamers.opentsdb.config;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.DownsampleFunction;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.DownsamplingSpecification;
import com.elastisys.autoscaler.metricstreamers.opentsdb.query.MetricAggregator;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.collection.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Verifies the that the {@link OpenTsdbMetricStreamDefinition} can be properly
 * parsed from a JSON representation.
 */
public class TestOpenTsdbMetricStreamDefinitionParsing {

    /**
     * Parses a minimal {@link OpenTsdbMetricStreamDefinition}, containing
     * required fields only.
     *
     * @throws IOException
     */
    @Test
    public void parseMinimalConfig() throws Exception {
        String asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"MAX\" }";
        OpenTsdbMetricStreamDefinition actualStream = parse(asJson);

        OpenTsdbMetricStreamDefinition expectedStream = new OpenTsdbMetricStreamDefinition("http.total.accesses.rate",
                "http.total.accesses", MetricAggregator.MAX, null, null, null, null, null);

        assertThat(actualStream, is(expectedStream));
        actualStream.validate();
        assertThat(actualStream.getQuery(), is("/q?m=max:http.total.accesses&ascii&nocache"));
    }

    @Test
    public void parseWithDifferentAggregationFunctions() throws IOException {
        // MAX
        String asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"MAX\" }";
        assertThat(parse(asJson).getAggregator(), is(MetricAggregator.MAX));
        // MIN
        asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"MIN\" }";
        assertThat(parse(asJson).getAggregator(), is(MetricAggregator.MIN));
        // AVERAGE
        asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"AVG\" }";
        assertThat(parse(asJson).getAggregator(), is(MetricAggregator.AVG));
        // SUM
        asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"SUM\" }";
        assertThat(parse(asJson).getAggregator(), is(MetricAggregator.SUM));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseWithInvalidAggregationFunction() throws Exception {
        String asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"UNRECOGNIZED_FUNCTION\" }";
        assertThat(parse(asJson).getAggregator(), is(nullValue()));
        parse(asJson).validate(); // expected to throw NPE
    }

    @Test
    public void parseWithRateConversion() throws Exception {
        // \"rate\": true
        String asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"SUM\", \"convertToRate\": \"true\" }";
        OpenTsdbMetricStreamDefinition actualStream = parse(asJson);

        OpenTsdbMetricStreamDefinition expectedStream = new OpenTsdbMetricStreamDefinition("http.total.accesses.rate",
                "http.total.accesses", MetricAggregator.SUM, true, null, null, null, null);

        assertThat(actualStream, is(expectedStream));
        actualStream.validate();
        assertThat(actualStream.getQuery(), is("/q?m=sum:rate:http.total.accesses&ascii&nocache"));

        // rate: false
        asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", "
                + "\"aggregator\": \"SUM\", \"convertToRate\": \"false\" }";
        actualStream = parse(asJson);
        assertThat(actualStream.isConvertToRate(), is(false));
        assertThat(actualStream.getQuery(), is("/q?m=sum:http.total.accesses&ascii&nocache"));
    }

    @Test
    public void parseWithDownsampling() throws Exception {
        String asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"SUM\", \"downsampling\": { \"function\": \"SUM\", \"interval\": { \"time\": 60, \"unit\": \"seconds\" }} }";
        OpenTsdbMetricStreamDefinition actualStream = parse(asJson);

        OpenTsdbMetricStreamDefinition expectedStream = new OpenTsdbMetricStreamDefinition("http.total.accesses.rate",
                "http.total.accesses", MetricAggregator.SUM, null,
                new DownsamplingSpecification(new TimeInterval(60L, TimeUnit.SECONDS), DownsampleFunction.SUM), null,
                null, null);

        assertThat(actualStream, is(expectedStream));
        actualStream.validate();
        assertThat(actualStream.getQuery(), is("/q?m=sum:60s-sum:http.total.accesses&ascii&nocache"));
    }

    @Test
    public void parseWithDifferentDownsamplingFunctions() throws IOException {
        // MAX
        String asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"SUM\", \"downsampling\": { \"function\": \"MAX\", \"interval\": { \"time\": 60, \"unit\": \"seconds\" } } }";
        assertThat(parse(asJson).getDownsampling(),
                is(new DownsamplingSpecification(new TimeInterval(60L, TimeUnit.SECONDS), DownsampleFunction.MAX)));
        // MIN
        asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"SUM\", \"downsampling\": { \"function\": \"MIN\", \"interval\": { \"time\": 30, \"unit\": \"seconds\" } } }";
        assertThat(parse(asJson).getDownsampling(),
                is(new DownsamplingSpecification(new TimeInterval(30L, TimeUnit.SECONDS), DownsampleFunction.MIN)));
        // AVG
        asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"SUM\", \"downsampling\": { \"function\": \"MEAN\", \"interval\": { \"time\": 15, \"unit\": \"seconds\" } } }";
        assertThat(parse(asJson).getDownsampling(),
                is(new DownsamplingSpecification(new TimeInterval(15L, TimeUnit.SECONDS), DownsampleFunction.MEAN)));
        // SUM
        asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"SUM\", \"downsampling\": { \"function\": \"SUM\", \"interval\": { \"time\": 10, \"unit\": \"seconds\" }} }";
        assertThat(parse(asJson).getDownsampling(),
                is(new DownsamplingSpecification(new TimeInterval(10L, TimeUnit.SECONDS), DownsampleFunction.SUM)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseWithIllegalDownsamplingFunction() throws Exception {
        String asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"SUM\", \"downsampling\": { \"function\": \"UNRECOGNIZED\", \"seconds\": 60 } }";
        parse(asJson).validate();
    }

    @Test
    public void parseWithSingleValuedTag() throws Exception {
        String asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"SUM\", \"tags\": { \"host\": [\"*\"] }}";
        OpenTsdbMetricStreamDefinition actualStream = parse(asJson);

        Map<String, List<String>> expectedTags = Maps.of("host", asList("*"));
        OpenTsdbMetricStreamDefinition expectedStream = new OpenTsdbMetricStreamDefinition("http.total.accesses.rate",
                "http.total.accesses", MetricAggregator.SUM, null, null, expectedTags, null, null);

        assertThat(actualStream, is(expectedStream));
        actualStream.validate();
        assertThat(actualStream.getQuery(), is("/q?m=sum:http.total.accesses{host=*}&ascii&nocache"));
    }

    @Test
    public void parseWithMultiValuedTag() throws Exception {
        String asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"SUM\", \"tags\": { \"host\": [\"host1\",\"host2\"] }}";
        OpenTsdbMetricStreamDefinition actualStream = parse(asJson);

        Map<String, List<String>> expectedTags = Maps.of("host", asList("host1", "host2"));
        OpenTsdbMetricStreamDefinition expectedStream = new OpenTsdbMetricStreamDefinition("http.total.accesses.rate",
                "http.total.accesses", MetricAggregator.SUM, null, null, expectedTags, null, null);

        assertThat(actualStream, is(expectedStream));
        actualStream.validate();
        assertThat(actualStream.getQuery(), is("/q?m=sum:http.total.accesses{host=host1|host2}&ascii&nocache"));
    }

    @Test
    public void parseWithMultipleTags() throws Exception {
        String asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"SUM\", \"tags\": { \"host\": [\"host1\",\"host2\"], \"zones\": [\"us-east-1a\"] }}";
        OpenTsdbMetricStreamDefinition actualStream = parse(asJson);

        Map<String, List<String>> expectedTags = Maps.of(//
                "host", asList("host1", "host2"), //
                "zones", asList("us-east-1a"));
        OpenTsdbMetricStreamDefinition expectedStream = new OpenTsdbMetricStreamDefinition("http.total.accesses.rate",
                "http.total.accesses", MetricAggregator.SUM, null, null, expectedTags, null, null);

        assertThat(actualStream, is(expectedStream));
        actualStream.validate();
        assertThat(actualStream.getQuery(),
                is("/q?m=sum:http.total.accesses{host=host1|host2,zones=us-east-1a}&ascii&nocache"));
    }

    @Test
    public void parseWithDataSettlingTime() throws Exception {
        String asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"MAX\", \"dataSettlingTime\": { \"time\": 15, \"unit\": \"seconds\" } }";
        OpenTsdbMetricStreamDefinition actualStream = parse(asJson);

        OpenTsdbMetricStreamDefinition expectedStream = new OpenTsdbMetricStreamDefinition("http.total.accesses.rate",
                "http.total.accesses", MetricAggregator.MAX, null, null, null, new TimeInterval(15L, TimeUnit.SECONDS),
                null);

        assertThat(actualStream, is(expectedStream));
        actualStream.validate();
        assertThat(actualStream.getQuery(), is("/q?m=max:http.total.accesses&ascii&nocache"));
        assertThat(actualStream.getDataSettlingTime(), is(new TimeInterval(15L, TimeUnit.SECONDS)));
    }

    /**
     * Parses a configuration with all fields specified.
     *
     * @throws IOException
     */
    @Test
    public void parseCompleteConfig() throws Exception {
        String asJson = "{ \"id\": \"http.total.accesses.rate\", \"metric\": \"http.total.accesses\", \"aggregator\": \"SUM\", \"convertToRate\": \"true\", \"downsampling\": { \"function\": \"SUM\", \"interval\": { \"time\": 60, \"unit\": \"seconds\" } }, \"tags\": { \"hosts\": [\"*\"], \"zones\": [\"us-east-1a\"] }, \"dataSettlingTime\": { \"time\": 30, \"unit\": \"seconds\" }, \"queryChunkSize\": { \"time\": 14, \"unit\": \"days\" }}";

        OpenTsdbMetricStreamDefinition actualStream = parse(asJson);

        Map<String, List<String>> expectedTags = Maps.of(//
                "hosts", asList("*"), //
                "zones", asList("us-east-1a"));
        DownsamplingSpecification expectedDownsampling = new DownsamplingSpecification(
                new TimeInterval(60L, TimeUnit.SECONDS), DownsampleFunction.SUM);
        OpenTsdbMetricStreamDefinition expectedStream = new OpenTsdbMetricStreamDefinition("http.total.accesses.rate",
                "http.total.accesses", MetricAggregator.SUM, true, expectedDownsampling, expectedTags,
                new TimeInterval(30L, TimeUnit.SECONDS), new TimeInterval(14L, TimeUnit.DAYS));

        assertThat(actualStream, is(expectedStream));
        actualStream.validate();
        assertThat(actualStream.getQuery(),
                is("/q?m=sum:60s-sum:rate:http.total.accesses{hosts=*,zones=us-east-1a}&ascii&nocache"));
    }

    /**
     * Parses out an {@link OpenTsdbMetricStreamDefinition} from its JSON
     * representation.
     *
     * @param asJson
     * @return
     * @throws IOException
     */
    private OpenTsdbMetricStreamDefinition parse(String asJson) throws IOException {
        JsonObject jsonConfig = JsonUtils.parseJsonString(asJson).getAsJsonObject();
        OpenTsdbMetricStreamDefinition actualStream = new Gson().fromJson(jsonConfig,
                OpenTsdbMetricStreamDefinition.class);
        return actualStream;
    }
}
