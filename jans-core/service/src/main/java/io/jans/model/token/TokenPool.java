package io.jans.model.token;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;
import jakarta.persistence.Transient;

/**
 * @author Yuriy Movchan
 * @version 1.0, 06/03/2024
 */
@DataEntry(sortBy = "jansNum")
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
    private TokenPoolStatus status;

    @AttributeName(name = "jansLastUpd")
    private Date lastUpdate;

    @AttributeName(name = "exp")
    private Date expirationDate;

    @AttributeName(name = "lockKey")
    private String lockKey;

    @Transient
    private transient Integer startIndex;

    @Transient
    private transient Integer endIndex;
    
    @Transient
    private transient AtomicInteger currentIndex = new AtomicInteger(-1);
    
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

	public TokenPoolStatus getStatus() {
		return status;
	}

	public void setStatus(TokenPoolStatus status) {
		this.status = status;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	public String getLockKey() {
		return lockKey;
	}

	public void setLockKey(String lockKey) {
		this.lockKey = lockKey;
	}

	public Integer getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(Integer startIndex) {
		this.startIndex = startIndex;
		this.currentIndex.set(startIndex);
	}

	public Integer getEndIndex() {
		return endIndex;
	}

	public void setEndIndex(Integer endIndex) {
		this.endIndex = endIndex;
	}
	
	public int nextIndex() {
		// This block not used for while and were expired already
		if ((expirationDate == null) || expirationDate.before(new Date())) {
			return -1;
		}

		int nextIndex = currentIndex.getAndIncrement();
		if (nextIndex > endIndex) {
			// Index out of pool range
			return -1;
		}
		
		// Correct index
		return nextIndex;
		
	}

}
