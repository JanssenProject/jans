/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.registration;

import org.apache.commons.lang.ArrayUtils;
import org.codehaus.jettison.json.JSONArray;
import org.gluu.site.ldap.persistence.annotation.*;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.CustomAttribute;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.common.Scope;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.exception.InvalidClaimException;
import org.xdi.oxauth.service.EncryptionService;
import org.xdi.oxauth.service.ScopeService;
import org.xdi.oxauth.util.LdapUtils;
import org.xdi.util.security.StringEncrypter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version October 16, 2015
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthClient"})
public class Client {

    @LdapDN
    private String dn;

    @LdapAttribute(name = "inum")
    private String clientId;

    @LdapAttribute(name = "oxAuthClientSecret")
    private String encodedClientSecret;

    @LdapAttribute(name = "oxAuthRegistrationAccessToken")
    private String registrationAccessToken;

    @LdapAttribute(name = "oxAuthClientIdIssuedAt")
    private Date clientIdIssuedAt;

    @LdapAttribute(name = "oxAuthClientSecretExpiresAt")
    private Date clientSecretExpiresAt;

    @LdapAttribute(name = "oxAuthRedirectURI")
    private String[] redirectUris;

    @LdapAttribute(name = "oxAuthResponseType")
    private ResponseType[] responseTypes;

    @LdapAttribute(name = "oxAuthGrantType")
    private String[] grantTypes;

    @LdapAttribute(name = "oxAuthAppType")
    private String applicationType;

    @LdapAttribute(name = "oxAuthContact")
    private String[] contacts;

    @LdapAttribute(name = "displayName")
    private String clientName;

    @LdapAttribute(name = "oxAuthLogoURI")
    private String logoUri;

    @LdapAttribute(name = "oxAuthClientURI")
    private String clientUri;

    @LdapAttribute(name = "oxAuthTokenEndpointAuthMethod")
    private String tokenEndpointAuthMethod;

    @LdapAttribute(name = "oxAuthPolicyURI")
    private String policyUri;

    @LdapAttribute(name = "oxAuthTosURI")
    private String tosUri;

    @LdapAttribute(name = "oxAuthJwksURI")
    private String jwksUri;

    @LdapAttribute(name = "oxAuthJwks")
    private String jwks;

    @LdapAttribute(name = "oxAuthSectorIdentifierURI")
    private String sectorIdentifierUri;

    @LdapAttribute(name = "oxAuthSubjectType")
    private String subjectType;

    @LdapAttribute(name = "oxAuthRequestObjectSigningAlg")
    private String requestObjectSigningAlg;

    @LdapAttribute(name = "oxAuthSignedResponseAlg")
    private String userInfoSignedResponseAlg;

    @LdapAttribute(name = "oxAuthUserInfoEncryptedResponseAlg")
    private String userInfoEncryptedResponseAlg;

    @LdapAttribute(name = "oxAuthUserInfoEncryptedResponseEnc")
    private String userInfoEncryptedResponseEnc;

    @LdapAttribute(name = "oxAuthIdTokenSignedResponseAlg")
    private String idTokenSignedResponseAlg;

    @LdapAttribute(name = "oxAuthIdTokenEncryptedResponseAlg")
    private String idTokenEncryptedResponseAlg;

    @LdapAttribute(name = "oxAuthIdTokenEncryptedResponseEnc")
    private String idTokenEncryptedResponseEnc;

    @LdapAttribute(name = "oxAuthDefaultMaxAge")
    private Integer defaultMaxAge;

    @LdapAttribute(name = "oxAuthRequireAuthTime")
    private Boolean requireAuthTime;

    @LdapAttribute(name = "oxAuthDefaultAcrValues")
    private String[] defaultAcrValues;

    @LdapAttribute(name = "oxAuthInitiateLoginURI")
    private String initiateLoginUri;

    @LdapAttribute(name = "oxAuthPostLogoutRedirectURI")
    private String[] postLogoutRedirectUris;

    @LdapAttribute(name = "oxAuthRequestURI")
    private String[] requestUris;

    @LdapAttribute(name = "oxAuthScope")
    private String[] scopes;

