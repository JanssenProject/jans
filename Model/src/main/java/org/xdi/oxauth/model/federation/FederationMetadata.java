package org.xdi.oxauth.model.federation;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/09/2012
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthFederationMetadata"})
public class FederationMetadata {
    @LdapDN
    private String dn;
    @LdapAttribute(name = "inum")
    private String id;
    @LdapAttribute(name = "displayName")
    private String displayName;
    @LdapAttribute(name = "oxAuthFederationRP")
    private List<String> rps;
    @LdapAttribute(name = "oxAuthFederationOP")
    private List<String> ops;
    @LdapAttribute(name = "oxAuthFederationMetadataIntervalCheck")
    private String intervalCheck;

    private List<FederationRP> rpList;
    private List<FederationOP> opList;

    public List<FederationOP> getOpList() {
        return opList;
    }

    public void setOpList(List<FederationOP> p_opList) {
        opList = p_opList;
    }

    public List<FederationRP> getRpList() {
        return rpList;
    }

    public void setRpList(List<FederationRP> p_rpList) {
        rpList = p_rpList;
    }

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

    public List<String> getOps() {
        return ops;
    }

    public void setOps(List<String> p_ops) {
        ops = p_ops;
    }

    public List<String> getRps() {
        return rps;
    }

    public void setRps(List<String> p_rps) {
        rps = p_rps;
    }

    public String getIntervalCheck() {
        return intervalCheck;
    }

    public void setIntervalCheck(String p_intervalCheck) {
        intervalCheck = p_intervalCheck;
    }

    public List<String> collectAllRedirectUris() {
        final List<String> result = new ArrayList<String>();
        if (rpList != null && !rpList.isEmpty()) {
            for (FederationRP rp : rpList) {
                final List<String> redirectUri = rp.getRedirectUri();
                if (redirectUri != null && !redirectUri.isEmpty()) {
                    result.addAll(redirectUri);
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("FederationMetadata");
        sb.append("{displayName='").append(displayName).append('\'');
        sb.append(", id='").append(id).append('\'');
        sb.append(", intervalCheck='").append(intervalCheck).append('\'');
        sb.append(", rpList=").append(rpList);
        sb.append(", opList=").append(opList);
        sb.append(", dn='").append(dn).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
