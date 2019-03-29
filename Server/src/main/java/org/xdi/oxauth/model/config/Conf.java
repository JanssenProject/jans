/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.config;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/01/2013
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthConfiguration"})
public class Conf {
    @LdapDN
    private String dn;
    @LdapAttribute(name = "oxAuthConfDynamic")
    private String dynamic;
    @LdapAttribute(name = "oxAuthConfStatic")
    private String statics;
    @LdapAttribute(name = "oxAuthConfErrors")
    private String errors;
    @LdapAttribute(name = "oxAuthConfWebKeys")
    private String webKeys;

    @LdapAttribute(name = "oxRevision")
    private long revision;

    public Conf() {
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
    }

    public String getDynamic() {
        return dynamic;
    }

    public void setDynamic(String p_dynamic) {
        dynamic = p_dynamic;
    }

    public String getErrors() {
        return errors;
    }

    public void setErrors(String p_errors) {
        errors = p_errors;
    }

    public String getStatics() {
        return statics;
    }

    public void setStatics(String p_statics) {
        statics = p_statics;
    }

    public String getWebKeys() {
        return webKeys;
    }

    public void setWebKeys(String p_webKeys) {
        webKeys = p_webKeys;
    }

    public long getRevision() {
		return revision;
	}

	public void setRevision(long revision) {
		this.revision = revision;
	}

	@Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Conf");
        sb.append("{m_dn='").append(dn).append('\'');
        sb.append(", m_dynamic='").append(dynamic).append('\'');
        sb.append(", m_static='").append(statics).append('\'');
        sb.append(", m_errors='").append(errors).append('\'');
        sb.append(", m_webKeys='").append(webKeys).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
