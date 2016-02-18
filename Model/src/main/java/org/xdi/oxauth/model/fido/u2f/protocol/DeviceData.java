/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.xdi.oxauth.model.fido.u2f.protocol;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * FIDO U2F device data
 *
 * @author Yuriy Movchan Date: 02/16/2016
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceData implements Serializable {

	private static final long serialVersionUID = -8173244116167488365L;

	@JsonProperty(value = "uuid")
	private final String deviceUuid;

	@JsonProperty(value = "push_token")
	private final String deviceToken;

	@JsonProperty(value = "type")
	private final String deviceType;

	@JsonProperty(value = "name")
	private final String deviceName;

	@JsonProperty(value = "os_name")
	private final String osName;

	@JsonProperty(value = "os_version")
	private final String osVersion;

	public DeviceData(@JsonProperty(value = "uuid") String deviceUuid, @JsonProperty(value = "token")String deviceToken,
			@JsonProperty(value = "type") String deviceType, @JsonProperty(value = "name") String deviceName,
			@JsonProperty(value = "os_name") String osName, @JsonProperty(value = "os_version") String osVersion) {
		this.deviceUuid = deviceUuid;
		this.deviceToken = deviceToken;
		this.deviceType = deviceType;
		this.deviceName = deviceName;
		this.osName = osName;
		this.osVersion = osVersion;
	}

	public String getDeviceUuid() {
		return deviceUuid;
	}

	public String getDeviceToken() {
		return deviceToken;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public String getOsName() {
		return osName;
	}

	public String getOsVersion() {
		return osVersion;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DeviceData [deviceUuid=").append(deviceUuid).append(", deviceToken=").append(deviceToken).append(", deviceType=").append(deviceType)
				.append(", deviceName=").append(deviceName).append(", osName=").append(osName).append(", osVersion=").append(osVersion).append("]");
		return builder.toString();
	}

}
