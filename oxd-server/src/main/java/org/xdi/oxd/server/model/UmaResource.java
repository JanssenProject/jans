package org.xdi.oxd.server.model;

import org.codehaus.jackson.annotate.JsonProperty;
import org.xdi.oxd.rs.protect.RsResource;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/06/2016
 */

public class UmaResource {

    @JsonProperty(value = "id")
    private String id;
    @JsonProperty(value = "resource")
    private RsResource resource;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RsResource getResource() {
        return resource;
    }

    public void setResource(RsResource resource) {
        this.resource = resource;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("UmaResource");
        sb.append("{id='").append(id).append('\'');
        sb.append(", resource=").append(resource);
        sb.append('}');
        return sb.toString();
    }
}
