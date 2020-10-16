/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.persistence.model;

import io.jans.as.model.common.ScopeType;
import io.jans.orm.annotation.*;
import io.jans.orm.model.base.DeletableEntity;

import java.io.Serializable;
import java.util.List;

/**
 * @author Javier Rojas Blum Date: 07.05.2012
 * @author Yuriy Movchan Date: 06/30/2015
 */
@DataEntry
@ObjectClass(value = "oxAuthCustomScope")
public class Scope extends DeletableEntity implements Serializable {

    private static final long serialVersionUID = 4308826784917052508L;

    @DN
    private String dn;
    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @AttributeName
    private String displayName;

    @AttributeName(name = "jsId", consistency = true)
    private String id;

    @AttributeName(name = "jsIconUrl")
    private String iconUrl;

    @AttributeName
    private String description;

    @AttributeName(name = "jsScopeTyp")
    private ScopeType scopeType;

    @AttributeName(name = "jsClaim")
    private List<String> jsClaims;

    @AttributeName(name = "jsDefScope")
    private Boolean jsDefScope;

    @AttributeName(name = "jsGrpClaims")
    private Boolean jsGrpClaims;

    @AttributeName(name = "jsScrDn")
    private List<String> dynamicScopeScripts;

    @AttributeName(name = "jsUmaPolicyScrDn")
    private List<String> umaAuthorizationPolicies;

    @AttributeName(name = "jsAttrs")
    @JsonObject
    private ScopeAttributes attributes;

    public ScopeAttributes getAttributes() {
        if (attributes == null) {
            attributes = new ScopeAttributes();
        }
        return attributes;
    }

    public void setAttributes(ScopeAttributes attributes) {
        this.attributes = attributes;
    }

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
        return this.jsClaims;
    }

    public void setOxAuthClaims(List<String> jsClaims) {
        this.jsClaims = jsClaims;
    }

    public Boolean isDefaultScope() {
        return this.jsDefScope;
    }

    public void setDefaultScope(Boolean jsDefScope) {
        this.jsDefScope = jsDefScope;
    }

    public Boolean isOxAuthGroupClaims() {
        return jsGrpClaims;
    }

    public void setOxAuthGroupClaims(boolean jsGrpClaims) {
        this.jsGrpClaims = jsGrpClaims;
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
        return jsGrpClaims;
    }

    public Boolean getDefaultScope() {
        return jsDefScope;
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
                ", jsClaims=" + jsClaims +
                ", jsDefScope=" + jsDefScope +
                ", jsGrpClaims=" + jsGrpClaims +
                ", dynamicScopeScripts=" + dynamicScopeScripts +
                ", umaAuthorizationPolicies=" + umaAuthorizationPolicies +
                ", deletable=" + isDeletable() +
                ", expirationDate=" + getExpirationDate() +
                ", attributes=" + attributes +
                '}';
    }
}