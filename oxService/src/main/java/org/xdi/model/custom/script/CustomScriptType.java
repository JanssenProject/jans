/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model.custom.script;

import java.util.HashMap;
import java.util.Map;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;
import org.xdi.model.custom.script.model.CustomScript;
import org.xdi.model.custom.script.model.auth.AuthenticationCustomScript;
import org.xdi.model.custom.script.type.BaseExternalType;
import org.xdi.model.custom.script.type.auth.DummyPersonAuthenticationType;
import org.xdi.model.custom.script.type.auth.PersonAuthenticationType;
import org.xdi.model.custom.script.type.client.ClientRegistrationType;
import org.xdi.model.custom.script.type.client.DummyClientRegistrationType;
import org.xdi.model.custom.script.type.id.DummyIdGeneratorType;
import org.xdi.model.custom.script.type.id.IdGeneratorType;
import org.xdi.model.custom.script.type.session.ApplicationSessionType;
import org.xdi.model.custom.script.type.session.DummyApplicationSessionType;
import org.xdi.model.custom.script.type.uma.AuthorizationPolicyType;
import org.xdi.model.custom.script.type.uma.DummyAuthorizationPolicyType;
import org.xdi.model.custom.script.type.user.CacheRefreshType;
import org.xdi.model.custom.script.type.user.DummyCacheRefreshType;
import org.xdi.model.custom.script.type.user.DummyUpdateUserType;
import org.xdi.model.custom.script.type.user.DummyUserRegistrationType;
import org.xdi.model.custom.script.type.user.UpdateUserType;
import org.xdi.model.custom.script.type.user.UserRegistrationType;

/**
 * List of supported custom scripts
 *
 * @author Yuriy Movchan Date: 11/11/2014
 */
public enum CustomScriptType implements LdapEnum {
	
	PERSON_AUTHENTICATION("person_authentication", "Person Authentication", PersonAuthenticationType.class, AuthenticationCustomScript.class, "PersonAuthentication", new DummyPersonAuthenticationType()),
	APPLICATION_SESSION("application_session", "Application Session", ApplicationSessionType.class, CustomScript.class, "ApplicationSession", new DummyApplicationSessionType()),
	CACHE_REFRESH("cache_refresh", "Cache Refresh", CacheRefreshType.class, CustomScript.class, "CacheRefresh", new DummyCacheRefreshType()),
	UPDATE_USER("update_user", "Update User", UpdateUserType.class, CustomScript.class, "UpdateUser", new DummyUpdateUserType()),
	USER_REGISTRATION("user_registration", "User Registration", UserRegistrationType.class, CustomScript.class, "UserRegistration", new DummyUserRegistrationType()),
	CLIENT_REGISTRATION("client_registration", "Client Registration", ClientRegistrationType.class, CustomScript.class, "ClientRegistration", new DummyClientRegistrationType()),
	ID_GENERATOR("id_generator", "Id Generator", IdGeneratorType.class, CustomScript.class, "IdGenerator", new DummyIdGeneratorType()),
	UMA_AUTHORIZATION_POLICY("uma_authorization_policy", "UMA Authorization Policies", AuthorizationPolicyType.class, CustomScript.class, "AuthorizationPolicy", new DummyAuthorizationPolicyType()),
	DYNAMIC_SCOPE("dynamic_scope", "Dynamic Scopes", AuthorizationPolicyType.class, CustomScript.class, "AuthorizationPolicy", new DummyAuthorizationPolicyType());

	private String value;
	private String displayName;
	private Class<? extends BaseExternalType> customScriptType;
	private Class<? extends CustomScript> customScriptModel;
	private String pythonClass;
	private BaseExternalType defaultImplementation;
	
	private static Map<String, CustomScriptType> mapByValues = new HashMap<String, CustomScriptType>();
	static {
		for (CustomScriptType enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private CustomScriptType(String value, String displayName, Class<? extends BaseExternalType> customScriptType, Class<? extends CustomScript> customScriptModel, String pythonClass, BaseExternalType defaultImplementation) {
		this.displayName = displayName;
		this.value = value;
		this.customScriptType = customScriptType;
		this.customScriptModel = customScriptModel;
		this.pythonClass = pythonClass;
		this.defaultImplementation = defaultImplementation;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public Class<? extends BaseExternalType> getCustomScriptType() {
		return customScriptType;
	}

	public Class<? extends CustomScript> getCustomScriptModel() {
		return customScriptModel;
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
