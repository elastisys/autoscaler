package com.elastisys.autoscaler.simulation.simulator.driver;

/**
 * An action that is performed when its associated {@link Event} is
 * scheduled/triggered.
 * <p/>
 * An {@link EventAction} can execute arbitrary code, be passed parameters
 * (through its {@link Event}), discover the simulation time, and inject further
 * {@link Event}s into the simulator ({@link DiscreteEventDriver}).
 * 
 * @see Event
 * 
 * 
 */
public interface EventAction {
    /**
     * Executes this {@link EventAction}.
     * 
     * @param context
     *            The {@link EventContext} that carries contextual state for
     *            this event. Through this object, the event can discover the
     *            current simulation time, schedule additional {@link Event}s
     *            with the simulator, etc.
     * @param parameters
     *            Any additional parameters that were passed to the
     *            {@link Event} during construction.
     * @throws Exception
     */
    void execute(EventContext context, Object... parameters) throws Exception;
}
