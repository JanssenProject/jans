/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.model.metric.ldap;

import org.gluu.model.ApplicationType;
import org.gluu.model.metric.MetricType;
import org.gluu.persist.annotation.LdapAttribute;
import org.gluu.persist.annotation.LdapDN;
import org.gluu.persist.annotation.LdapEntry;
import org.gluu.persist.annotation.LdapObjectClass;

import java.util.Date;

/**
 * Base metric entry
 *
 * @author Yuriy Movchan Date: 07/27/2015
 */
@Entry(sortBy = "startDate")
@ObjectClass(values = { "top", "oxMetric" })
public class MetricEntry {

    @DN
    private String dn;

    @Attribute(name = "uniqueIdentifier", ignoreDuringUpdate = true)
    private String id;

    @Attribute(name = "oxStartDate")
    private Date startDate;

    @Attribute(name = "oxEndDate")
    private Date endDate;

    @Attribute(name = "oxApplicationType")
    private ApplicationType applicationType;

    @Attribute(name = "oxMetricType")
    private MetricType metricType;

    @Attribute(name = "creationDate")
    private Date creationDate;

    @Attribute(name = "oxAuthExpiration")
    private Date expirationDate;
    @Attribute(name = "oxDeletable")
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
        return "MetricEntry{" +
                "dn='" + dn + '\'' +
                ", id='" + id + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", applicationType=" + applicationType +
                ", metricType=" + metricType +
                ", creationDate=" + creationDate +
                ", expirationDate=" + expirationDate +
                ", deletable=" + deletable +
                '}';
    }
}
