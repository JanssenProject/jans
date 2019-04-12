package org.gluu.oxauth.model.common;

import java.util.HashMap;
import java.util.Map;

import org.gluu.persist.annotation.AttributeEnum;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version 0.9, 09/02/2015
 */

public enum SessionIdState  implements AttributeEnum {

    UNAUTHENTICATED("unauthenticated"), AUTHENTICATED("authenticated");

    private final String value;

    private static Map<String, SessionIdState> mapByValues = new HashMap<String, SessionIdState>();

	static {
		for (SessionIdState enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

    private SessionIdState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

	public static SessionIdState getByValue(String value) {
		return mapByValues.get(value);
	}

	public Enum<? extends AttributeEnum> resolveByValue(String value) {
		return getByValue(value);
	}

	@Override
	public String toString() {
		return value;
	}

}
