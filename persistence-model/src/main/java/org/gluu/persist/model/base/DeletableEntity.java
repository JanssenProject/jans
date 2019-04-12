package org.gluu.persist.model.base;

import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DataEntry;

import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 */
@DataEntry
public class DeletableEntity extends BaseEntry implements Deletable {

    @AttributeName(name = "oxAuthExpiration")
    private Date expirationDate;
    @AttributeName(name = "oxDeletable")
    private boolean deletable = true;

    @Override
    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean canDelete() {
        return canDelete(new Date());
    }

    public boolean canDelete(Date now) {
        return deletable && (expirationDate == null || expirationDate.before(now));
    }

    @Override
    public String toString() {
        return "DeletableEntity{" +
                "expirationDate=" + expirationDate +
                ", deletable=" + deletable +
                "} " + super.toString();
    }
}
