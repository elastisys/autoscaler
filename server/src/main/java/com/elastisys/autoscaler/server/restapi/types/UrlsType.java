package com.elastisys.autoscaler.server.restapi.types;

import java.util.ArrayList;
import java.util.List;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * REST API response type that represents a list of URLs.
 */
public class UrlsType {

    /** The list of <code>URL</code>s. */
    private List<String> urls = new ArrayList<>();

    public UrlsType(List<String> urls) {
        this.urls = new ArrayList<>(urls);
    }

    public List<String> getUrls() {
        return this.urls;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
