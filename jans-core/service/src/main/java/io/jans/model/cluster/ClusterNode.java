package io.jans.model.cluster;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

import java.util.Date;

/**
 * @author Yuriy Movchan
 * @version 1.0, 06/03/2024
 */
@DataEntry(sortBy = "jansNum")
@ObjectClass(value = "jansNode")
public class ClusterNode extends BaseEntry {

	private static final long serialVersionUID = -2122431771066187529L;

	@AttributeName(ignoreDuringUpdate = true, name = "jansNum")
    private Integer id;

    @AttributeName(name = "jansType")
    private String type;

    @AttributeName(name = "creationDate")
    private Date creationDate;

    @AttributeName(name = "jansLastUpd")
    private Date lastUpdate;

    @AttributeName(name = "lockKey")
    private String lockKey;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	// workaround: io.jans.orm.exception.PropertyNotFoundException: Could not find a getter for jansNum in class io.jans.model.cluster.ClusterNode
    //  at io.jans.orm.reflect.property.BasicPropertyAccessor.createGetter(BasicPropertyAccessor.java:214)
	public Integer getJansNum() {
	    return getId();
    }

    // workaround: io.jans.orm.exception.PropertyNotFoundException: Could not find a getter for jansNum in class io.jans.model.cluster.ClusterNode
    //  at io.jans.orm.reflect.property.BasicPropertyAccessor.createGetter(BasicPropertyAccessor.java:214)
    public void setJansNum(Integer id) {
        setId(id);
    }

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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

	public String getLockKey() {
		return lockKey;
	}

	public void setLockKey(String lockKey) {
		this.lockKey = lockKey;
	}

    @Override
    public String toString() {
        return "ClusterNode{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", creationDate=" + creationDate +
                ", lastUpdate=" + lastUpdate +
                ", lockKey='" + lockKey + '\'' +
                ", dn='" + getDn() + '\'' +
                "} ";
    }
}
