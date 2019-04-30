package com.elastisys.autoscaler.testutils;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple {@link HttpServlet} that delegates all service response handling to a
 * {@link RequestHandler} class.
 * <p/>
 * <b>Note: intended for testing purposes</b>.
 *
 */
public class MockedServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final RequestHandler handler;

    public MockedServlet(RequestHandler handler) {
        this.handler = handler;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpResponse response = this.handler.handle(req);

        resp.setContentType("application/json");
        resp.setStatus(response.getStatusCode());
        PrintWriter responseWriter = resp.getWriter();
        if (response.getResponseBody() != null) {
            responseWriter.write(response.getResponseBody());
            responseWriter.flush();
        }
        responseWriter.close();
    }
}