/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.metric.ldap;

import io.jans.model.ApplicationType;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.Expiration;
import io.jans.orm.annotation.ObjectClass;

import java.util.Date;

/**
 * Base metric entry
 *
 * @author Yuriy Movchan Date: 07/27/2015
 */
@DataEntry(sortBy = "startDate", sortByName = "jansStartDate")
@ObjectClass(value = "jansMetric")
public class MetricEntry {

    @DN
    private String dn;

    @AttributeName(name = "uniqueIdentifier", ignoreDuringUpdate = true)
    private String id;

    @AttributeName(name = "jansStartDate")
    private Date startDate;

    @AttributeName(name = "jansEndDate")
    private Date endDate;

    @AttributeName(name = "jansAppTyp")
    private String applicationType;

    @AttributeName(name = "jansMetricTyp")
    private String metricType;

    @AttributeName(name = "jansMetricSubTyp")
    private String metricSubType;

    @AttributeName(name = "creationDate")
    private Date creationDate;

    @AttributeName(name = "jansHost")
    private String nodeIdentifier;

    @AttributeName(name = "exp")
    private Date expirationDate;

    @Expiration
    private Integer ttl;

    @AttributeName(name = "del")
    private boolean deletable = true;

    public MetricEntry() {
    }

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

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getMetricSubType() {
        return metricSubType;
    }

    public void setMetricSubType(String metricSubType) {
        this.metricSubType = metricSubType;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getNodeIdentifier() {
		return nodeIdentifier;
	}

	public void setNodeIdentifier(String nodeIdentifier) {
		this.nodeIdentifier = nodeIdentifier;
	}

	public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Integer getTtl() {
		return ttl;
	}

	public void setTtl(Integer ttl) {
		this.ttl = ttl;
	}

	public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    @Override
	public String toString() {
		return "MetricEntry [dn=" + dn + ", id=" + id + ", startDate=" + startDate + ", endDate=" + endDate
				+ ", applicationType=" + applicationType + ", metricType=" + metricType + ", metricSubType="
				+ metricSubType + ", creationDate=" + creationDate + ", nodeIdentifier=" + nodeIdentifier
				+ ", expirationDate=" + expirationDate + ", ttl=" + ttl + ", deletable=" + deletable + "]";
	}
}
