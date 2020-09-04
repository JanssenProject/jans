package org.gluu.configapi.util;

public class ApiEnumConverter<T extends Enum<T>> {

	Class<T> type;

	public ApiEnumConverter(Class<T> type) {
		this.type = type;
	}

	public Enum<T> convert(String text) {
		for (Enum<T> candidate : type.getEnumConstants()) {
			if (candidate.name().equalsIgnoreCase(text)) {
				return candidate;
			}
		}
		return null;
	}
}
