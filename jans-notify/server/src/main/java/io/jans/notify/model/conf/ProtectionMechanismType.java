package io.jans.notify.model.conf;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Yuriy Movchan
 * @version 05/11/2023
 */
public enum ProtectionMechanismType {

	DEFAULT("default", "DEFAULT"), EXTERNAL("external", "EXTERNAL");

	private String value;
	private String displayName;

	private static final Map<String, ProtectionMechanismType> mapByValues = new HashMap<String, ProtectionMechanismType>();

	static {
		for (ProtectionMechanismType enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	@JsonCreator
	public static ProtectionMechanismType forValues(String value) {
		return getByValue(value);
	}

	/**
	 * 
	 * @param value
	 * @param displayName
	 */
	private ProtectionMechanismType(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static ProtectionMechanismType getByValue(String value) {
		return mapByValues.get(value);
	}

	@JsonValue
	@Override
	public String toString() {
		return value;
	}
}
