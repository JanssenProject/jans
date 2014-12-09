/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma.persistence;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.model.ProgrammingLanguage;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version 0.9, 22/02/2013
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthUmaPolicy"})
public class UmaPolicy implements Serializable {

	private static final long serialVersionUID = 7211549273913028625L;

	@LdapDN
    private String dn;

	@LdapAttribute(ignoreDuringUpdate = true)
	private String inum;
 
    @NotNull(message = "Display name should be not empty")
    @LdapAttribute(name = "displayName")
	private String displayName;

    @LdapAttribute(name = "description")
	private String description;

    @LdapAttribute(name = "oxPolicyScript")
	private String policyScript;

    @LdapAttribute(name = "programmingLanguage")
	private ProgrammingLanguage programmingLanguage;

    @LdapAttribute(name = "oxAuthUmaScope")
	private List<String> scopeDns;

    public UmaPolicy() {
    }

    public UmaPolicy(String p_description, String p_displayName, String p_dn, String p_inum, List<String> p_scopeDns, String p_policyScript, ProgrammingLanguage p_programmingLanguage) {
        description = p_description;
        displayName = p_displayName;
        dn = p_dn;
        inum = p_inum;
        scopeDns = p_scopeDns;
        policyScript = p_policyScript;
        programmingLanguage = p_programmingLanguage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String p_description) {
        description = p_description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String p_displayName) {
        displayName = p_displayName;
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

    public List<String> getScopeDns() {
        return scopeDns;
    }

    public void setScopeDns(List<String> p_scopeDns) {
        scopeDns = p_scopeDns;
    }

    public String getPolicyScript() {
        return policyScript;
    }

    public void setPolicyScript(String p_policyScript) {
        policyScript = p_policyScript;
    }

    public ProgrammingLanguage getProgrammingLanguage() {
        return programmingLanguage;
    }

    public void setProgrammingLanguage(ProgrammingLanguage p_programmingLanguage) {
        programmingLanguage = p_programmingLanguage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UmaPolicy umaPolicy = (UmaPolicy) o;

        return !(inum != null ? !inum.equals(umaPolicy.inum) : umaPolicy.inum != null);
    }

    @Override
    public int hashCode() {
        return inum != null ? inum.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("UmaPolicy");
        sb.append("{description='").append(description).append('\'');
        sb.append(", dn='").append(dn).append('\'');
        sb.append(", inum='").append(inum).append('\'');
        sb.append(", displayName='").append(displayName).append('\'');
        sb.append(", oxPolicyScript='").append(policyScript).append('\'');
        sb.append(", programmingLanguage='").append(programmingLanguage).append('\'');
        sb.append(", oxAuthUmaScope=").append(scopeDns);
        sb.append('}');
        return sb.toString();
    }
}
