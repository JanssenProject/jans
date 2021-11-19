package io.jans.as.model.config.adminui;

import java.util.List;

public class DynamicConfig {

    private List<RoleScopeMapping> roleScopeMapping;

    public List<RoleScopeMapping> getRoleScopeMapping() {
        return roleScopeMapping;
    }

    public void setRoleScopeMapping(List<RoleScopeMapping> roleScopeMapping) {
        this.roleScopeMapping = roleScopeMapping;
    }
}
