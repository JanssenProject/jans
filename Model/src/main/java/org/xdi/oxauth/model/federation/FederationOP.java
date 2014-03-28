package org.xdi.oxauth.model.federation;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/09/2012
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthFederationOP"})
public class FederationOP {
    @LdapDN
    private String dn;
    @LdapAttribute(name = "inum")
    private String id;
    @LdapAttribute(name = "displayName")
    private String displayName;
    @LdapAttribute(name = "oxAuthFederationOpDomain")
    private String domain;
    @LdapAttribute(name = "oxAuthFederationOpId")
    private String opId;

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

    public String getDomain() {
        return domain;
    }

    public void setDomain(String p_domain) {
        domain = p_domain;
    }

    public String getOpId() {
        return opId;
    }

    public void setOpId(String p_opId) {
        opId = p_opId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("FederationOP");
        sb.append("{displayName='").append(displayName).append('\'');
        sb.append(", dn='").append(dn).append('\'');
        sb.append(", id='").append(id).append('\'');
        sb.append(", domain='").append(domain).append('\'');
        sb.append(", opId='").append(opId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
