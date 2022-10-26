/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.uma;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jans.as.model.discovery.OAuth2Discovery;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;

/**
 * UMA2 metadata
 */
@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class UmaMetadata extends OAuth2Discovery {

    @JsonProperty(value = "claims_interaction_endpoint")
    @XmlElement(name = "claims_interaction_endpoint")
    private String claimsInteractionEndpoint;

    @JsonProperty(value = "uma_profiles_supported")
    @XmlElement(name = "uma_profiles_supported")
    private String[] umaProfilesSupported;

    @JsonProperty(value = "permission_endpoint")
    @XmlElement(name = "permission_endpoint")
    private String permissionEndpoint;

    @JsonProperty(value = "resource_registration_endpoint")
    @XmlElement(name = "resource_registration_endpoint")
    private String resourceRegistrationEndpoint;

    @JsonProperty(value = "scope_endpoint")
    @XmlElement(name = "scope_endpoint")
    private String scopeEndpoint;

    public String getClaimsInteractionEndpoint() {
        return claimsInteractionEndpoint;
    }

    public void setClaimsInteractionEndpoint(String claimsInteractionEndpoint) {
        this.claimsInteractionEndpoint = claimsInteractionEndpoint;
    }

    public String[] getUmaProfilesSupported() {
        return umaProfilesSupported;
    }

    public void setUmaProfilesSupported(String[] umaProfilesSupported) {
        this.umaProfilesSupported = umaProfilesSupported;
    }

    public String getPermissionEndpoint() {
        return permissionEndpoint;
    }

    public void setPermissionEndpoint(String permissionEndpoint) {
        this.permissionEndpoint = permissionEndpoint;
    }

    public String getResourceRegistrationEndpoint() {
        return resourceRegistrationEndpoint;
    }

    public void setResourceRegistrationEndpoint(String resourceRegistrationEndpoint) {
        this.resourceRegistrationEndpoint = resourceRegistrationEndpoint;
    }

    public String getScopeEndpoint() {
        return scopeEndpoint;
    }

    public void setScopeEndpoint(String scopeEndpoint) {
        this.scopeEndpoint = scopeEndpoint;
    }

    @Override
    public String toString() {
        return "UmaConfiguration{" +
                "claimsInteractionEndpoint='" + claimsInteractionEndpoint + '\'' +
                ", umaProfilesSupported=" + Arrays.toString(umaProfilesSupported) +
                ", permissionEndpoint='" + permissionEndpoint + '\'' +
                ", resourceRegistrationEndpoint='" + resourceRegistrationEndpoint + '\'' +
                ", scopeEndpoint='" + scopeEndpoint + '\'' +
                "} " + super.toString();
    }
}
