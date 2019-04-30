package com.elastisys.autoscaler.predictors.rulebased.rule;

/**
 * Represents the collection of valid resize units that can be used to define a
 * {@link ScalingRule}.
 * 
 * @see ScalingRule
 * 
 * 
 */
public enum ResizeUnit {

    /**
     * The resize adjustment defined by the {@link ScalingRule} is specified in
     * number of instances.
     */
    INSTANCES,
    /**
     * The resize adjustment defined by the {@link ScalingRule} is specified in
     * percent of the current capacity.
     */
    PERCENT
}
