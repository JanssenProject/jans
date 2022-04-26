package io.jans.as.model.config.adminui;

import java.util.Objects;

public class AdminPermission {
    private String permission;
    private String description;
    private Boolean defaultPermissionInToken;

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getDefaultPermissionInToken() {
        return defaultPermissionInToken;
    }

    public void setDefaultPermissionInToken(Boolean defaultPermissionInToken) {
        this.defaultPermissionInToken = defaultPermissionInToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdminPermission that = (AdminPermission) o;
        return permission.equals(that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permission);
    }

    @Override
    public String toString() {
        return "AdminPermission{" +
                "permission='" + permission + '\'' +
                ", description='" + description + '\'' +
                ", defaultPermissionInToken='" + defaultPermissionInToken + '\'' +
                '}';
    }
}
