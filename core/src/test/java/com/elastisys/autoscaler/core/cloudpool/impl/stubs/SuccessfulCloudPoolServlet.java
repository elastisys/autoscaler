package com.elastisys.autoscaler.core.cloudpool.impl.stubs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.types.MachinePool;
import com.elastisys.scale.cloudpool.api.types.PoolSizeSummary;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.JsonElement;

/**
 * A cloud pool endpoint for which all requests always succeed.
 */
public class SuccessfulCloudPoolServlet extends HttpServlet {
    static Logger logger = LoggerFactory.getLogger(SuccessfulCloudPoolServlet.class);

    /** The machine pool to return whenever a GET request is received. */
    private MachinePool machinePool;

    /** The set desired size of the cloud pool. */
    private int desiredSize;

    /**
     * Creates a {@link SuccessfulCloudPoolServlet}.
     *
     * @param machinePool
     *            The machine pool to return whenever a GET request is received.
     */
    public SuccessfulCloudPoolServlet(MachinePool machinePool, int desiredSize) {
        this.machinePool = machinePool;
        this.desiredSize = desiredSize;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestURI = req.getRequestURI();
        logger.info("GET recevied: {}", requestURI);
        String json = JsonUtils.toPrettyString(JsonUtils.toJson(this.machinePool));

        resp.setContentType("application/json");
        // remove any trailing slash
        requestURI = requestURI.replaceAll("/$", "");
        // make sure request was made to right path
        if (requestURI.equals("/pool")) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(json);
        } else if (requestURI.equals("/pool/size")) {
            resp.setStatus(HttpServletResponse.SC_OK);
            PoolSizeSummary poolSizeSummary = new PoolSizeSummary(this.desiredSize,
                    this.machinePool.getAllocatedMachines().size(), this.machinePool.getActiveMachines().size());
            resp.getWriter().write(JsonUtils.toString(JsonUtils.toJson(poolSizeSummary)));
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.info("POST recevied: {}", req.getRequestURI());
        JsonElement json = JsonUtils
                .parseJsonString(IO.toString(req.getInputStream(), StandardCharsets.UTF_8.displayName()));
        logger.info("request message: " + json);

        resp.setContentType("application/json");
        // make sure request was made to right path
        if (req.getRequestURI().equals("/pool/size")) {
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
