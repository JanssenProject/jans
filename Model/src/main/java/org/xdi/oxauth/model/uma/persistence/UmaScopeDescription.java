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
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthUmaScopeDescription"})
public class UmaScopeDescription {

    @LdapDN
    private String dn;

    @LdapAttribute(ignoreDuringUpdate = true)
    private String inum;

    @NotNull
    @Size(min = 2, max = 2083, message = "Length of scope should be between 2 and 500")
//    @Pattern(regexp = "^[a-zA-Z\\d_]{4,30}$", message = "Invalid Scope Id .Only alphanumeric and underscore are allowed.")
    @LdapAttribute(name = "oxId")
    private String id; // keep scope, id can be url or plain scope (edit, view, delete)

    @NotNull(message = "Display name should be not empty")
    @LdapAttribute(name = "displayName")
    private String displayName;

    @LdapAttribute(name = "description")
    private String description;

    @LdapAttribute(name = "owner")
    private String owner;

    @LdapAttribute(name = "oxFaviconImage")
    private String faviconImageAsXml;

    @LdapAttribute(name = "oxIconUrl")
    private String iconUrl;

    @LdapAttribute(name = "oxPolicyScriptDn")
    private List<String> authorizationPolicies;

    public UmaScopeDescription() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String p_inum) {
        inum = p_inum;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String p_displayName) {
        displayName = p_displayName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String p_owner) {
        owner = p_owner;
    }

    public String getFaviconImageAsXml() {
        return faviconImageAsXml;
    }

    public void setFaviconImageAsXml(String p_faviconImageAsXml) {
        faviconImageAsXml = p_faviconImageAsXml;
    }

    public String getId() {
        return id;
    }

    public void setId(String p_id) {
        id = p_id;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String p_iconUrl) {
        iconUrl = p_iconUrl;
    }

	public List<String> getAuthorizationPolicies() {
		return authorizationPolicies;
	}

	public void setAuthorizationPolicies(List<String> authorizationPolicies) {
		this.authorizationPolicies = authorizationPolicies;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UmaScopeDescription that = (UmaScopeDescription) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "UmaScopeDescription{" +
                "dn='" + dn + '\'' +
                ", inum='" + inum + '\'' +
                ", id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                ", owner='" + owner + '\'' +
                ", faviconImageAsXml='" + faviconImageAsXml + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", authorizationPolicies=" + authorizationPolicies +
                '}';
    }
}
