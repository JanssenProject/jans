package io.jans.as.model.config.adminui;

import java.util.List;

public class DynamicConfig {

    private List<AdminRole> roles;
    private List<AdminPermission> permissions;
    private List<RolePermissionMapping> rolePermissionMapping;
    private LicenseSpringCredentials licenseSpringCredentials;

    public List<AdminRole> getRoles() {
        return roles;
    }

    public void setRoles(List<AdminRole> roles) {
        this.roles = roles;
    }

    public List<AdminPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<AdminPermission> permissions) {
        this.permissions = permissions;
    }

    public List<RolePermissionMapping> getRolePermissionMapping() {
        return rolePermissionMapping;
    }

    public void setRolePermissionMapping(List<RolePermissionMapping> rolePermissionMapping) {
        this.rolePermissionMapping = rolePermissionMapping;
    }

    public LicenseSpringCredentials getLicenseSpringCredentials() {
        return licenseSpringCredentials;
    }

    public void setLicenseSpringCredentials(LicenseSpringCredentials licenseSpringCredentials) {
        this.licenseSpringCredentials = licenseSpringCredentials;
    }
}
