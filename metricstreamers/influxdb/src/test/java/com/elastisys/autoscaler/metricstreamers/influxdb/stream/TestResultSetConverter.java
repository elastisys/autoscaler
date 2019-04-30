package com.elastisys.autoscaler.metricstreamers.influxdb.stream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.metricstreamers.influxdb.resultset.ResultSet;
import com.elastisys.autoscaler.metricstreamers.influxdb.stream.ResultSetConverter;
import com.elastisys.autoscaler.metricstreamers.influxdb.stream.errors.NonNumericalValueException;
import com.elastisys.autoscaler.metricstreamers.influxdb.stream.errors.QueryErrorException;
import com.elastisys.autoscaler.metricstreamers.influxdb.stream.errors.ResultParsingException;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Exercises the {@link ResultSetConverter} on different sample result sets.
 *
 */
public class TestResultSetConverter {
    private static final String directory = "resultsets";
    private static final String emptyDocument = "empty.json";
    private static final String noResults = "no-results.json";
    private static final String resultWithoutSeries = "result-without-series.json";

    private static final String seriesWithoutMeasurement = "series-without-measurement.json";
    private static final String seriesWithoutColumns = "series-without-columns.json";
    private static final String seriesWithoutValueColumns = "series-without-value-columns.json";
    private static final String seriesWithoutValues = "series-without-values.json";

    private static final String seriesWithoutDatapoints = "series-without-datapoints.json";
    private static final String seriesWithDatapoints = "series-with-datapoints.json";
    private static final String seriesWithNonNumericalDatapoints = "series-with-nonnumerical-datapoints.json";

    private static final String seriesWithError = "series-with-error.json";

    /**
     * An empty query response should result in an error.
     */
    @Test(expected = ResultParsingException.class)
    public void convertEmptyResponse() {
        converter("cpu").toMetricValues(load(emptyDocument));
    }

    /**
     * Query response must contain at least one entry in {@code results}.
     */
    @Test(expected = ResultParsingException.class)
    public void convertEmptyResultSet() {
        converter("cpu").toMetricValues(load(noResults));
    }

    /**
     * A query response result without any {@code series} entry marks an result
     * that did not contain any data points.
     */
    @Test
    public void convertResultWithoutSeries() {
        assertThat(converter("cpu").toMetricValues(load(resultWithoutSeries)), is(Collections.emptyList()));
    }

    /**
     * Result series must contain a {@code name} element.
     */
    @Test(expected = ResultParsingException.class)
    public void convertSeriesWithoutMeasurement() {
        converter("cpu").toMetricValues(load(seriesWithoutMeasurement));
    }

    /**
     * Result series must contain a {@code columns} element.
     */
    @Test(expected = ResultParsingException.class)
    public void convertSeriesWithoutColumns() {
        converter("cpu").toMetricValues(load(seriesWithoutColumns));
    }

    /**
     * Result series must contain a {@code values} element.
     */
    @Test(expected = ResultParsingException.class)
    public void convertSeriesWithoutValues() {
        converter("cpu").toMetricValues(load(seriesWithoutValues));
    }

    /**
     * It should be okay for a query to return no datapoints.
     */
    @Test
    public void convertSeriesWithoutDatapointsValues() {
        List<MetricValue> metricValues = converter("cpu").toMetricValues(load(seriesWithoutDatapoints));
        assertThat(metricValues.isEmpty(), is(true));
    }

    /**
     * Handle a series with both floating point- and integer-valued data points.
     */
    @Test
    public void convertSeriesWithDatapointValues() {
        List<MetricValue> metricValues = converter("cpu").toMetricValues(load(seriesWithDatapoints));
        assertThat(metricValues.size(), is(3));
        assertThat(metricValues.get(0),
                is(new MetricValue("cpu", 0.55, UtcTime.parse("2015-01-29T21:55:43.702900257Z"))));
        assertThat(metricValues.get(1),
                is(new MetricValue("cpu", 23422, UtcTime.parse("2015-01-29T21:55:43.702900257Z"))));
        assertThat(metricValues.get(2), is(new MetricValue("cpu", 0.64, UtcTime.parse("2015-06-11T20:46:02Z"))));
    }

    /**
     * The value field must be of a numerical type (otherwise it won't be useful
     * for predictions).
     */
    @Test(expected = NonNumericalValueException.class)
    public void convertSeriesWithNonNumericalDatapoints() {
        converter("cpu").toMetricValues(load(seriesWithNonNumericalDatapoints));
    }

    /**
     * Parsing a query result with the {@code error} field set should result in
     * a {@link QueryErrorException}.
     */
    @Test(expected = QueryErrorException.class)
    public void convertResultWithError() {
        converter("cpu").toMetricValues(load(seriesWithError));

    }

    /**
     * A series must contain at least one value column in addition to the time
     * column.
     */
    @Test(expected = ResultParsingException.class)
    public void convertResultWithNoValueColumns() {
        converter("cpu").toMetricValues(load(seriesWithoutValueColumns));
    }

    /**
     * A {@link ResultSetConverter} not being told which metric name to set for
     * produced {@link MetricValue}s should use a default name of
     * {@code <series name>.<first value column>}
     */
    @Test
    public void withDefaultMetricName() {
        ResultSetConverter defaultNameConverter = converter(null);
        String expectedMetricName = "cpu.system";

        List<MetricValue> metricValues = defaultNameConverter.toMetricValues(load(seriesWithDatapoints));
        assertThat(metricValues.size(), is(3));

        assertThat(metricValues.get(0),
                is(new MetricValue(expectedMetricName, 0.55, UtcTime.parse("2015-01-29T21:55:43.702900257Z"))));
        assertThat(metricValues.get(1),
                is(new MetricValue(expectedMetricName, 23422, UtcTime.parse("2015-01-29T21:55:43.702900257Z"))));
        assertThat(metricValues.get(2),
                is(new MetricValue(expectedMetricName, 0.64, UtcTime.parse("2015-06-11T20:46:02Z"))));
    }

    private static ResultSet load(String resultSetResouce) {
        return JsonUtils.toObject(JsonUtils.parseJsonResource(directory + "/" + resultSetResouce), ResultSet.class);
    }

    /**
     * Creates a {@link ResultSetConverter} that will produce
     * {@link MetricValue}s with the given {@code metricName}.
     *
     * @param metricName
     *            Metric name to use. Can be <code>null</code>, which causes a
     *            default metric name to be created (see
     *            {@link ResultSetConverter}).
     * @return
     */
    private static ResultSetConverter converter(String metricName) {
        return new ResultSetConverter(metricName);
    }
}
