package org.xdi.oxd.rs.protect;

import com.google.common.collect.Lists;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/12/2015
 */

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
}
