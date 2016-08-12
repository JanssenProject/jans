package org.xdi.oxauth.client;

import java.util.Map;
import java.util.Set;

/**
 * Created by eugeniuparvan on 8/12/16.
 */
public class GluuConfigurationResponse extends BaseResponse {

    private String federationMetadataEndpoint;

    private String federationEndpoint;

    private String idGenerationEndpoint;

    private String introspectionEndpoint;

    private Map<Integer, Set<String>> authLevelMapping;

    private Map<String, Set<String>> scopeToClaimsMapping;

    private String httpLogoutSupported;

    private String logoutSessionSupported;

    public String getFederationMetadataEndpoint() {
        return federationMetadataEndpoint;
    }

    public void setFederationMetadataEndpoint(String federationMetadataEndpoint) {
        this.federationMetadataEndpoint = federationMetadataEndpoint;
    }

    public String getFederationEndpoint() {
        return federationEndpoint;
    }

    public void setFederationEndpoint(String federationEndpoint) {
        this.federationEndpoint = federationEndpoint;
    }

    public String getIdGenerationEndpoint() {
        return idGenerationEndpoint;
    }

    public void setIdGenerationEndpoint(String idGenerationEndpoint) {
        this.idGenerationEndpoint = idGenerationEndpoint;
    }

    public String getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    public void setIntrospectionEndpoint(String introspectionEndpoint) {
        this.introspectionEndpoint = introspectionEndpoint;
    }

    public Map<Integer, Set<String>> getAuthLevelMapping() {
        return authLevelMapping;
    }

    public void setAuthLevelMapping(Map<Integer, Set<String>> authLevelMapping) {
        this.authLevelMapping = authLevelMapping;
    }

    public Map<String, Set<String>> getScopeToClaimsMapping() {
        return scopeToClaimsMapping;
    }

    public void setScopeToClaimsMapping(Map<String, Set<String>> scopeToClaimsMapping) {
        this.scopeToClaimsMapping = scopeToClaimsMapping;
    }

    public String getHttpLogoutSupported() {
        return httpLogoutSupported;
    }

    public void setHttpLogoutSupported(String httpLogoutSupported) {
        this.httpLogoutSupported = httpLogoutSupported;
    }

    public String getLogoutSessionSupported() {
        return logoutSessionSupported;
    }

    public void setLogoutSessionSupported(String logoutSessionSupported) {
        this.logoutSessionSupported = logoutSessionSupported;
    }

    @Override
    public String toString() {
        return "GluuConfigurationResponse{" +
                "federationMetadataEndpoint='" + federationMetadataEndpoint + '\'' +
                ", federationEndpoint='" + federationEndpoint + '\'' +
                ", idGenerationEndpoint='" + idGenerationEndpoint + '\'' +
                ", introspectionEndpoint='" + introspectionEndpoint + '\'' +
                ", authLevelMapping=" + authLevelMapping +
                ", scopeToClaimsMapping=" + scopeToClaimsMapping +
                ", httpLogoutSupported='" + httpLogoutSupported + '\'' +
                ", logoutSessionSupported='" + logoutSessionSupported + '\'' +
                '}';
    }
}
