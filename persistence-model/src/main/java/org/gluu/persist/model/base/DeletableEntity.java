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
    @AttributeName(name = "exp")
    private Date newExpirationDate;
    @AttributeName(name = "del")
    private Boolean deletable;

    @Override
    public Boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(Boolean deletable) {
        this.deletable = deletable;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Date getNewExpirationDate() {
		return newExpirationDate;
	}

	public void setNewExpirationDate(Date newExpirationDate) {
		this.newExpirationDate = newExpirationDate;
	}

	public boolean canDelete() {
        return canDelete(new Date());
    }

    public boolean canDelete(Date now) {
    	Date exp = expirationDate != null ? expirationDate :  newExpirationDate;
        return deletable != null && deletable && (exp == null || exp.before(now));
    }

    @Override
    public String toString() {
    	Date exp = expirationDate != null ? expirationDate :  newExpirationDate;
        return "DeletableEntity{" +
                "expirationDate=" + exp +
                ", deletable=" + deletable +
                "} " + super.toString();
    }
}
