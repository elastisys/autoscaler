package com.elastisys.autoscaler.simulation.simulator.driver;

import java.util.Objects;

import com.elastisys.scale.commons.util.precond.Preconditions;

/**
 * Carries information about and provides access to the simulation being carried
 * out (via the {@link DiscreteEventDriver}).
 * <p/>
 * The {@link SimulatorContext} for a running simulation can be associated with
 * a {@link Thread} by calling {@link SimulatorContext#set(SimulatorContext)}
 * and can later be retrieved by simulation objects via
 * {@link SimulatorContext#get()} as long as all invocations are made from the
 * same {@link Thread}.
 */
public class SimulatorContext {

    /**
     * The simulator context associated with the current {@link Thread}.
     */
    private static ThreadLocal<SimulatorContext> threadContext = new ThreadLocal<SimulatorContext>();

    /**
     * Sets the {@link SimulatorContext} associated with the current
     * {@link Thread}.
     *
     * @param threadContext
     */
    public static void set(SimulatorContext context) {
        Objects.requireNonNull(context, "attempt to set null simulator context");
        threadContext.set(context);
    }

    /**
     * Returns the {@link SimulatorContext} associated with the current
     * {@link Thread} or throws an {@link IllegalStateException} if no context
     * has been associated with the calling {@link Thread}.
     *
     * @return
     */
    public static SimulatorContext get() {
        Preconditions.checkState(threadContext.get() != null, "no simulator context set for this thread");
        return threadContext.get();
    }

    /** The {@link DiscreteEventDriver} of this {@link SimulatorContext}. */
    private final DiscreteEventDriver eventDriver;

    /**
     * Constructs a new {@link SimulatorContext}.
     *
     * @param eventDriver
     *            The {@link DiscreteEventDriver} of this
     *            {@link SimulatorContext}.
     */
    public SimulatorContext(DiscreteEventDriver eventDriver) {
        this.eventDriver = eventDriver;
    }

    /**
     * Returns the {@link DiscreteEventDriver} of this {@link SimulatorContext}.
     */
    public DiscreteEventDriver getEventDriver() {
        return this.eventDriver;
    }

}
