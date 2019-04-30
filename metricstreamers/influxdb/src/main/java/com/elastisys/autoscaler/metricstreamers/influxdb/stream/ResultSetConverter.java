package com.elastisys.autoscaler.metricstreamers.influxdb.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.joda.time.DateTime;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.metricstreamers.influxdb.resultset.Result;
import com.elastisys.autoscaler.metricstreamers.influxdb.resultset.ResultSet;
import com.elastisys.autoscaler.metricstreamers.influxdb.resultset.Serie;
import com.elastisys.autoscaler.metricstreamers.influxdb.stream.errors.NonNumericalValueException;
import com.elastisys.autoscaler.metricstreamers.influxdb.stream.errors.QueryErrorException;
import com.elastisys.autoscaler.metricstreamers.influxdb.stream.errors.ResultParsingException;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * Converts an InfluxDB {@link ResultSet} produced in response to an
 * {@link InfluxdbFetcher} query to the corresponding {@link MetricValue}s.
 *
 * @see InfluxdbFetcher
 */
class ResultSetConverter {

    /**
     * The metric name to assign to produced {@link MetricValue}s. May be
     * <code>null</code>, in which case, the series {@code <name>} and the first
     * result {@code <column>} will form the metric name as {@code <name>.
     * <column>}.
     */
    private final String metricName;

    /**
     * Creates a new {@link ResultSetConverter}.
     *
     * @param metricName
     *            The metric name to assign to produced {@link MetricValue}s.
     *            May be <code>null</code>, in which case, the series
     *            {@code <name>} and the first result {@code <column>} will form
     *            the metric name as {@code <name>.<column>}.
     */
    public ResultSetConverter(String metricName) {
        this.metricName = metricName;
    }

    /**
     * Takes a {@link ResultSet} produced by InfluxDB and converts it to a list
     * of {@link MetricValue}s.
     * <p/>
     * A few assumptions are made on the input {@link ResultSet}: it is assumed
     * to stem from a single InfluxDB query, and have a single series (that is,
     * no group-by tags query) and the selected value is assumed to be of a
     * numerical type. That is, it expects {@link ResultSet}s similar to:
     *
     * <pre>
     *{
     *  "results": [
     *    {
     *      "series": [
     *        {
     *          "name": "cpu",
     *          "columns": [
     *            "time",
     *            "cpu_load"
     *          ],
     *          "values": [
     *            [
     *              "2016-09-16T12:10:00Z",
     *              1.0
     *            ],
     *            [
     *              "2016-09-16T12:20:00Z",
     *              2.0
     *            ],
     *            ...
     * }
     * </pre>
     *
     * It <i>can</i> handle {@link ResultSet}s with more series and more
     * columns, however it will ignore everything but the first series and the
     * first value column of that series.
     *
     * @param resultSet
     * @return
     * @throws ResultParsingException
     * @throws NonNumericalValueException
     * @throws QueryErrorException
     */
    public List<MetricValue> toMetricValues(ResultSet resultSet)
            throws ResultParsingException, NonNumericalValueException, QueryErrorException {
        // we assume that we only pose a single query, therefore there will only
        // be a single result
        Result result = getFirstResultOrFail(resultSet);

        // since we never query grouping by tags, there should only be a single
        // series (or at least, we only care about the first series)
        Optional<Serie> serie = getFirstSeries(result);
        if (!serie.isPresent()) {
            // no series in result means there are no data points
            return Collections.emptyList();
        }

        // make sure all relevant fields are available in series
        ensureFieldsAvailable(serie.get());

        // convert series data points to MetricValues
        List<List<Object>> dataPoints = serie.get().getValues();
        List<MetricValue> values = new ArrayList<>();
        for (List<Object> dataPoint : dataPoints) {
            DateTime time = UtcTime.parse((String) dataPoint.get(0));
            // we never query for more than one column
            Object value = dataPoint.get(1);
            // we only handle numeric values
            if (!Number.class.isInstance(value)) {
                throw new NonNumericalValueException(String.format(
                        "value column ('%s') in influxdb query result is not of a numeric type but of type %s",
                        serie.get().getColumns().get(1), value.getClass().getSimpleName()));
            }
            double numericValue = Double.class.cast(value);
            values.add(new MetricValue(metricName(serie.get()), numericValue, time));
        }

        return values;
    }

    /**
     * Returns the metric name to set for produced {@link MetricValue}s for a
     * given time {@link Serie} result. If a {@link #metricName} was specified
     * for this {@link ResultSetConverter}, that name will be used. If no name
     * was specified, the series {@code <name>} and the first result {@code
     * <column>} will form the metric name as {@code <name>.<column>}.
     *
     * @param serie
     *            A query result {@link Serie}.
     * @return
     */
    private String metricName(Serie serie) {
        return Optional.ofNullable(this.metricName).orElse(serie.getName() + "." + serie.getColumns().get(1));
    }

    /**
     * Validate that a result set {@link Serie} is syntactically well-formed.
     * See the <a href=
     * "https://docs.influxdata.com/influxdb/v1.0/guides/querying_data/">
     * InfluxDB documentation</a>.
     *
     * @param serie
     * @throws ResultParsingException
     */
    private static void ensureFieldsAvailable(Serie serie) throws ResultParsingException {
        if (serie.getName() == null) {
            throw new ResultParsingException("influxdb query result series did not contain a measurement name");
        }
        if (serie.getColumns() == null) {
            throw new ResultParsingException("influxdb query result series did not contain a columns element");
        }
        if (serie.getColumns().size() < 2) {
            throw new ResultParsingException(String
                    .format("influxdb query result series must at least contain a time column and one value column. "
                            + "result series only contained columns: %s", serie.getColumns()));
        }
        if (serie.getValues() == null) {
            throw new ResultParsingException("influxdb query result series did not contain a values element");
        }
    }

    /**
     * Returns the first {@link Result} in a {@link ResultSet} or fails if the
     * set did not contain any {@link Result}s or if the result contained an
     * error.
     *
     * @param resultSet
     * @return
     * @throws ResultParsingException
     * @throws QueryErrorException
     */
    private static Result getFirstResultOrFail(ResultSet resultSet) throws ResultParsingException, QueryErrorException {
        List<Result> results = resultSet.getResults();
        if (results == null || results.isEmpty()) {
            throw new ResultParsingException("influxdb response did not contain any results");
        }
        Result result = results.get(0);
        if (result.isError()) {
            throw new QueryErrorException("influxdb query resulted in an error: " + result.getError());
        }
        return result;
    }

    /**
     * Returns the first {@link Serie} from a query {@link Result} in case there
     * is one. If no series exist, which marks an empty result set, an empty
     * {@link Optional} is returned.
     *
     * @param result
     * @return
     * @throws ResultParsingException
     */
    private static Optional<Serie> getFirstSeries(Result result) {
        if (result.getSeries() == null || result.getSeries().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(result.getSeries().get(0));
    }

}
