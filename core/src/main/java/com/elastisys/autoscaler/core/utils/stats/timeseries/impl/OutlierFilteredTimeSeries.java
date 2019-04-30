package com.elastisys.autoscaler.core.utils.stats.timeseries.impl;

import static com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeriesPredicates.isOutlier;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeries;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeriesPredicates;

/**
 * A {@link TimeSeries} that filters out outlier {@link DataPoint}s from an
 * underlying {@link TimeSeries}.
 * <p/>
 * Outliers are filtered according to {@link TimeSeriesPredicates.IsOutlier}.
 * <p/>
 * Note that the {@link OutlierFilteredTimeSeries} does not modify its backing
 * {@link TimeSeries}. Filtering of time series values are only applied when
 * {@link #getDataPoints()} is called and filtered values are only hidden from
 * the caller, they are not discarded from the underlying data set.
 */
public class OutlierFilteredTimeSeries implements TimeSeries {

    static final Logger LOG = LoggerFactory.getLogger(OutlierFilteredTimeSeries.class);

    /** The {@link TimeSeries} to which the outlier filter will be applied. */
    private final TimeSeries filteredSeries;

    /**
     * Creates a new {@link OutlierFilteredTimeSeries} that wraps a given
     * {@link TimeSeries}.
     *
     * @param backingSeries
     *            The {@link TimeSeries} to which the outlier filter will be
     *            applied.
     */
    public OutlierFilteredTimeSeries(TimeSeries backingSeries) {
        this.filteredSeries = new FilteredTimeSeries(backingSeries, new OutlierIdentificationPredicate(backingSeries));
    }

    @Override
    public void add(DataPoint dataPoint) {
        this.filteredSeries.add(dataPoint);
    }

    @Override
    public void addAll(Collection<? extends DataPoint> dataPoints) {
        this.filteredSeries.addAll(dataPoints);
    }

    @Override
    public void remove(int index) throws IndexOutOfBoundsException {
        this.filteredSeries.remove(index);
    }

    @Override
    public List<DataPoint> getDataPoints() {
        return this.filteredSeries.getDataPoints();
    }

    @Override
    public boolean isEmpty() {
        return this.filteredSeries.isEmpty();
    }

    @Override
    public int size() {
        return this.filteredSeries.size();
    }

    /**
     * A {@link Predicate} that, for a certain {@link TimeSeries}, determines
     * for suspect {@link DataPoint}s if they are to be considered outliers of
     * the time series.
     */
    public static class OutlierIdentificationPredicate implements Predicate<DataPoint> {

        private final TimeSeries series;

        public OutlierIdentificationPredicate(TimeSeries series) {
            this.series = series;
        }

        @Override
        public boolean test(DataPoint suspect) {
            // calculate statistical properties of data set
            SummaryStatistics stats = new SummaryStatistics();
            for (DataPoint dataPoint : this.series.getDataPoints()) {
                stats.addValue(dataPoint.getValue());
            }
            double mean = stats.getMean();
            double standardDeviation = stats.getStandardDeviation();
            long samples = stats.getN();
            final double sigmasFromMean = Math.abs(suspect.getValue() - mean) / standardDeviation;

            // apply Chauvenet's criterion on the suspect data point
            boolean isOutlier = isOutlier(mean, standardDeviation, samples).test(suspect);
            if (isOutlier) {
                LOG.warn(
                        "detected outlier {} ({} sigmas from mean) in series with "
                                + "mean: {}, stddev: {}, samples: {}",
                        suspect, sigmasFromMean, mean, standardDeviation, samples);
            }
            return !isOutlier;
        }
    }
}
