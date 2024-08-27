/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SAMLMetadata implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Schema(description = " URI reference corresponding to a name identifier format.")
    private String nameIDPolicyFormat;
    
    @Schema(description = "Entity ID that will be used to uniquely identify this SAML Service Provider.")
    private String entityId;
    
    @Schema(description = "Url used to send logout requests.")
    private String singleLogoutServiceUrl;
    
    @Schema(description = "GET URL the Identity provider (IdP) will send the SAML Response containing the assertions.")
    private String jansAssertionConsumerServiceGetURL;
    
    @Schema(description = "POST URL the Identity provider (IdP) will send the SAML Response containing the assertions.")
    private String jansAssertionConsumerServicePostURL;
    
    public String getNameIDPolicyFormat() {
        return nameIDPolicyFormat;
    }
    
    public void setNameIDPolicyFormat(String nameIDPolicyFormat) {
        this.nameIDPolicyFormat = nameIDPolicyFormat;
    }
    
    public String getEntityId() {
        return entityId;
    }
    
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
    
    public String getSingleLogoutServiceUrl() {
        return singleLogoutServiceUrl;
    }
    
    public void setSingleLogoutServiceUrl(String singleLogoutServiceUrl) {
        this.singleLogoutServiceUrl = singleLogoutServiceUrl;
    }
    
    public String getJansAssertionConsumerServiceGetURL() {
        return jansAssertionConsumerServiceGetURL;
    }
    
    public void setJansAssertionConsumerServiceGetURL(String jansAssertionConsumerServiceGetURL) {
        this.jansAssertionConsumerServiceGetURL = jansAssertionConsumerServiceGetURL;
    }
    
    public String getJansAssertionConsumerServicePostURL() {
        return jansAssertionConsumerServicePostURL;
    }
    
    public void setJansAssertionConsumerServicePostURL(String jansAssertionConsumerServicePostURL) {
        this.jansAssertionConsumerServicePostURL = jansAssertionConsumerServicePostURL;
    }
    
    @Override
    public String toString() {
        return "SAMLMetadata [nameIDPolicyFormat=" + nameIDPolicyFormat + ", entityId=" + entityId
                + ", singleLogoutServiceUrl=" + singleLogoutServiceUrl + ", jansAssertionConsumerServiceGetURL="
                + jansAssertionConsumerServiceGetURL + ", jansAssertionConsumerServicePostURL="
                + jansAssertionConsumerServicePostURL + "]";
    }     
    
}
