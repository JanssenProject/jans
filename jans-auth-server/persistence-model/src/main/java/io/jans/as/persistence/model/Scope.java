/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.persistence.model;

import io.jans.as.model.common.CreatorType;
import io.jans.as.model.common.ScopeType;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.DeletableEntity;

import java.io.Serializable;
import java.util.*;

/**
 * @author Javier Rojas Blum Date: 07.05.2012
 * @author Yuriy Movchan Date: 06/30/2015
 */
@DataEntry
@ObjectClass(value = "jansScope")
public class Scope extends DeletableEntity implements Serializable {

    private static final long serialVersionUID = 4308826784917052508L;

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

    @AttributeName(name = "creatorId")
    private String creatorId;

    @AttributeName(name = "creatorTyp")
    private CreatorType creatorType;

    @AttributeName(name = "creationDate")
    private Date creationDate = new Date();

    @JsonObject // store creator attributes for case when object is deleted (e.g. user creates scope but then removed, we need display name to show on UI)
    @AttributeName(name = "creatorAttrs")
    private Map<String, String> creatorAttributes;

    public Map<String, String> getCreatorAttributes() {
        if (creatorAttributes == null) {
            creatorAttributes = new HashMap<>();
        }
        return creatorAttributes;
    }

    public void setCreatorAttributes(Map<String, String> creatorAttributes) {
        this.creatorAttributes = creatorAttributes;
    }

    public ScopeAttributes getAttributes() {
        if (attributes == null) {
            attributes = new ScopeAttributes();
        }
        return attributes;
    }

    public void setAttributes(ScopeAttributes attributes) {
        this.attributes = attributes;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public CreatorType getCreatorType() {
        return creatorType;
    }

    public void setCreatorType(CreatorType creatorType) {
        this.creatorType = creatorType;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
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

    public Boolean getDefaultScope() {
        return defaultScope;
    }

    public void setDefaultScope(Boolean defaultScope) {
        this.defaultScope = defaultScope;
    }

    public boolean isUmaType() {
        return scopeType != null && ScopeType.UMA.getValue().equalsIgnoreCase(scopeType.getValue());
    }

    @Override
    public String toString() {
        return "Scope{" +
                "dn='" + getDn() + '\'' +
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
                ", creatorId=" + creatorId +
                ", creatorType=" + creatorType +
                ", creationDate=" + creationDate +
                ", creatorAttributes=" + creatorAttributes +
                ", deletable=" + isDeletable() +
                ", expirationDate=" + getExpirationDate() +
                ", attributes=" + attributes +
                '}';
    }
}