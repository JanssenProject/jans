package io.jans.shibboleth.trust.persistence.activation;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

/**
 * jans-orm storage entry for a {@code Worker}. Object class {@code jansActivationWorker}, under
 * {@code ou=activationWorkers,o=jans}. The primary key is the DN, formed as
 * {@code inum=<origin>,ou=activationWorkers,o=jans}; the worker's id is its caller-supplied origin string.
 * Timestamps are stored as ISO-8601 strings (UTC).
 */
@DataEntry(sortBy = "jansLastHeartbeatAt", sortByName = "jansLastHeartbeatAt")
@ObjectClass("jansActivationWorker")
public class WorkerEntry extends BaseEntry {

    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "jansRegisteredAt")
    private String registeredAt;

    @AttributeName(name = "jansLastHeartbeatAt")
    private String lastHeartbeatAt;

    public String getInum() {

        return inum;
    }

    public void setInum(String inum) {

        this.inum = inum;
    }

    public String getRegisteredAt() {

        return registeredAt;
    }

    public void setRegisteredAt(String registeredAt) {

        this.registeredAt = registeredAt;
    }

    public String getLastHeartbeatAt() {

        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(String lastHeartbeatAt) {

        this.lastHeartbeatAt = lastHeartbeatAt;
    }
}