    @LdapAttribute(name = "oxAuthTrustedClient")
    private String trustedClient;

    @LdapAttribute(name = "oxAuthClientUserGroup")
    private String[] userGroups;

    @LdapAttribute(name = "oxAuthFederationId")
    private String federationId;

    @LdapAttribute(name = "oxAuthFederationMetadataURI")
    private String federationURI;

    @LdapAttribute(name = "oxLastAccessTime")
    private Date lastAccessTime;

    @LdapAttribute(name = "oxLastLogonTime")
    private Date lastLogonTime;

    @LdapAttribute(name = "oxPersistClientAuthorizations")
    private Boolean persistClientAuthorizations;

    @LdapAttributesList(name = "name", value = "values", sortByName = true)
    private List<CustomAttribute> customAttributes = new ArrayList<CustomAttribute>();

    @LdapCustomObjectClass
    private String[] customObjectClasses;

    public AuthenticationMethod getAuthenticationMethod() {
        return AuthenticationMethod.fromString(tokenEndpointAuthMethod);
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    /**
     * Returns the Unique Client identifier.
     *
     * @return The Unique Client identifier.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the Unique Client identifier.
     *
     * @param clientId The client identifier.
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Returns the encoded client secret.
     *
     * @return The encoded client secret.
     */
    public String getEncodedClientSecret() {
        return encodedClientSecret;
    }

    /**
     * Sets the client secret.
     *
     * @param encodedClientSecret The encoded client secret.
     */
    public void setEncodedClientSecret(String encodedClientSecret) {
        this.encodedClientSecret = encodedClientSecret;
    }

    /**
     * Returns the client secret.
     *
     * @return The client secret.
     */
    public String getClientSecret() throws StringEncrypter.EncryptionException {
        return EncryptionService.instance().decrypt(encodedClientSecret);
    }

    /**
     * Sets the client secret.
     *
     * @param clientSecret The client secret.
     */
    public void setClientSecret(String clientSecret) throws StringEncrypter.EncryptionException {
        encodedClientSecret = EncryptionService.instance().encrypt(clientSecret);
    }

    /**
     * Returns the Access Token that is used by the Client to perform subsequent operations upon the resulting
     * Client registration.
     *
     * @return The registration access token.
     */
    public String getRegistrationAccessToken() {
        return registrationAccessToken;
    }

    /**
     * Sets the Access Token that is used by the Client to perform subsequent operations upon the resulting Client
     * registration.
     *
     * @param registrationAccessToken The registration access token.
     */
    public void setRegistrationAccessToken(String registrationAccessToken) {
        this.registrationAccessToken = registrationAccessToken;
    }

    /**
     * Returns the time when the Client Identifier was issued.
     *
     * @return The Client ID issued at value.
     */
    public Date getClientIdIssuedAt() {
        return clientIdIssuedAt;
    }

    /**
     * Sets the time then the Client Identifier was issued.
     *
     * @param clientIdIssuedAt The Client ID issued at value.
     */
    public void setClientIdIssuedAt(Date clientIdIssuedAt) {
        this.clientIdIssuedAt = clientIdIssuedAt;
    }

    /**
     * Returns the time at which the client secret will expire.
     *
     * @return The Client Secret expiration date.
     */
    public Date getClientSecretExpiresAt() {
        return clientSecretExpiresAt != null ? new Date(clientSecretExpiresAt.getTime()) : null;
    }

    /**
     * Sets the time at which the client secret will expire.
     *
     * @param clientSecretExpiresAt The Client Secret expiration date.
     */
    public void setClientSecretExpiresAt(Date clientSecretExpiresAt) {
        this.clientSecretExpiresAt = clientSecretExpiresAt != null ? new Date(clientSecretExpiresAt.getTime()) : null;
    }

    /**
     * Returns an Array of redirect URIs values used in the Authorization Code and Implicit grant types.
     * One of the these registered redirect URI values must match the Scheme, Host, and Path segments of
     * the Redirect URI parameter value used in each Authorization Request.
     *
     * @return The redirect URIs.
     */
    public String[] getRedirectUris() {
        return redirectUris;
    }

    /**
     * Sets an Array of redirect URIs values used in the Authorization Code and Implicit grant types.
     * One of the these registered redirect URI values must match the Scheme, Host, and Path segments of
     * the Redirect URI parameter value used in each Authorization Request.
     *
     * @param redirectUris The redirect URIs.
     */
    public void setRedirectUris(String[] redirectUris) {
        this.redirectUris = redirectUris;
    }

    /**
     * Returns a JSON array containing a list of the OAuth 2.0 response type values that the Client is declaring
     * that it will restrict itself to using.
     *
     * @return The response types.
     */
    public ResponseType[] getResponseTypes() {
        return responseTypes;
    }

    /**
     * Sets a JSON array containing a list of the OAuth 2.0 response type values that the Client is declaring that
     * it will restrict itself to using.
     *
     * @param responseTypes The response types.
     */
    public void setResponseTypes(ResponseType[] responseTypes) {
        this.responseTypes = responseTypes;
    }

    /**
     * Returns a JSON array containing a list of the OAuth 2.0 grant types that the Client is declaring that it will
     * restrict itself to using.
     *
     * @return The grant types.
     */
    public String[] getGrantTypes() {
        return grantTypes;
    }

    /**
     * Sets a JSON array containing a list of the OAuth 2.0 grant types that the Client is declaring that it will
     * restrict itself to using.
     *
     * @param grantTypes The grant types.
     */
    public void setGrantTypes(String[] grantTypes) {
        this.grantTypes = grantTypes;
    }

    /**
     * Returns the Kind of the application. The default if not specified is web. The defined values are native or web.
     * Web Clients using the OAuth implicit grant type must only register URLs using the https scheme as redirect_uris;
     * they may not use localhost as the hostname.
     * Native Clients must only register redirect_uris using custom URI schemes or URLs using the http: scheme with
     * localhost as the hostname.
     *
     * @return The type of the client application.
     */
    public String getApplicationType() {
        return applicationType;
    }

    /**
     * Sets the Kind of the application. The default if not specified is web. The defined values are native or web.
     * Web Clients using the OAuth implicit grant type must only register URLs using the https scheme as redirect_uris;
     * they may not use localhost as the hostname.
     * Native Clients must only register redirect_uris using custom URI schemes or URLs using the http: scheme with
     * localhost as the hostname.
     *
     * @param applicationType The type of the client application.
     */
    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    /**
     * Returns an Array of e-mail addresses of people responsible for this Client.
     * This may be used by some providers to enable a Web user interface to modify the Client information.
     *
     * @return A list of contact email addresses.
     */
    public String[] getContacts() {
        return contacts;
    }

    /**
     * Sets an Array of e-mail addresses of people responsible for this Client.
     * This may be used by some providers to enable a Web user interface to modify the Client information.
     *
     * @param contacts A list of contact email addresses.
     */
    public void setContacts(String[] contacts) {
        this.contacts = contacts;
    }

    /**
     * Returns the name of the Client to be presented to the user.
     *
     * @return The name of the Client to be presented to the user.
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * Sets the name of the Client to be presented to the user.
     *
     * @param clientName The name of the Client to be presented to the user.
     */
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    /**
     * Returns an URL that references a logo for the Client application.
     *
     * @return The URL of a logo image for the Client where it can be retrieved.
     */
    public String getLogoUri() {
        return logoUri;
    }

    /**
     * Sets an URL that references a logo for the Client application.
     *
     * @param logoUri The URL of a logo image for the Client where it can be retrieved.
     */
    public void setLogoUri(String logoUri) {
        this.logoUri = logoUri;
    }

    /**
     * Returns an URL of the home page of the Client.
     *
     * @return The URL of the home page of the Client.
     */
    public String getClientUri() {
        return clientUri;
    }

    /**
     * Sets an URL of the home page of the Client.
     *
     * @param clientUri The URL of the home page of the Client.
     */
    public void setClientUri(String clientUri) {
        this.clientUri = clientUri;
    }

    /**
     * Returns the Requested authentication method for the Token Endpoint.
     *
     * @return The authentication type for the Token Endpoint.
     */
    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    /**
     * Sets the Requested authentication method for the Token Endpoint.
     *
     * @param tokenEndpointAuthMethod The authentication type for the Token Endpoint.
     */
    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    /**
     * Returns an that the Relying Party Client provides to the End-User to read about the how the profile data will
     * be used.
     *
     * @return An URL location about the how the profile data will be used.
     */
    public String getPolicyUri() {
        return policyUri;
    }

    /**
     * Sets an that the Relying Party Client provides to the End-User to read about the how the profile data will
     * be used.
     *
     * @param policyUri An URL location about the how the profile data will be used.
     */
    public void setPolicyUri(String policyUri) {
        this.policyUri = policyUri;
    }

    /**
     * Returns an URL that the Relying Party Client provides to the End-User to read about the Relying Party's terms
     * of service.
     *
     * @return The terms of service URL.
     */
    public String getTosUri() {
        return tosUri;
    }

    /**
     * Sets an URL that the Relying Party Client provides to the End-User to read about the Relying Party's terms of
     * service.
     *
     * @param tosUri The terms of service URL.
     */
    public void setTosUri(String tosUri) {
        this.tosUri = tosUri;
    }

    /**
     * Return the URL for the Client's JSON Web Key (JWK) document containing key(s) that are used for signing requests
     * to the OP. The JWK Set may also contain the Client's encryption key(s) that are used by the OP to encrypt the
     * responses to the Client.
     *
     * @return The URL for the Client's JWK Set.
     */
    public String getJwksUri() {
        return jwksUri;
    }

    /**
     * Sets the URL for the Client's JSON Web Key (JWK) document containing key(s) that are used for signing requests
     * to the OP. The JWK Set may also contain the Client's encryption key(s) that are used by the OP to encrypt the
     * responses to the Client.
     *
     * @param jwksUri The URL for the Client's JWK Set.
     */
    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
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
     * Section 10 of OpenID Connect Core 1.0). The jwks_uri and jwks parameters must not be used together.
     *
     * @param jwks The Client's JSON Web Key Set (JWK) document.
     */
    public void setJwks(String jwks) {
        this.jwks = jwks;
    }

