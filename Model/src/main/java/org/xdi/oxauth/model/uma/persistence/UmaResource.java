/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma.persistence;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Resource description.
 *
 * @author Yuriy Zabrovarnyy Date: 10/03/2012
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxUmaResource"})
public class UmaResource {

    @LdapDN
    private String dn;

    @LdapAttribute(ignoreDuringUpdate = true)
    private String inum;

    @LdapAttribute(name = "oxId")
    private String id;

    @NotNull(message = "Display name should be not empty")
    @LdapAttribute(name = "displayName")
    private String name;

    @LdapAttribute(name = "oxFaviconImage")
    private String iconUri;

    @LdapAttribute(name = "oxAuthUmaScope")
    private List<String> scopes;

    @LdapAttribute(name = "oxScopeExpression")
    private String scopeExpression;

    @LdapAttribute(name = "oxAssociatedClient")
    private List<String> clients;

	@LdapAttribute(name = "oxResource")
	private List<String> resources;

    @LdapAttribute(name = "oxRevision")
    private String rev;

    @LdapAttribute(name = "owner")
    private String creator;

    @LdapAttribute(name = "description")
    private String description;

    @LdapAttribute(name = "oxType")
    private String type;

    public String getScopeExpression() {
        return scopeExpression;
    }

    public void setScopeExpression(String scopeExpression) {
        this.scopeExpression = scopeExpression;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

	public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getClients() {
        return clients;
    }

    public void setClients(List<String> p_clients) {
        clients = p_clients;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconUri() {
        return iconUri;
    }

    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public List<String> getResources() {
		return resources;
	}

	public void setResources(List<String> resources) {
		this.resources = resources;
	}

    public String getRev() {
        return rev;
    }

    public void setRev(String rev) {
        this.rev = rev;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

}
