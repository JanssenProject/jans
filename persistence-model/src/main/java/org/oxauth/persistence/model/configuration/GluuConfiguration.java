/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.oxauth.persistence.model.configuration;

import java.io.Serializable;
import java.util.List;

import org.gluu.persist.model.base.GluuBoolean;
import org.gluu.persist.model.base.InumEntry;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.JsonObject;
import org.gluu.persist.annotation.ObjectClass;
import org.gluu.model.SmtpConfiguration;
import org.gluu.service.cache.CacheConfiguration;

/**
 * Gluu Configuration
 *
 * @author Yuriy Movchan Date: 08.27.2012
 */
@DataEntry
@ObjectClass(values = { "top", "gluuConfiguration" })
public class GluuConfiguration extends InumEntry implements Serializable {

	private static final long serialVersionUID = -2818003894646725601L;

	@AttributeName(ignoreDuringUpdate = true)
	private String inum;

	@AttributeName(name = "oxSmtpConfiguration")
	@JsonObject
	private SmtpConfiguration smtpConfiguration;

	@AttributeName(name = "oxCacheConfiguration")
	@JsonObject
	private CacheConfiguration cacheConfiguration;
	
	@AttributeName(name = "oxIDPAuthentication")
	@JsonObject
	private List<oxIDPAuthConf> oxIDPAuthentication;

	@AttributeName(name = "oxAuthenticationMode")
	private String authenticationMode;
	
	@AttributeName(name = "gluuPassportEnabled")
	private Boolean passportEnabled;

	public Boolean getPassportEnabled() {
		return passportEnabled;
	}

	public void setPassportEnabled(Boolean passportEnabled) {
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

	public List<oxIDPAuthConf> getOxIDPAuthentication(){
		return this.oxIDPAuthentication;
	}
	
	public void setOxIDPAuthentication(List<oxIDPAuthConf> oxIDPAuthentication){
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
		builder.append("GluuConfiguration [inum=").append(inum).append(", smtpConfiguration=").append(smtpConfiguration).append(", oxIDPAuthentication=")
				.append(oxIDPAuthentication).append(", authenticationMode=").append(authenticationMode).append(", toString()=").append(super.toString())
				.append("]");
		return builder.toString();
	}

}
