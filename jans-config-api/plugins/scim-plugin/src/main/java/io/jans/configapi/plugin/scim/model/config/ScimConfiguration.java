package io.jans.configapi.plugin.scim.model.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ScimConfiguration {

    @Inject
    Config config;

    @Inject
    @ConfigProperty(name = "scim.relative.path")
    private String scimRelativePath;

    public String getScimRelativePath() {
        return scimRelativePath;
    }

    public void setScimRelativePath(String scimRelativePath) {
        this.scimRelativePath = scimRelativePath;
    }

}
