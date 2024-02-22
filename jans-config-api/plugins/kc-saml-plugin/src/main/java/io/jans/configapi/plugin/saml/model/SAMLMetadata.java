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
    
    @Override
    public String toString() {
        return "SPMetadata [nameIDPolicyFormat=" + nameIDPolicyFormat + ", entityId=" + entityId
                + ", singleLogoutServiceUrl=" + singleLogoutServiceUrl + "]";
    }
     
    
}