    /**
     * Return an URL using the HTTPS scheme to be used in calculating Pseudonymous Identifiers by the OP.
     * The URL references a file with a single JSON array of Redirect URI values.
     *
     * @return A HTTPS scheme URL to be used in calculating Pseudonymous Identifiers by the OP.
     */
    public String getSectorIdentifierUri() {
        return sectorIdentifierUri;
    }

    /**
     * Sets an URL using the HTTPS scheme to be used in calculating Pseudonymous Identifiers by the OP.
     * The URL references a file with a single JSON array of Redirect URI values.
     *
     * @param sectorIdentifierUri A HTTPS scheme URL to be used in calculating Pseudonymous Identifiers by the OP.
     */
    public void setSectorIdentifierUri(String sectorIdentifierUri) {
        this.sectorIdentifierUri = sectorIdentifierUri;
    }

    /**
     * Returns the Subject type requested for the Client ID. Valid types include pairwise and public.
     *
     * @return The subject type.
     */
    public String getSubjectType() {
        return subjectType;
    }

    /**
     * Sets the Subject type quested for the Client ID. Valid types include pairwise and public.
     *
     * @param subjectType The subject type.
     */
    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    /**
     * Returns the JWS alg algorithm (JWA) that must be required by the Authorization Server.
     *
     * @return The JWS signature algorithm that must be required by the Authorization Server.
     */
    public String getRequestObjectSigningAlg() {
        return requestObjectSigningAlg;
    }

