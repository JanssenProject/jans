/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.conf.model;

import java.io.Serializable;
import java.util.Date;

import org.gluu.model.SmtpConfiguration;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.CustomObjectClass;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.JsonObject;
import org.gluu.persist.annotation.ObjectClass;
import org.gluu.persist.model.base.InumEntry;
import org.gluu.service.cache.CacheConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * GluuConfiguration
 * 
 * @author Yuriy Movchan Date: 01/21/2020
 */
@DataEntry
@ObjectClass(value = "gluuConfiguration")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SharedConfigurationEntry extends InumEntry implements Serializable {

	private static final long serialVersionUID = -1817003894646725601L;

	@AttributeName
	private String description;

	@AttributeName
	private String displayName;

	@AttributeName(name = "gluuLastUpdate", updateOnly = true)
	private Date lastUpdate;

	@AttributeName(name = "gluuConfigurationPollingInterval")
	private String pollingInterval;

	@AttributeName(name = "oxTrustEmail")
	private String contactEmail;

	@AttributeName(name = "oxSmtpConfiguration")
	@JsonObject
	private SmtpConfiguration smtpConfiguration;

	@AttributeName(name = "oxCacheConfiguration")
	@JsonObject
	private CacheConfiguration cacheConfiguration;

	@CustomObjectClass
	private String[] customObjectClasses;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public String getPollingInterval() {
		return pollingInterval;
	}

	public void setPollingInterval(String pollingInterval) {
		this.pollingInterval = pollingInterval;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	public SmtpConfiguration getSmtpConfiguration() {
		return smtpConfiguration;
	}

	public void setSmtpConfiguration(SmtpConfiguration smtpConfiguration) {
		this.smtpConfiguration = smtpConfiguration;
	}

	public CacheConfiguration getCacheConfiguration() {
		return cacheConfiguration;
	}

	public void setCacheConfiguration(CacheConfiguration cacheConfiguration) {
		this.cacheConfiguration = cacheConfiguration;
	}

	public String[] getCustomObjectClasses() {
		return customObjectClasses;
	}

	public void setCustomObjectClasses(String[] customObjectClasses) {
		this.customObjectClasses = customObjectClasses;
	}

}