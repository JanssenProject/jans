package org.xdi.oxauth.model.register;

import org.apache.commons.lang.StringUtils;

/**
 * Listed all standard parameters involved in client registration request.
 *
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version 0.9, 03/22/2013
 */

public enum RegisterRequestParam {

    /**
     * Array of redirect URIs values used in the Authorization Code and Implicit grant types. One of the these
     * registered redirect URI values must match the Scheme, Host, and Path segments of the redirect_uri parameter
     * value used in each Authorization Request.
     */
    REDIRECT_URIS("redirect_uris"),

    /**
     * JSON array containing a list of the OAuth 2.0 response_type values that the Client is declaring that it will
     * restrict itself to using. If omitted, the default is that the Client will use only the code response type.
     */
    RESPONSE_TYPES("response_types"),

    /**
     * JSON array containing a list of the OAuth 2.0 grant types that the Client is declaring that it will restrict
     * itself to using.
     */
    GRANT_TYPES("grant_types"),

    /**
     * Kind of the application. The default if not specified is web. The defined values are native or web.
     * Web Clients using the OAuth implicit grant type must only register URLs using the https scheme as redirect_uris;
     * they may not use localhost as the hostname.
     * Native Clients must only register redirect_uris using custom URI schemes or URLs using the http: scheme with
     * localhost as the hostname.
     */
    APPLICATION_TYPE("application_type"),

    /**
     * Array of e-mail addresses of people responsible for this Client. This may be used by some providers to enable a
     * Web user interface to modify the Client information.
     */
    CONTACTS("contacts"),

    /**
     * Name of the Client to be presented to the user.
     */
    CLIENT_NAME("client_name"),

    /**
     * URL that references a logo for the Client application.
     */
    LOGO_URI("logo_uri"),

    /**
     * URL of the home page of the Client.
     */
    CLIENT_URI("client_uri"),

    /**
     * Requested authentication method for the Token Endpoint.
     */
    TOKEN_ENDPOINT_AUTH_METHOD("token_endpoint_auth_method"),

    /**
     * URL that the Relying Party Client provides to the End-User to read about the how the profile data will be used.
     */
    POLICY_URI("policy_uri"),

    /**
     * URL that the Relying Party Client provides to the End-User to read about the Relying Party's terms of service.
     */
    TOS_URI("tos_uri"),

    /**
     * URL for the Client's JSON Web Key Set (JWK) document containing key(s) that are used for signing requests to
     * the OP. The JWK Set may also contain the Client's encryption keys(s) that are used by the OP to encrypt the
     * responses to the Client.
     */
    JWKS_URI("jwks_uri"),

    /**
     * URL using the https scheme to be used in calculating Pseudonymous Identifiers by the OP.
     * The URL references a file with a single JSON array of redirect_uri values.
     */
    SECTOR_IDENTIFIER_URI("sector_identifier_uri"),

    /**
     * Subject type requested for the Client ID. Valid types include pairwise and public.
     */
    SUBJECT_TYPE("subject_type"),

    /**
     * JWS alg algorithm (JWA) that must be required by the Authorization Server.
     */
    REQUEST_OBJECT_SIGNING_ALG("request_object_signing_alg"),

    /**
     * JWS alg algorithm (JWA) required for UserInfo Responses.
     */
    USERINFO_SIGNED_RESPONSE_ALG("userinfo_signed_response_alg"),

    /**
     * JWE alg algorithm (JWA) required for encrypting UserInfo Responses.
     */
    USERINFO_ENCRYPTED_RESPONSE_ALG("userinfo_encrypted_response_alg"),

    /**
     * JWE enc algorithm (JWA) required for symmetric encryption of UserInfo Responses.
     */
    USERINFO_ENCRYPTED_RESPONSE_ENC("userinfo_encrypted_response_enc"),

    /**
     * JWS alg algorithm (JWA)0 required for the issued ID Token.
     */
    ID_TOKEN_SIGNED_RESPONSE_ALG("id_token_signed_response_alg"),

    /**
     * JWE alg algorithm (JWA) required for encrypting the ID Token.
     */
    ID_TOKEN_ENCRYPTED_RESPONSE_ALG("id_token_encrypted_response_alg"),

    /**
     * JWE enc algorithm (JWA) required for symmetric encryption of the ID Token.
     */
    ID_TOKEN_ENCRYPTED_RESPONSE_ENC("id_token_encrypted_response_enc"),

    /**
     * Default Maximum Authentication Age. Specifies that the End-User must be actively authenticated if the End-User
     * was authenticated longer ago than the specified number of seconds. The max_age request parameter overrides this
     * default value.
     */
    DEFAULT_MAX_AGE("default_max_age"),

    /**
     * Boolean value specifying whether the auth_time Claim in the ID Token is required. It is required when the value
     * is true. The auth_time Claim request in the Request Object overrides this setting.
     */
    REQUIRE_AUTH_TIME("require_auth_time"),

    /**
     * Default requested Authentication Context Class Reference values. Array of strings that specifies the default acr
     * values that the Authorization Server must use for processing requests from the Client.
     */
    DEFAULT_ACR_VALUES("default_acr_values"),

    /**
     * URI using the https scheme that the Authorization Server can call to initiate a login at the Client.
     */
    INITIATE_LOGIN_URI("initiate_login_uri"),

    /**
     * URL supplied by the RP to request that the user be redirected to this location after a logout has been performed,
     */
    POST_LOGOUT_REDIRECT_URIS("post_logout_redirect_uris"),

    /**
     * Array of request_uri values that are pre-registered by the Client for use at the Authorization Server.
     */
    REQUEST_URIS("request_uris"),

    SCOPES("scopes"),

    // Federation Params
    FEDERATION_METADATA_URL("federation_metadata_url"),
    FEDERATION_METADATA_ID("federation_metadata_id");

    /**
     * Parameter name
     */
    private final String m_name;

    /**
     * Constructor
     *
     * @param p_name parameter name
     */
    private RegisterRequestParam(String p_name) {
        m_name = p_name;
    }

    /**
     * Gets parameter name.
     *
     * @return parameter name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Returns whether parameter is standard
     *
     * @param p_parameterName parameter name
     * @return whether parameter is standard
     */
    public static boolean isStandard(String p_parameterName) {
        if (StringUtils.isNotBlank(p_parameterName)) {
            for (RegisterRequestParam t : values()) {
                if (t.getName().equalsIgnoreCase(p_parameterName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns whether custom parameter is valid.
     *
     * @param p_parameterName parameter name
     * @return whether custom parameter is valid
     */
    public static boolean isCustomParameterValid(String p_parameterName) {
        return StringUtils.isNotBlank(p_parameterName) && !isStandard(p_parameterName);
    }


    @Override
    public String toString() {
        return m_name;
    }
}