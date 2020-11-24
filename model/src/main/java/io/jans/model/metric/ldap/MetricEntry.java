/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.metric.ldap;

import io.jans.model.ApplicationType;
import io.jans.model.metric.MetricType;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

import java.util.Date;

/**
 * Base metric entry
 *
 * @author Yuriy Movchan Date: 07/27/2015
 */
@DataEntry(sortBy = "startDate")
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
    private ApplicationType applicationType;

    @AttributeName(name = "jansMetricTyp")
    private MetricType metricType;

    @AttributeName(name = "creationDate")
    private Date creationDate;

    @AttributeName(name = "jansHost")
    private String nodeIndetifier;

    @AttributeName(name = "exp")
    private Date expirationDate;
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

    public String getNodeIndetifier() {
		return nodeIndetifier;
	}

	public void setNodeIndetifier(String nodeIndetifier) {
		this.nodeIndetifier = nodeIndetifier;
	}

	public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    @Override
	public String toString() {
		return "MetricEntry [dn=" + dn + ", id=" + id + ", startDate=" + startDate + ", endDate=" + endDate + ", applicationType="
				+ applicationType + ", metricType=" + metricType + ", creationDate=" + creationDate + ", nodeIndetifier=" + nodeIndetifier
				+ ", expirationDate=" + expirationDate + ", deletable=" + deletable + "]";
	}
}
