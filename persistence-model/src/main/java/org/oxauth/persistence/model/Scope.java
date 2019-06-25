/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.oxauth.persistence.model;

import org.gluu.oxauth.model.common.ScopeType;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;

import java.io.Serializable;
import java.util.List;

/**
 * @author Javier Rojas Blum Date: 07.05.2012
 * @author Yuriy Movchan Date: 06/30/2015
 */
@DataEntry
@ObjectClass(values = {"top", "oxAuthCustomScope"})
public class Scope implements Serializable {

    private static final long serialVersionUID = 4308826784917052508L;

    @DN
    private String dn;
    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @AttributeName
    private String displayName;

    @AttributeName(name = "oxId")
    private String id;

    @AttributeName(name = "oxIconUrl")
    private String iconUrl;

    @AttributeName
    private String description;

    @AttributeName(name = "oxScopeType")
    private ScopeType scopeType;
 
    @AttributeName(name = "oxAuthClaim")
    private List<String> oxAuthClaims;

    @AttributeName(name = "defaultScope")
    private Boolean defaultScope;

    @AttributeName(name = "oxAuthGroupClaims")
    private Boolean oxAuthGroupClaims;

    @AttributeName(name = "oxScriptDn")
    private List<String> dynamicScopeScripts;

    @AttributeName(name = "oxUmaPolicyScriptDn")
    private List<String> umaAuthorizationPolicies;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getInum() {
        return this.inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ScopeType getScopeType() {
		return scopeType;
	}

	public void setScopeType(ScopeType scopeType) {
		this.scopeType = scopeType;
	}

	public List<String> getOxAuthClaims() {
        return this.oxAuthClaims;
    }

    public void setOxAuthClaims(List<String> oxAuthClaims) {
        this.oxAuthClaims = oxAuthClaims;
    }

    public Boolean isDefaultScope() {
        return this.defaultScope;
    }

    public void setDefaultScope(Boolean defaultScope) {
        this.defaultScope = defaultScope;
    }

    public Boolean isOxAuthGroupClaims() {
        return oxAuthGroupClaims;
    }

    public void setOxAuthGroupClaims(boolean oxAuthGroupClaims) {
        this.oxAuthGroupClaims = oxAuthGroupClaims;
    }

    public List<String> getDynamicScopeScripts() {
		return dynamicScopeScripts;
	}

	public void setDynamicScopeScripts(List<String> dynamicScopeScripts) {
        this.dynamicScopeScripts = dynamicScopeScripts;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public List<String> getUmaAuthorizationPolicies() {
        return umaAuthorizationPolicies;
    }

    public void setUmaAuthorizationPolicies(List<String> umaAuthorizationPolicies) {
        this.umaAuthorizationPolicies = umaAuthorizationPolicies;
    }

    public Boolean getOxAuthGroupClaims() {
        return oxAuthGroupClaims;
    }

    public Boolean getDefaultScope() {
        return defaultScope;
    }

    public boolean isUmaType() {
        return scopeType != null && ScopeType.UMA.getValue().equalsIgnoreCase(scopeType.getValue());
    }

    @Override
    public String toString() {
        return "Scope{" +
                "dn='" + dn + '\'' +
                ", inum='" + inum + '\'' +
                ", displayName='" + displayName + '\'' +
                ", id='" + id + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", description='" + description + '\'' +
                ", scopeType=" + scopeType +
                ", oxAuthClaims=" + oxAuthClaims +
                ", defaultScope=" + defaultScope +
                ", oxAuthGroupClaims=" + oxAuthGroupClaims +
                ", dynamicScopeScripts=" + dynamicScopeScripts +
                ", umaAuthorizationPolicies=" + umaAuthorizationPolicies +
                '}';
    }
}