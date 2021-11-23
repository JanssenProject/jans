package io.jans.as.model.config.adminui;

import java.util.List;

public class RoleScopeMapping {
    private String role;
    private List<String> scopes;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    @Override
    public String toString() {
        return "RoleScopeMapping{" +
                "role='" + role + '\'' +
                ", scopes=" + scopes +
                '}';
    }
}
