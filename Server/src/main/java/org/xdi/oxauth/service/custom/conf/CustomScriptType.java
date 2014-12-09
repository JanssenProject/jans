/*
 /*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.custom.conf;

import java.util.HashMap;
import java.util.Map;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;
import org.xdi.oxauth.service.custom.interfaces.BaseExternalType;
import org.xdi.oxauth.service.custom.interfaces.auth.CustomAuthenticatorType;
import org.xdi.oxauth.service.custom.interfaces.auth.DummyCustomAuthenticatorType;
import org.xdi.oxauth.service.custom.interfaces.client.ClientRegistrationType;
import org.xdi.oxauth.service.custom.interfaces.client.DummyClientRegistrationType;

/**
 * List of supported custom scripts
 *
 * @author Yuriy Movchan Date: 11/11/2014
 */
public enum CustomScriptType implements LdapEnum {
	
	CUSTOM_AUTHENTICATION("custom_authentication", CustomAuthenticatorType.class, "ExternalAuthenticator", new DummyCustomAuthenticatorType()),
	CLIENT_REGISTRATION("client_registration", ClientRegistrationType.class, "ClientRegistration", new DummyClientRegistrationType());

	private String value;
	private Class<? extends BaseExternalType> customScriptType;
	private String pythonClass;
	private BaseExternalType defaultImplementation;

	private static Map<String, CustomScriptType> mapByValues = new HashMap<String, CustomScriptType>();
	static {
		for (CustomScriptType enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private CustomScriptType(String value, Class<? extends BaseExternalType> customScriptType, String pythonClass, BaseExternalType defaultImplementation) {
		this.value = value;
		this.customScriptType = customScriptType;
		this.pythonClass = pythonClass;
		this.defaultImplementation = defaultImplementation;
	}

	public String getValue() {
		return value;
	}

	public Class<? extends BaseExternalType> getCustomScriptType() {
		return customScriptType;
	}

	public String getPythonClass() {
		return pythonClass;
	}

	public BaseExternalType getDefaultImplementation() {
		return defaultImplementation;
	}

	public static CustomScriptType getByValue(String value) {
		return mapByValues.get(value);
	}

	public Enum<? extends LdapEnum> resolveByValue(String value) {
		return getByValue(value);
	}

	@Override
	public String toString() {
		return value;
	}

}
