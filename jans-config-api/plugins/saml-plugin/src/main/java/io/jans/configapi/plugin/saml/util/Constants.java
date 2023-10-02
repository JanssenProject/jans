/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.util;

public class Constants {

    private Constants() {}

    public static final String SAML_CONFIG = "/samlConfig";
    public static final String SAML_TRUST_RELATIONSHIP = "/trust-relationship";
    public static final String SAML_SCOPE = "/scope";
    public static final String NAME_PARAM_PATH = "/{name}";
    public static final String NAME = "name";
    public static final String ID_PATH = "/id";
    public static final String ID_PATH_PARAM = "/{id}";
    public static final String ID = "id";
    public static final String CLIENTID_PATH = "/{clientId}";
    public static final String CLIENTID = "clientId";
    public static final String PROCESS_META_FILE = "/PROCESS_META_FILE";
    
    
    //Scopes
    public static final String SAML_READ_ACCESS = "https://jans.io/oauth/config/saml.readonly";
    public static final String SAML_WRITE_ACCESS = "https://jans.io/oauth/config/saml.write";

    public static final String SAML_CONFIG_READ_ACCESS = "https://jans.io/oauth/config/saml-config.readonly";
    public static final String SAML_CONFIG_WRITE_ACCESS = "https://jans.io/oauth/config/saml-config.write";
    
    public static final String SAML_SCOPE_READ_ACCESS = "https://jans.io/oauth/config/saml-scope.readonly";
    public static final String SAML_SCOPE_WRITE_ACCESS = "https://jans.io/oauth/config/saml-scope.write";
    
}