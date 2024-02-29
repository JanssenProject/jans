/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SAMLMetadata implements Serializable {

    private static final long serialVersionUID = 1L;
    private String nameIDPolicyFormat;
    private String entityId;
    private String singleLogoutServiceUrl;
    private String jansAssertionConsumerServiceGetURL;
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
