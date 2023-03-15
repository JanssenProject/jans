/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model.fido2;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FIDO2 U2F device data
 *
 * @author Yuriy Movchan Date: 02/16/2016
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fido2DeviceData implements Serializable {

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

    @JsonProperty(value = "custom_data")
    private final Map<String, String> customData;

	public Fido2DeviceData(@JsonProperty(value = "uuid") String uuid, @JsonProperty(value = "token") String pushToken,
			@JsonProperty(value = "type") String type, @JsonProperty(value = "platform") String platform,
			@JsonProperty(value = "name") String name, @JsonProperty(value = "os_name") String osName,
			@JsonProperty(value = "os_version") String osVersion, @JsonProperty(value = "custom_data") Map<String, String> customData) {
		this.uuid = uuid;
		this.pushToken = pushToken;
		this.type = type;
		this.platform = platform;
		this.name = name;
		this.osName = osName;
		this.osVersion = osVersion;
        this.customData = customData;
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

	public final Map<String, String> getCustomData() {
        return customData;
    }

    @Override
    public String toString() {
        return "DeviceData [uuid=" + uuid + ", pushToken=" + pushToken + ", type=" + type + ", platform=" + platform + ", name=" + name + ", osName="
                + osName + ", osVersion=" + osVersion + ", customData=" + customData + "]";
    }

}
