/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.util;

public class Constants {

    private Constants() {}

    public static final String IDP_MODULE = "idp-module";
    public static final String JANS_IDP_CONFIG_PROP_PATH = "jans.idp.prop.path";
    public static final String KEYCLOAK_USER = "/keycloak-user";
    public static final String BASE_URL = "https://localhost";
    public static final String REALM_MASTER = "master";
    

	public static final String IDENTITY_PROVIDER = "/idp";
    public static final String KEYCLOAK= "/kc";
    public static final String SAML_PATH = "/saml";
    public static final String REALM_PATH = "/realm";
	public static final String NAME_PATH = "/name";

    public static final String ID_PATH_PARAM = "/{id}";
    public static final String INUM_PATH_PARAM = "/{inum}";
    public static final String NAME_PATH_PARAM = "/{name}";

	public static final String IDP = "idp";
    public static final String SAML = "saml";
    public static final String OIDC = "oidc";
    public static final String ID = "id";
	public static final String NAME = "name";

    //properties
    public static final String KEYCLOAK_SERVER_URL = "keycloak.server.url";
    public static final String AUTH_TOKEN_ENDPOINT = "auth.token.endpoint";
    public static final String KEYCLOAK_SCIM_CLIENT_ID = "keycloak.scim.client.id";
    public static final String KEYCLOAK_SCIM_CLIENT_PASSWORD = "keycloak.scim.client.password";
    
    
	//Scope
    public static final String JANS_KC_CONFIG_READ_ACCESS = "https://jans.io/keycloak/config.readonly";
    public static final String JANS_KC_CONFIG_WRITE_ACCESS = "https://jans.io/keycloak/config.write";
	public static final String KC_REALM_READ_ACCESS = "https://jans.io/keycloak/realm.readonly";
    public static final String KC_REALM_WRITE_ACCESS = "https://jans.io/keycloak/realm.write";
    public static final String KC_SAML_IDP_READ_ACCESS = "https://jans.io/keycloak/saml/idp.readonly";
    public static final String KC_SAML_IDP_WRITE_ACCESS = "https://jans.io/keycloak/saml/idp.write";
    
}