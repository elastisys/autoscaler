# Standard autoscaler server distribution

This is a standard autoscaler server distribution, which supports all autoscaler
subsystems and plug-ins (metric streamers, predictors, etc).

This build module produces an all-in-one server executable jar file. When
executed, an embedded web server is started that publishes the REST API
endpoint.

A docker image for the server can also be built. More on that below.


## Usage
This module produces an executable jar file for the server.
The simplest way of starting the server is to run


    java -jar autoscaler.server.standard-<version>.jar --http-port 8080

which will start a server listening on HTTP port `8080`. 

*Note: for production settings, it is recommended to run the server with an HTTPS port.*

For a complete list of options, including the available security options,
run the server with the ``--help`` flag:

    java -jar autoscaler.server.standard-<version>.jar --help


## Running the autoscaler server in a Docker container
The autoscaler server can be executed inside a Docker container. First, however,
a docker image needs to be built that includes the autoscaler server. The steps
for building the image and running a container from the image are outlined
below.


### Building the docker image
The module's build file contains a build goal that can be used to produce a
Docker image, once the project binary has been built in the `target` directory
(for example, via `mvn package`). Whenever `mvn install` is executed the Docker
image gets built locally on the machine. When `mvn deploy` is run, such as
during a release, the image also gets pushed to our private docker registry.

*Note: make sure that you have issued `docker login` against the docker registry
before trying to push an image.*


### Running a container from the image
Once the docker image is built for the server, it can be run by either 
specfying a HTTP port or an HTTPS port. For example, running with an HTTP port:

    docker run -d -p 8080:80 -e HTTP_PORT=80 elastisys/autoscaler-standard:<version>

This will start publish the container's HTTP port on host port `8080`.

*Note: for production settings, it is recommended to run the server with an HTTPS port.*

The following environment variables can be passed to the Docker container (`-e`)
to control its behavior. At least one of `${HTTP_PORT}` and `${HTTPS_PORT}` 
_must_ be specified.

HTTP/HTTPS configuration:

  - `HTTP_PORT`: Enables a HTTP port on the server.  

  - `HTTPS_PORT`: Enables a HTTPS port on the server.  
    *Note: when specified, a `${SSL_KEYSTORE}` must be specified to identify to clients.*
    
  - `SSL_KEYSTORE`: The location of the server's SSL key store (PKCS12 format).  
     You typically combine this with mounting a volume that holds the key store.  
     *Note: when specified, an `${SSL_KEYSTORE_PASSWORD}` must is required.*
     
  - `SSL_KEYSTORE_PASSWORD`: The password that protects the key store.  

Runtime configuration:

  - `STORAGE_DIR`: destination folder for autoscaler runtime state.  
    *Note: to persist across container recreation, this directory should be 
    mapped via a volume to a directory on the host.*  
    Default: `/var/lib/elastisys/autoscaler/instances`.
    
  - `ADDONS_CONFIG`: a JSON file holding a collection of add-on subsystems that
    will be added to all created autoscaler instances. These add-on subsystems 
    are not strictly necessary for the autoscaler to operate, but may extend 
    it with additional functionality. The file should hold a map where keys 
    are names, such as `accountingSubsystem`, and values are class names, such 
    as `com.elastisys.AccountingSubsystemImpl`. These implementation classes
    must be on the classpath of the server.  
    
    Default: `/etc/elastisys/autoscaler/addons/addons.json`. The file
    provided in the image holds an empty map of add-on subsystem classes.
    Map a host directory volume to the `addons` directory to replace it with
    addons of your liking.

Client authentication:

  - `LOG_CONFIG`: [logback](http://logback.qos.ch/manual/configuration.html)
    logging configuration file (`logback.xml`).  
    Default: `/etc/elastisys/autoscaler/logback.xml`.
  - `JUL_CONFIG`: `java.util.logging` `logging.properties` configuration.  
    Default: `/etc/elastisys/autoscaler/logging.properties`.
  - `LOG_DIR`: destination folder for log files (when using default
    `${LOG_CONFIG}` setup).  
    Default: `/var/log/elastisys/autoscaler`.
  - `STDOUT_LOG_LEVEL`: output level for logging to stdout (note: log output 
    that is written to file includes `DEBUG` level).  
    Default: `INFO`.

Security-related:

  - `REQUIRE_BASIC_AUTH`: If `true`, require clients to provide username/password
    credentials according to the HTTP BASIC authentication scheme.  
    *Note: when specified, `${BASIC_AUTH_REALM_FILE}` and `${BASIC_AUTH_ROLE}` must be specified to identify trusted clients.*  
    Default: `false`.
  - `BASIC_AUTH_ROLE`: The role that an authenticated user must be assigned to be granted access to the server.  
  - `BASIC_AUTH_REALM_FILE`: A credentials store with users, passwords, and
    roles according to the format prescribed by the [Jetty HashLoginService](http://www.eclipse.org/jetty/documentation/9.2.6.v20141205/configuring-security-authentication.html#configuring-login-service).  
  - `REQUIRE_CERT_AUTH`: Require SSL clients to authenticate with a certificate,
    which must be included in the server's trust store.  
    *Note: when specified, `${CERT_AUTH_TRUSTSTORE}` and `${CERT_AUTH_TRUSTSTORE_PASSWORD}` must be specified to identify trusted clients.*     
  - `CERT_AUTH_TRUSTSTORE`. The location of a SSL trust store (JKS format), containing trusted client certificates.
  - `CERT_AUTH_TRUSTSTORE_PASSWORD`: The password that protects the SSL trust store.

JVM-related:

  - `JVM_OPTS`: JVM settings such as heap size. Default: `-Xmx128m`.



### Debugging a running container
The simplest way to debug a running container is to get a shell session via
  
    docker exec -it <container-id/name> /bin/bash

and check out the log files under `/var/log/elastisys`. Configurations are
located under `/etc/elastisys` and binaries under `/opt/elastisys`.
