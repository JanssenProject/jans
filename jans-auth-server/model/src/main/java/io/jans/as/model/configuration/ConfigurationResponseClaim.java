/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.configuration;

/**
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
public final class ConfigurationResponseClaim {

    private ConfigurationResponseClaim() {
    }

    public static final String ISSUER = "issuer";
    public static final String AUTHORIZATION_ENDPOINT = "authorization_endpoint";
    public static final String TOKEN_ENDPOINT = "token_endpoint";
    public static final String REVOCATION_ENDPOINT = "revocation_endpoint";
    public static final String SESSION_REVOCATION_ENDPOINT = "session_revocation_endpoint";
    public static final String USER_INFO_ENDPOINT = "userinfo_endpoint";
    public static final String CLIENT_INFO_ENDPOINT = "clientinfo_endpoint";
    public static final String CHECK_SESSION_IFRAME = "check_session_iframe";
    public static final String END_SESSION_ENDPOINT = "end_session_endpoint";
    public static final String JWKS_URI = "jwks_uri";
    public static final String REGISTRATION_ENDPOINT = "registration_endpoint";
    public static final String ID_GENERATION_ENDPOINT = "id_generation_endpoint";
    public static final String INTROSPECTION_ENDPOINT = "introspection_endpoint";
    public static final String DEVICE_AUTHZ_ENDPOINT = "device_authorization_endpoint";
    public static final String PAR_ENDPOINT = "pushed_authorization_request_endpoint";
    public static final String REQUIRE_PAR = "require_pushed_authorization_requests";
    public static final String SCOPES_SUPPORTED = "scopes_supported";
    public static final String SCOPE_TO_CLAIMS_MAPPING = "scope_to_claims_mapping";
    public static final String RESPONSE_TYPES_SUPPORTED = "response_types_supported";
    public static final String RESPONSE_MODES_SUPPORTED = "response_modes_supported";
    public static final String GRANT_TYPES_SUPPORTED = "grant_types_supported";
    public static final String ACR_VALUES_SUPPORTED = "acr_values_supported";
    public static final String SUBJECT_TYPES_SUPPORTED = "subject_types_supported";
    public static final String AUTHORIZATION_SIGNING_ALG_VALUES_SUPPORTED = "authorization_signing_alg_values_supported";
    public static final String AUTHORIZATION_ENCRYPTION_ALG_VALUES_SUPPORTED = "authorization_encryption_alg_values_supported";
    public static final String AUTHORIZATION_ENCRYPTION_ENC_VALUES_SUPPORTED = "authorization_encryption_enc_values_supported";
    public static final String USER_INFO_SIGNING_ALG_VALUES_SUPPORTED = "userinfo_signing_alg_values_supported";
    public static final String USER_INFO_ENCRYPTION_ALG_VALUES_SUPPORTED = "userinfo_encryption_alg_values_supported";
    public static final String USER_INFO_ENCRYPTION_ENC_VALUES_SUPPORTED = "userinfo_encryption_enc_values_supported";
    public static final String ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED = "id_token_signing_alg_values_supported";
    public static final String ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED = "id_token_encryption_alg_values_supported";
    public static final String ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED = "id_token_encryption_enc_values_supported";
    public static final String ACCESS_TOKEN_SIGNING_ALG_VALUES_SUPPORTED = "access_token_signing_alg_values_supported";
    public static final String REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED = "request_object_signing_alg_values_supported";
    public static final String REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED = "request_object_encryption_alg_values_supported";
    public static final String REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED = "request_object_encryption_enc_values_supported";
    public static final String TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED = "token_endpoint_auth_methods_supported";
    public static final String TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED = "token_endpoint_auth_signing_alg_values_supported";
    public static final String DPOP_SIGNING_ALG_VALUES_SUPPORTED = "dpop_signing_alg_values_supported";
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
    public static final String ID_TOKEN_TOKEN_BINDING_CNF_VALUES_SUPPORTED = "id_token_token_binding_cnf_values_supported";
    public static final String TLS_CLIENT_CERTIFICATE_BOUND_ACCESS_TOKENS = "tls_client_certificate_bound_access_tokens";
    public static final String FRONTCHANNEL_LOGOUT_SUPPORTED = "frontchannel_logout_supported";
    public static final String FRONTCHANNEL_LOGOUT_SESSION_SUPPORTED = "frontchannel_logout_session_supported";
    public static final String AUTH_LEVEL_MAPPING = "auth_level_mapping";
    public static final String FRONT_CHANNEL_LOGOUT_SESSION_SUPPORTED = "frontchannel_logout_session_supported";
    public static final String BACKCHANNEL_LOGOUT_SUPPORTED = "backchannel_logout_supported";
    public static final String BACKCHANNEL_LOGOUT_SESSION_SUPPORTED = "backchannel_logout_session_supported";
    public static final String MTLS_ENDPOINT_ALIASES = "mtls_endpoint_aliases";

    // CIBA
    public static final String BACKCHANNEL_AUTHENTICATION_ENDPOINT = "backchannel_authentication_endpoint";
    public static final String BACKCHANNEL_TOKEN_DELIVERY_MODES_SUPPORTED = "backchannel_token_delivery_modes_supported";
    public static final String BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG_VALUES_SUPPORTED = "backchannel_authentication_request_signing_alg_values_supported";
    public static final String BACKCHANNEL_USER_CODE_PAREMETER_SUPPORTED = "backchannel_user_code_parameter_supported";

    // SSA
    public static final String SSA_ENDPOINT = "ssa_endpoint";
}
