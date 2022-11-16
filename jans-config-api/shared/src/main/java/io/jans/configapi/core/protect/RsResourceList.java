package io.jans.configapi.core.protect;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.List;

public class RsResourceList implements Serializable {

    @JsonProperty(value = "resources")
    private List<RsResource> resources = Lists.newArrayList();

    public RsResourceList() {
    }

    public RsResourceList(List<RsResource> resources) {
        this.resources = resources;
    }

    public List<RsResource> getResources() {
        return resources;
    }

    public void setResources(List<RsResource> resources) {
        this.resources = resources;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RsResourceList");
        sb.append("{resources=").append(resources);
        sb.append('}');
        return sb.toString();
    }
}
