package org.gluu.oxauth.client.util;

import org.xdi.util.properties.FileConfiguration;

/**
 * oAuth properties and constants
 * 
 * @author Yuriy Movchan
 * @version 0.1, 03/20/2013
 */
public final class Configuration {
	
    /**
     * Represents the constant for where the OAuth data will be located in memory
     */
	public static final String SESSION_OAUTH_DATA = "_oauth_data_";

	/**
	 * OAuth constants
	 */
	public static final String OAUTH_CLIENT_ID = "client_id";
	public static final String OAUTH_CLIENT_PASSWORD = "client_password";
	public static final String OAUTH_CLIENT_CREDENTIALS = "client_credentials";
	public static final String OAUTH_REDIRECT_URI = "redirect_uri";
	public static final String OAUTH_RESPONSE_TYPE = "response_type";
	public static final String OAUTH_SCOPE = "scope";
	public static final String OAUTH_STATE = "state";
	public static final String OAUTH_CODE = "code";
	public static final String OAUTH_ID_TOKEN = "id_token";
	public static final String OAUTH_ERROR = "error";
	public static final String OAUTH_NONCE = "nonce";
	public static final String OAUTH_ERROR_DESCRIPTION = "error_description";
	public static final String OAUTH_ACCESS_TOKEN = "access_token";
	public static final String OAUTH_AUTH_MODE = "auth_mode";
	public static final String OAUTH_AUTH_LEVEL = "auth_level";
    public static final String OAUTH_ID_TOKEN_HINT = "id_token_hint";
    public static final String OAUTH_POST_LOGOUT_REDIRECT_URI = "post_logout_redirect_uri";

	/**
	 * OAuth properties
	 */
	public static final String OAUTH_PROPERTY_AUTHORIZE_URL = "oxauth.authorize.url";
	public static final String OAUTH_PROPERTY_TOKEN_URL = "oxauth.token.url";
	public static final String OAUTH_PROPERTY_TOKEN_VALIDATION_URL = "oxauth.token.validation.url";
	public static final String OAUTH_PROPERTY_CHECKSESSION_URL = "oxauth.checksession.url";
	public static final String OAUTH_PROPERTY_USERINFO_URL = "oxauth.userinfo.url";
	public static final String OAUTH_PROPERTY_LOGOUT_URL = "oxauth.logout.url";
	public static final String OAUTH_PROPERTY_CLIENT_ID = "oxauth.client.id";
	public static final String OAUTH_PROPERTY_CLIENT_PASSWORD = "oxauth.client.password";
	public static final String OAUTH_PROPERTY_CLIENT_SCOPE = "oxauth.client.scope";

	/**
	 * Configuration files
	 */
	public static final String CONFIGURATION_FILE_APPLICATION_CONFIGURATION = "oxTrust.properties";

	private static class ConfigurationSingleton {
		static Configuration INSTANCE = new Configuration();
	}

	private FileConfiguration applicationConfiguration;

	private Configuration() {
		applicationConfiguration = new FileConfiguration(CONFIGURATION_FILE_APPLICATION_CONFIGURATION);
	}

	public static Configuration instance() {
		return ConfigurationSingleton.INSTANCE;
	}

	public String getPropertyValue(String propertyName) {
		return applicationConfiguration.getString(propertyName);
	}

}
