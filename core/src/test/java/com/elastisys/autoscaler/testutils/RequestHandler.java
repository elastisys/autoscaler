package com.elastisys.autoscaler.testutils;

import javax.servlet.http.HttpServletRequest;

/**
 * {@link RequestHandler} to be used with a {@link MockedServlet}.
 * <p/>
 * <b>Note: intended for testing purposes</b>.
 *
 * @see MockedServlet
 */
public interface RequestHandler {
    HttpResponse handle(HttpServletRequest req);
}
