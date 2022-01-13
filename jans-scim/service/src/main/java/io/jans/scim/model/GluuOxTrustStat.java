/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model;

import java.io.Serializable;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.InumEntry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@DataEntry
@ObjectClass(value = "jansOxtrustStat")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GluuOxTrustStat extends InumEntry implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2684923415744323028L;

	@AttributeName(name = "jansFreeDiskSpace", updateOnly = true)
	private String freeDiskSpace;

	@AttributeName(name = "jansFreeMem", updateOnly = true)
	private String freeMemory;

	@AttributeName(name = "jansFreeSwap", updateOnly = true)
	private String freeSwap;

	@AttributeName(name = "jansGrpCount", updateOnly = true)
	private String groupCount;

	@AttributeName(name = "jansPersonCount", updateOnly = true)
	private String personCount;

	@AttributeName(name = "jansIpAddr", updateOnly = true)
	private String ipAddress;

	@AttributeName(name = "jansSystemUptime", updateOnly = true)
	private String systemUptime;

	@AttributeName(name = "jansLoadAvg", updateOnly = true)
	private String loadAvg;

	public String getFreeDiskSpace() {
		return freeDiskSpace;
	}

	public void setFreeDiskSpace(String freeDiskSpace) {
		this.freeDiskSpace = freeDiskSpace;
	}

	public String getFreeMemory() {
		return freeMemory;
	}

	public void setFreeMemory(String freeMemory) {
		this.freeMemory = freeMemory;
	}

	public String getFreeSwap() {
		return freeSwap;
	}

	public void setFreeSwap(String freeSwap) {
		this.freeSwap = freeSwap;
	}

	public String getGroupCount() {
		return groupCount;
	}

	public void setGroupCount(String groupCount) {
		this.groupCount = groupCount;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getLoadAvg() {
		return loadAvg;
	}

	public void setLoadAvg(String loadAvg) {
		this.loadAvg = loadAvg;
	}

	public String getPersonCount() {
		return personCount;
	}

	public void setPersonCount(String personCount) {
		this.personCount = personCount;
	}

	public String getSystemUptime() {
		return systemUptime;
	}

	public void setSystemUptime(String systemUptime) {
		this.systemUptime = systemUptime;
	}

	@Override
	public String toString() {
		return "GluuConfiguration [freeDiskSpace=" + freeDiskSpace + ", freeMemory=" + freeMemory + ", freeSwap="
				+ freeSwap + ", groupCount=" + groupCount + ", personCount=" + personCount + ", ipAddress=" + ipAddress
				+ ", systemUptime=" + systemUptime;
	}

}