    /**
     * Sets the JWS alg algorithm (JWA) that must be required by the Authorization Server.
     *
     * @param requestObjectSigningAlg The JWS signature algorithm that must be required by the Authorization Server.
     */
    public void setRequestObjectSigningAlg(String requestObjectSigningAlg) {
        this.requestObjectSigningAlg = requestObjectSigningAlg;
    }

    /**
     * Returns the JWS alg algorithm (JWA) required for UserInfo Responses.
     *
     * @return The JWS encryption algorithm required for UserInfo responses.
     */
    public String getUserInfoSignedResponseAlg() {
        return userInfoSignedResponseAlg;
    }

    /**
     * Sets the JWS alg algorithm (JWA) required for UserInfo Responses.
     *
     * @param userInfoSignedResponseAlg The JWS encryption algorithm required for UserInfo responses.
     */
    public void setUserInfoSignedResponseAlg(String userInfoSignedResponseAlg) {
        this.userInfoSignedResponseAlg = userInfoSignedResponseAlg;
    }

    /**
     * Returns the JWE alg algorithm (JWA) required for encrypting UserInfo Responses.
     *
     * @return The JWE alg algorithm required for UserInfo responses.
     */
    public String getUserInfoEncryptedResponseAlg() {
        return userInfoEncryptedResponseAlg;
    }

