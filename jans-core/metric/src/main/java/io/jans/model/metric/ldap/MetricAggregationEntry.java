/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.model.metric.ldap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import io.jans.model.metric.MetricAggregationType;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.Expiration;
import io.jans.orm.annotation.ObjectClass;

import java.util.Date;

/**
 * Aggregated metric entry. Holds a pre-computed aggregate of {@link MetricEntry}
 * rows for one period (HOURLY, DAILY, WEEKLY or MONTHLY). Aggregation rows are
 * produced on application nodes; readers must treat them as immutable.
 *
 * Application type and metric type are open string sets: applications declare
 * their own metric types, hence no enum binding here.
 *
 * @author Yuriy Movchan
 */
@DataEntry(sortBy = "startDate", sortByName = "jansStartDate")
@ObjectClass(value = "jansMetricAggregation")
public class MetricAggregationEntry {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @DN
    private String dn;

    @AttributeName(name = "uniqueIdentifier", ignoreDuringUpdate = true)
    private String id;

    @AttributeName(name = "jansAggregationType")
    private MetricAggregationType aggregationType;

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

    @AttributeName(name = "jansData")
    private String data;

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

    public MetricAggregationEntry() {
    }

    public MetricAggregationEntry(String dn, String id, Date creationDate) {
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

    public MetricAggregationType getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(MetricAggregationType aggregationType) {
        this.aggregationType = aggregationType;
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

    @JsonIgnore
    public String getData() {
        return data;
    }

    /**
     * Exposes the raw jansData string as parsed JSON, so that a REST response carries a JSON object
     * instead of an escaped string. jansData is written by the aggregation producer as JSON, but a
     * foreign or damaged row may hold text, so parse failures fall back to a text node rather than
     * raising an error.
     */
    @JsonProperty("data")
    public JsonNode getDataAsJson() {
        if (data == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(data);
        } catch (Exception e) {
            return TextNode.valueOf(data);
        }
    }

    public void setData(String data) {
        this.data = data;
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
        return "MetricAggregationEntry [dn=" + dn + ", id=" + id + ", aggregationType=" + aggregationType
                + ", startDate=" + startDate + ", endDate=" + endDate + ", applicationType=" + applicationType
                + ", metricType=" + metricType + ", metricSubType=" + metricSubType + ", creationDate=" + creationDate
                + ", nodeIdentifier=" + nodeIdentifier + ", expirationDate=" + expirationDate + ", ttl=" + ttl
                + ", deletable=" + deletable + "]";
    }
}
