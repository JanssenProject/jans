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
 * @author Yuriy Movchan
 * @version 0.9, 05/16/2013
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxTrustConfiguration"})
public class LdapOxTrustConfiguration extends Entry {

	@LdapDN
    private String dn;

	@LdapAttribute(name = "oxTrustConfApplication")
    private String application;

    public LdapOxTrustConfiguration() {
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LdapAppConfiguration [dn=").append(dn).append(", application=").append(application).append("]");
		return builder.toString();
	}

}
