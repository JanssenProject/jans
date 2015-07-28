/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.model.statistic;

import java.util.Date;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.ldap.model.BaseEntry;
import org.xdi.model.ApplicationType;

/**
 * Statistic entry 
 *
 * @author Yuriy Movchan Date: 07/27/2015
 */
@LdapEntry(sortBy = "level")
@LdapObjectClass(values = {"top", "oxStatistic"})
public class StatisticEntry extends BaseEntry {

	@LdapAttribute(ignoreDuringUpdate = true)
	private String id;

    @LdapAttribute(name = "oxStartDate")
	private Date startDate;

    @LdapAttribute(name = "oxEndDate")
	private Date endDate;

    @LdapAttribute(name = "oxApplicationType")
	private ApplicationType applicationType;

    @LdapAttribute(name = "oxEventType")
	private StatisticEventType eventType;

    @LdapAttribute(name = "description")
	private String description;

    @LdapAttribute(name = "creationDate")
	private Date creationDate;

	public StatisticEntry() {}

	public StatisticEntry(String dn, String id, Date creationDate) {
		super(dn);
		this.id = id;
		this.creationDate = creationDate;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public ApplicationType getApplicationType() {
		return applicationType;
	}

	public void setApplicationType(ApplicationType applicationType) {
		this.applicationType = applicationType;
	}

	public StatisticEventType getEventType() {
		return eventType;
	}

	public void setEventType(StatisticEventType eventType) {
		this.eventType = eventType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

}
