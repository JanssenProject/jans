package io.jans.ca.plugin.adminui.model.auth;

import java.util.List;

public class ApiTokenRequest {
    private List<String> permissionTag;
    private String ujwt;

    public List<String> getPermissionTag() {
        return permissionTag;
    }

    public void setPermissionTag(List<String> permissionTag) {
        this.permissionTag = permissionTag;
    }

    public String getUjwt() {
        return ujwt;
    }

    public void setUjwt(String ujwt) {
        this.ujwt = ujwt;
    }
}
