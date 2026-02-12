package io.jans.configapi.plugin.shibboleth.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShibbolethIdpConfigurationProperties implements Serializable {

    private static final long serialVersionUID = 1L;

    private String entityId;
    private String scope;
    private boolean enabled;
    private List<String> metadataProviders;
    private List<TrustedServiceProvider> trustedServiceProviders;
    private List<AttributeMapping> attributeMappings;

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getMetadataProviders() {
        return metadataProviders;
    }

    public void setMetadataProviders(List<String> metadataProviders) {
        this.metadataProviders = metadataProviders;
    }

    public List<TrustedServiceProvider> getTrustedServiceProviders() {
        return trustedServiceProviders;
    }

    public void setTrustedServiceProviders(List<TrustedServiceProvider> trustedServiceProviders) {
        this.trustedServiceProviders = trustedServiceProviders;
    }

    public List<AttributeMapping> getAttributeMappings() {
        return attributeMappings;
    }

    public void setAttributeMappings(List<AttributeMapping> attributeMappings) {
        this.attributeMappings = attributeMappings;
    }

    @Override
    public String toString() {
        return "ShibbolethIdpConfigurationProperties{" +
                "entityId='" + entityId + '\'' +
                ", scope='" + scope + '\'' +
                ", enabled=" + enabled +
                ", metadataProviders=" + metadataProviders +
                ", trustedServiceProviders=" + trustedServiceProviders +
                '}';
    }
}
