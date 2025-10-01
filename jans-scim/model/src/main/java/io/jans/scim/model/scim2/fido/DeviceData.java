package io.jans.scim.model.scim2.fido;

import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.AttributeDefinition;

public class DeviceData {

	@Attribute(description = "", mutability = AttributeDefinition.Mutability.IMMUTABLE)
	private String uuid;

	@Attribute(description = "", mutability = AttributeDefinition.Mutability.IMMUTABLE)
	private String pushToken;

	@Attribute(description = "", mutability = AttributeDefinition.Mutability.IMMUTABLE)
	private String type;

	@Attribute(description = "", mutability = AttributeDefinition.Mutability.IMMUTABLE)
	private String platform;

	@Attribute(description = "", mutability = AttributeDefinition.Mutability.IMMUTABLE)
	private String name;

	@Attribute(description = "", mutability = AttributeDefinition.Mutability.IMMUTABLE)
	private String osName;

	@Attribute(description = "", mutability = AttributeDefinition.Mutability.IMMUTABLE)
	private String osVersion;

    @Attribute(description = "", mutability = AttributeDefinition.Mutability.IMMUTABLE)
    private String customData;

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

	public String getCustomData() {
        return customData;
    }

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public void setPushToken(String pushToken) {
		this.pushToken = pushToken;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOsName(String osName) {
		this.osName = osName;
	}

	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}

	public void setCustomData(String customData) {
        this.customData = customData;
    }

}
