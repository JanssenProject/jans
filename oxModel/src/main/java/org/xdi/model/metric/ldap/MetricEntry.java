/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.model.metric.ldap;

import java.util.Date;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.model.ApplicationType;
import org.xdi.model.metric.MetricType;

/**
 * Base metric entry 
 *
 * @author Yuriy Movchan Date: 07/27/2015
 */
@LdapEntry(sortBy = "startDate")
@LdapObjectClass(values = {"top", "oxMetric"})
public class MetricEntry {

	@LdapDN
	private String dn;

	@LdapAttribute(name = "uniqueIdentifier", ignoreDuringUpdate = true)
	private String id;

    @LdapAttribute(name = "oxStartDate")
	private Date startDate;

    @LdapAttribute(name = "oxEndDate")
	private Date endDate;

    @LdapAttribute(name = "oxApplicationType")
	private ApplicationType applicationType;

    @LdapAttribute(name = "oxMetricType")
	private MetricType metricType;

    @LdapAttribute(name = "creationDate")
	private Date creationDate;

	public MetricEntry() {}

	public MetricEntry(String dn, String id, Date creationDate) {
		this.dn = dn;
		this.id = id;
		this.creationDate = creationDate;
	}

	public String getDn() {
		return dn;
	}

	public void setDn(String dn) {
		this.dn = dn;
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

	public MetricType getMetricType() {
		return metricType;
	}

	public void setMetricType(MetricType metricType) {
		this.metricType = metricType;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MetricEntry [dn=").append(dn).append(", id=").append(id).append(", startDate=").append(startDate).append(", endDate=").append(endDate)
				.append(", applicationType=").append(applicationType).append(", metricType=").append(metricType).append(", creationDate=").append(creationDate)
				.append("]");
		return builder.toString();
	}

}