    /**
     * Sets the JWE alg algorithm (JWA) required for encrypting UserInfo Responses.
     *
     * @param userInfoEncryptedResponseAlg The JWE alg algorithm required for UserInfo responses.
     */
    public void setUserInfoEncryptedResponseAlg(String userInfoEncryptedResponseAlg) {
        this.userInfoEncryptedResponseAlg = userInfoEncryptedResponseAlg;
    }

    /**
     * Returns the JWE enc algorithm (JWA) required for symmetric encryption of UserInfo Responses.
     *
     * @return The JWE enc algorithm required for UserInfo responses.
     */
    public String getUserInfoEncryptedResponseEnc() {
        return userInfoEncryptedResponseEnc;
    }

    /**
     * Sets the JWE enc algorithm (JWA) required for symmetric encryption of UserInfo Responses.
     *
     * @param userInfoEncryptedResponseEnc The JWE enc algorithm required for UserInfo responses.
     */
    public void setUserInfoEncryptedResponseEnc(String userInfoEncryptedResponseEnc) {
        this.userInfoEncryptedResponseEnc = userInfoEncryptedResponseEnc;
    }

    /**
     * Returns the JWS alg algorithm (JWA)0 required for the issued ID Token.
     *
     * @return The JWS signing algorithm required for the ID Token issued.
     */
    public String getIdTokenSignedResponseAlg() {
        return idTokenSignedResponseAlg;
    }

    /**
     * Sets the JWS alg algorithm (JWA)0 required for the issued ID Token.
     *
     * @param idTokenSignedResponseAlg The JWS signing algorithm required for the ID Token issued.
     */
    public void setIdTokenSignedResponseAlg(String idTokenSignedResponseAlg) {
        this.idTokenSignedResponseAlg = idTokenSignedResponseAlg;
    }

    /**
     * Returns the JWE alg algorithm (JWA) required for encrypting the ID Token.
     *
     * @return The JWE alg algorithm required for the ID Token issued.
     */
    public String getIdTokenEncryptedResponseAlg() {
        return idTokenEncryptedResponseAlg;
    }

    /**
     * Sets the JWE alg algorithm (JWA) required for encrypting the ID Token.
     *
     * @param idTokenEncryptedResponseAlg The JWE alg algorithm required for the ID Token issued.
     */
    public void setIdTokenEncryptedResponseAlg(String idTokenEncryptedResponseAlg) {
        this.idTokenEncryptedResponseAlg = idTokenEncryptedResponseAlg;
    }

    /**
     * Returns the JWE enc algorithm (JWA) required for symmetric encryption of the ID Token.
     *
     * @return The JWE enc algorithm required for the ID token issued.
     */
    public String getIdTokenEncryptedResponseEnc() {
        return idTokenEncryptedResponseEnc;
    }

    /**
     * Sets the JWE enc algorithm (JWA) required for symmetric encryption of the ID Token.
     *
     * @param idTokenEncryptedResponseEnc The JWE enc algorithm required for the ID token issued.
     */
    public void setIdTokenEncryptedResponseEnc(String idTokenEncryptedResponseEnc) {
        this.idTokenEncryptedResponseEnc = idTokenEncryptedResponseEnc;
    }

