/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.gluu;

import com.wordnik.swagger.annotations.ApiModel;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by eugeniuparvan on 8/5/16.
 */
@IgnoreMediaTypes("application/*+json")
@JsonPropertyOrder({
        "clientinfo_endpoint",
        "check_session_iframe",
        "end_session_endpoint",
        "validate_token_endpoint",
        "federation_metadata_endpoint",
        "federation_endpoint",
        "id_generation_endpoint",
        "introspection_endpoint",
        "auth_level_mapping",
        "scope_to_claims_mapping",
        "http_logout_supported",
        "logout_session_supported"
})
@ApiModel(value = "Gluu Configuration")
public class GluuConfiguration {

    @JsonProperty(value = "clientinfo_endpoint")
    private String clientinfoEndpoint;

    @JsonProperty(value = "check_session_iframe")
    private String checkSessionIframe;

    @JsonProperty(value = "end_session_endpoint")
    private String endSessionEndpoint;

    @JsonProperty(value = "validate_token_endpoint")
    private String validateTokenEndpoint;

    @JsonProperty(value = "federation_metadata_endpoint")
    private String federationMetadataEndpoint;

    @JsonProperty(value = "federation_endpoint")
    private String federationEndpoint;

    @JsonProperty(value = "id_generation_endpoint")
    private String idGenerationEndpoint;

    @JsonProperty(value = "introspection_endpoint")
    private String introspectionEndpoint;

    @JsonProperty(value = "auth_level_mapping")
    private Map<Integer, Set<String>> authLevelMapping;

    @JsonProperty(value = "scope_to_claims_mapping")
    private List<Map<String, Set<String>>> scopeToClaimsMapping;

    @JsonProperty(value = "http_logout_supported")
    private String httpLogoutSupported;

    @JsonProperty(value = "logout_session_supported")
    private String logoutSessionSupported;

    public String getClientinfoEndpoint() {
        return clientinfoEndpoint;
    }

    public void setClientinfoEndpoint(String clientinfoEndpoint) {
        this.clientinfoEndpoint = clientinfoEndpoint;
    }

    public String getCheckSessionIframe() {
        return checkSessionIframe;
    }

    public void setCheckSessionIframe(String checkSessionIframe) {
        this.checkSessionIframe = checkSessionIframe;
    }

    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    public void setEndSessionEndpoint(String endSessionEndpoint) {
        this.endSessionEndpoint = endSessionEndpoint;
    }

    public String getValidateTokenEndpoint() {
        return validateTokenEndpoint;
    }

    public void setValidateTokenEndpoint(String validateTokenEndpoint) {
        this.validateTokenEndpoint = validateTokenEndpoint;
    }

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

    public List<Map<String, Set<String>>> getScopeToClaimsMapping() {
        return scopeToClaimsMapping;
    }

    public void setScopeToClaimsMapping(List<Map<String, Set<String>>> scopeToClaimsMapping) {
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
        return "GluuConfiguration{" +
                "clientinfoEndpoint='" + clientinfoEndpoint + '\'' +
                ", checkSessionIframe='" + checkSessionIframe + '\'' +
                ", endSessionEndpoint='" + endSessionEndpoint + '\'' +
                ", validateTokenEndpoint='" + validateTokenEndpoint + '\'' +
                ", federationMetadataEndpoint='" + federationMetadataEndpoint + '\'' +
                ", federationEndpoint='" + federationEndpoint + '\'' +
                ", idGenerationEndpoint='" + idGenerationEndpoint + '\'' +
                ", introspectionEndpoint='" + introspectionEndpoint + '\'' +
                ", authLevelMapping='" + authLevelMapping + '\'' +
                ", scopeToClaimsMapping='" + scopeToClaimsMapping + '\'' +
                ", httpLogoutSupported='" + httpLogoutSupported + '\'' +
                ", logoutSessionSupported='" + logoutSessionSupported + '\'' +
                '}';
    }
}
