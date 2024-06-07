package io.jans.model.token;

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
@ObjectClass(value = "jansTokenPool")
public class TokenPool extends BaseEntry {

	private static final long serialVersionUID = -2122431771066187529L;

	@AttributeName(ignoreDuringUpdate = true, name = "jansNum")
    private Integer id;

	@AttributeName(ignoreDuringUpdate = true, name = "jansNodeId")
    private Integer nodeId;

    @AttributeName(name = "dat")
    private String data;

    @AttributeName(name = "tokenStatus")
    private TokenStatus status;

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

	public Integer getNodeId() {
		return nodeId;
	}

	public void setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public TokenStatus getStatus() {
		return status;
	}

	public void setStatus(TokenStatus status) {
		this.status = status;
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
