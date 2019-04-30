package com.elastisys.autoscaler.core.utils.stats.timeseries;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;

import org.apache.commons.math3.exception.MathRuntimeException;
import org.apache.commons.math3.special.Erf;
import org.joda.time.DateTime;
import org.joda.time.Interval;

/**
 * {@link Predicate}s related to {@link TimeSeries} and {@link DataPoint}s.
 *
 * @see TimeSeries
 * @see DataPoint
 */
public class TimeSeriesPredicates {

    /**
     * Returns a {@link Predicate} that returns <code>true</code> when passed a
     * {@link DataPoint} whose time stamp lies within a given time
     * {@link Interval}.
     * <p/>
     * Note: the semantics of the
     * {@link Interval#contains(org.joda.time.ReadableInstant)} method is used
     * to decide whether a {@link DataPoint} falls within the given
     * {@link Interval}.
     *
     * @param interval
     *            The time interval.
     * @return
     */
    public static Predicate<? super DataPoint> within(Interval interval) {
        return new DataPointValueInInterval(interval);
    }

    /**
     * Returns a {@link Predicate} that returns <code>true</code> when passed a
     * {@link DataPoint} with a time stamp that is older than a given time
     * stamp.
     *
     * @param timestamp
     *            The time stamp to compare {@link DataPoint}s against.
     * @return
     */
    public static Predicate<? super DataPoint> olderThan(DateTime timestamp) {
        return new DataPointOlderThan(timestamp);
    }

    /**
     * Returns a {@link Predicate} that returns <code>true</code> when passed a
     * {@link DataPoint} with a time stamp that is younger than a given time
     * stamp.
     *
     * @param timestamp
     *            The time stamp to compare {@link DataPoint}s against.
     * @return
     */
    public static Predicate<? super DataPoint> youngerThan(DateTime timestamp) {
        return new DataPointYoungerThan(timestamp);
    }

    /**
     * Returns a {@link Predicate} that returns <code>true</code> for any
     * {@link DataPoint} that is regarded an outlier in a {@link TimeSeries}
     * with a given mean and standard deviation. The outlier detection algorithm
     * is based on <a href=
     * "http://en.wikipedia.org/wiki/Chauvenet%27s_criterion">Chauvenet's
     * criterion</a>.
     *
     * @param mean
     *            The mean of the time series.
     * @param standardDeviation
     *            The standard deviation of the values in the time series.
     * @param samples
     *            The number of data points in the time series.
     * @return An outlier detection {@link Predicate} for the time series.
     */
    public static Predicate<? super DataPoint> isOutlier(double mean, double standardDeviation, long samples) {
        return new IsOutlier(mean, standardDeviation, samples);
    }

    /**
     * A {@link Predicate} that returns <code>true</code> when passed a
     * {@link DataPoint} with a time stamp that is older than a given time
     * stamp.
     */
    public static class DataPointOlderThan implements Predicate<DataPoint> {
        private final DateTime timestamp;

        public DataPointOlderThan(DateTime timestamp) {
            requireNonNull(timestamp);
            this.timestamp = timestamp;
        }

        @Override
        public boolean test(DataPoint dataPoint) {
            return dataPoint.getTime().isBefore(this.timestamp);
        }
    }

    /**
     * A {@link Predicate} that returns <code>true</code> when passed a
     * {@link DataPoint} with a time stamp that is younger than a given time
     * stamp.
     */
    public static class DataPointYoungerThan implements Predicate<DataPoint> {
        private final DateTime timestamp;

        public DataPointYoungerThan(DateTime timestamp) {
            requireNonNull(timestamp);
            this.timestamp = timestamp;
        }

        @Override
        public boolean test(DataPoint dataPoint) {
            return dataPoint.getTime().isAfter(this.timestamp);
        }
    }

    /**
     * A {@link Predicate} that returns <code>true</code> when passed a
     * {@link DataPoint} whose time stamp lies within a given time
     * {@link Interval}.
     * <p/>
     * Note: the semantics of the
     * {@link Interval#contains(org.joda.time.ReadableInstant)} method is used
     * to decide whether a {@link DataPoint} falls within the given
     * {@link Interval}.
     */
    public static class DataPointValueInInterval implements Predicate<DataPoint> {
        private final Interval interval;

        public DataPointValueInInterval(Interval interval) {
            requireNonNull(interval);
            this.interval = interval;
        }

        @Override
        public boolean test(DataPoint dataPoint) {
            return this.interval.contains(dataPoint.getTime());
        }
    }

    /**
     * A {@link Predicate} that return <code>true</code> for any
     * {@link DataPoint} that is regarded an outlier in a {@link TimeSeries}
     * with a given mean and standard deviation. The outlier detection algorithm
     * is based on <a href=
     * "http://en.wikipedia.org/wiki/Chauvenet%27s_criterion">Chauvenet's
     * criterion</a>.
     */
    public static class IsOutlier implements Predicate<DataPoint> {
        /** The mean of the time series. */
        private final double mean;
        /** The standard deviation of the values in the time series. */
        private final double standardDeviation;
        /** The number of data points in the time series. */
        private final long samples;

        /**
         * Constructs a new {@link IsOutlier} predicate.
         *
         * @param mean
         *            The mean of the time series.
         * @param standardDeviation
         *            The standard deviation of the values in the time series.
         * @param samples
         *            The number of data points in the time series.
         */
        public IsOutlier(double mean, double standardDeviation, long samples) {
            this.mean = mean;
            this.standardDeviation = standardDeviation;
            this.samples = samples;
        }

        @Override
        public boolean test(DataPoint suspect) {
            checkArgument(suspect != null, "dataPoint cannot be null");
            double sigmasFromMean = Math.abs(suspect.getValue() - this.mean) / this.standardDeviation;
            // the probability of a data value being more than 'sigmasFromMean'
            // standard deviations away from the mean
            double probability;
            try {
                probability = Erf.erfc(sigmasFromMean / Math.sqrt(2));
            } catch (MathRuntimeException e) {
                throw new RuntimeException(String.format("failed to do time series outlier check: %s", e.getMessage()),
                        e);
            }
            // outlier score: data size times data point probability
            double outlierScore = this.samples * probability;
            return outlierScore < 0.5;
        }

    }
}
