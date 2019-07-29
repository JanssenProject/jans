package org.gluu.oxd.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/03/2016
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateSiteParams implements HasAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "post_logout_redirect_uris")
    private List<String> post_logout_redirect_uris;

    @JsonProperty(value = "redirect_uris")
    private List<String> redirect_uris;
    @JsonProperty(value = "response_types")
    private List<String> response_types;

    @JsonProperty(value = "client_id")
    private String client_id;
    @JsonProperty(value = "client_secret")
    private String client_secret;
    @JsonProperty(value = "client_jwks_uri")
    private String client_jwks_uri;
    @JsonProperty(value = "client_sector_identifier_uri")
    private String client_sector_identifier_uri;
    @JsonProperty(value = "client_token_endpoint_auth_method")
    private String client_token_endpoint_auth_method;
    @JsonProperty(value = "client_request_uris")
    private List<String> client_request_uris;
    @JsonProperty(value = "client_frontchannel_logout_uris")
    private List<String> client_frontchannel_logout_uris;

    @JsonProperty(value = "scope")
    private List<String> scope;
    @JsonProperty(value = "ui_locales")
    private List<String> ui_locales;
    @JsonProperty(value = "claims_locales")
    private List<String> claims_locales;
    @JsonProperty(value = "acr_values")
    private List<String> acr_values;
    @JsonProperty(value = "grant_types")
    private List<String> grant_types;
    @JsonProperty(value = "contacts")
    private List<String> contacts;
    @JsonProperty(value = "token")
    private String token;
    @JsonProperty(value = "access_token_as_jwt")
    private Boolean access_token_as_jwt;
    @JsonProperty(value = "access_token_signing_alg")
    private String access_token_signing_alg;
    @JsonProperty(value = "rpt_as_jwt")
    private Boolean rpt_as_jwt;
    @JsonProperty(value = "claims_redirect_uri")
    private List<String> claims_redirect_uri;

    @JsonProperty(value = "client_registration_access_token")
    private String client_registration_access_token;
    @JsonProperty(value = "client_registration_client_uri")
    private String client_registration_client_uri;
    @JsonProperty(value = "client_name")
    private String client_name;
    @JsonProperty(value = "client_token_endpoint_auth_signing_alg")
    private String client_token_endpoint_auth_signing_alg;
    @JsonProperty(value = "trusted_client")
    private Boolean trusted_client;
    @JsonProperty(value = "logo_uri")
    private String logo_uri;
    @JsonProperty(value = "client_uri")
    private String client_uri;
    @JsonProperty(value = "policy_uri")
    private String policy_uri;
    @JsonProperty(value = "front_channel_logout_session_required")
    private Boolean front_channel_logout_session_required;
    @JsonProperty(value = "tos_uri")
    private String tos_uri;
    @JsonProperty(value = "jwks")
    private String jwks;
    @JsonProperty(value = "id_token_binding_cnf")
    private String id_token_binding_cnf;
    @JsonProperty(value = "tls_client_auth_subject_dn")
    private String tls_client_auth_subject_dn;
    @JsonProperty(value = "subject_type")
    private String subject_type;
    @JsonProperty(value = "run_introspection_script_beforeaccess_token_as_jwt_creation_and_include_claims")
    private Boolean run_introspection_script_beforeaccess_token_as_jwt_creation_and_include_claims;
    @JsonProperty(value = "id_token_signed_response_alg")
    private String id_token_signed_response_alg;
    @JsonProperty(value = "id_token_encrypted_response_alg")
    private String id_token_encrypted_response_alg;
    @JsonProperty(value = "id_token_encrypted_response_enc")
    private String id_token_encrypted_response_enc;
    @JsonProperty(value = "user_info_signed_response_alg")
    private String user_info_signed_response_alg;
    @JsonProperty(value = "user_info_encrypted_response_alg")
    private String user_info_encrypted_response_alg;
    @JsonProperty(value = "user_info_encrypted_response_enc")
    private String user_info_encrypted_response_enc;
    @JsonProperty(value = "request_object_signing_alg")
    private String request_object_signing_alg;
    @JsonProperty(value = "request_object_encryption_alg")
    private String request_object_encryption_alg;
    @JsonProperty(value = "request_object_encryption_enc")
    private String request_object_encryption_enc;
    @JsonProperty(value = "default_max_age")
    private Integer default_max_age;
    @JsonProperty(value = "require_auth_time")
    private Boolean require_auth_time;
    @JsonProperty(value = "initiate_login_uri")
    private String initiate_login_uri;
    @JsonProperty(value = "authorized_origins")
    private List<String> authorized_origins;
    @JsonProperty(value = "access_token_lifetime")
    private Integer access_token_lifetime;
    @JsonProperty(value = "software_id")
    private String software_id;
    @JsonProperty(value = "software_version")
    private String software_version;
    @JsonProperty(value = "software_statement")
    private String software_statement;
    @JsonProperty(value = "custom_attributes")
    private Map<String, String> custom_attributes;

    public UpdateSiteParams() {
    }

    public List<String> getClaimsRedirectUri() {
        return claims_redirect_uri;
    }

    public void setClaimsRedirectUri(List<String> claims_redirect_uri) {
        this.claims_redirect_uri = claims_redirect_uri;
    }

    public Boolean getRptAsJwt() {
        return rpt_as_jwt;
    }

    public void setRptAsJwt(Boolean rpt_as_jwt) {
        this.rpt_as_jwt = rpt_as_jwt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getClientSectorIdentifierUri() {
        return client_sector_identifier_uri;
    }

    public void setClientSectorIdentifierUri(String clientSectorIdentifierUri) {
        this.client_sector_identifier_uri = clientSectorIdentifierUri;
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
    }

    public List<String> getClientFrontchannelLogoutUris() {
        return client_frontchannel_logout_uris;
    }

    public void setClientFrontchannelLogoutUris(List<String> clientFrontchannelLogoutUris) {
        this.client_frontchannel_logout_uris = clientFrontchannelLogoutUris;
    }

    public List<String> getClientRequestUris() {
        return client_request_uris;
    }

    public void setClientRequestUris(List<String> clientRequestUris) {
        this.client_request_uris = clientRequestUris;
    }

    public String getClientTokenEndpointAuthMethod() {
        return client_token_endpoint_auth_method;
    }

    public void setClientTokenEndpointAuthMethod(String clientTokenEndpointAuthMethod) {
        this.client_token_endpoint_auth_method = clientTokenEndpointAuthMethod;
    }

    public List<String> getPostLogoutRedirectUris() {
        return post_logout_redirect_uris;
    }

    public void setPostLogoutRedirectUris(List<String> postLogoutRedirectUris) {
        this.post_logout_redirect_uris = postLogoutRedirectUris;
    }

    public String getClientJwksUri() {
        return client_jwks_uri;
    }

    public void setClientJwksUri(String clientJwksUri) {
        this.client_jwks_uri = clientJwksUri;
    }

    public List<String> getClaimsLocales() {
        return claims_locales;
    }

    public void setClaimsLocales(List<String> claimsLocales) {
        this.claims_locales = claimsLocales;
    }

    public String getClientId() {
        return client_id;
    }

    public void setClientId(String clientId) {
        this.client_id = clientId;
    }

    public String getClientSecret() {
        return client_secret;
    }

    public void setClientSecret(String clientSecret) {
        this.client_secret = clientSecret;
    }

    public List<String> getGrantType() {
        return grant_types;
    }

    public void setGrantType(List<String> grantType) {
        this.grant_types = grantType;
    }

    public List<String> getRedirectUris() {
        return redirect_uris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirect_uris = redirectUris;
    }

    public List<String> getResponseTypes() {
        return response_types;
    }

    public void setResponseTypes(List<String> responseTypes) {
        this.response_types = responseTypes;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public List<String> getUiLocales() {
        return ui_locales;
    }

    public void setUiLocales(List<String> uiLocales) {
        this.ui_locales = uiLocales;
    }

    public List<String> getAcrValues() {
        return acr_values;
    }

    public void setAcrValues(List<String> acrValues) {
        this.acr_values = acrValues;
    }

    public List<String> getContacts() {
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    public Boolean getAccessTokenAsJwt() {
        return access_token_as_jwt;
    }

    public void setAccessTokenAsJwt(Boolean access_token_as_jwt) {
        this.access_token_as_jwt = access_token_as_jwt;
    }

    public String getAccessTokenSigningAlg() {
        return access_token_signing_alg;
    }

    public void setAccessTokenSigningAlg(String access_token_signing_alg) {
        this.access_token_signing_alg = access_token_signing_alg;
    }

    public String getClientRegistrationAccessToken() {
        return client_registration_access_token;
    }

    public void setClientRegistrationAccessToken(String clientRegistrationAccessToken) {
        this.client_registration_access_token = clientRegistrationAccessToken;
    }

    public String getClientRegistrationClientUri() {
        return client_registration_client_uri;
    }

    public void setClientRegistrationClientUri(String clientRegistrationClientUri) {
        this.client_registration_client_uri = clientRegistrationClientUri;
    }

    public String getClientName() {
        return client_name;
    }

    public void setClientName(String clientName) {
        this.client_name = clientName;
    }

    public String getClientTokenEndpointAuthSigningAlg() {
        return client_token_endpoint_auth_signing_alg;
    }

    public void setClientTokenEndpointAuthSigningAlg(String clientTokenEndpointAuthSigningAlg) {
        this.client_token_endpoint_auth_signing_alg = clientTokenEndpointAuthSigningAlg;
    }

    public Boolean getTrustedClient() {
        return trusted_client;
    }

    public void setTrustedClient(Boolean trustedClient) {
        this.trusted_client = trustedClient;
    }

    /**
     * Returns an URL that references a logo for the Client application.
     *
     * @return The URL that references a logo for the Client application.
     */
    public String getLogoUri() {
        return logo_uri;
    }

    /**
     * Sets an URL that references a logo for the Client application.
     *
     * @param logoUri The URL that references a logo for the Client application.
     */
    public void setLogoUri(String logoUri) {
        this.logo_uri = logoUri;
    }

    /**
     * Returns an URL of the home page of the Client.
     *
     * @return The URL of the home page of the Client.
     */
    public String getClientUri() {
        return client_uri;
    }

    /**
     * Sets an URL of the home page of the Client.
     *
     * @param clientUri The URL of the home page of the Client.
     */
    public void setClientUri(String clientUri) {
        this.client_uri = clientUri;
    }

    /**
     * Returns an URL that the Relying Party Client provides to the End-User to read about the how the profile data
     * will be used.
     *
     * @return The policy URL.
     */
    public String getPolicyUri() {
        return policy_uri;
    }

    /**
     * Sets an URL that the Relying Party Client provides to the End-User to read about the how the profile data will
     * be used.
     *
     * @param policyUri The policy URL.
     */
    public void setPolicyUri(String policyUri) {
        this.policy_uri = policyUri;
    }

    /**
     * Gets logout session required.
     *
     * @return logout session required
     */
    public Boolean getFrontChannelLogoutSessionRequired() {
        return front_channel_logout_session_required;
    }

    /**
     * Sets front channel logout session required.
     *
     * @param frontChannelLogoutSessionRequired front channel logout session required
     */
    public void setFrontChannelLogoutSessionRequired(Boolean frontChannelLogoutSessionRequired) {
        this.front_channel_logout_session_required = frontChannelLogoutSessionRequired;
    }

    /**
     * Returns an URL that the Relying Party Client provides to the End-User to read about the Relying Party's terms
     * of service.
     *
     * @return The tems of service URL.
     */
    public String getTosUri() {
        return tos_uri;
    }

    /**
     * Sets an URL that the Relying Party Client provides to the End-User to read about the Relying Party's terms of
     * service.
     *
     * @param tosUri The term of service URL.
     */
    public void setTosUri(String tosUri) {
        this.tos_uri = tosUri;
    }

    /**
     * Client's JSON Web Key Set (JWK) document, passed by value. The semantics of the jwks parameter are the same as
     * the jwks_uri parameter, other than that the JWK Set is passed by value, rather than by reference.
     * This parameter is intended only to be used by Clients that, for some reason, are unable to use the jwks_uri
     * parameter, for instance, by native applications that might not have a location to host the contents of the JWK
     * Set. If a Client can use jwks_uri, it must not use jwks.
     * One significant downside of jwks is that it does not enable key rotation (which jwks_uri does, as described in
     * Section 10 of OpenID Connect Core 1.0). The jwks_uri and jwks parameters must not be used together.
     *
     * @return The Client's JSON Web Key Set (JWK) document.
     */
    public String getJwks() {
        return jwks;
    }

    /**
     * Client's JSON Web Key Set (JWK) document, passed by value. The semantics of the jwks parameter are the same as
     * the jwks_uri parameter, other than that the JWK Set is passed by value, rather than by reference.
     * This parameter is intended only to be used by Clients that, for some reason, are unable to use the jwks_uri
     * parameter, for instance, by native applications that might not have a location to host the contents of the JWK
     * Set. If a Client can use jwks_uri, it must not use jwks.
     * One significant downside of jwks is that it does not enable key rotation (which jwks_uri does, as described in
     * Section LogoUri10 of OpenID Connect Core 1.0). The jwks_uri and jwks parameters must not be used together.
     *
     * @param jwks The Client's JSON Web Key Set (JWK) document.
     */
    public void setJwks(String jwks) {
        this.jwks = jwks;
    }

    public String getIdTokenBindingCnf() {
        return id_token_binding_cnf;
    }

    public void setIdTokenBindingCnf(String idTokenTokenBindingCnf) {
        this.id_token_binding_cnf = idTokenTokenBindingCnf;
    }

    public String getTlsClientAuthSubjectDn() {
        return tls_client_auth_subject_dn;
    }

    public void setTlsClientAuthSubjectDn(String tlsClientAuthSubjectDn) {
        this.tls_client_auth_subject_dn = tlsClientAuthSubjectDn;
    }

    /**
     * Returns the Subject Type. Valid types include pairwise and public.
     *
     * @return The Subject Type.
     */
    public String getSubjectType() {
        return subject_type;
    }

    /**
     * Sets the Subject Type. Valid types include pairwise and public.
     *
     * @param subjectType The Subject Type.
     */
    public void setSubjectType(String subjectType) {
        this.subject_type = subjectType;
    }

    public Boolean getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims() {
        return run_introspection_script_beforeaccess_token_as_jwt_creation_and_include_claims;
    }

    public void setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(Boolean runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims) {
        this.run_introspection_script_beforeaccess_token_as_jwt_creation_and_include_claims = runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims;
    }

    /**
     * Returns th JWS alg algorithm (JWA) required for the ID Token issued to this client_id.
     *
     * @return The JWS algorithm (JWA).
     */
    public String getIdTokenSignedResponseAlg() {
        return id_token_signed_response_alg;
    }

    /**
     * Sets the JWS alg algorithm (JWA) required for the ID Token issued to this client_id.
     *
     * @param idTokenSignedResponseAlg The JWS algorithm (JWA).
     */
    public void setIdTokenSignedResponseAlg(String idTokenSignedResponseAlg) {
        this.id_token_signed_response_alg = idTokenSignedResponseAlg;
    }

    /**
     * Returns the JWE alg algorithm (JWA) required for encrypting the ID Token issued to this client_id.
     *
     * @return The JWE algorithm (JWA).
     */
    public String getIdTokenEncryptedResponseAlg() {
        return id_token_encrypted_response_alg;
    }

    /**
     * Sets the JWE alg algorithm (JWA) required for encrypting the ID Token issued to this client_id.
     *
     * @param idTokenEncryptedResponseAlg The JWE algorithm (JWA).
     */
    public void setIdTokenEncryptedResponseAlg(String idTokenEncryptedResponseAlg) {
        this.id_token_encrypted_response_alg = idTokenEncryptedResponseAlg;
    }

    /**
     * Returns the JWE enc algorithm (JWA) required for symmetric encryption of the ID Token issued to this client_id.
     *
     * @return The JWE algorithm (JWA).
     */
    public String getIdTokenEncryptedResponseEnc() {
        return id_token_encrypted_response_enc;
    }

    /**
     * Sets the JWE enc algorithm (JWA) required for symmetric encryption of the ID Token issued to this client_id.
     *
     * @param idTokenEncryptedResponseEnc The JWE algorithm (JWA).
     */
    public void setIdTokenEncryptedResponseEnc(String idTokenEncryptedResponseEnc) {
        this.id_token_encrypted_response_enc = idTokenEncryptedResponseEnc;
    }

    /**
     * Returns the JWS alg algorithm (JWA) required for UserInfo responses.
     *
     * @return The JWS algorithm (JWA).
     */
    public String getUserInfoSignedResponseAlg() {
        return user_info_signed_response_alg;
    }

    /**
     * Sets the JWS alg algorithm (JWA) required for UserInfo responses.
     *
     * @param userInfoSignedResponseAlg The JWS algorithm (JWA).
     */
    public void setUserInfoSignedResponseAlg(String userInfoSignedResponseAlg) {
        this.user_info_signed_response_alg = userInfoSignedResponseAlg;
    }

    /**
     * Returns the JWE alg algorithm (JWA) required for encrypting UserInfo responses.
     *
     * @return The JWE algorithm (JWA).
     */
    public String getUserInfoEncryptedResponseAlg() {
        return user_info_encrypted_response_alg;
    }

    /**
     * Sets the JWE alg algorithm (JWA) required for encrypting UserInfo responses.
     *
     * @param userInfoEncryptedResponseAlg The JWE algorithm (JWA).
     */
    public void setUserInfoEncryptedResponseAlg(String userInfoEncryptedResponseAlg) {
        this.user_info_encrypted_response_alg = userInfoEncryptedResponseAlg;
    }

    /**
     * Returns the JWE enc algorithm (JWA) required for symmetric encryption of UserInfo responses.
     *
     * @return The JWE algorithm (JWA).
     */
    public String getUserInfoEncryptedResponseEnc() {
        return user_info_encrypted_response_enc;
    }

    /**
     * Sets the JWE enc algorithm (JWA) required for symmetric encryption of UserInfo responses.
     *
     * @param userInfoEncryptedResponseEnc The JWE algorithm (JWA).
     */
    public void setUserInfoEncryptedResponseEnc(String userInfoEncryptedResponseEnc) {
        this.user_info_encrypted_response_enc = userInfoEncryptedResponseEnc;
    }

    /**
     * Returns the JWS alg algorithm (JWA) that must be required by the Authorization Server.
     *
     * @return The JWS algorithm (JWA).
     */
    public String getRequestObjectSigningAlg() {
        return request_object_signing_alg;
    }

    /**
     * Sets the JWS alg algorithm (JWA) that must be required by the Authorization Server.
     *
     * @param requestObjectSigningAlg The JWS algorithm (JWA).
     */
    public void setRequestObjectSigningAlg(String requestObjectSigningAlg) {
        this.request_object_signing_alg = requestObjectSigningAlg;
    }

    /**
     * Returns the JWE alg algorithm (JWA) the RP is declaring that it may use for encrypting Request Objects
     * sent to the OP.
     *
     * @return The JWE alg algorithm (JWA).
     */
    public String getRequestObjectEncryptionAlg() {
        return request_object_encryption_alg;
    }

    /**
     * Sets the JWE alg algorithm (JWA) the RP is declaring that it may use for encrypting Request Objects
     * sent to the OP.
     *
     * @param requestObjectEncryptionAlg The JWE alg algorithm (JWA).
     */
    public void setRequestObjectEncryptionAlg(String requestObjectEncryptionAlg) {
        this.request_object_encryption_alg = requestObjectEncryptionAlg;
    }

    /**
     * Returns the JWE enc algorithm (JWA) the RP is declaring that it may use for encrypting Request Objects
     * sent to the OP.
     *
     * @return The JWE enc algorithm (JWA).
     */
    public String getRequestObjectEncryptionEnc() {
        return request_object_encryption_enc;
    }

    /**
     * Sets the JWE enc algorithm (JWA) the RP is declaring that it may use for encrypting Request Objects
     * sent to the OP.
     *
     * @param requestObjectEncryptionEnc The JWE enc algorithm (JWA).
     */
    public void setRequestObjectEncryptionEnc(String requestObjectEncryptionEnc) {
        this.request_object_encryption_enc = requestObjectEncryptionEnc;
    }

    /**
     * Returns the Default Maximum Authentication Age.
     *
     * @return The Default Maximum Authentication Age.
     */
    public Integer getDefaultMaxAge() {
        return default_max_age;
    }

    /**
     * Sets the Default Maximum Authentication Age.
     *
     * @param defaultMaxAge The Default Maximum Authentication Age.
     */
    public void setDefaultMaxAge(Integer defaultMaxAge) {
        this.default_max_age = defaultMaxAge;
    }

    /**
     * Returns the Boolean value specifying whether the auth_time claim in the id_token is required.
     * It is required when the value is true. The auth_time claim request in the request object overrides this setting.
     *
     * @return The Boolean value specifying whether the auth_time claim in the id_token is required.
     */
    public Boolean getRequireAuthTime() {
        return require_auth_time;
    }

    /**
     * Sets the Boolean value specifying whether the auth_time claim in the id_token is required.
     * Ir is required when the value is true. The auth_time claim request in the request object overrides this setting.
     *
     * @param requireAuthTime The Boolean value specifying whether the auth_time claim in the id_token is required.
     */
    public void setRequireAuthTime(Boolean requireAuthTime) {
        this.require_auth_time = requireAuthTime;
    }

    /**
     * Returns the URI using the https: scheme that the authorization server can call to initiate a login at the client.
     *
     * @return The URI using the https: scheme that the authorization server can call to initiate a login at the client.
     */
    public String getInitiateLoginUri() {
        return initiate_login_uri;
    }

    /**
     * Sets the URI using the https: scheme that the authorization server can call to initiate a login at the client.
     *
     * @param initiateLoginUri The URI using the https: scheme that the authorization server can call to initiate a
     *                         login at the client.
     */
    public void setInitiateLoginUri(String initiateLoginUri) {
        this.initiate_login_uri = initiateLoginUri;
    }

    /**
     * Returns authorized JavaScript origins.
     *
     * @return Authorized JavaScript origins.
     */
    public List<String> getAuthorizedOrigins() {
        return authorized_origins;
    }

    /**
     * Sets authorized JavaScript origins.
     *
     * @param authorizedOrigins Authorized JavaScript origins.
     */
    public void setAuthorizedOrigins(List<String> authorizedOrigins) {
        this.authorized_origins = authorizedOrigins;
    }

    /**
     * Returns the Client-specific access token expiration.
     *
     * @return The Client-specific access token expiration.
     */
    public Integer getAccessTokenLifetime() {
        return access_token_lifetime;
    }

    /**
     * Sets the Client-specific access token expiration (in seconds). Set it to Null or Zero to use the system default value.
     *
     * @param accessTokenLifetime The Client-specific access token expiration.
     */
    public void setAccessTokenLifetime(Integer accessTokenLifetime) {
        this.access_token_lifetime = accessTokenLifetime;
    }

    /**
     * Returns a unique identifier string (UUID) assigned by the client developer or software publisher used by
     * registration endpoints to identify the client software to be dynamically registered.
     *
     * @return The software identifier.
     */
    public String getSoftwareId() {
        return software_id;
    }

    /**
     * Sets a unique identifier string (UUID) assigned by the client developer or software publisher used by
     * registration endpoints to identify the client software to be dynamically registered.
     *
     * @param softwareId The software identifier.
     */
    public void setSoftwareId(String softwareId) {
        this.software_id = softwareId;
    }

    /**
     * Returns a version identifier string for the client software identified by "software_id".
     * The value of the "software_version" should change on any update to the client software identified by the same
     * "software_id".
     *
     * @return The version identifier.
     */
    public String getSoftwareVersion() {
        return software_version;
    }

    /**
     * Sets a version identifier string for the client software identified by "software_id".
     * The value of the "software_version" should change on any update to the client software identified by the same
     * "software_id".
     *
     * @param softwareVersion The version identifier.
     */
    public void setSoftwareVersion(String softwareVersion) {
        this.software_version = softwareVersion;
    }

    /**
     * Returns a software statement containing client metadata values about the client software as claims.
     * This is a string value containing the entire signed JWT.
     *
     * @return The software statement.
     */
    public String getSoftwareStatement() {
        return software_statement;
    }

    /**
     * Sets  a software statement containing client metadata values about the client software as claims.
     * This is a string value containing the entire signed JWT.
     *
     * @param softwareStatement The software statement.
     */
    public void setSoftwareStatement(String softwareStatement) {
        this.software_statement = softwareStatement;
    }

    /**
     * Gets custom attribute map copy.
     *
     * @return custom attribute map copy
     */
    public Map<String, String> getCustomAttributes() {
        return custom_attributes;
    }

    public void setCustomAttributes(Map<String, String> customAttributes) {
        this.custom_attributes = customAttributes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("UpdateSiteParams");
        sb.append("{acr_values=").append(acr_values);
        sb.append(", oxd_id='").append(oxd_id).append('\'');
        sb.append(", post_logout_redirect_uris='").append(post_logout_redirect_uris).append('\'');
        sb.append(", redirect_uris=").append(redirect_uris);
        sb.append(", response_types=").append(response_types);
        sb.append(", client_id='").append(client_id).append('\'');
        sb.append(", client_secret='").append(client_secret).append('\'');
        sb.append(", client_sector_identifier_uri='").append(client_sector_identifier_uri).append('\'');
        sb.append(", scope=").append(scope);
        sb.append(", ui_locales=").append(ui_locales);
        sb.append(", claims_locales=").append(claims_locales);
        sb.append(", grant_types=").append(grant_types);
        sb.append(", contacts=").append(contacts);
        sb.append(", token=").append(token);
        sb.append(", access_token_as_jwt=").append(access_token_as_jwt);
        sb.append(", access_token_signing_alg=").append(access_token_signing_alg);
        sb.append(", rpt_as_jwt=").append(rpt_as_jwt);
        sb.append(",claims_redirect_uri=").append(claims_redirect_uri);
        sb.append(",client_frontchannel_logout_uris=").append(client_frontchannel_logout_uris);


        sb.append(", client_registration_access_token=").append(client_registration_access_token);
        sb.append(", client_registration_client_uri=").append(client_registration_client_uri);
        sb.append(", client_name=").append(client_name);
        sb.append(", client_token_endpoint_auth_signing_alg=").append(client_token_endpoint_auth_signing_alg);
        sb.append(", trusted_client=").append(trusted_client);
        sb.append(", logo_uri=").append(logo_uri);
        sb.append(", client_uri=").append(client_uri);
        sb.append(", policy_uri=").append(policy_uri);
        sb.append(", front_channel_logout_session_required=").append(front_channel_logout_session_required);
        sb.append(", tos_uri=").append(tos_uri);
        sb.append(", jwks=").append(jwks);
        sb.append(", id_token_binding_cnf=").append(id_token_binding_cnf);
        sb.append(", tls_client_auth_subject_dn=").append(tls_client_auth_subject_dn);
        sb.append(", subject_type=").append(subject_type);
        sb.append(", run_introspection_script_beforeaccess_token_as_jwt_creation_and_include_claims=").append(run_introspection_script_beforeaccess_token_as_jwt_creation_and_include_claims);
        sb.append(", id_token_signed_response_alg=").append(id_token_signed_response_alg);
        sb.append(", id_token_encrypted_response_alg=").append(id_token_encrypted_response_alg);
        sb.append(", id_token_encrypted_response_enc=").append(id_token_encrypted_response_enc);
        sb.append(", user_info_signed_response_alg=").append(user_info_signed_response_alg);
        sb.append(", user_info_encrypted_response_alg=").append(user_info_encrypted_response_alg);
        sb.append(", user_info_encrypted_response_enc=").append(user_info_encrypted_response_enc);
        sb.append(", request_object_signing_alg=").append(request_object_signing_alg);
        sb.append(", request_object_encryption_alg=").append(request_object_encryption_alg);
        sb.append(", request_object_encryption_enc=").append(request_object_encryption_enc);
        sb.append(", default_max_age=").append(default_max_age);
        sb.append(", require_auth_time=").append(require_auth_time);
        sb.append(", initiate_login_uri=").append(initiate_login_uri);
        sb.append(", authorized_origins=").append(authorized_origins);
        sb.append(", access_token_lifetime=").append(access_token_lifetime);
        sb.append(", software_id=").append(software_id);
        sb.append(", software_version=").append(software_version);
        sb.append(", software_statement=").append(software_statement);
        sb.append(", custom_attributes=").append(custom_attributes);
        sb.append('}');
        return sb.toString();
    }

}