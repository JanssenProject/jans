package io.jans.shibboleth.trust.persistence.activation;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

import java.util.Date;

/**
 * jans-orm storage entry for a {@code Worker}. Object class {@code jansActivationWorker}, under
 * {@code ou=activationWorkers,o=jans}. The primary key is the DN, formed as
 * {@code inum=<origin>,ou=activationWorkers,o=jans}; the worker's id is its caller-supplied origin string.
 * Timestamps are stored as native timestamps, letting jans-orm own the date codec.
 */
@DataEntry(sortBy = "jansLastHeartbeatAt", sortByName = "jansLastHeartbeatAt")
@ObjectClass("jansActivationWorker")
public class WorkerEntry extends BaseEntry {

    private static final long serialVersionUID = 1L;
    
    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "jansRegisteredAt")
    private Date registeredAt;

    @AttributeName(name = "jansLastHeartbeatAt")
    private Date lastHeartbeatAt;

    public String getInum() {

        return inum;
    }

    public void setInum(String inum) {

        this.inum = inum;
    }

    public Date getRegisteredAt() {

        return registeredAt;
    }

    public void setRegisteredAt(Date registeredAt) {

        this.registeredAt = registeredAt;
    }

    public Date getLastHeartbeatAt() {

        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Date lastHeartbeatAt) {

        this.lastHeartbeatAt = lastHeartbeatAt;
    }
}
