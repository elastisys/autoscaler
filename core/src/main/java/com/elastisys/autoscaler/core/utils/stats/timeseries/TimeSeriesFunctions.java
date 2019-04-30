package com.elastisys.autoscaler.core.utils.stats.timeseries;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.elastisys.autoscaler.core.utils.stats.functions.AggregationFunction;
import com.elastisys.autoscaler.core.utils.stats.timeseries.impl.OutlierFilteredTimeSeries;

/**
 * {@link Function}s related to {@link TimeSeries} and {@link DataPoint}s.
 *
 * @see TimeSeries
 * @see DataPoint
 */
public class TimeSeriesFunctions {

    /**
     * Applies an {@link AggregationFunction} to a {@link TimeSeries}, to
     * produce a single double value from the {@link DataPoint}s in the time
     * series.
     *
     * @param timeSeries
     *            A {@link TimeSeries}.
     * @param aggregationFunction
     *            An aggregation function to apply to the values of the
     *            {@link TimeSeries}.
     * @return The aggregate value, if one could be determined.
     */
    public static Optional<Double> aggregate(TimeSeries timeSeries, AggregationFunction<Double> aggregationFunction) {
        return aggregate(timeSeries, aggregationFunction, false);
    }

    /**
     * Applies an {@link AggregationFunction} to a {@link TimeSeries}, whose
     * values can optionally be filtered from outliers before aggregating. The
     * method produces a single double value from the {@link DataPoint}s in the
     * time series.
     * <p/>
     * If requested, outliers are filtered according to
     * {@link TimeSeriesPredicates.IsOutlier}.
     *
     * @param timeSeries
     *            A {@link TimeSeries}.
     * @param aggregationFunction
     *            An aggregation function to apply to the values of the
     *            {@link TimeSeries}.
     * @param filterOutliers
     *            <code>true</code> if outliers are to be filtered out before
     *            applying the {@link AggregationFunction}.
     * @return The aggregate value, if one could be determined.
     */
    public static Optional<Double> aggregate(TimeSeries timeSeries, AggregationFunction<Double> aggregationFunction,
            boolean filterOutliers) {
        if (filterOutliers) {
            timeSeries = new OutlierFilteredTimeSeries(timeSeries);
        }
        List<Double> values = timeSeries.getDataPoints().stream().map(DataPoint::getValue).collect(Collectors.toList());
        return aggregationFunction.apply(values);
    }

    /**
     * Returns a {@link Function} that returns {@link SummaryStatistics} for
     * {@link TimeSeries}.
     *
     * @return
     */
    public static Function<? super TimeSeries, SummaryStatistics> timeSeriesStats() {
        return new TimeSeriesStatistics();
    }

    /**
     * Returns a transformation {@link Function} that given a {@link DataPoint}
     * extracts its value.
     *
     * @return
     */
    public static Function<? super DataPoint, Double> toValue() {
        return new DataPointToValueTransformer();
    }

    /**
     * A transformation {@link Function} that when applied to a
     * {@link DataPoint} extracts the value of the {@link DataPoint}.
     * <p/>
     * Can be used to transform a collection of {@link DataPoint}s to a
     * collection of values. See
     * {@link Iterables#transform(Iterable, Function)}.
     *
     * @see http://code.google.com/p/guava-libraries/wiki/FunctionalExplained
     */
    public static class DataPointToValueTransformer implements Function<DataPoint, Double> {
        /**
         * Extracts the value field of a {@link DataPoint}.
         *
         * @see Function#apply(Object)
         */
        @Override
        public Double apply(DataPoint dataPoint) {
            return dataPoint.getValue();
        }
    }

    /**
     * Function that returns {@link SummaryStatistics} for a {@link TimeSeries}.
     */
    public static class TimeSeriesStatistics implements Function<TimeSeries, SummaryStatistics> {

        @Override
        public SummaryStatistics apply(TimeSeries input) {
            SummaryStatistics statistics = new SummaryStatistics();
            for (DataPoint dataPoint : input.getDataPoints()) {
                statistics.addValue(dataPoint.getValue());
            }
            return statistics;
        }

    }
}
