/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.config.oxtrust;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.ldap.model.Entry;

/**
 * @author Rahat Ali
 * @version 2.1, 19/04/2015
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthConfiguration"})
public class LdapOxAuthConfiguration extends Entry {
	private static final long serialVersionUID = 2453308522994526877L;

	@LdapDN
    private String dn;

	@LdapAttribute(name = "oxAuthConfDynamic")
    private String oxAuthConfigDynamic;
	
	@LdapAttribute(name = "oxAuthConfStatic")
    private String oxAuthConfstatic;

	public LdapOxAuthConfiguration() {
	}

	
	public String getOxAuthConfigDynamic() {
		return oxAuthConfigDynamic;
	}

	public void setOxAuthConfigDynamic(String oxAuthConfigDynamic) {
		this.oxAuthConfigDynamic = oxAuthConfigDynamic;
	}

    public String getOxAuthConfstatic() {
		return oxAuthConfstatic;
	}

	public void setOxAuthConfstatic(String oxAuthConfstatic) {
		this.oxAuthConfstatic = oxAuthConfstatic;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LdapAppConfiguration [dn=").append(dn).append(", application=").append(oxAuthConfigDynamic).append("]");
		return builder.toString();
	}

}
