package com.elastisys.autoscaler.server;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Map;

import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactory;
import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactoryConfig;
import com.elastisys.autoscaler.server.restapi.AutoScalerFactoryRestApi;
import com.elastisys.scale.commons.cli.CommandLineParser;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.rest.filters.RequestLogFilter;
import com.elastisys.scale.commons.rest.responsehandlers.ExitHandler;
import com.elastisys.scale.commons.rest.server.JaxRsApplication;
import com.elastisys.scale.commons.server.ServletDefinition;
import com.elastisys.scale.commons.server.ServletServerBuilder;
import com.elastisys.scale.commons.server.SslKeyStoreType;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

/**
 * Main class with factory method for creating a REST API server that exposes an
 * {@link AutoScalerFactory}.
 *
 * @see AutoScalerFactory
 * @see AutoScalerFactoryRestApi
 */
public class AutoScalerFactoryServer {
    static Logger log = LoggerFactory.getLogger(AutoScalerFactoryServer.class);

    /**
     * Parses command-line arguments and start an HTTPS server that serves REST
     * API requests for an {@link AutoScalerFactory}.
     * <p/>
     * A failure to parse the command-line arguments will cause the program to
     * print a usage message and exit with an error code.
     * <p/>
     * The function blocks until the started server is stopped.
     *
     * @param args
     *            The command-line arguments.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        AutoScalerFactoryServerOptions options = parseCommandLine(args);

        // parse --addons-config if specified
        Map<String, String> addonSubsystems = null;
        if (options.addonsConfig != null) {
            addonSubsystems = loadAddons(options.addonsConfig);
        }

        launchServer(options, addonSubsystems);
    }

    /**
     * Launches an {@link AutoScalerFactoryServer} with the options and add-on
     * subsystems.
     * <p/>
     * The function blocks until the started server is stopped.
     *
     * @param options
     *            A set of options that control the behavior of the server.
     * @param addonSubsystems
     *            The collection of add-on subsystems that will be added to all
     *            {@link AutoScaler} instances the {@link AutoScalerFactory}
     *            creates. These add-on subsystems are not strictly necessary
     *            for the {@link AutoScaler} to operate, but may extend it with
     *            additional functionality. Accounting and high-availability are
     *            two examples of what such add-on subsystems could achieve.
     *            Keys are names, such as {@code accountingSubsystem}, and
     *            values are class names, such as
     *            {@code com.elastisys.AccountingSubsystemImpl}.
     * @throws IllegalArgumentException
     * @throws InterruptedException
     */
    public static void launchServer(AutoScalerFactoryServerOptions options, Map<String, String> addonSubsystems)
            throws IllegalArgumentException, InterruptedException {
        AutoScalerFactory factory = createFactory(options.storageDir, addonSubsystems);
        Server server = createServer(factory, options);

        // start server and wait
        log.info("starting server ...");
        try {
            server.start();
        } catch (Exception e) {
            log.error("failed to start server: " + e.getMessage(), e);
            System.exit(-1);
        }
        if (options.httpPort != null) {
            log.info("server listening on HTTP port {}", options.httpPort);
        }
        if (options.httpsPort != null) {
            log.info("server listening on HTTPS port {}", options.httpsPort);
        }
        server.join();
    }

    /**
     * Parses the command-line arguments and returns the
     * {@link AutoScalerFactoryServerOptions} that were specified. Will exit the
     * process in case parsing failed or if {@code --help} or {@code --version}
     * was specified.
     *
     * @param args
     *            Command-line arguments.
     * @return
     */
    public static AutoScalerFactoryServerOptions parseCommandLine(String[] args) {
        CommandLineParser<AutoScalerFactoryServerOptions> parser = new CommandLineParser<>(
                AutoScalerFactoryServerOptions.class);

        return parser.parseCommandLine(args);
    }

