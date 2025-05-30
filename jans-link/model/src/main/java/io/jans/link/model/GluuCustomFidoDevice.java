/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package io.jans.link.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

/**
 * @author Val Pecaoco
 */
@DataEntry(sortBy = { "id" }, sortByName = { "oxId" })
@ObjectClass(value = "oxDeviceRegistration")
public class GluuCustomFidoDevice extends Entry {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4463359164739925541L;

	@AttributeName(name = "oxId", ignoreDuringUpdate = true)
	private String id;

	@AttributeName(name = "creationDate", ignoreDuringUpdate = true)
	private String creationDate;

	@AttributeName(name = "oxApplication", ignoreDuringUpdate = true)
	private String application;

	@AttributeName(name = "oxCounter", ignoreDuringUpdate = true)
	private String counter;

	@AttributeName(name = "oxDeviceData", ignoreDuringUpdate = true)
	private String deviceData;

	@AttributeName(name = "oxDeviceHashCode", ignoreDuringUpdate = true)
	private String deviceHashCode;

	@AttributeName(name = "oxDeviceKeyHandle", ignoreDuringUpdate = true)
	private String deviceKeyHandle;

	@AttributeName(name = "oxDeviceRegistrationConf", ignoreDuringUpdate = true)
	private String deviceRegistrationConf;

	@AttributeName(name = "oxLastAccessTime", ignoreDuringUpdate = true)
	private String lastAccessTime;

	@AttributeName(name = "oxStatus")
	private String status;

	@AttributeName(name = "displayName")
	private String displayName;

	@AttributeName(name = "description")
	private String description;

	@AttributeName(name = "oxNickName")
	private String nickname;

	@AttributeName(name = "oxTrustMetaLastModified")
	private String metaLastModified;

	@AttributeName(name = "oxTrustMetaLocation")
	private String metaLocation;

	@AttributeName(name = "oxTrustMetaVersion")
	private String metaVersion;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getCounter() {
		return counter;
	}

	public void setCounter(String counter) {
		this.counter = counter;
	}

	public String getDeviceData() {
		return deviceData;
	}

	public void setDeviceData(String deviceData) {
		this.deviceData = deviceData;
	}

	public String getDeviceHashCode() {
		return deviceHashCode;
	}

	public void setDeviceHashCode(String deviceHashCode) {
		this.deviceHashCode = deviceHashCode;
	}

	public String getDeviceKeyHandle() {
		return deviceKeyHandle;
	}

	public void setDeviceKeyHandle(String deviceKeyHandle) {
		this.deviceKeyHandle = deviceKeyHandle;
	}

	public String getDeviceRegistrationConf() {
		return deviceRegistrationConf;
	}

	public void setDeviceRegistrationConf(String deviceRegistrationConf) {
		this.deviceRegistrationConf = deviceRegistrationConf;
	}

	public String getLastAccessTime() {
		return lastAccessTime;
	}

	public void setLastAccessTime(String lastAccessTime) {
		this.lastAccessTime = lastAccessTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getMetaLastModified() {
		return metaLastModified;
	}

	public void setMetaLastModified(String metaLastModified) {
		this.metaLastModified = metaLastModified;
	}

	public String getMetaLocation() {
		return metaLocation;
	}

	public void setMetaLocation(String metaLocation) {
		this.metaLocation = metaLocation;
	}

	public String getMetaVersion() {
		return metaVersion;
	}

	public void setMetaVersion(String metaVersion) {
		this.metaVersion = metaVersion;
	}
}
