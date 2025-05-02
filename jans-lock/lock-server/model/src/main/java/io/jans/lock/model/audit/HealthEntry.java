package io.jans.lock.model.audit;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

@DataEntry(sortByName = "eventTime")
@ObjectClass(value = "jansHealthEntry")
public class HealthEntry extends BaseEntry implements Serializable {

	private static final long serialVersionUID = -5080284664895065766L;

	@JsonProperty("inum")
	@AttributeName(name = "inum", ignoreDuringUpdate = true)
	private String inum;

	@AttributeName(name = "creationDate")
	private Date creationDate;

    @AttributeName(name = "eventTime")
    private Date eventTime;

	@AttributeName(name = "jansService")
	private String service;

	@AttributeName(name = "jansNodeName")
	private String nodeName;

	@AttributeName(name = "jansStatus")
	private String status;

	// Details: cedarEngineStatus, cedarPolicyStatus, tokenDataStatus. etc..
	@JsonObject
	@AttributeName(name = "engineStatus")
	private String engineStatus;

	public String getInum() {
		return inum;
	}

	public void setInum(String inum) {
		this.inum = inum;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public Date getEventTime() {
		return eventTime;
	}

	public void setEventTime(Date eventTime) {
		this.eventTime = eventTime;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getEngineStatus() {
		return engineStatus;
	}

	public void setEngineStatus(String engineStatus) {
		this.engineStatus = engineStatus;
	}

	@Override
	public String toString() {
		return "HealthEntry [inum=" + inum + ", creationDate=" + creationDate + ", eventTime=" + eventTime
				+ ", service=" + service + ", nodeName=" + nodeName + ", status=" + status + ", engineStatus="
				+ engineStatus + ", toString()=" + super.toString() + "]";
	}

}
