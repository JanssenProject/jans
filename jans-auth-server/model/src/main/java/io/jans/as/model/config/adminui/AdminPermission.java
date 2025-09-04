package io.jans.as.model.config.adminui;

import java.util.Objects;

public class AdminPermission {
    private String tag;
    private String permission;
    private String description;
    private Boolean defaultPermissionInToken;
    private Boolean essentialPermissionInAdminUI;

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

    public Boolean getEssentialPermissionInAdminUI() {
        return essentialPermissionInAdminUI;
    }

    public void setEssentialPermissionInAdminUI(Boolean essentialPermissionInAdminUI) {
        this.essentialPermissionInAdminUI = essentialPermissionInAdminUI;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdminPermission that = (AdminPermission) o;
        return tag.equals(that.tag) && permission.equals(that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag, permission);
    }

    @Override
    public String toString() {
        return "AdminPermission{" +
                "tag='" + tag + '\'' +
                ", permission='" + permission + '\'' +
                ", description='" + description + '\'' +
                ", essentialPermissionInAdminUI='" + essentialPermissionInAdminUI + '\'' +
                ", defaultPermissionInToken=" + defaultPermissionInToken +
                '}';
    }
}
