package com.elastisys.autoscaler.metricstreamers.influxdb.stream;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonElement;

/**
 * A fake InfluxDB server query endpoint intended for testing. It can be
 * prepared with responses via {@link #prepareResponse(String)} and the
 * parameters of the latest calls can be retrieved via getters.
 *
 */
public class FakeInfluxdbQueryEndpoint extends HttpServlet {
    private final static Logger LOG = LoggerFactory.getLogger(FakeInfluxdbQueryEndpoint.class);

    /** The response that will be used for the next received call. */
    private JsonElement nextResponse;

    /** The URL parameters included in the last call */
    private Map<String, List<String>> lastCallParameters = null;
    /** The HTTP parameters included in the last call */
    private Map<String, String> lastCallHeaders = null;
    /** The URL that was used in the last call received. */
    private String lastCallUrl = null;

    public FakeInfluxdbQueryEndpoint(JsonElement nextResponse) {
        this.nextResponse = nextResponse;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestURI = req.getRequestURI();
        LOG.info("fake influxdb recevied GET: {}", requestURI);

        recordUrl(req);
        recordParameters(req);
        recordHeaders(req);

        resp.setContentType("application/json");
        // remove any trailing slash
        requestURI = requestURI.replaceAll("/$", "");
        // make sure request was made to right path
        if (requestURI.equals("/query")) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtils.toPrettyString(this.nextResponse));
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    public void prepareResponse(JsonElement nextResponse) {
        this.nextResponse = nextResponse;
    }

    private void recordHeaders(HttpServletRequest req) {
        this.lastCallHeaders = new HashMap<>();
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            this.lastCallHeaders.put(header, req.getHeader(header));
        }
        LOG.debug("call headers: {}", this.lastCallHeaders);
    }

    private void recordParameters(HttpServletRequest req) {
        this.lastCallParameters = new HashMap<>();
        req.getParameterMap().forEach((p, v) -> this.lastCallParameters.put(p, Arrays.asList(v)));
        LOG.debug("call parameters: {}", this.lastCallParameters);
    }

    private void recordUrl(HttpServletRequest req) {
        LOG.info("called with URL: {}", req.getRequestURL().toString());
        this.lastCallUrl = req.getRequestURL().toString();
    }

    /**
     * Returns the HTTP headers of the last call to the servlet or
     * <code>null</code> if no call has been received yet.
     *
     * @return
     */
    public Map<String, String> getLastCallHeaders() {
        return this.lastCallHeaders;
    }

    /**
     * Returns the URL parameters of the last call to the servlet or
     * <code>null</code> if no call has been received yet.
     *
     * @return
     */
    public Map<String, List<String>> getLastCallParameters() {
        return this.lastCallParameters;
    }

    /**
     * The URL that was used in the last call received or <code>null</code> if
     * no call has been received yet.
     *
     * @return
     */
    public String getLastCallUrl() {
        return this.lastCallUrl;
    }
}
