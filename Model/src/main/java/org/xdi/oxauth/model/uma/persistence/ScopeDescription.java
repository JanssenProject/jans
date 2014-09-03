package org.xdi.oxauth.model.uma.persistence;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/04/2013
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthUmaScopeDescription"})
public class ScopeDescription {

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
    @Pattern(regexp = "^[a-zA-Z\\d_]{4,30}$", message = "Invalid scope name")
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
//    @LdapAttribute(name = "oxAssociatedClient")
//    private List<String> clients;

    public ScopeDescription() {
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

//    public List<String> getClients() {
//        return clients;
//    }
//
//    public void setClients(List<String> p_clients) {
//        clients = p_clients;
//    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("UmaScope");
        sb.append("{dn='").append(dn).append('\'');
        sb.append(", inum='").append(inum).append('\'');
        sb.append(", displayName='").append(displayName).append('\'');
        sb.append(", owner='").append(owner).append('\'');
        sb.append(", faviconImageAsXml='").append(faviconImageAsXml).append('\'');
        sb.append(", oxId='").append(id).append('\'');
        sb.append(", policyRule='").append(policyRule).append('\'');
        sb.append(", revision='").append(revision).append('\'');
        sb.append(", iconUrl='").append(iconUrl).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
