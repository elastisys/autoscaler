package com.elastisys.autoscaler.core.alerter.impl.standard.lab;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple HTTP server that echoes any incoming requests. Can be used together
 * with {@link StandardAlerterLab} as a HTTP endpoint.
 *
 * @see StandardAlerterLab
 */
public class HttpEchoServerMain {
    static Logger logger = LoggerFactory.getLogger(HttpEchoServerMain.class);

    public static void main(String[] args) throws Exception {
        Server server = new Server(8000);
        server.setHandler(new EchoHandler());
        server.start();
        server.join();
    }

    private static class EchoHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            logger.debug("received {} request: {}\n  Body: '{}'", request.getMethod(), request.getRequestURI(),
                    IO.toString(request.getInputStream(), StandardCharsets.UTF_8.displayName()));

            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            String responseMessage = "This is the response";
            response.getWriter().println(responseMessage);
        }
    }
}
