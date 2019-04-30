package com.elastisys.autoscaler.core.monitoring.metricstreamer.commons;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.joda.time.Duration;

import com.elastisys.autoscaler.core.api.types.MetricValue;

/**
 * A {@link Callable} task that, when executed, fetches {@link MetricValue}s and
 * converts them from absolute metric values to change rate values. That is,
 * instead of returning each metric value as-is, the observed change rate from
 * the prior metric value is calculated:
 *
 * <pre>
 * rate[i] = (v(t[i]) - v(t[i - 1])) / (t[i] - t[i - 1])
 * </pre>
 *
 * This also means that for an N-point data series, only {@code N - 1} values
 * will be returned (no rate can be calculated for the first value).
 */
public class RateConverter implements Function<List<MetricValue>, List<MetricValue>> {

    /**
     * Constructs a new {@link RateConverter} that convert a collection of
     * metric values from absolute values to change rates.
     *
     * @param metricValueFetcher
     *            The task that will collect the {@link MetricValue}s to be
     *            converted.
     */
    public RateConverter() {
    }

    @Override
    public List<MetricValue> apply(List<MetricValue> values) {
        return toRate(values);
    }

    private List<MetricValue> toRate(List<MetricValue> dataPoints) {
        Objects.requireNonNull(dataPoints, "dataPoints is null");
        checkArgument(dataPoints.size() >= 2, "cannot convert to rate with less than two data points");

        List<MetricValue> rateValues = new ArrayList<>();

        Iterator<MetricValue> datapointIterator = dataPoints.iterator();
        MetricValue previous = datapointIterator.next();
        while (datapointIterator.hasNext()) {
            MetricValue next = datapointIterator.next();
            double nextValue = next.getValue();
            double previousValue = previous.getValue();
            double timeDelta = new Duration(previous.getTime(), next.getTime()).getStandardSeconds();
            double rate = (nextValue - previousValue) / timeDelta;
            rateValues.add(new MetricValue(next.getMetric(), rate, next.getTime()));

            previous = next;
        }

        return rateValues;
    }

}