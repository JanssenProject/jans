/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package org.oxauth.persistence.model.configuration;

import java.io.Serializable;
import java.util.List;

import io.jans.model.SmtpConfiguration;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.InumEntry;
import io.jans.service.cache.CacheConfiguration;
import io.jans.service.document.store.conf.DocumentStoreConfiguration;

/**
 * Gluu Configuration
 *
 * @author Yuriy Movchan Date: 08.27.2012
 */
@DataEntry
@ObjectClass(value = "gluuConfiguration")
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

	@AttributeName(name = "oxDocumentStoreConfiguration")
	@JsonObject
	private DocumentStoreConfiguration documentStoreConfiguration;

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

	public DocumentStoreConfiguration getDocumentStoreConfiguration() {
		return documentStoreConfiguration;
	}

	public void setDocumentStoreConfiguration(DocumentStoreConfiguration documentStoreConfiguration) {
		this.documentStoreConfiguration = documentStoreConfiguration;
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