    /**
     * Returns the Default Maximum Authentication Age.
     * Specifies that the End-User must be actively authenticated if the End-User was authenticated longer ago than
     * the specified number of seconds.
     *
     * @return The default maximum authentication age.
     */
    public Integer getDefaultMaxAge() {
        return defaultMaxAge;
    }

    /**
     * Sets the Default Maximum Authentication Age.
     * Specified that the End-User must be actively authenticated if the End-User was authenticated longer ago than
     * the specified number of seconds.
     *
     * @param defaultMaxAge The default maximum authentication age.
     */
    public void setDefaultMaxAge(Integer defaultMaxAge) {
        this.defaultMaxAge = defaultMaxAge;
    }

    /**
     * Returns a Boolean value specifying whether the auth_time Claim in the ID Token is required.
     * It is required when the value is true. The auth_time Claim request in the Request Object overrides this setting.
     *
     * @return The required authentication time.
     */
    public Boolean getRequireAuthTime() {
        return requireAuthTime;
    }

    /**
     * Sets a Boolean value specifying whether the auth_time Claim in the ID Token is required.
     * It is required when the value is true. The auth_time Claim request in the Request Object overrides this setting.
     *
     * @param requireAuthTime The required authentication time.
     */
    public void setRequireAuthTime(Boolean requireAuthTime) {
        this.requireAuthTime = requireAuthTime;
    }

    /**
     * Returns the Default requested Authentication Context Class Reference values.
     * Array of strings that specifies the default acr values that the Authorization Server must use for processing
     * requests from the Client.
     *
     * @return The default acr values.
     */
    public String[] getDefaultAcrValues() {
        return defaultAcrValues;
    }

    /**
     * Sets the Default requested Authentication Context Class Reference values.
     * Array of strings that specifies the default acr values that the Authorization Server must use for processing
     * request from the Client.
     *
     * @param defaultAcrValues The default acr values.
     */
    public void setDefaultAcrValues(String[] defaultAcrValues) {
        this.defaultAcrValues = defaultAcrValues;
    }

    /**
     * Returns an URI using the https scheme that the Authorization Server can call to initiate a login at the Client.
     *
     * @return The initiate login URI.
     */
    public String getInitiateLoginUri() {
        return initiateLoginUri;
    }

    /**
     * Sets an URI using the https scheme that the Authorization Server can call to initiate a login at the Client.
     *
     * @param initiateLoginUri The initiate login URI.
     */
    public void setInitiateLoginUri(String initiateLoginUri) {
        this.initiateLoginUri = initiateLoginUri;
    }

    /**
     * Returns an Array of URIs supplied by the RP to request that the user be redirected to this location after a
     * logout has been performed.
     *
     * @return The Array of post logout redirect URIs.
     */
    public String[] getPostLogoutRedirectUris() {
        return postLogoutRedirectUris;
    }

    /**
     * Sets an Array of URIs supplied by the RP to request that the user be redirected to this location after a logout
     * has been performed.
     *
     * @param postLogoutRedirectUris The post logout redirect URI.
     */
    public void setPostLogoutRedirectUris(String[] postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
    }

    /**
     * Returns an Array of Request URI values that are pre-registered by the Client for use at the Authorization Server.
     *
     * @return The request URIs.
     */
    public String[] getRequestUris() {
        return requestUris;
    }

    /**
     * Sets an Array of Request URI values that are pre-registered by the Client for use at the Authorization Server.
     *
     * @param requestUris The request URIs.
     */
    public void setRequestUris(String[] requestUris) {
        this.requestUris = requestUris;
    }

    public String[] getScopes() {
        return scopes;
    }

    public void setScopes(String[] scopes) {
        this.scopes = scopes;
    }

    public String getTrustedClient() {
        return trustedClient;
    }

    public void setTrustedClient(String trustedClient) {
        this.trustedClient = trustedClient;
    }

    /**
     * Gets user group.
     * <p/>
     * Example:
     * "inum=@!1111!0003!D9B4,ou=groups,o=@!1111,o=gluu",
     * "inum=@!1111!0003!A3F4,ou=groups,o=@!1111,o=gluu"
     *
     * @return user group
     */
    public String[] getUserGroups() {
        return userGroups;
    }

