/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.kc.spi.storage.util;

public class Constants {

    private Constants() {}

    public static final String PROVIDER_ID = "kc-jans-storage";
    public static final String JANS_CONFIG_PROP_PATH = "jans.config.prop.path";
    public static final String KEYCLOAK_USER = "/keycloak-user";
    public static final String BASE_URL = "https://localhost";
    
    public static final String SCOPE_TYPE_OPENID = "openid";
    public static final String UTF8_STRING_ENCODING = "UTF-8";
    public static final String CLIENT_SECRET_BASIC = "client_secret_basic";
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    public static final String RESOURCE_OWNER_PASSWORD_CREDENTIALS = "password";

    //properties
    public static final String KEYCLOAK_SERVER_URL = "keycloak.server.url";
    public static final String AUTH_TOKEN_ENDPOINT = "auth.token.endpoint";
    public static final String SCIM_USER_ENDPOINT = "scim.user.endpoint";
    public static final String SCIM_USER_SEARCH_ENDPOINT = "scim.user.search.endpoint";
    public static final String SCIM_OAUTH_SCOPE = "scim.oauth.scope";
    public static final String KEYCLOAK_SCIM_CLIENT_ID = "keycloak.scim.client.id";
    public static final String KEYCLOAK_SCIM_CLIENT_PASSWORD = "keycloak.scim.client.password";
    

}