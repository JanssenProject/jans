/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.kc.idp.broker.util;

public class Constants {

    private Constants() {}

    public static final String JANS_KC_IDP_CONFIG_PROP_PATH = "jans.kc.idp.prop.path";
    public static final String KEYCLOAK_USER = "/keycloak-user";
    public static final String BASE_URL = "https://localhost";
    
    public static final String KEYCLOAK= "/kc";
    public static final String IDENTITY_PROVIDER = "/identity-provider";
    public static final String SAML_PATH = "/saml";

    public static final String IDP_PATH = "/idp";
    public static final String INUM_PARAM_PATH = "/{inum}";
    public static final String NAME_PARAM_PATH = "/{name}";

    public static final String SAML = "saml";
    public static final String IDP = "idp";
    public static final String ID = "id";
    

    public static final String UTF8_STRING_ENCODING = "UTF-8";
    public static final String CLIENT_SECRET_BASIC = "client_secret_basic";
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    public static final String RESOURCE_OWNER_PASSWORD_CREDENTIALS = "password";

    //properties
    public static final String KEYCLOAK_SERVER_URL = "keycloak.server.url";
    public static final String AUTH_TOKEN_ENDPOINT = "auth.token.endpoint";
    public static final String KEYCLOAK_SCIM_CLIENT_ID = "keycloak.scim.client.id";
    public static final String KEYCLOAK_SCIM_CLIENT_PASSWORD = "keycloak.scim.client.password";
    
    
    public static final String KC_SAML_IDP_READ_ACCESS = "https://jans.io/keycloak/saml/idp/config.readonly";
    public static final String KC_SAML_IDP_WRITE_ACCESS = "https://jans.io/keycloak/saml/idp/config.write";
    
    
}