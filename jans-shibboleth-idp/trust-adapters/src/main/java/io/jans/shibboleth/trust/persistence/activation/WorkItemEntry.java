package io.jans.shibboleth.trust.persistence.activation;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

import java.util.Date;

/**
 * jans-orm storage entry for a {@code WorkItem}. Object class {@code jansWorkItem}, under
 * {@code ou=workItems,o=jans}. The primary key is the DN ({@code @DN}, inherited from {@link BaseEntry}),
 * formed as {@code inum=<uuid>,ou=workItems,o=jans}; {@code inum} is a random work-item id.
 *
 * <p>{@code jansWorkItemStatus} stores only the terminal flag ({@code COMPLETED}/{@code CANCELLED}); a null
 * status is a non-terminal item whose {@code PENDING}/{@code ASSIGNED} state is derived from lease presence.
 * Timestamps are stored as native timestamps, letting jans-orm own the date codec.
 */
@DataEntry(sortBy = "jansCreatedAt", sortByName = "jansCreatedAt")
@ObjectClass("jansWorkItem")
public class WorkItemEntry extends BaseEntry {

    private static final long serialVersionUID = 1L;
    
    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "jansWorkItemType")
    private String type;

    @AttributeName(name = "jansTrId")
    private String trustRelationshipId;

    @AttributeName(name = "jansWorkItemStatus")
    private String status;

    @AttributeName(name = "jansCreatedAt")
    private Date createdAt;

    @AttributeName(name = "jansLastTransitionAt")
    private Date lastTransitionAt;

    public String getInum() {

        return inum;
    }

    public void setInum(String inum) {

        this.inum = inum;
    }

    public String getType() {

        return type;
    }

    public void setType(String type) {

        this.type = type;
    }

    public String getTrustRelationshipId() {

        return trustRelationshipId;
    }

    public void setTrustRelationshipId(String trustRelationshipId) {

        this.trustRelationshipId = trustRelationshipId;
    }

    public String getStatus() {

        return status;
    }

    public void setStatus(String status) {

        this.status = status;
    }

    public Date getCreatedAt() {

        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {

        this.createdAt = createdAt;
    }

    public Date getLastTransitionAt() {

        return lastTransitionAt;
    }

    public void setLastTransitionAt(Date lastTransitionAt) {

        this.lastTransitionAt = lastTransitionAt;
    }
}
