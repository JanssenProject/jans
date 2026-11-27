package io.jans.model.token;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

import java.util.Date;

/**
 * Partial view of {@link StatusIndexPool} that maps only the expiration attributes. Merging it updates
 * "exp" without rewriting the status list data held by the same entry.
 *
 * @author Yuriy Z
 */
@DataEntry
@ObjectClass(value = "jansStatusIdxPool")
public class StatusIndexPoolExpiration extends BaseEntry {

    private static final long serialVersionUID = 4217396655437163571L;

    @AttributeName(name = "exp")
    private Date expirationDate;

    @AttributeName(name = "jansLastUpd")
    private Date lastUpdate;

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
