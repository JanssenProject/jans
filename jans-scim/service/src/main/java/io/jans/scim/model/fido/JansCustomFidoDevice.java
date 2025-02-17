/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.fido;

import io.jans.orm.model.base.Entry;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

/**
 * @author Val Pecaoco
 */
@DataEntry(sortBy = { "id" }, sortByName = { "jansId" })
@ObjectClass(value = "jansDeviceRegistration")
public class JansCustomFidoDevice extends Entry {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4463359164739925541L;

	@AttributeName(name = "jansId", ignoreDuringUpdate = true)
	private String id;

	@AttributeName(name = "creationDate", ignoreDuringUpdate = true)
	private String creationDate;

	@AttributeName(name = "jansApp", ignoreDuringUpdate = true)
	private String application;

	@AttributeName(name = "jansCounter", ignoreDuringUpdate = true)
	private String counter;

	@AttributeName(name = "jansDeviceData", ignoreDuringUpdate = true)
	private String deviceData;

	@AttributeName(name = "jansDeviceHashCode", ignoreDuringUpdate = true)
	private String deviceHashCode;

	@AttributeName(name = "jansDeviceKeyHandle", ignoreDuringUpdate = true)
	private String deviceKeyHandle;

	@AttributeName(name = "jansDeviceRegistrationConf", ignoreDuringUpdate = true)
	private String deviceRegistrationConf;

	@AttributeName(name = "jansLastAccessTime", ignoreDuringUpdate = true)
	private String lastAccessTime;

	@AttributeName(name = "jansStatus")
	private String status;

	@AttributeName(name = "displayName")
	private String displayName;

	@AttributeName(name = "description")
	private String description;

	@AttributeName(name = "jansNickName")
	private String nickname;

	@AttributeName(name = "jansMetaLastMod")
	private String metaLastModified;

	@AttributeName(name = "jansMetaLocation")
	private String metaLocation;

	@AttributeName(name = "jansMetaVer")
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
