package com.elastisys.autoscaler.server;

import java.nio.charset.StandardCharsets;

import org.kohsuke.args4j.Option;

import com.elastisys.autoscaler.core.autoscaler.factory.AutoScalerFactoryConfig;
import com.elastisys.scale.commons.cli.server.BaseServerCliOptions;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.io.IoUtils;

/**
 * Captures (command-line) options accepted by the
 * {@link AutoScalerFactoryServer}.
 */
public class AutoScalerFactoryServerOptions extends BaseServerCliOptions {

    @Option(name = "--storage-dir", metaVar = "DIR", usage = "The directory "
            + "where the server will store its runtime state. Each "
            + "subdirectory of this directory is assumed to contain runtime "
            + "state for an autoscaler instance. Needs to be writable by the " + "user running the server.")
    public String storageDir = AutoScalerFactoryConfig.DEFAULT_STORAGE_DIR;

    @Option(name = "--addons-config", metaVar = "FILE", usage = "A JSON file "
            + "containing a map of add-on subsystems, which will be added to "
            + "each created autoscaler instance. Keys are names (such as "
            + "'accountingSubsystem') and values are class names.")
    public String addonsConfig = null;

    @Option(name = "--exit-handler", usage = "Publish an /exit handler that shuts down the server on 'GET /exit'. Default: False.")
    public boolean enableExitHandler = false;

    @Override
    public void validate() throws IllegalArgumentException {
        super.validate();
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this, true));
    }

    @Override
    public String getVersion() {
        return IoUtils.toString("VERSION.txt", StandardCharsets.UTF_8);
    }
}
