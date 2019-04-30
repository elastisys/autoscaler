package com.elastisys.autoscaler.systemhistorians.opentsdb;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.commons.util.concurrent.Waiter;
import com.elastisys.scale.commons.util.io.IoUtils;

/**
 * A simple TCP server that responds to all connection attempts with a given
 * message (can be emtpy).
 * <p/>
 * The response message can be (re)set during runtime via the
 * {@link #setResponse(Optional)} method.
 * <p/>
 * This class is not production quality and is only intended to be used as a
 * tool in unit tests.
 */
public class FixedReplySocketServer implements Runnable, Closeable {

    private static Logger logger = LoggerFactory.getLogger(FixedReplySocketServer.class);

    /**
     * The response to send to connecting clients. An {@link Optional#absent()}
     * value means that no reply is sent.
     */
    private Optional<String> response;
    /**
     * The port on which to listen for incoming connections. Use {@code 0} to
     * use any available port.
     */
    private final int port;
    /** The server listening socket. */
    private ServerSocket serverSocket;
    // private final AtomicBoolean stopped;
    /** A {@link Waiter} object that waits for the server to be started. */
    private final Waiter<Boolean> upAndRunning;

    /**
     * Constructs a new {@link FixedReplySocketServer}. The server needs to be
     * {@link #run()} before accepting incoming connections.
     *
     * @param response
     *            The response to send to connecting clients. An
     *            {@link Optional#absent()} value means that no reply is sent.
     * @param listenPort
     *            The port on which to listen for incoming connections. Use
     *            {@code 0} to use any available port.
     * @throws IOException
     */
    public FixedReplySocketServer(Optional<String> response, int listenPort) throws IOException {
        this.response = response;
        this.port = listenPort;
        // this.stopped = new AtomicBoolean(true);
        this.upAndRunning = new Waiter<Boolean>();
        this.serverSocket = new ServerSocket(this.port);
    }

    /**
     * Starts the server (blocking call, returns when server is {@link #close()}
     * d).
     *
     * @throws IOException
     */
    @Override
    public void run() {
        // this.stopped.set(true);
        this.upAndRunning.set(true);
        logger.debug("starting to listen on port {}", getListenPort());
        // will run until the server socket is closed
        while (true) {
            try {
                Socket socket = this.serverSocket.accept();
                logger.debug("Servicing request from " + socket.getRemoteSocketAddress());
                InputStream input = socket.getInputStream();

                String incomingMessage = IoUtils.toString(new InputStreamReader(input));
                logger.debug("Server received message: '{}'", incomingMessage);
                OutputStream output = socket.getOutputStream();
                if (this.response.isPresent()) {
                    output.write(this.response.get().getBytes());
                    output.flush();
                }
                input.close();
                output.close();
                socket.close();
            } catch (Exception e) {
                if (e.getMessage().equalsIgnoreCase("Socket closed")) {
                    // server socket was closed due to server shut down
                    return;
                }
                logger.error("Failed to service request: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        logger.info("stopping server");
        if (this.serverSocket != null && !this.serverSocket.isClosed()) {
            this.serverSocket.close();
            this.serverSocket = null;
        }
        // this.stopped.set(true);
    }

    public int getListenPort() {
        Objects.requireNonNull(this.serverSocket, "server socket has not been setup");
        return this.serverSocket.getLocalPort();
    }

    /**
     * Sets the response to send to connecting clients.
     *
     * @param response
     *            The response to send to connecting clients. An
     *            {@link Optional#absent()} value means that no reply is sent.
     */
    public void setResponse(Optional<String> response) {
        this.response = response;
    }

    /**
     * Waits for the server to be started.
     *
     * @throws InterruptedException
     */
    public void awaitStartup() throws InterruptedException {
        this.upAndRunning.await();
    }
}
