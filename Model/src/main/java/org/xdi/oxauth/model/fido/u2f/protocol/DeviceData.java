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
	private final String uuid;

	@JsonProperty(value = "push_token")
	private final String pushToken;

	@JsonProperty(value = "type")
	private final String type;

	@JsonProperty(value = "platform")
	private final String platform;

	@JsonProperty(value = "name")
	private final String name;

	@JsonProperty(value = "os_name")
	private final String osName;

	@JsonProperty(value = "os_version")
	private final String osVersion;

	public DeviceData(@JsonProperty(value = "uuid") String uuid, @JsonProperty(value = "token") String pushToken,
			@JsonProperty(value = "type") String type, @JsonProperty(value = "platform") String platform,
			@JsonProperty(value = "name") String name, @JsonProperty(value = "os_name") String osName,
			@JsonProperty(value = "os_version") String osVersion) {
		this.uuid = uuid;
		this.pushToken = pushToken;
		this.type = type;
		this.platform = platform;
		this.name = name;
		this.osName = osName;
		this.osVersion = osVersion;
	}

	public String getUuid() {
		return uuid;
	}

	public String getPushToken() {
		return pushToken;
	}

	public String getType() {
		return type;
	}

	public String getPlatform() {
		return platform;
	}

	public String getName() {
		return name;
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
		builder.append("DeviceData [uuid=").append(uuid).append(", pushToken=").append(pushToken).append(", type=").append(type).append(", platform=")
				.append(platform).append(", name=").append(name).append(", osName=").append(osName).append(", osVersion=").append(osVersion).append("]");
		return builder.toString();
	}

}