    /**
     * Creates a HTTPS server that serves REST API requests for a given
     * {@link AutoScalerFactory}.
     * <p/>
     * The created server is returned with the {@link AutoScalerFactory} REST
     * API deployed, but in an <i>unstarted</i> state, so the client is
     * responsible for starting the server.
     * <p/>
     * The behavior of the HTTPS server is controlled via a set of
     * {@link AutoScalerFactoryServerOptions}.
     *
     * @param factory
     *            The {@link AutoScalerFactory} to be published by the
     *            {@link Server}. Note: assumed to be passed in a configured
     *            state.
     * @param options
     *            A set of options that control the behavior of the HTTPS
     *            server.
     * @return The created {@link Server}.
     * @throws Exception
     *             Thrown on a failure to initialize the
     *             {@link AutoScalerFactory} or create the server.
     */
    public static Server createServer(AutoScalerFactory factory, AutoScalerFactoryServerOptions options) {
        JaxRsApplication application = new JaxRsApplication();
        // deploy autoscaler factory handler
        application.addHandler(new AutoScalerFactoryRestApi(factory));
        // enable request logging
        ResourceConfig appConfig = ResourceConfig.forApplication(application);
        appConfig.register(new RequestLogFilter());

        if (options.enableExitHandler) {
            // optionally deploy exit handler
            application.addHandler(new ExitHandler());
        }

        ServletContainer restApiServlet = new ServletContainer(appConfig);
        // build server
        ServletDefinition servlet = new ServletDefinition.Builder().servlet(restApiServlet).requireHttps(false)
                .requireBasicAuth(options.requireBasicAuth).realmFile(options.realmFile)
                .requireRole(options.requireRole).build();

        ServletServerBuilder server = ServletServerBuilder.create();
        if (options.httpPort != null) {
            server.httpPort(options.httpPort);
        }
        if (options.httpsPort != null) {
            server.httpsPort(options.httpsPort).sslKeyStoreType(SslKeyStoreType.PKCS12)
                    .sslKeyStorePath(options.sslKeyStore).sslKeyStorePassword(options.sslKeyStorePassword)
                    .sslTrustStoreType(SslKeyStoreType.JKS).sslTrustStorePath(options.sslTrustStore)
                    .sslTrustStorePassword(options.sslTrustStorePassword)
                    .sslRequireClientCert(options.requireClientCert);
        }
        server.addServlet(servlet);

        return server.build();
    }

    /**
     * Creates an {@link AutoScalerFactory} configured to use a given storage
     * directory.
     *
     * @param storageDir
     *            File system path to the directory where the
     *            {@link AutoScalerFactory} will persist instance state.
     * @param addonSubsystems
     *            The collection of add-on subsystems that will be added to all
     *            {@link AutoScaler} instances the {@link AutoScalerFactory}
     *            creates. These add-on subsystems are not strictly necessary
     *            for the {@link AutoScaler} to operate, but may extend it with
     *            additional functionality. Accounting and high-availability are
     *            two examples of what such add-on subsystems could achieve.
     *            Keys are names, such as {@code accountingSubsystem}, and
     *            values are class names, such as
     *            {@code com.elastisys.AccountingSubsystemImpl}.
     * @return
     * @throws IllegalArgumentException
     */
    private static AutoScalerFactory createFactory(String storageDir, Map<String, String> addonSubsystems)
            throws IllegalArgumentException {
        AutoScalerFactory factory = AutoScalerFactory.create();
        AutoScalerFactoryConfig configuration = new AutoScalerFactoryConfig(storageDir, addonSubsystems);
        factory.validate(configuration);
        factory.configure(configuration);
        factory.start();

        return factory;
    }

    /**
     * Loads any add-on subsystems from the given file path. On failure to parse
     * the JSON file, an exception is raised.
     *
     * @param addonConfigFile
     *            A file system path.
     * @return The collection of add-on subsystems to configure the autoscaler
     *         server with.
     */
    public static Map<String, String> loadAddons(String addonConfigPath) {
        File addonConfigFile = new File(addonConfigPath);
        checkArgument(addonConfigFile.isFile(), "add-on config file %s does not exist", addonConfigPath);
        try {
            JsonElement json = JsonUtils.parseJsonFile(addonConfigFile);
            Type mapType = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, String> addonConfig = JsonUtils.toObject(json, mapType);
            return addonConfig;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("failed to parse provided addon-config file %s: %s", addonConfigPath, e.getMessage()),
                    e);
        }
    }
}
