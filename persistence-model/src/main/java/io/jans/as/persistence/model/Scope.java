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
@ObjectClass(value = "jansScope")
public class Scope extends DeletableEntity implements Serializable {

    private static final long serialVersionUID = 4308826784917052508L;

    @DN
    private String dn;
    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @AttributeName
    private String displayName;

    @AttributeName(name = "jansId", consistency = true)
    private String id;

    @AttributeName(name = "jansIconUrl")
    private String iconUrl;

    @AttributeName
    private String description;

    @AttributeName(name = "jansScopeTyp")
    private ScopeType scopeType;

    @AttributeName(name = "jansClaim")
    private List<String> claims;

    @AttributeName(name = "jansDefScope")
    private Boolean defaultScope;

    @AttributeName(name = "jansGrpClaims")
    private Boolean groupClaims;

    @AttributeName(name = "jansScrDn")
    private List<String> dynamicScopeScripts;

    @AttributeName(name = "jansUmaPolicyScrDn")
    private List<String> umaAuthorizationPolicies;

    @AttributeName(name = "jansAttrs")
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

    public List<String> getClaims() {
        return this.claims;
    }

    public void setClaims(List<String> claims) {
        this.claims = claims;
    }

    public Boolean isDefaultScope() {
        return this.defaultScope;
    }

    public void setDefaultScope(Boolean defaultScope) {
        this.defaultScope = defaultScope;
    }

    public Boolean isGroupClaims() {
        return groupClaims;
    }

    public void setGroupClaims(boolean groupClaims) {
        this.groupClaims = groupClaims;
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
        return groupClaims;
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
                ", claims=" + claims +
                ", defaultScope=" + defaultScope +
                ", groupClaims=" + groupClaims +
                ", dynamicScopeScripts=" + dynamicScopeScripts +
                ", umaAuthorizationPolicies=" + umaAuthorizationPolicies +
                ", deletable=" + isDeletable() +
                ", expirationDate=" + getExpirationDate() +
                ", attributes=" + attributes +
                '}';
    }
}