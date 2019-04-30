package com.elastisys.autoscaler.metricstreamers.ceilometer.config;

/**
 * The Ceilometer aggregation functions that are supported by the <a href=
 * "http://docs.openstack.org/developer/ceilometer/webapi/v2.html#meters">statistics
 * endpoint</a> .
 */
public enum CeilometerFunction {
    /** Calculates the average over each query period. */
    Average,
    /** Calculates the sum over each query period. */
    Sum,
    /** Calculates the sample count over a query. */
    SampleCount,
    /** Calculates the maximum over each query period. */
    Maximum,
    /** Calculates the minimum over each query period. */
    Minimum;
}
