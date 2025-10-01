package io.jans.as.model.config.adminui;

import java.util.List;

public class RolePermissionMapping {
    private String role;
    private List<String> permissions;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    @Override
    public String toString() {
        return "RolePermissionMapping{" +
                "role='" + role + '\'' +
                ", permissions=" + permissions +
                '}';
    }
}
