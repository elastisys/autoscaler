package com.elastisys.autoscaler.core.utils.stats.slope;

/**
 * Represents a direction for a {@link Slope}.
 * 
 * @see Slope
 * 
 * 
 */
public enum SlopeDirection {
    /** {@link Slope} is going up (positive slope). */
    UP,
    /** {@link Slope} is going down (negative slope). */
    DOWN,
    /**
     * {@link Slope} is considered horizontal (zero slope, or a slope that is so
     * small that it is considered to be zero).
     */
    HORIZONTAL
}
