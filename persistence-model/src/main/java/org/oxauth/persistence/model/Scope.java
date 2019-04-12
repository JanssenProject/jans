/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.oxauth.persistence.model;

import java.io.Serializable;
import java.util.List;

import org.gluu.oxauth.model.common.ScopeType;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;

/**
 * @author Javier Rojas Blum Date: 07.05.2012
 * @author Yuriy Movchan Date: 06/30/2015
 */
@DataEntry
@ObjectClass(values = {"top", "oxAuthCustomScope"})
public class Scope implements Serializable {

    private static final long serialVersionUID = 4308826784917052508L;

    private transient boolean isDefault;
    private transient boolean isGroupClaims;

    @DN
    private String dn;
    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @AttributeName
    private String displayName;

    @AttributeName
    private String description;

    @AttributeName(name = "oxScopeType")
    private ScopeType scopeType;
 
    @AttributeName(name = "oxAuthClaim")
    private List<String> oxAuthClaims;

    @AttributeName(name = "defaultScope")
    private String defaultScope;

    @AttributeName(name = "oxAuthGroupClaims")
    private String oxAuthGroupClaims;

    @AttributeName(name = "oxScriptDn")
    private List<String> dynamicScopeScripts;

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

    public String getDefaultScope() {
        return this.defaultScope;
    }

    public void setDefaultScope(String defaultScope) {
        this.defaultScope = defaultScope;
    }

    public boolean getIsDefault() {
        if (this.defaultScope == null) {
            return false;
        }
        if (this.defaultScope.equalsIgnoreCase("true")) {
            this.isDefault = true;
            return this.isDefault;
        }
        this.isDefault = false;
        return this.isDefault;
    }

    public String getOxAuthGroupClaims() {
        return oxAuthGroupClaims;
    }

    public void setOxAuthGroupClaims(String oxAuthGroupClaims) {
        this.oxAuthGroupClaims = oxAuthGroupClaims;
    }

    public boolean getIsOxAuthGroupClaims() {
        if (this.oxAuthGroupClaims == null) {
            return false;
        }
        if (this.oxAuthGroupClaims.equalsIgnoreCase("true")) {
            this.isGroupClaims = true;
            return this.isGroupClaims;
        }
        this.isGroupClaims = false;
        return this.isGroupClaims;
    }

    public List<String> getDynamicScopeScripts() {
		return dynamicScopeScripts;
	}

	public void setDynamicScopeScripts(List<String> dynamicScopeScripts) {
        this.dynamicScopeScripts = dynamicScopeScripts;
    }

    @Override
    public String toString() {
        return String.format(
                "Scope [description=%s, displayName=%s, inum=%s, oxAuthClaims=%s, defaultScope=%s, oxAuthGroupClaims=%s, toString()=%s]",
                description, displayName, inum, oxAuthClaims, defaultScope, oxAuthGroupClaims, super.toString());
    }
}