/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Constants {

    private Constants() {
    }

    //Config values
    public static final String PRINCIPAL_ATTRIBUTE = "principalAttribute";
    public static final String PRINCIPAL_TYPE  = "principalType";
    public static final String NAME_ID_POLICY_FORMAT_DEFAULT_VALUE = "urn:oasis:names:tc:SAML:2.0:nameid-format:transient";
    public static final String PRINCIPAL_ATTRIBUTE_DEFAULT_VALUE = "uid";
    public static final String PRINCIPAL_TYPE_DEFAULT_VALUE = "FRIENDLY_ATTRIBUTE";
    
    public static final String IDP_MODULE = "idp-module";
    public static final String SP_MODULE = "sp-module";
    public static final String REALM_MASTER = "master";
    public static final String SP_METADATA_FILE_PATTERN = "%s_sp-metadata.xml";
    public static final String IDP_METADATA_FILE_PATTERN = "%s_idp-metadata.xml";

    public static final String SAML_CONFIG = "/samlConfig";
    public static final String TRUST_RELATIONSHIP = "/trust-relationship";
    public static final String SCOPE = "/scope";
    public static final String PROCESS_SP_META_FILE = "/process-sp-meta-file";
    public static final String PROCESS_IDP_META_FILE = "/process-idp-meta-file";
    public static final String IDENTITY_PROVIDER = "/idp";
    public static final String KEYCLOAK = "/kc";
    public static final String SAML_PATH = "/saml";
    public static final String IDP_CONFIG_PATH = "/idp-config";
    public static final String REALM_PATH = "/realm";
    public static final String NAME_PATH = "/name";
    public static final String UPLOAD_PATH = "/upload";
    public static final String SP_METADATA_PATH = "/sp-metadata";
    public static final String SP_METADATA_FILE_PATH = "/sp-metadata-file";

    public static final String ID_PATH = "/id";
    public static final String INUM_PATH_PARAM = "/{inum}";
    public static final String NAME_PATH_PARAM = "/{name}";
    public static final String ID_PATH_PARAM = "/{id}";
    public static final String CLIENTID_PATH_PARAM = "/{clientId}";

    public static final String CLIENTID = "clientId";
    public static final String ID = "id";
    public static final String IDP = "idp";
    public static final String INUM = "inum";
    public static final String NAME = "name";
    public static final String OIDC = "oidc";
    public static final String SAML = "saml";
    public static final String REALM = "realm";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String INTERNAL_ID = "internalId";
    public static final String ALIAS = "alias";

    public static final String SIGNING_CERTIFICATES = "signingCertificates";
    public static final String VALIDATE_SIGNATURE = "validateSignature";
    public static final String SINGLE_LOGOUT_SERVICE_URL = "singleLogoutServiceUrl";
    public static final String NAME_ID_POLICY_FORMAT = "nameIDPolicyFormat";
    public static final String IDP_ENTITY_ID = "idpEntityId";
    public static final String SINGLE_SIGN_ON_SERVICE_URL = "singleSignOnServiceUrl";
    public static final String ENCRYPTION_PUBLIC_KEY = "encryptionPublicKey";

    public static final List<String> SAML_IDP_CONFIG = new ArrayList<>(
            Arrays.asList(VALIDATE_SIGNATURE, SINGLE_LOGOUT_SERVICE_URL, "postBindingLogout", "postBindingResponse",
                    "postBindingAuthnRequest", SINGLE_SIGN_ON_SERVICE_URL, "wantAuthnRequestsSigned",
                    "signingCertificate", "addExtensionsElementWithKeyInfo"));

    // Scopes
    public static final String SAML_READ_ACCESS = "https://jans.io/oauth/config/saml.readonly";
    public static final String SAML_WRITE_ACCESS = "https://jans.io/oauth/config/saml.write";
    public static final String SAML_DELETE_ACCESS = "https://jans.io/oauth/config/saml.delete";

    public static final String SAML_CONFIG_READ_ACCESS = "https://jans.io/oauth/config/saml-config.readonly";
    public static final String SAML_CONFIG_WRITE_ACCESS = "https://jans.io/oauth/config/saml-config.write";

    public static final String SAML_SCOPE_READ_ACCESS = "https://jans.io/oauth/config/saml-scope.readonly";
    public static final String SAML_SCOPE_WRITE_ACCESS = "https://jans.io/oauth/config/saml-scope.write";

    public static final String JANS_IDP_CONFIG_READ_ACCESS = "https://jans.io/idp/config.readonly";
    public static final String JANS_IDP_CONFIG_WRITE_ACCESS = "https://jans.io/idp/config.write";

    public static final String JANS_IDP_REALM_READ_ACCESS = "https://jans.io/idp/realm.readonly";
    public static final String JANS_IDP_REALM_WRITE_ACCESS = "https://jans.io/idp/realm.write";

    public static final String JANS_IDP_REALM_DELETE_ACCESS = "https://jans.io/idp/realm.delete";
    public static final String JANS_IDP_SAML_READ_ACCESS = "https://jans.io/idp/saml.readonly";

    public static final String JANS_IDP_SAML_WRITE_ACCESS = "https://jans.io/idp/saml.write";
    public static final String JANS_IDP_SAML_DELETE_ACCESS = "https://jans.io/idp/saml.delete";
}