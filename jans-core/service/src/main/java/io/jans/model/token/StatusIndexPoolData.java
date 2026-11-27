package io.jans.model.token;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

import java.util.Date;

/**
 * Partial view of {@link StatusIndexPool} that maps only the status list data attributes. Merging it updates
 * "dat" without rewriting "exp" held by the same entry.
 *
 * @author Yuriy Z
 */
@DataEntry
@ObjectClass(value = "jansStatusIdxPool")
public class StatusIndexPoolData extends BaseEntry {

    private static final long serialVersionUID = -6093470412583472211L;

    @AttributeName(name = "dat")
    private String data;

    @AttributeName(name = "jansLastUpd")
    private Date lastUpdate;

    @AttributeName(name = "lockKey")
    private String lockKey;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getLockKey() {
        return lockKey;
    }

    public void setLockKey(String lockKey) {
        this.lockKey = lockKey;
    }
}
