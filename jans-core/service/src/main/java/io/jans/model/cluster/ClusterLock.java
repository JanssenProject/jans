/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.model.cluster;

import java.util.Date;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.Expiration;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

/**
 * Cluster claim entry (object class {@code jansClusterLock}).
 *
 * <p>One entry is either a claim of one run slot of a task, or the state row of a task. A claim is
 * created with an insert that fails when the DN already exists, so at most one node holds a slot.</p>
 *
 * @see io.jans.service.cluster.ClusterLockService
 */
@DataEntry
@ObjectClass(value = "jansClusterLock")
public class ClusterLock extends BaseEntry {

    private static final long serialVersionUID = 4630417352287166124L;

    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_DONE = "done";
    public static final String STATUS_SKIPPED = "skipped";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_STATE = "state";

    @AttributeName(name = "jansId", ignoreDuringUpdate = true)
    private String id;

    @AttributeName(name = "jansType")
    private String task;

    @AttributeName(name = "lockKey")
    private String lockKey;

    @AttributeName(name = "jansHost")
    private String nodeIdentifier;

    @AttributeName(name = "jansNum")
    private Integer clusterNodeId;

    @AttributeName(name = "creationDate")
    private Date creationDate;

    @AttributeName(name = "jansLastUpd")
    private Date lastUpdate;

    @AttributeName(name = "exp")
    private Date expirationDate;

    @Expiration
    private Integer ttl;

    @AttributeName(name = "del")
    private Boolean deletable;

    @AttributeName(name = "jansStatus")
    private String status;

    @AttributeName(name = "jansData")
    private String data;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getLockKey() {
        return lockKey;
    }

    public void setLockKey(String lockKey) {
        this.lockKey = lockKey;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public void setNodeIdentifier(String nodeIdentifier) {
        this.nodeIdentifier = nodeIdentifier;
    }

    public Integer getClusterNodeId() {
        return clusterNodeId;
    }

    public void setClusterNodeId(Integer clusterNodeId) {
        this.clusterNodeId = clusterNodeId;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
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

    public Boolean getDeletable() {
        return deletable;
    }

    public void setDeletable(Boolean deletable) {
        this.deletable = deletable;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ClusterLock{" +
                "id='" + id + '\'' +
                ", task='" + task + '\'' +
                ", nodeIdentifier='" + nodeIdentifier + '\'' +
                ", clusterNodeId=" + clusterNodeId +
                ", creationDate=" + creationDate +
                ", lastUpdate=" + lastUpdate +
                ", expirationDate=" + expirationDate +
                ", status='" + status + '\'' +
                ", dn='" + getDn() + '\'' +
                "} ";
    }
}
