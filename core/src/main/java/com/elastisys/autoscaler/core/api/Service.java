package com.elastisys.autoscaler.core.api;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.util.concurrent.RestartableScheduledExecutorService;

/**
 * A {@link Service} is a subsystem of an {@link AutoScaler} instance and can be
 * started, stopped and return its current status.
 * <p/>
 * A service is also {@link Configurable}.
 * <p/>
 * At creation time, a {@link Service} can be passed references to certain
 * objects of its parent {@link AutoScaler} via dependency injection. To have
 * dependencies injected, a {@link Service} implementation class can make use of
 * any of the
 * <a href="http://code.google.com/p/google-guice/wiki/Injections">Guice-
 * supported injection methods</a>. For example, a {@link Inject} -annotated
 * constructor.
 * <p/>
 * Through Guice-based dependency injection, the following objects of the parent
 * {@link AutoScaler} can be injected in the {@link Service}:
 * <ul>
 * <li>{@link Logger}: the instance's logger.</li>
 * <li>{@link EventBus}: the instance's event bus.</li>
 * <li>{@link ExecutorService} or {@link ScheduledExecutorService} or
 * {@link RestartableScheduledExecutorService}: the instance's executor service.
 * </li>
 * <li>a "{@code StorageDir}" {@link Named}-annotated {@link File}: the
 * instance's storage directory.</li>
 * <li>a "{@code AutoScalerId}" {@link Named}-annotated {@link String}: the
 * instance's unique identifier.</li>
 * <li>subsystem references: references to other subsystems ({@link Service}s)
 * of the instance.</li>
 * </ul>
 * <p/>
 * Note that it is recommended that a {@link Service} makes use of logger,
 * executor, etc provided by its parent {@link AutoScaler} instance rather than
 * using its own.
 *
 * @see AutoScaler
 *
 *
 * @param <T>
 *            The type of the configuration objects accepted by this
 *            {@link Service}
 */
public interface Service<T> extends Configurable<T> {

    /**
     * Starts the service.
     * <p>
     * This method must complete and return to its caller in a timely manner. If
     * this {@link Service} needs to perform any long-running tasks, these
     * should be started in a separate thread of execution.
     * <p/>
     * An attempt to start a {@link Service} that has not been configured should
     * result in a {@link IllegalStateException} being thrown.
     * <p/>
     * Starting an already started {@link Service} is a no-op.
     *
     * @throws IllegalStateException
     *             If an attempt is made to start a {@link Service} that has not
     *             been configured.
     */
    void start() throws IllegalStateException;

    /**
     * Stops the service.
     * <p/>
     * This method must complete and return to its caller in a timely manner. If
     * this {@link Service} needs to perform any long-running tasks, these
     * should be started in a separate thread of execution.
     * <p/>
     * Stopping an already stopped {@link Service} is a no-op.
     */
    void stop();

    /**
     * Returns the current status of this {@link Service}.
     *
     * @return
     */
    ServiceStatus getStatus();
}
