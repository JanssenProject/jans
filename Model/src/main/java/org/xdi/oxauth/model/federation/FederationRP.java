package org.xdi.oxauth.model.federation;

import java.util.List;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/09/2012
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthFederationRP"})
public class FederationRP {
    @LdapDN
    private String dn;
    @LdapAttribute(name = "inum")
    private String id;
    @LdapAttribute(name = "displayName")
    private String displayName;
    @LdapAttribute(name = "oxAuthRedirectURI")
    private List<String> redirectUri;

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
    }

    public String getId() {
        return id;
    }

    public void setId(String p_id) {
        id = p_id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String p_displayName) {
        displayName = p_displayName;
    }

    public List<String> getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(List<String> p_redirectUri) {
        redirectUri = p_redirectUri;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("FederationRP");
        sb.append("{displayName='").append(displayName).append('\'');
        sb.append(", dn='").append(dn).append('\'');
        sb.append(", id='").append(id).append('\'');
        sb.append(", redirectUri=").append(redirectUri);
        sb.append('}');
        return sb.toString();
    }
}
