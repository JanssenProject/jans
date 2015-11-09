/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.configuration;

/**
 * @author Javier Rojas Blum
 * @version 0.9 January 22, 2015
 */
public interface ConfigurationResponseClaim {

    public static final String ISSUER = "issuer";
    public static final String AUTHORIZATION_ENDPOINT = "authorization_endpoint";
    public static final String TOKEN_ENDPOINT = "token_endpoint";
    public static final String USER_INFO_ENDPOINT = "userinfo_endpoint";
    public static final String CLIENT_INFO_ENDPOINT = "clientinfo_endpoint";
    public static final String CHECK_SESSION_IFRAME = "check_session_iframe";
    public static final String END_SESSION_ENDPOINT = "end_session_endpoint";
    public static final String END_SESSION_PAGE = "end_session_page";
    public static final String JWKS_URI = "jwks_uri";
    public static final String VALIDATE_TOKEN_ENDPOINT = "validate_token_endpoint";
    public static final String REGISTRATION_ENDPOINT = "registration_endpoint";
    public static final String FEDERATION_METADATA_ENDPOINT = "federation_metadata_endpoint";
    public static final String FEDERATION_ENDPOINT = "federation_endpoint";
    public static final String ID_GENERATION_ENDPOINT = "id_generation_endpoint";
    public static final String INTROSPECTION_ENDPOINT = "introspection_endpoint";
    public static final String SCOPES_SUPPORTED = "scopes_supported";
    public static final String SCOPE_TO_CLAIMS_MAPPING = "scope_to_claims_mapping";
    public static final String RESPONSE_TYPES_SUPPORTED = "response_types_supported";
    public static final String GRANT_TYPES_SUPPORTED = "grant_types_supported";
    public static final String ACR_VALUES_SUPPORTED = "acr_values_supported";
    public static final String SUBJECT_TYPES_SUPPORTED = "subject_types_supported";
    public static final String USER_INFO_SIGNING_ALG_VALUES_SUPPORTED = "userinfo_signing_alg_values_supported";
    public static final String USER_INFO_ENCRYPTION_ALG_VALUES_SUPPORTED = "userinfo_encryption_alg_values_supported";
    public static final String USER_INFO_ENCRYPTION_ENC_VALUES_SUPPORTED = "userinfo_encryption_enc_values_supported";
    public static final String ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED = "id_token_signing_alg_values_supported";
    public static final String ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED = "id_token_encryption_alg_values_supported";
    public static final String ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED = "id_token_encryption_enc_values_supported";
    public static final String REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED = "request_object_signing_alg_values_supported";
    public static final String REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED = "request_object_encryption_alg_values_supported";
    public static final String REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED = "request_object_encryption_enc_values_supported";
    public static final String TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED = "token_endpoint_auth_methods_supported";
    public static final String TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED = "token_endpoint_auth_signing_alg_values_supported";
    public static final String DISPLAY_VALUES_SUPPORTED = "display_values_supported";
    public static final String CLAIM_TYPES_SUPPORTED = "claim_types_supported";
    public static final String CLAIMS_SUPPORTED = "claims_supported";
    public static final String SERVICE_DOCUMENTATION = "service_documentation";
    public static final String CLAIMS_LOCALES_SUPPORTED = "claims_locales_supported";
    public static final String UI_LOCALES_SUPPORTED = "ui_locales_supported";
    public static final String CLAIMS_PARAMETER_SUPPORTED = "claims_parameter_supported";
    public static final String REQUEST_PARAMETER_SUPPORTED = "request_parameter_supported";
    public static final String REQUEST_URI_PARAMETER_SUPPORTED = "request_uri_parameter_supported";
    public static final String REQUIRE_REQUEST_URI_REGISTRATION = "require_request_uri_registration";
    public static final String OP_POLICY_URI = "op_policy_uri";
    public static final String OP_TOS_URI = "op_tos_uri";
    public static final String SCOPE_KEY = "scope";
    public static final String CLAIMS_KEY = "claims";
    public static final String HTTP_LOGOUT_SUPPORTED = "http_logout_supported";
    public static final String LOGOUT_SESSION_SUPPORTED = "logout_session_supported";
}