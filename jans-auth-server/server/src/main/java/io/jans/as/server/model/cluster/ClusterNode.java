package io.jans.as.server.model.cluster;

import java.util.Date;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

/**
 * @author Yuriy Movchan
 * @version 1.0, 06/03/2024
 */
@DataEntry(sortBy = "jansId")
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
    private String lockkey;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
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

	public String getLockkey() {
		return lockkey;
	}

	public void setLockkey(String lockkey) {
		this.lockkey = lockkey;
	}

}
