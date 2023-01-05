package io.jans.as.model.config.adminui;

import java.util.Objects;

public class AdminRole {
    private String role;
    private String description;
    private Boolean deletable;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getDeletable() {
        return deletable;
    }

    public void setDeletable(Boolean deletable) {
        this.deletable = deletable;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdminRole adminRole = (AdminRole) o;
        return role.equals(adminRole.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role);
    }

    @Override
    public String toString() {
        return "AdminRole{" +
                "role='" + role + '\'' +
                ", description='" + description + '\'' +
                ", deletable='" + deletable + '\'' +
                '}';
    }
}
