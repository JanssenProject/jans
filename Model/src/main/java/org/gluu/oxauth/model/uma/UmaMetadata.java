/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.uma;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.gluu.oxauth.model.discovery.OAuth2Discovery;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;

/**
 * UMA2 metadata
 */
@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@XmlRootElement
@ApiModel(value = "UMA2 Metadata")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UmaMetadata extends OAuth2Discovery {

    @ApiModelProperty(required = false, value = "Static endpoint URI at which the authorization server declares that it interacts with end-user requesting parties to gather claims. If the authorization server also provides a claims interaction endpoint URI as part of its redirect_user hint in a need_info response to a client on authorization failure (see Section 3.3.6), that value overrides this metadata value. Providing the static endpoint URI is useful for enabling interactive claims gathering prior to any pushed-claims flows taking place, so that, for example, it is possible to gather requesting party authorization interactively for collecting all other claims in a \"silent\" fashion.")
    @JsonProperty(value = "claims_interaction_endpoint")
    @XmlElement(name = "claims_interaction_endpoint")
    private String claimsInteractionEndpoint;

    @ApiModelProperty(required = false, value = "UMA profiles supported by this authorization server. The value is an array of string values, where each string value is a URI identifying an UMA profile")
    @JsonProperty(value = "uma_profiles_supported")
    @XmlElement(name = "uma_profiles_supported")
    private String[] umaProfilesSupported;

    @ApiModelProperty(required = true, value = "The endpoint URI at which the resource server requests permissions on the client's behalf.")
    @JsonProperty(value = "permission_endpoint")
    @XmlElement(name = "permission_endpoint")
    private String permissionEndpoint;

    @ApiModelProperty(required = true, value = "The endpoint URI at which the resource server registers resources to put them under authorization manager protection.")
    @JsonProperty(value = "resource_registration_endpoint")
    @XmlElement(name = "resource_registration_endpoint")
    private String resourceRegistrationEndpoint;

    @ApiModelProperty(required = true, value = "The Scope endpoint URI.")
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
