/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.persistence.model.configuration;

import io.jans.model.SmtpConfiguration;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.InumEntry;
import io.jans.service.cache.CacheConfiguration;
import io.jans.service.document.store.conf.DocumentStoreConfiguration;

import java.io.Serializable;
import java.util.List;

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

	@AttributeName(name = "jsSmtpConf")
	@JsonObject
	private SmtpConfiguration smtpConfiguration;

	@AttributeName(name = "jsCacheConf")
	@JsonObject
	private CacheConfiguration cacheConfiguration;

	@AttributeName(name = "jsDocStoreConf")
	@JsonObject
	private DocumentStoreConfiguration documentStoreConfiguration;

	@AttributeName(name = "jsIDPAuthn")
	@JsonObject
	private List<IDPAuthConf> jsIDPAuthn;

	@AttributeName(name = "jsenticationMode")
	private String authenticationMode;
	
	@AttributeName(name = "jsPassportEnabled")
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

	public List<IDPAuthConf> getOxIDPAuthentication(){
		return this.jsIDPAuthn;
	}
	
	public void setOxIDPAuthentication(List<IDPAuthConf> jsIDPAuthn){
		this.jsIDPAuthn = jsIDPAuthn;
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
		builder.append("GluuConfiguration [inum=").append(inum).append(", smtpConfiguration=").append(smtpConfiguration).append(", jsIDPAuthn=")
				.append(jsIDPAuthn).append(", authenticationMode=").append(authenticationMode).append(", toString()=").append(super.toString())
				.append("]");
		return builder.toString();
	}

}
