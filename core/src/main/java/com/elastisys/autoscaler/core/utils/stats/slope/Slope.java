package com.elastisys.autoscaler.core.utils.stats.slope;

import java.util.List;
import java.util.Objects;

import org.apache.commons.math3.exception.MathRuntimeException;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.elastisys.autoscaler.core.utils.stats.timeseries.DataPoint;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeries;

/**
 * Calculates the slope for a {@link TimeSeries}.
 *
 * @see TimeSeries
 * @see SlopeDirection
 */
public class Slope {

    /** Observed data points. */
    private TimeSeries dataSeries;

    /**
     * When slope is to be considered horizontal (that is, the upper limit when
     * the absolute delta is to be considered negligible)
     */
    private double horizontalSlopeMargin;

    /**
     * Constructs a {@link Slope} without a horizontal slope margin. The slope
     * will only be considered horizontal if the slope is exactly zero.
     *
     * @param dataSeries
     */
    public Slope(TimeSeries dataSeries) {
        this(dataSeries, 0.0);
    }

    /**
     * Constructs a {@link Slope}.
     *
     * @param dataSeries
     *            The {@link SlidingDataSeries} on which to base slope
     *            calculations.
     * @param horizontalSlopeMargin
     *            The slope error margin within which the slope is to be
     *            considered horizontal. That is, if the (absolute) slope is
     *            smaller than <code>horizontalSlopeMargin</code> the slope is
     *            considered horizontal.
     */
    public Slope(TimeSeries dataSeries, double horizontalSlopeMargin) {
        this.dataSeries = dataSeries;
        this.horizontalSlopeMargin = horizontalSlopeMargin;
    }

    /**
     * Returns <code>true</code> if slope is up.
     *
     * @return
     */
    public boolean isUp() {
        return !isHorizontal() && slope() > 0;
    }

    /**
     * Returns <code>true</code> if slope is down.
     *
     * @return
     */
    public boolean isDown() {
        return !isHorizontal() && slope() < 0;
    }

    /**
     * Returns the slope for the data sample as a {@link SlopeDirection} object.
     *
     * @return The slope direction as a {@link SlopeDirection} instance.
     */
    public SlopeDirection getDirection() {
        return isHorizontal() ? SlopeDirection.HORIZONTAL : isUp() ? SlopeDirection.UP : SlopeDirection.DOWN;
    }

    /**
     * Returns <code>true</code> if the slope is considered horizontal. That is,
     * the (absolute) slope is within the {@link #horizontalSlopeMargin}.
     *
     * @return
     */
    public boolean isHorizontal() {
        return Math.abs(slope()) <= this.horizontalSlopeMargin;
    }

    /**
     * Returns the approximated slope of the data point observations.
     *
     * @return
     */
    public double slope() {
        if (this.dataSeries.isEmpty()) {
            return 0.0;
        }
        if (this.dataSeries.getDataPoints().size() <= 1) {
            return 0.0;
        }
        // try fitting a line to data points
        SimpleRegression fittedLine = getLineFit(this.dataSeries.getDataPoints());
        return fittedLine.getSlope();
    }

    /**
     * Returns a best-fit straight line through a collection of data points.
     *
     * @param dataPoints
     *            The data points that the line is to be fitted against.
     * @return The fitted line.
     */
    private SimpleRegression getLineFit(List<DataPoint> dataPoints) {
        SimpleRegression regression = new SimpleRegression();
        for (DataPoint dataPoint : dataPoints) {
            regression.addData(dataPoint.getTime().getMillis() / 1000.0, dataPoint.getValue());
        }
        return regression;
    }

    @Override
    public String toString() {
        String slopeDirection = isHorizontal() ? "STABLE" : isUp() ? "UP" : "DOWN";
        SimpleRegression lineFit = getLineFit(this.dataSeries.getDataPoints());
        try {
            return String.format("%s (slope: %.4f, slope std err: %.4f, " + "slope confidence interval: %.4f)",
                    slopeDirection, lineFit.getSlope(), lineFit.getSlopeStdErr(), lineFit.getSlopeConfidenceInterval());
        } catch (MathRuntimeException e) {
            throw new RuntimeException("Failed to calculate fitted line properties: " + e.getMessage(), e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.horizontalSlopeMargin, this.dataSeries);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Slope) {
            Slope that = (Slope) obj;
            return Objects.equals(this.horizontalSlopeMargin, that.horizontalSlopeMargin)
                    && Objects.equals(this.dataSeries, that.dataSeries);
        }
        return false;
    }
}
