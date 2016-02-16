/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.xdi.oxauth.model.fido.u2f.protocol;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FIDO U2F device data
 *
 * @author Yuriy Movchan Date: 02/16/2016
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceData implements Serializable {

	private static final long serialVersionUID = -8173244116167488365L;

	@JsonProperty(value = "device_uuid")
	private String deviceUuid;

	@JsonProperty(value = "device_token")
	private String deviceToken;

	@JsonProperty(value = "device_type")
	private String deviceType;

	@JsonProperty(value = "device_name")
	private String deviceName;

	@JsonProperty(value = "os_name")
	private String osName;

	@JsonProperty(value = "os_version")
	private String osVersion;

	public DeviceData() {
	}

	public String getDeviceUuid() {
		return deviceUuid;
	}

	public void setDeviceUuid(String deviceUuid) {
		this.deviceUuid = deviceUuid;
	}

	public String getDeviceToken() {
		return deviceToken;
	}

	public void setDeviceToken(String deviceToken) {
		this.deviceToken = deviceToken;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getOsName() {
		return osName;
	}

	public void setOsName(String osName) {
		this.osName = osName;
	}

	public String getOsVersion() {
		return osVersion;
	}

	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DeviceData [deviceUuid=").append(deviceUuid).append(", deviceToken=").append(deviceToken).append(", deviceType=").append(deviceType)
				.append(", deviceName=").append(deviceName).append(", osName=").append(osName).append(", osVersion=").append(osVersion).append("]");
		return builder.toString();
	}

}
