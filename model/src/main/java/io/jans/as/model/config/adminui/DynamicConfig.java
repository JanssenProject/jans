package io.jans.as.model.config.adminui;

import java.util.List;

public class DynamicConfig {

    private List<String> roles;
    private List<String> permissions;
    private List<RolePermissionMapping> rolePermissionMapping;

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public List<RolePermissionMapping> getRolePermissionMapping() {
        return rolePermissionMapping;
    }

    public void setRolePermissionMapping(List<RolePermissionMapping> rolePermissionMapping) {
        this.rolePermissionMapping = rolePermissionMapping;
    }
}
