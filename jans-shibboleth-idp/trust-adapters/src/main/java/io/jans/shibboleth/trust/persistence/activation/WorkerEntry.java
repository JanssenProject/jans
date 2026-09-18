package io.jans.shibboleth.trust.persistence.activation;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

import java.util.Date;

/**
 * jans-orm storage entry for a {@code Worker}. Object class {@code jansTrustActivationWorker}, under
 * {@code ou=trustActivationWorkers,o=jans}. The primary key is the DN, formed as
 * {@code inum=<name-uuid>,ou=trustActivationWorkers,o=jans}, where {@code inum} is a deterministic name-based
 * UUID of the worker's origin — a stable, uniformly shaped id like every other entry, rather than the raw
 * origin string. The origin itself is stored in {@code jansWorkerOrigin}, from which the worker id is rebuilt.
 * Timestamps are stored as native timestamps, letting jans-orm own the date codec.
 */
@DataEntry(sortBy = "jansLastHeartbeatAt", sortByName = "jansLastHeartbeatAt")
@ObjectClass("jansTrustActivationWorker")
public class WorkerEntry extends BaseEntry {

    private static final long serialVersionUID = 1L;

    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "jansWorkerOrigin")
    private String origin;

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

    public String getOrigin() {

        return origin;
    }

    public void setOrigin(String origin) {

        this.origin = origin;
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
