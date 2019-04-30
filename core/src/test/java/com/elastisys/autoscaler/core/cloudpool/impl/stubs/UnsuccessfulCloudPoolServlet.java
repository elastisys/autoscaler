package com.elastisys.autoscaler.core.cloudpool.impl.stubs;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A cloud pool endpoint for which all requests always fail (emulates a cloud
 * pool with problems, for example, connecting to its cloud provider).
 */
public class UnsuccessfulCloudPoolServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String error = "{\"message\": \"an error occurred\", \"detail\": \"stacktrace...\" }";
        resp.getWriter().write(error);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String error = "{\"message\": \"an error occurred\", \"detail\": \"stacktrace...\" }";
        resp.getWriter().write(error);
    }
}
