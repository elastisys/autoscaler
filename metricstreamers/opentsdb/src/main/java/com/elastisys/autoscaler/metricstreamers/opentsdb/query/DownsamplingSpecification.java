package com.elastisys.autoscaler.metricstreamers.opentsdb.query;

import java.util.Objects;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.DownsampleFunction;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.precond.Preconditions;

/**
 * Specifies how to reduce the amount of returned {@link MetricValue}s from an
 * OpenTSDB query. To support this, OpenTSDB queries support the notion of
 * down-sampling, specifying a smallest time period between returned
 * {@link MetricValue}s and a function (such as average or sum) to apply to the
 * time-series value in each time period to represent the (single) reported
 * value for the period. For example, data can be down-sampled such that only
 * the average over a 10 minute period is reported.
 * <p/>
 * Refer to the <a href="http://opentsdb.net/metrics.html">OpenTSDB
 * documentation</a> for details.
 */
public class DownsamplingSpecification {
    /** The sampling interval. */
    private final TimeInterval interval;
    /**
     * The function used to aggregate data points within each sampling interval
     * to a single value.
     */
    private final DownsampleFunction function;

    /**
     * Specifies a down-sampling using the specified parameters.
     *
     * @param interval
     *            The sampling interval.
     * @param function
     *            The function used to aggregate data points within each
     *            sampling interval to a single value.
     */
    public DownsamplingSpecification(TimeInterval interval, DownsampleFunction function) {
        this.interval = interval;
        this.function = function;

    }

    /**
     * The sampling interval.
     *
     * @return
     */
    public TimeInterval getInterval() {
        return this.interval;
    }

    /**
     * The function used to aggregate data points within each sampling interval
     * to a single value.
     *
     * @return
     */
    public DownsampleFunction getFunction() {
        return this.function;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.function, this.interval);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DownsamplingSpecification) {
            DownsamplingSpecification that = (DownsamplingSpecification) obj;
            return Objects.equals(this.function, that.function) && Objects.equals(this.interval, that.interval);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }

    public void validate() throws IllegalArgumentException {
        Preconditions.checkArgument(this.interval != null, "downsampling: no interval specified");
        Preconditions.checkArgument(this.function != null, "downsampling: no function specified");
    }
}
