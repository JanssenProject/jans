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
import javax.validation.constraints.Pattern;
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
    @NotNull(message = "Display name should be not empty")
    @LdapAttribute(name = "displayName")
    private String displayName;
    @LdapAttribute(name = "owner")
    private String owner;
    @LdapAttribute(name = "oxFaviconImage")
    private String faviconImageAsXml;

    @NotNull
    @Size(min = 4, max = 30, message = "Length of scope name should be between 4 and 30")
    @Pattern(regexp = "^[a-zA-Z\\d_]{4,30}$", message = "Invalid Scope Id .Only alphanumeric and underscore are allowed.")
    @LdapAttribute(name = "oxId")
    private String id;
    @LdapAttribute(name = "oxPolicyRule")
    private String policyRule;
    @LdapAttribute(name = "oxRevision")
    private String revision;
    @LdapAttribute(name = "oxIconUrl")
    private String iconUrl;
    @LdapAttribute(name = "oxUrl")
    private String url;
    @LdapAttribute(name = "oxType")
    private InternalExternal type;
    
    @LdapAttribute(name = "oxPolicyScriptDn")
    private List<String> authorizationPolicies;

    public UmaScopeDescription() {
    }

    public InternalExternal getType() {
        return type;
    }

    public void setType(InternalExternal p_type) {
        type = p_type;
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

    public String getPolicyRule() {
        return policyRule;
    }

    public void setPolicyRule(String p_policyRule) {
        policyRule = p_policyRule;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String p_revision) {
        revision = p_revision;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String p_iconUrl) {
        iconUrl = p_iconUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String p_url) {
        url = p_url;
    }

	public List<String> getAuthorizationPolicies() {
		return authorizationPolicies;
	}

	public void setAuthorizationPolicies(List<String> authorizationPolicies) {
		this.authorizationPolicies = authorizationPolicies;
	}

    @Override
    public String toString() {
        return "UmaScopeDescription{" +
                "dn='" + dn + '\'' +
                ", inum='" + inum + '\'' +
                ", displayName='" + displayName + '\'' +
                ", owner='" + owner + '\'' +
                ", faviconImageAsXml='" + faviconImageAsXml + '\'' +
                ", id='" + id + '\'' +
                ", policyRule='" + policyRule + '\'' +
                ", revision='" + revision + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", url='" + url + '\'' +
                ", type=" + type +
                ", authorizationPolicies=" + authorizationPolicies +
                '}';
    }
}
