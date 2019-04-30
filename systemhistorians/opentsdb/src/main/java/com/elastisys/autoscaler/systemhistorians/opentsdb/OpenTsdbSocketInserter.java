package com.elastisys.autoscaler.systemhistorians.opentsdb;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.scale.commons.util.concurrent.Sleep;
import com.elastisys.scale.commons.util.io.IoUtils;

/**
 * An {@link OpenTsdbInserter} that uses OpenTSDB's <a href=
 * "https://groups.google.com/forum/?fromgroups=#!topic/opentsdb/wB8WJMgd2eE"
 * >simplistic telnet-style protocol</a> to insert values.
 * <p/>
 * The operation of the {@link OpenTsdbSocketInserter} is equivalent to
 * inserting values over the telnet-style protocol via a command such as the
 * following:
 *
 * <pre>
 *   echo "put my.metric `date +%s` 1.0 tag1=value1" | nc 1.2.3.4 4242
 * </pre>
 *
 * In the current version of the protocol, the OpenTSDB server only replies if
 * an error occurs.
 */
public class OpenTsdbSocketInserter implements OpenTsdbInserter {

    /** {@link Logger} instance. */
    private final Logger logger;
    /** The hostname or IP address to connect to. */
    private final String host;
    /** The port number to connect to. */
    private final int port;

    /**
     * Creates a new instance that will stream values (using the telnet-style
     * protocol) to a OpenTSDB server listening on a given host and port.
     *
     * @param logger
     *            The {@link Logger} instance to make use of.
     * @param host
     *            The hostname or IP address to connect to.
     * @param port
     *            The port number to connect to.
     */
    public OpenTsdbSocketInserter(Logger logger, String host, int port) {
        requireNonNull(logger, "logger cannot be null");
        requireNonNull(host, "OpenTSDB host cannot be null");
        checkArgument(!host.isEmpty(), "OpenTSDB host cannot be empty");
        checkArgument(1 <= port && port <= 65353, "Port number not in allowed range [1,65353]");
        this.host = host;
        this.port = port;
        this.logger = logger;
    }

    @Override
    public void insert(MetricValue value) throws OpenTsdbException {
        requireNonNull(value, "value cannot be null");

        Socket socket = null;
        try {
            socket = connect();
            String telnetCommand = "put " + OpenTsdbDataPointRepresenter.representDataPoint(value) + "\n";
            write(socket, telnetCommand);
            Optional<String> response = awaitResponse(socket, 100);
            // receiving a response from OpenTSDB is a sign of an error
            // such as 'illegal argument: Need at least one tags'.
            if (response.isPresent()) {
                throw new OpenTsdbException(format("OpenTSDB responded with error: '%s'", response.get()));
            }
        } catch (Exception e) {
            throw new OpenTsdbException(
                    format("failed to post data to OpenTSDB server %s:%d: '%s'", this.host, this.port, e.getMessage()),
                    e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    new RuntimeException(e);
                }
            }
        }
    }

    private Socket connect() throws OpenTsdbException {
        try {
            return new Socket(this.host, this.port);
        } catch (IOException e) {
            throw new OpenTsdbException(
                    format("failed to connect to OpenTSDB %s:%d: %s", this.host, this.port, e.getMessage()), e);
        }
    }

    private void write(Socket socket, String data) throws IOException, OpenTsdbException {
        try {
            OutputStream output = socket.getOutputStream();
            output.write(data.getBytes());
            output.flush();
            socket.shutdownOutput();
            if (this.logger.isTraceEnabled()) {
                this.logger.trace("wrote to {}:{}: [{}]", this.host, this.port, data);
            }
        } catch (Exception e) {
            throw new OpenTsdbException(format("failed to write to %s:%d: %s", this.host, this.port, e.getMessage()),
                    e);
        }
    }

    /**
     * Waits for a given period of time before checking for a response on a
     * {@link Socket}.
     *
     * @param socket
     * @param millis
     * @return
     * @throws IOException
     * @throws OpenTsdbException
     */
    private Optional<String> awaitResponse(Socket socket, long millis) throws IOException, OpenTsdbException {
        Sleep.forTime(millis, TimeUnit.MILLISECONDS);
        try (Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)) {
            Optional<String> response = Optional.empty();
            InputStream input = socket.getInputStream();
            if (input.available() > 0) {
                response = Optional.of(IoUtils.toString(reader).trim());
                this.logger.debug("OpenTSDB error response: '{}'", response.get());
            }
            return response;
        } catch (Exception e) {
            throw new OpenTsdbException(format("failed to read from %s:%d: %s", this.host, this.port, e.getMessage()),
                    e);
        }
    }

}
