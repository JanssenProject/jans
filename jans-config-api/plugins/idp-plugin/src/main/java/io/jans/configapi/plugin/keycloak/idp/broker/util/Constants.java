/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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
	public static final String UPLOAD_PATH = "/upload";

    public static final String ID_PATH_PARAM = "/{id}";
    public static final String INUM_PATH_PARAM = "/{inum}";
    public static final String NAME_PATH_PARAM = "/{name}";

	public static final String IDP = "idp";
    public static final String SAML = "saml";
    public static final String OIDC = "oidc";
    public static final String ID = "id";
	public static final String NAME = "name";
	public static final String REALM = "realm";
	
	public static final  List<String> SAML_IDP_CONFIG = new ArrayList<>(Arrays.asList( "validateSignature",
	          "singleLogoutServiceUrl",
	          "postBindingLogout",
	          "postBindingResponse",
	          "postBindingAuthnRequest",
	          "singleSignOnServiceUrl",
	          "wantAuthnRequestsSigned",
	          "signingCertificate",
	          "addExtensionsElementWithKeyInfo"));

    //properties
    public static final String IDP_SERVER_URL = "idp.server.url";
    public static final String AUTH_TOKEN_ENDPOINT = "auth.token.endpoint";
    public static final String IDP_CLIENT_ID = "idp.client.id";
    public static final String IDP_CLIENT_PASSWORD = "idp.client.password";
    
    
	//Scope
    public static final String JANS_IDP_CONFIG_READ_ACCESS = "https://jans.io/idp/config.readonly";
    public static final String JANS_IDP_CONFIG_WRITE_ACCESS = "https://jans.io/idp/config.write";
	public static final String JANS_IDP_REALM_READ_ACCESS = "https://jans.io/idp/realm.readonly";
    public static final String JANS_IDP_REALM_WRITE_ACCESS = "https://jans.io/idp/realm.write";
    public static final String JANS_IDP_REALM_DELETE_ACCESS = "https://jans.io/idp/realm.delete";
    public static final String JANS_IDP_SAML_READ_ACCESS = "https://jans.io/idp/saml.readonly";
    public static final String JANS_IDP_SAML_WRITE_ACCESS = "https://jans.io/idp/saml.write";
    public static final String JANS_IDP_SAML_DELETE_ACCESS = "https://jans.io/idp/saml.delete";
    
}