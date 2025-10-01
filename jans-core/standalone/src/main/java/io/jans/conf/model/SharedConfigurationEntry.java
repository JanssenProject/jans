/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.conf.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.model.SmtpConfiguration;
import io.jans.orm.annotation.*;
import io.jans.orm.model.base.InumEntry;
import io.jans.service.cache.CacheConfiguration;

import java.io.Serializable;
import java.util.Date;

/**
 * GluuConfiguration
 * 
 * @author Yuriy Movchan Date: 01/21/2020
 */
@DataEntry
@ObjectClass(value = "jansConfiguration")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SharedConfigurationEntry extends InumEntry implements Serializable {

	private static final long serialVersionUID = -1817003894646725601L;

	@AttributeName
	private String description;

	@AttributeName
	private String displayName;

	@AttributeName(name = "jansLastUpd", updateOnly = true)
	private Date lastUpdate;

	@AttributeName(name = "gluuConfigurationPollingInterval")
	private String pollingInterval;

	@AttributeName(name = "Janssen ProjectEmail")
	private String contactEmail;

	@AttributeName(name = "jansSmtpConf")
	@JsonObject
	private SmtpConfiguration smtpConfiguration;

	@AttributeName(name = "jansCacheConf")
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