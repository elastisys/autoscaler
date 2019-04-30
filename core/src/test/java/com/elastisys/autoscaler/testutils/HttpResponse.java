package com.elastisys.autoscaler.testutils;

/**
 * Simple HTTP response produced by {@link RequestHandler}.
 * <p/>
 * <b>Note: intended for testing purposes</b>.
 *
 * @see MockedServlet
 */
public class HttpResponse {
    /** The status code of the HTTP response. */
    private final int statusCode;
    /** The message body of the HTTP response. */
    private final String responseBody;

    public HttpResponse(int statusCode, String responseBody) {
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public String getResponseBody() {
        return this.responseBody;
    }

    public int getStatusCode() {
        return this.statusCode;
    }
}
