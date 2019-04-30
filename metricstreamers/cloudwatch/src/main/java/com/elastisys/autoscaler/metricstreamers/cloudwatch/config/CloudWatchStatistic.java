package com.elastisys.autoscaler.metricstreamers.cloudwatch.config;

import com.amazonaws.services.cloudwatch.model.Statistic;

/**
 * An enumeration for the set of Amazon CloudWatch-supported metric statistics.
 * Refer to the <a href=
 * "http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/cloudwatch_concepts.html#Statistic"
 * >CloudWatch documentation</a> for details.
 */
public enum CloudWatchStatistic {

    /**
     * CloudWatch statistic that calculates the average over each query period.
     */
    Average,
    /** CloudWatch statistic that calculates the sum over each query period. */
    Sum,
    /** CloudWatch statistic that calculates the sample count over a query. */
    SampleCount,
    /**
     * CloudWatch statistic that calculates the maximum over each query period.
     */
    Maximum,
    /**
     * CloudWatch statistic that calculates the minimum over each query period.
     */
    Minimum;

    /**
     * Convert this {@link CloudWatchStatistic} to the corresponding
     * {@link Statistics} object that jclouds understands.
     *
     * @return
     */
    public Statistic toStatistic() {
        switch (this) {
        case Average:
            return Statistic.Average;
        case Sum:
            return Statistic.Sum;
        case SampleCount:
            return Statistic.SampleCount;
        case Maximum:
            return Statistic.Maximum;
        case Minimum:
            return Statistic.Minimum;
        default:
            throw new IllegalArgumentException(String.format("unrecognized CloudWatch statistic '%s'", name()));
        }
    }
}
