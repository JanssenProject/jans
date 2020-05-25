package org.gluu.oxauthconfigapi.util;

public class ApiStringUtils {

	private ApiStringUtils() {
	}
	
	
	public static boolean isEmptyOrNull(String value) {
		return value == null || value.isEmpty();
	}

}