    /**
     * Sets user group. Must be valid DN.
     * <p/>
     * Example:
     * "inum=@!1111!0003!D9B4,ou=groups,o=@!1111,o=gluu",
     * "inum=@!1111!0003!A3F4,ou=groups,o=@!1111,o=gluu"
     *
     * @param p_userGroups user group
     */
    public void setUserGroups(String[] p_userGroups) {
        if (LdapUtils.isValidDNs(p_userGroups)) {
            userGroups = p_userGroups;
        }
    }

    public String getFederationId() {
        return federationId;
    }

    public void setFederationId(String p_federationId) {
        federationId = p_federationId;
    }

    public String getFederationURI() {
        return federationURI;
    }

    public void setFederationURI(String p_federationURI) {
        federationURI = p_federationURI;
    }

    public Date getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(Date lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public Date getLastLogonTime() {
        return lastLogonTime;
    }

    public void setLastLogonTime(Date lastLogonTime) {
        this.lastLogonTime = lastLogonTime;
    }

    public Boolean getPersistClientAuthorizations() {
        return persistClientAuthorizations;
    }

    public void setPersistClientAuthorizations(Boolean persistClientAuthorizations) {
        this.persistClientAuthorizations = persistClientAuthorizations;
    }

    public List<CustomAttribute> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(List<CustomAttribute> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public String[] getCustomObjectClasses() {
        return customObjectClasses;
    }

    public void setCustomObjectClasses(String[] p_customObjectClasses) {
        customObjectClasses = p_customObjectClasses;
    }

    public static Client instance() {
        return new Client();
    }

    public static String buildClientDn(String p_clientId) {
        final StringBuilder dn = new StringBuilder();
        dn.append(String.format("inum=%s,", p_clientId));
        dn.append(ConfigurationFactory.instance().getBaseDn().getClients()); // ou=clients,o=@!1111,o=gluu
        return dn.toString();
    }

    public Client setClientIdWithDn(String p_clientId) {
        setClientId(p_clientId);
        setDn(buildClientDn(p_clientId));
        return this;
    }

    /**
     * Returns whether client contains user groups.
     *
     * @return whether client contains user groups
     */
    public boolean hasUserGroups() {
        return !ArrayUtils.isEmpty(userGroups);
    }

    public Object getAttribute(String clientAttribute) throws InvalidClaimException {
        Object attribute = null;

        ScopeService scopeService = ScopeService.instance();

        if (clientAttribute != null) {
            if (clientAttribute.equals("displayName")) {
                attribute = clientName;
            } else if (clientAttribute.equals("inum")) {
                attribute = clientId;
            } else if (clientAttribute.equals("oxAuthAppType")) {
                attribute = applicationType;
            } else if (clientAttribute.equals("oxAuthIdTokenSignedResponseAlg")) {
                attribute = idTokenSignedResponseAlg;
            } else if (clientAttribute.equals("oxAuthRedirectURI") && redirectUris != null) {
                JSONArray array = new JSONArray();
                for (String redirectUri : redirectUris) {
                    array.put(redirectUri);
                }
                attribute = array;
            } else if (clientAttribute.equals("oxAuthScope") && scopes != null) {
                JSONArray array = new JSONArray();
                for (String scopeDN : scopes) {
                    Scope s = scopeService.getScopeByDn(scopeDN);
                    if (s != null) {
                        String scopeName = s.getDisplayName();
                        array.put(scopeName);
                    }
                }
                attribute = array;
            } else {
                for (CustomAttribute customAttribute : customAttributes) {
                    if (customAttribute.getName().equals(clientAttribute)) {
                        List<String> values = customAttribute.getValues();
                        if (values != null) {
                            if (values.size() == 1) {
                                attribute = values.get(0);
                            } else {
                                JSONArray array = new JSONArray();
                                for (String v : values) {
                                    array.put(v);
                                }
                                attribute = array;
                            }
                        }

                        break;
                    }
                }
            }
        }

        return attribute;
    }
}