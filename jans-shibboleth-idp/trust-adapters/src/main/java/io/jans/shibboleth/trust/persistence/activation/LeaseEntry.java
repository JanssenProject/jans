package io.jans.shibboleth.trust.persistence.activation;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

import java.util.Date;

/**
 * jans-orm storage entry for a {@code Lease}. Object class {@code jansWorkItemLease}, under
 * {@code ou=workItemLeases,o=jans}. The primary key is the DN, formed as
 * {@code inum=<name-uuid>,ou=workItemLeases,o=jans}, where {@code inum} is a deterministic name-based UUID of
 * {@code (workItemRef, generation)} — so two workers racing for the same generation derive the same inum and
 * collide, letting the store's identity uniqueness elect a single winner.
 *
 * <p>Because the inum is an opaque hash, the identifying fields are also stored as their own queryable columns
 * ({@code jansWorkItemRef}, {@code jansLeaseGen}) — that is what a read reconstructs the lease from, not the inum.
 */
@DataEntry(sortBy = "jansLeaseGen", sortByName = "jansLeaseGen")
@ObjectClass("jansWorkItemLease")
public class LeaseEntry extends BaseEntry {

    private static final long serialVersionUID = 1L;
    
    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "jansWorkItemRef")
    private String workItemRef;

    @AttributeName(name = "jansLeaseGen")
    private Integer generation;

    @AttributeName(name = "jansLeaseWorker")
    private String worker;

    @AttributeName(name = "jansLeaseGrantedAt")
    private Date grantedAt;

    @AttributeName(name = "jansLeaseExpiresAt")
    private Date expiresAt;

    public String getInum() {

        return inum;
    }

    public void setInum(String inum) {

        this.inum = inum;
    }

    public String getWorkItemRef() {

        return workItemRef;
    }

    public void setWorkItemRef(String workItemRef) {

        this.workItemRef = workItemRef;
    }

    public Integer getGeneration() {

        return generation;
    }

    public void setGeneration(Integer generation) {

        this.generation = generation;
    }

    public String getWorker() {

        return worker;
    }

    public void setWorker(String worker) {

        this.worker = worker;
    }

    public Date getGrantedAt() {

        return grantedAt;
    }

    public void setGrantedAt(Date grantedAt) {

        this.grantedAt = grantedAt;
    }

    public Date getExpiresAt() {

        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {

        this.expiresAt = expiresAt;
    }
}
