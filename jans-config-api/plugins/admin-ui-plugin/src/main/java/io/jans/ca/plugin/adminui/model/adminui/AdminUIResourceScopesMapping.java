package io.jans.ca.plugin.adminui.model.adminui;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import java.util.List;

@DataEntry(sortBy = {"jansResource"})
@ObjectClass(value = "adminUIResourceScopesMapping")
public class AdminUIResourceScopesMapping {
    @DN
    private String dn;
    @AttributeName(
            ignoreDuringUpdate = true
    )
    private String inum;
    @AttributeName(name = "jansResource")
    private String resource;
    @AttributeName(name = "jansAccessType")
    private String accessType;
    @AttributeName(name = "jansScope")
    private List<String> scopes;

    public String getDn() {
        return dn;
    }
    public void setDn(String dn) {
        this.dn = dn;
    }
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

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }
}
