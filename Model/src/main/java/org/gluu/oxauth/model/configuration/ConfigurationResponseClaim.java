/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.configuration;

/**
 * @author Javier Rojas Blum
 * @version January 16, 2019
 */
public interface ConfigurationResponseClaim {

    String ISSUER = "issuer";
    String AUTHORIZATION_ENDPOINT = "authorization_endpoint";
    String TOKEN_ENDPOINT = "token_endpoint";
    String TOKEN_REVOCATION_ENDPOINT = "token_revocation_endpoint";
    String USER_INFO_ENDPOINT = "userinfo_endpoint";
    String CLIENT_INFO_ENDPOINT = "clientinfo_endpoint";
    String CHECK_SESSION_IFRAME = "check_session_iframe";
    String END_SESSION_ENDPOINT = "end_session_endpoint";
    String JWKS_URI = "jwks_uri";
    String REGISTRATION_ENDPOINT = "registration_endpoint";
    String ID_GENERATION_ENDPOINT = "id_generation_endpoint";
    String INTROSPECTION_ENDPOINT = "introspection_endpoint";
    String SCOPES_SUPPORTED = "scopes_supported";
    String SCOPE_TO_CLAIMS_MAPPING = "scope_to_claims_mapping";
    String RESPONSE_TYPES_SUPPORTED = "response_types_supported";
    String GRANT_TYPES_SUPPORTED = "grant_types_supported";
    String ACR_VALUES_SUPPORTED = "acr_values_supported";
    String SUBJECT_TYPES_SUPPORTED = "subject_types_supported";
    String USER_INFO_SIGNING_ALG_VALUES_SUPPORTED = "userinfo_signing_alg_values_supported";
    String USER_INFO_ENCRYPTION_ALG_VALUES_SUPPORTED = "userinfo_encryption_alg_values_supported";
    String USER_INFO_ENCRYPTION_ENC_VALUES_SUPPORTED = "userinfo_encryption_enc_values_supported";
    String ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED = "id_token_signing_alg_values_supported";
    String ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED = "id_token_encryption_alg_values_supported";
    String ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED = "id_token_encryption_enc_values_supported";
    String REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED = "request_object_signing_alg_values_supported";
    String REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED = "request_object_encryption_alg_values_supported";
    String REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED = "request_object_encryption_enc_values_supported";
    String TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED = "token_endpoint_auth_methods_supported";
    String TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED = "token_endpoint_auth_signing_alg_values_supported";
    String DISPLAY_VALUES_SUPPORTED = "display_values_supported";
    String CLAIM_TYPES_SUPPORTED = "claim_types_supported";
    String CLAIMS_SUPPORTED = "claims_supported";
    String SERVICE_DOCUMENTATION = "service_documentation";
    String CLAIMS_LOCALES_SUPPORTED = "claims_locales_supported";
    String UI_LOCALES_SUPPORTED = "ui_locales_supported";
    String CLAIMS_PARAMETER_SUPPORTED = "claims_parameter_supported";
    String REQUEST_PARAMETER_SUPPORTED = "request_parameter_supported";
    String REQUEST_URI_PARAMETER_SUPPORTED = "request_uri_parameter_supported";
    String REQUIRE_REQUEST_URI_REGISTRATION = "require_request_uri_registration";
    String OP_POLICY_URI = "op_policy_uri";
    String OP_TOS_URI = "op_tos_uri";
    String SCOPE_KEY = "scope";
    String CLAIMS_KEY = "claims";
    String ID_TOKEN_TOKEN_BINDING_CNF_VALUES_SUPPORTED = "id_token_token_binding_cnf_values_supported";
    String TLS_CLIENT_CERTIFICATE_BOUND_ACCESS_TOKENS = "tls_client_certificate_bound_access_tokens";
    String FRONTCHANNEL_LOGOUT_SUPPORTED = "frontchannel_logout_supported";
    String FRONTCHANNEL_LOGOUT_SESSION_SUPPORTED = "frontchannel_logout_session_supported";
    String AUTH_LEVEL_MAPPING = "auth_level_mapping";
    String FRONT_CHANNEL_LOGOUT_SESSION_SUPPORTED = "frontchannel_logout_session_supported";
}