package com.elastisys.autoscaler.server.restapi;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.api.Service;
import com.elastisys.autoscaler.core.api.types.ServiceStatus;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerBlueprint;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactory;
import com.elastisys.autoscaler.server.restapi.types.ServiceStatusType;
import com.elastisys.autoscaler.server.restapi.types.UrlsType;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.ErrorType;
import com.elastisys.scale.commons.util.strings.StringUtils;
import com.google.gson.JsonObject;

/**
 * A JAX-RS REST response handler web resource that provides a remote management
 * API for an {@link AutoScalerFactory} and its set of {@link AutoScaler}
 * instances.
 * <p/>
 * This REST API is a thin wrapper on top of the {@link AutoScalerFactory} and
 * {@link AutoScaler} APIs.
 *
 * @see AutoScalerFactory
 * @see AutoScaler
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AutoScalerFactoryRestApi {

    /** {@link Logger} instance. */
    private static Logger logger = LoggerFactory.getLogger(AutoScalerFactoryRestApi.class);

    /**
     * Request information that gets injected by the JAX-RS runtime for each
     * method invocation. As such, its use is thread-safe.
     */
    @Context
    private UriInfo requestUri;

    /** The {@link AutoScalerFactory} backing this REST endpoint. */
    private final AutoScalerFactory autoScalerFactory;

    /**
     * Constructs a new {@link AutoScalerFactoryRestApi} with a given backing
     * {@link AutoScalerFactory}.
     *
     * @param autoScalerFactory
     *            The {@link AutoScalerFactory} backing this REST endpoint.
     */
    public AutoScalerFactoryRestApi(AutoScalerFactory autoScalerFactory) {
        this.autoScalerFactory = autoScalerFactory;
    }

    public void start() {
        logger.info("REST endpoint started.");
        this.autoScalerFactory.start();
    }

    public void stop() {
        logger.info("REST endpoint stopped.");
        this.autoScalerFactory.stop();
    }

    //
    // Factory methods
    //

    /**
     * Returns URLs to all available {@link AutoScaler} instances.
     *
     * @return A list of {@link AutoScaler} instance URLs.
     */
    @GET
    @Path("/autoscaler/instances")
    public Response getInstances() {
        try {
            Set<String> instanceIds = this.autoScalerFactory.getAutoScalerIds();
            List<String> absoluteUris = instanceIds.stream().map(StringUtils.prepend(requestUri() + "/"))
                    .collect(Collectors.toList());
            return Response.ok(new UrlsType(absoluteUris)).build();
        } catch (Exception e) {
            String message = String.format("failed to get autoscaler instances: %s", e.getMessage());
            logger.warn(message, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorType(message, e)).build();
        }
    }

    /**
     * Creates an {@link AutoScaler} from a JSON-formatted
     * {@link AutoScalerBlueprint}. The {@link AutoScalerBlueprint} specifies
     * the implementation classes to use for the various {@link AutoScaler}
     * subsystems.
     * <p/>
     * For cases where the provided blueprint doesn't explicitly provide an
     * implementation class for a certain subsystem, the factory will fall back
     * to using defaults as specified in the {@link AutoScaler.Defaults}.
     * <p/>
     * The created {@link AutoScaler} instance is in an unconfigured and
     * unstarted state.
     *
     * @param systemBlueprint
     *            A JSON-formatted {@link AutoScalerBlueprint} that specifies
     *            the implementation classes to use for the various
     *            {@link AutoScaler} subsystems.
     * @return The URL to the created {@link AutoScaler} instance.
     */
    @POST
    @Path("/autoscaler/instances")
    public Response createInstance(JsonObject jsonBlueprint) {
        try {
            AutoScaler autoScaler = this.autoScalerFactory.createAutoScaler(jsonBlueprint);
            saveAutoScaler(autoScaler.getId());
            URI autoscalerUri = this.requestUri.getAbsolutePathBuilder().path(autoScaler.getId()).build();
            return Response.created(autoscalerUri).build();
        } catch (IllegalArgumentException e) {
            String message = String.format("failed to create autoscaler instance: %s", e.getMessage());
            logger.warn(message, e);
            return Response.status(Status.BAD_REQUEST).entity(new ErrorType(message, e)).build();
        } catch (Exception e) {
            String message = String.format("failed to create autoscaler instance: %s", e.getMessage());
            logger.warn(message, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorType(message, e)).build();
        }
    }

    /**
     * Returns the {@link AutoScalerBlueprint} from which a given
     * {@link AutoScaler} instance was created.
     * <p/>
     * The blueprint could, for example, be used as factory input to produce a
     * clone of a certain {@link AutoScaler} instance.
     *
     * @param id
     *            The identifier of the {@link AutoScaler} instance of interest.
     * @return The blueprint from which the {@link AutoScaler} instance was
     *         created.
     */
    @GET
    @Path("/autoscaler/instances/{id}/blueprint")
    public Response getInstanceBlueprint(@PathParam("id") String autoScalerId) {
        try {
            AutoScalerBlueprint blueprint = this.autoScalerFactory.getBlueprint(autoScalerId);
            return Response.ok(JsonUtils.toJson(blueprint)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.NOT_FOUND).entity(new ErrorType(e)).build();
        } catch (Exception e) {
            String message = String.format("failed to get instance blueprint: %s", e.getMessage());
            logger.warn(message, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorType(message, e)).build();
        }
    }

    /**
     * Returns the {@link AutoScalerBlueprint} from which a given
     * {@link AutoScaler} instance was created.
     * <p/>
     * The blueprint could, for example, be used as factory input to produce a
     * clone of a certain {@link AutoScaler} instance.
     *
     * @param id
     *            The identifier of the {@link AutoScaler} instance of interest.
     * @return The blueprint from which the {@link AutoScaler} instance was
     *         created.
     */
    @DELETE
    @Path("/autoscaler/instances/{id}")
    public Response deleteInstance(@PathParam("id") String autoScalerId) {
        try {
            this.autoScalerFactory.deleteAutoScaler(autoScalerId);
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.NOT_FOUND).entity(new ErrorType(e)).build();
        } catch (Exception e) {
            String message = String.format("failed to get delete instance: %s", e.getMessage());
            logger.warn(message, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorType(message, e)).build();
        }
    }

    //
    // Instance management methods
    //

    /**
     * Retrieves the universally unique identifier (UUID) assigned to the
     * {@link AutoScaler} instance at the time of creation.
     *
     * @param autoScalerId
     * @return
     */
    @GET
    @Path("/autoscaler/instances/{id}/uuid")
    public Response getInstanceUuid(@PathParam("id") String autoScalerId) {
        return Response.ok().entity(getAutoScaler(autoScalerId).getUuid().toString()).build();
    }

    /**
     * Sets a new configuration for all subsystems of a certain
     * {@link AutoScaler} instance.
     *
     * @param autoScalerId
     * @param config
     * @return
     */
    @POST
    @Path("/autoscaler/instances/{id}/config")
    public Response postInstanceConfig(@PathParam("id") String autoScalerId, JsonObject config) {
        try {
            Response response = setSubsystemConfig(getAutoScaler(autoScalerId), config);
            saveAutoScaler(autoScalerId);
            return response;
        } catch (Exception e) {
            String message = String.format("failed to set instance config: %s", e.getMessage());
            logger.warn(message, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorType(message, e)).build();
        }
    }

    /**
     * Retrieves the full (JSON) configuration document of a certain
     * {@link AutoScaler} instance and all its subsystems.
     * <p/>
     * Note: an {@link AutoScaler} instance clone could be produced by
     * instantiating another {@link AutoScaler} instance with the same blueprint
     * and then applying the same configuration document to the created
     * instance.
     *
     * @param autoScalerId
     * @return
     */
    @GET
    @Path("/autoscaler/instances/{id}/config")
    public Response getInstanceConfig(@PathParam("id") String autoScalerId) {
        return getSubsystemConfig(getAutoScaler(autoScalerId));
    }

    /**
     * Retrieves the {@link ServiceStatus} for a certain {@link AutoScaler}
     * instance and all its subsystems.
     *
     * @param autoScalerId
     * @return
     */
    @GET
    @Path("/autoscaler/instances/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstanceStatus(@PathParam("id") String autoScalerId) {
        return getSubsystemStatus(getAutoScaler(autoScalerId));
    }

    /**
     * Starts an {@link AutoScaler} instance and all its subsystems.
     *
     * @param autoScalerId
     * @return
     */
    @POST
    @Path("/autoscaler/instances/{id}/start")
    public Response startInstance(@PathParam("id") String autoScalerId) {
        Response response = startSubsystem(getAutoScaler(autoScalerId));
        saveAutoScaler(autoScalerId);
        return response;
    }

    /**
     * Stops an {@link AutoScaler} instance and all its subsystems.
     *
     * @param autoScalerId
     * @return
     */
    @POST
    @Path("/autoscaler/instances/{id}/stop")
    public Response stopInstance(@PathParam("id") String autoScalerId) {
        Response response = stopSubsystem(getAutoScaler(autoScalerId));
        saveAutoScaler(autoScalerId);
        return response;
    }

    /**
     * Returns the {@link AutoScalerFactory} backing this REST endpoint.
     *
     * @return
     */
    public AutoScalerFactory getAutoScalerFactory() {
        return this.autoScalerFactory;
    }

    /**
     * Returns the full URI of the request currently being processed with any
     * trailing slash(es) removed.
     *
     * @return
     */
    private String requestUri() {
        return this.requestUri.getRequestUri().toString().replaceFirst("/*$", "");
    }

    /**
     * Returns a particular {@link AutoScaler} instance or throws a
     * {@link WebApplicationException} if the {@link AutoScaler} instance does
     * not exist. The {@link WebApplicationException} gets translated to an
     * error {@link Response} by JAX-RS.
     *
     * @param autoScalerId
     *            The identifier of the requested {@link AutoScaler} instance.
     * @return The {@link AutoScaler}.
     * @throws WebApplicationException
     */
    private AutoScaler getAutoScaler(String autoScalerId) throws WebApplicationException {
        try {
            requireNonNull(autoScalerId, "null auto-scaler id");
            return this.autoScalerFactory.getAutoScaler(autoScalerId);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(new ErrorType(e)).build());
        }
    }

    /**
     * Saves the state of a certain {@link AutoScaler} instance or throws a
     * {@link WebApplicationException} if the operation fails. The
     * {@link WebApplicationException} gets translated to an error
     * {@link Response} by JAX-RS.
     *
     * @param autoScalerId
     *            An {@link AutoScaler} instance identifier.
     * @throws WebApplicationException
     */
    private void saveAutoScaler(String autoScalerId) throws WebApplicationException {
        try {
            requireNonNull(autoScalerId, "null auto-scaler id");
            this.autoScalerFactory.save(autoScalerId);
        } catch (Exception e) {
            throw new WebApplicationException(e,
                    Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorType(e)).build());
        }
    }

    /**
     * Attempts to configures an {@link AutoScaler} subsystem ({@link Service})
     * with a given JSON document.
     * <p/>
     * The configuration is validated against the subsystem before being
     * applied.
     * <p/>
     * Returns a {@link Status#OK} HTTP {@link Response} if configuration was
     * properly applied. Otherwise a {@link Response} with a suitable HTTP error
     * code is returned.
     *
     * @param subsystem
     *            The {@link AutoScaler} subsystem to configure.
     * @param config
     *            The configuration document.
     * @return A HTTP {@link Response} message.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Response setSubsystemConfig(Service subsystem, JsonObject config) {
        Object configuration = null;
        try {
            requireNonNull(subsystem, "no subsystem specified");
            requireNonNull(config, "missing json configuration document");
            if (subsystem.getConfigurationClass() == JsonObject.class) {
                configuration = config;
            } else {
                configuration = JsonUtils.toObject(config, subsystem.getConfigurationClass());
            }
        } catch (Exception e) {
            String message = String.format("failed to set config: %s", e.getMessage());
            logger.warn(message, e);
            return Response.status(Status.BAD_REQUEST).entity(new ErrorType(message, e)).build();
        }
        try {
            subsystem.validate(configuration);
            subsystem.configure(configuration);
        } catch (IllegalArgumentException e) {
            String message = String.format("failed to set config: %s", e.getMessage());
            logger.warn(message, e);
            return Response.status(Status.BAD_REQUEST).entity(new ErrorType(message, e)).build();
        }
        return Response.ok().build();
    }

    /**
     * Retrieves a configuration JSON document for a given {@link AutoScaler}
     * subsystem ({@link Service}).
     * <p/>
     * Returns a {@link Status#OK} HTTP {@link Response} with the
     * (JSON-formatted) configuration document on success. Otherwise a
     * {@link Response} with a suitable HTTP error code is returned.
     *
     * @param subsystem
     *            The {@link AutoScaler} subsystem for which to fetch
     *            configuration.
     * @return A HTTP {@link Response} message.
     */
    @SuppressWarnings("rawtypes")
    private Response getSubsystemConfig(Service subsystem) {
        try {
            requireNonNull(subsystem, "no subsystem specified");
            Object configuration = subsystem.getConfiguration();
            requireNonNull(configuration,
                    String.format("no configuration set for subsystem '%s'", subsystem.getClass().getName()));
            JsonObject jsonConfig = null;
            if (configuration.getClass() == JsonObject.class) {
                jsonConfig = JsonObject.class.cast(configuration);
            } else {
                jsonConfig = JsonUtils.toJson(configuration).getAsJsonObject();
            }
            return Response.ok().entity(jsonConfig).build();
        } catch (NullPointerException e) {
            String message = String.format("failed to get config: %s", e.getMessage());
            logger.warn(message, e);
            return Response.status(Status.NOT_FOUND).entity(new ErrorType(message, e)).build();
        } catch (Exception e) {
            String message = String.format("failed to get config: %s", e.getMessage());
            logger.warn(message, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorType(message, e)).build();
        }
    }

    /**
     * Retrieves the status for a given {@link AutoScaler} subsystem (
     * {@link Service}).
     * <p/>
     * Returns a {@link Status#OK} HTTP {@link Response} with the
     * (JSON-formatted) {@link ServiceStatusType} document on success. Otherwise
     * a {@link Response} with a suitable HTTP error code is returned.
     *
     * @param subsystem
     *            The {@link AutoScaler} subsystem for which to fetch status.
     * @return A HTTP {@link Response} message.
     */
    @SuppressWarnings("rawtypes")
    private Response getSubsystemStatus(Service subsystem) {
        try {
            requireNonNull(subsystem, "no subsystem specified");
            ServiceStatus status = subsystem.getStatus();
            requireNonNull(status,
                    String.format("no status could be retrieved for subsystem '%s'", subsystem.getClass().getName()));
            return Response.ok().entity(new ServiceStatusType(status)).build();
        } catch (Exception e) {
            String message = String.format("failed to get status: %s", e.getMessage());
            logger.warn(message, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorType(message, e)).build();
        }
    }

    /**
     * Starts an {@link AutoScaler} subsystem ({@link Service}).
     * <p/>
     * Returns a {@link Status#OK} HTTP {@link Response} on success. Otherwise a
     * {@link Response} with a suitable HTTP error code is returned.
     *
     * @param subsystem
     *            The {@link AutoScaler} subsystem which to start.
     * @return A HTTP {@link Response} message.
     */
    @SuppressWarnings("rawtypes")
    private Response startSubsystem(Service subsystem) {
        try {
            checkArgument(subsystem != null, "startSubsystem: no subsystem specified");
            subsystem.start();
            return Response.ok().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            String message = String.format("failed to start: %s", e.getMessage());
            logger.warn(message, e);
            return Response.status(Status.BAD_REQUEST).entity(new ErrorType(message, e)).build();
        } catch (Exception e) {
            String message = String.format("failed to start: %s", e.getMessage());
            logger.warn(message, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorType(message, e)).build();
        }
    }

    /**
     * Stops an {@link AutoScaler} subsystem ({@link Service}).
     * <p/>
     * Returns a {@link Status#OK} HTTP {@link Response} on success. Otherwise a
     * {@link Response} with a suitable HTTP error code is returned.
     *
     * @param subsystem
     *            The {@link AutoScaler} subsystem which to stop.
     * @return A HTTP {@link Response} message.
     */
    @SuppressWarnings("rawtypes")
    private Response stopSubsystem(Service subsystem) {
        try {
            checkArgument(subsystem != null, "stopSubsystem: no subsystem specified");
            subsystem.stop();
            return Response.ok().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            String message = String.format("failed to stop: %s", e.getMessage());
            logger.warn(message, e);
            return Response.status(Status.BAD_REQUEST).entity(new ErrorType(message, e)).build();
        } catch (Exception e) {
            String message = String.format("failed to stop: %s", e.getMessage());
            logger.warn(message, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorType(message, e)).build();
        }
    }
}
