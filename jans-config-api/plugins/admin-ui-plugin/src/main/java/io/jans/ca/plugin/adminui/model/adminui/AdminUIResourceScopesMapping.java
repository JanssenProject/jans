package io.jans.ca.plugin.adminui.model.adminui;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import java.util.List;

@DataEntry(sortBy = {"resource"})
@ObjectClass(value = "adminUIResourceScopesMapping")
public class AdminUIResourceScopesMapping {
    @AttributeName(name = "resource")
    private String resource;
    @AttributeName(name = "accessType")
    private String accessType;
    @AttributeName(name = "scopes")
    private List<String> scopes;

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
}
