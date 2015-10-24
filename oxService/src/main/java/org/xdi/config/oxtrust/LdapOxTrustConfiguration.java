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

	private static final long serialVersionUID = -15289347651306279L;

	@LdapDN
    private String dn;

	@LdapAttribute(name = "oxTrustConfApplication")
    private String application;

	@LdapAttribute(name = "oxTrustConfCacheRefresh")
    private String cacheRefresh;

    @LdapAttribute(name = "oxRevision")
    private long revision;

    public LdapOxTrustConfiguration() {
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getCacheRefresh() {
		return cacheRefresh;
	}

	public void setCacheRefresh(String cacheRefresh) {
		this.cacheRefresh = cacheRefresh;
	}

	public long getRevision() {
		return revision;
	}

	public void setRevision(long revision) {
		this.revision = revision;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LdapOxTrustConfiguration [dn=").append(dn).append(", application=").append(application).append(", cacheRefresh=").append(cacheRefresh)
				.append(", revision=").append(revision).append("]");
		return builder.toString();
	}

}
