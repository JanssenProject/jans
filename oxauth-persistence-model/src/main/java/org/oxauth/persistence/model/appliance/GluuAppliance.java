/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.oxauth.persistence.model.appliance;

import java.io.Serializable;
import java.util.List;

import org.gluu.persist.model.base.GluuBoolean;
import org.gluu.persist.model.base.InumEntry;
import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapJsonObject;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.model.SmtpConfiguration;
import org.xdi.service.cache.CacheConfiguration;

/**
 * Gluu Appliance
 *
 * @author Yuriy Movchan Date: 08.27.2012
 */
@LdapEntry
@LdapObjectClass(values = { "top", "gluuAppliance" })
public class GluuAppliance extends InumEntry implements Serializable {

	private static final long serialVersionUID = -2818003894646725601L;

	@LdapAttribute(ignoreDuringUpdate = true)
	private String inum;

	@LdapAttribute(name = "oxSmtpConfiguration")
	@LdapJsonObject
	private SmtpConfiguration smtpConfiguration;

	@LdapAttribute(name = "oxCacheConfiguration")
	@LdapJsonObject
	private CacheConfiguration cacheConfiguration;
	
	@LdapAttribute(name = "oxIDPAuthentication")
	private List<String> oxIDPAuthentication;

	@LdapAttribute(name = "oxAuthenticationMode")
	private String authenticationMode;
	
	@LdapAttribute(name = "gluuPassportEnabled")
	private GluuBoolean passportEnabled;

	public GluuBoolean getPassportEnabled() {
		return passportEnabled;
	}

	public void setPassportEnabled(GluuBoolean passportEnabled) {
		this.passportEnabled = passportEnabled;
	}

	public String getInum() {
		return inum;
	}

	public void setInum(String inum) {
		this.inum = inum;
	}

	public SmtpConfiguration getSmtpConfiguration() {
		return smtpConfiguration;
	}

	public CacheConfiguration getCacheConfiguration() {
		return cacheConfiguration;
	}

	public void setCacheConfiguration(CacheConfiguration cacheConfiguration) {
		this.cacheConfiguration = cacheConfiguration;
	}

	public void setSmtpConfiguration(SmtpConfiguration smtpConfiguration) {
		this.smtpConfiguration = smtpConfiguration;
	}

	public List<String> getOxIDPAuthentication(){
		return this.oxIDPAuthentication;
	}
	
	public void setOxIDPAuthentication(List<String> oxIDPAuthentication){
		this.oxIDPAuthentication = oxIDPAuthentication;
	}

	public String getAuthenticationMode() {
		return authenticationMode;
	}

	public void setAuthenticationMode(String authenticationMode) {
		this.authenticationMode = authenticationMode;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GluuAppliance [inum=").append(inum).append(", smtpConfiguration=").append(smtpConfiguration).append(", oxIDPAuthentication=")
				.append(oxIDPAuthentication).append(", authenticationMode=").append(authenticationMode).append(", toString()=").append(super.toString())
				.append("]");
		return builder.toString();
	}

}
