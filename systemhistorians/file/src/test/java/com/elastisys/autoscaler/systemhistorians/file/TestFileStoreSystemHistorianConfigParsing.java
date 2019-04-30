package com.elastisys.autoscaler.systemhistorians.file;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Test;

import com.elastisys.autoscaler.systemhistorians.file.FileStoreSystemHistorianConfig;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Verifies that parsing of {@link FileStoreSystemHistorianConfig}s from JSON
 * works as expected.
 */
public class TestFileStoreSystemHistorianConfigParsing {

    @Test
    public void parseValidConfig() throws Exception {
        FileStoreSystemHistorianConfig config = parseConfig("systemHistorian/file/valid.json");
        config.validate();
        assertThat(config.getLog().getPath(), is("target/historian.log"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseConfigWithoutLog() throws Exception {
        FileStoreSystemHistorianConfig config = parseConfig("systemHistorian/file/invalid-nolog.json");
        config.validate();
    }

    private FileStoreSystemHistorianConfig parseConfig(String resourceFile) throws IOException {
        JsonObject jsonConfig = JsonUtils.parseJsonResource(resourceFile).getAsJsonObject();
        FileStoreSystemHistorianConfig config = new Gson().fromJson(jsonConfig.get("systemHistorian"),
                FileStoreSystemHistorianConfig.class);
        return config;
    }

}
