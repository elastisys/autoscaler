package com.elastisys.autoscaler.systemhistorians.influxdb.inserter.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

import com.elastisys.scale.commons.util.io.IoUtils;

/**
 * A fake InfluxDB server {@code /query} and {@code /write} endpoint intended
 * for testing. It can be prepared with response status code via
 * {@link #prepareResponse(int)} and the parameters of the latest call can be
 * retrieved via getters.
 */
public class FakeInfluxdbEndpoint extends HttpServlet {
    private final static Logger LOG = LoggerFactory.getLogger(FakeInfluxdbEndpoint.class);

    /**
     * The response status code that will be used for the next received call.
     */
    private int nextStatusCode = 200;

    /** Stores the URL parameters included in every received call (in order). */
    private List<Map<String, List<String>>> callParametersHistory = new ArrayList<>();
    /**
     * Stores the HTTP parameters included in every received call (in order).
     */
    private List<Map<String, String>> callHeadersHistory = new ArrayList<>();

    /** Stores the body contents of every received call (in order). */
    private List<String> callBodyHistory = new ArrayList<>();

    /** The URL that was used in the last call received. */
    private List<String> callUrlHistory = new ArrayList<>();

    /**
     * Creates a {@link FakeInfluxdbEndpoint} prepared with a given response
     * status code.
     *
     * @param nextStatusCode
     */
    public FakeInfluxdbEndpoint(int nextStatusCode) {
        this.nextStatusCode = nextStatusCode;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp);
    }

    private void handle(HttpServletRequest req, HttpServletResponse resp) {
        String requestURI = req.getRequestURI();
        LOG.info("fake influxdb recevied {}: {}", req.getMethod(), requestURI);

        recordUrl(req);
        recordParameters(req);
        recordHeaders(req);
        recordBody(req);

        resp.setContentType("application/json");
        // remove any trailing slash
        requestURI = requestURI.replaceAll("/$", "");
        // make sure request was made to right path
        if (requestURI.equals("/query") || requestURI.equals("/write")) {
            resp.setStatus(this.nextStatusCode);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    public void prepareResponse(int nextStatusCode) {
        this.nextStatusCode = nextStatusCode;
    }

    private void recordHeaders(HttpServletRequest req) {
        Map<String, String> callHeaders = new HashMap<>();
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            callHeaders.put(header, req.getHeader(header));
        }
        LOG.debug("call headers: {}", callHeaders);
        this.callHeadersHistory.add(callHeaders);
    }

    private void recordParameters(HttpServletRequest req) {
        Map<String, List<String>> callParameters = new HashMap<>();
        req.getParameterMap().forEach((p, v) -> callParameters.put(p, Arrays.asList(v)));
        LOG.debug("call parameters: {}", callParameters);
        this.callParametersHistory.add(callParameters);
    }

    private void recordBody(HttpServletRequest req) {
        try {
            String body = IoUtils.toString(req.getInputStream(), StandardCharsets.UTF_8);
            LOG.debug("call body:\n{}", body);
            this.callBodyHistory.add(body);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void recordUrl(HttpServletRequest req) {
        LOG.info("called with URL: {}", req.getRequestURL().toString());
        this.callUrlHistory.add(req.getRequestURL().toString());
    }

    /**
     * Returns the HTTP headers of all received calls.
     *
     * @return
     */
    public List<Map<String, String>> getCallHeadersHistory() {
        return this.callHeadersHistory;
    }

    /**
     * Returns the URL parameters of all received calls.
     *
     * @return
     */
    public List<Map<String, List<String>>> getCallParametersHistory() {
        return this.callParametersHistory;
    }

    /**
     * Returns the URLs that were used in all received calls.
     *
     * @return
     */
    public List<String> getCallUrlHistory() {
        return this.callUrlHistory;
    }

    /**
     * Returns the body contents that were used in all received calls.
     *
     * @return
     *
     */
    public List<String> getCallBodyHistory() {
        return this.callBodyHistory;
    }
}
