/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.registration;

import org.apache.commons.lang.StringUtils;
import org.gluu.persist.model.base.CustomAttribute;
import org.gluu.site.ldap.persistence.annotation.*;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.ClientAttributes;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.ref.ClientReference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version December 4, 2018
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthClient"})
public class Client implements Serializable, ClientReference {

    private static final long serialVersionUID = -6832496019942067969L;

    @LdapDN
    private String dn;

    @LdapAttribute(name = "inum")
    private String clientId;

    @LdapAttribute(name = "oxAuthClientSecret")
    private String encodedClientSecret;

    @LdapAttribute(name = "oxAuthLogoutURI")
    private String[] frontChannelLogoutUri;

    @LdapAttribute(name = "oxAuthLogoutSessionRequired")
    private Boolean frontChannelLogoutSessionRequired;

    @LdapAttribute(name = "oxAuthRegistrationAccessToken")
    private String registrationAccessToken;

    @LdapAttribute(name = "oxAuthClientIdIssuedAt")
    private Date clientIdIssuedAt;

    @LdapAttribute(name = "oxAuthClientSecretExpiresAt")
    private Date clientSecretExpiresAt;

    @LdapAttribute(name = "oxAuthRedirectURI")
    private String[] redirectUris;

    @LdapAttribute(name = "oxClaimRedirectURI")
    private String[] claimRedirectUris;

    @LdapAttribute(name = "oxAuthResponseType")
    private ResponseType[] responseTypes;

    @LdapAttribute(name = "oxAuthGrantType")
    private GrantType[] grantTypes;

    @LdapAttribute(name = "oxAuthAppType")
    private String applicationType;

    @LdapAttribute(name = "oxAuthContact")
    private String[] contacts;

    @LdapAttribute(name = "displayName")
    private String clientName;

    @LdapAttribute(name = "oxIdTokenTokenBindingCnf")
    private String idTokenTokenBindingCnf;

    @LdapAttribute(name = "oxAuthLogoURI")
    private String logoUri;

    @LdapAttribute(name = "oxAuthClientURI")
    private String clientUri;

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

    @LdapAttribute(name = "oxAuthIdTokenSignedResponseAlg")
    private String idTokenSignedResponseAlg;

    @LdapAttribute(name = "oxAuthIdTokenEncryptedResponseAlg")
    private String idTokenEncryptedResponseAlg;

    @LdapAttribute(name = "oxAuthIdTokenEncryptedResponseEnc")
    private String idTokenEncryptedResponseEnc;

    @LdapAttribute(name = "oxAuthSignedResponseAlg")
    private String userInfoSignedResponseAlg;

    @LdapAttribute(name = "oxAuthUserInfoEncryptedResponseAlg")
    private String userInfoEncryptedResponseAlg;

    @LdapAttribute(name = "oxAuthUserInfoEncryptedResponseEnc")
    private String userInfoEncryptedResponseEnc;

    @LdapAttribute(name = "oxAuthRequestObjectSigningAlg")
    private String requestObjectSigningAlg;

    @LdapAttribute(name = "oxAuthRequestObjectEncryptionAlg")
    private String requestObjectEncryptionAlg;

    @LdapAttribute(name = "oxAuthRequestObjectEncryptionEnc")
    private String requestObjectEncryptionEnc;

    @LdapAttribute(name = "oxAuthTokenEndpointAuthMethod")
    private String tokenEndpointAuthMethod;

    @LdapAttribute(name = "oxAuthTokenEndpointAuthSigningAlg")
    private String tokenEndpointAuthSigningAlg;

    @LdapAttribute(name = "oxAuthDefaultMaxAge")
    private Integer defaultMaxAge;

    @LdapAttribute(name = "oxAuthRequireAuthTime")
    private boolean requireAuthTime;

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

    @LdapAttribute(name = "oxAuthClaim")
    private String[] claims;

    @LdapAttribute(name = "oxAuthTrustedClient")
    private boolean trustedClient;

    @LdapAttribute(name = "oxLastAccessTime")
    private Date lastAccessTime;

    @LdapAttribute(name = "oxLastLogonTime")
    private Date lastLogonTime;

    @LdapAttribute(name = "oxPersistClientAuthorizations")
    private boolean persistClientAuthorizations;

    @LdapAttribute(name = "oxIncludeClaimsInIdToken")
    private boolean includeClaimsInIdToken;

    @LdapAttribute(name = "oxRefreshTokenLifetime")
    private Integer refreshTokenLifetime;

    @LdapAttribute(name = "oxAccessTokenLifetime")
    private Integer accessTokenLifetime;

    @LdapAttributesList(name = "name", value = "values", sortByName = true)
    private List<CustomAttribute> customAttributes = new ArrayList<CustomAttribute>();

    @LdapCustomObjectClass
    private String[] customObjectClasses;

    @LdapAttribute(name = "oxRptAsJwt")
    private boolean rptAsJwt = false;

    @LdapAttribute(name = "oxAccessTokenAsJwt")
    private boolean accessTokenAsJwt = false;

    @LdapAttribute(name = "oxAccessTokenSigningAlg")
    private String accessTokenSigningAlg;

    @LdapAttribute(name = "oxDisabled")
    private boolean disabled;

    @LdapAttribute(name = "oxAuthAuthorizedOrigins")
    private String[] authorizedOrigins;

    @LdapAttribute(name = "oxSoftwareId")
    private String softwareId;

    @LdapAttribute(name = "oxSoftwareVersion")
    private String softwareVersion;

    @LdapAttribute(name = "oxSoftwareStatement")
    private String softwareStatement;

    @LdapAttribute(name = "oxAttributes")
    @LdapJsonObject
    private ClientAttributes attributes;

    public ClientAttributes getAttributes() {
        if (attributes == null) {
            attributes = new ClientAttributes();
        }
        return attributes;
    }

    public void setAttributes(ClientAttributes attributes) {
        this.attributes = attributes;
    }

    public boolean isRptAsJwt() {
        return rptAsJwt;
    }

    public void setRptAsJwt(boolean rptAsJwt) {
        this.rptAsJwt = rptAsJwt;
    }

    public boolean isAccessTokenAsJwt() {
        return accessTokenAsJwt;
    }

    public void setAccessTokenAsJwt(boolean accessTokenAsJwt) {
        this.accessTokenAsJwt = accessTokenAsJwt;
    }

    public String getAccessTokenSigningAlg() {
        return accessTokenSigningAlg;
    }

    public void setAccessTokenSigningAlg(String accessTokenSigningAlg) {
        this.accessTokenSigningAlg = accessTokenSigningAlg;
    }

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
     * Gets logout session required.
     *
     * @return logout session required
     */
    public Boolean getFrontChannelLogoutSessionRequired() {
        return frontChannelLogoutSessionRequired;
    }

    /**
     * Sets frontchannel logout session required.
     *
     * @param frontChannelLogoutSessionRequired frontchannel logout session required
     */
    public void setFrontChannelLogoutSessionRequired(Boolean frontChannelLogoutSessionRequired) {
        this.frontChannelLogoutSessionRequired = frontChannelLogoutSessionRequired;
    }

    /**
     * Gets logout uri
     *
     * @return logout uri
     */
    public String[] getFrontChannelLogoutUri() {
        return frontChannelLogoutUri;
    }

    /**
     * Sets logout uri.
     *
     * @param frontChannelLogoutUri logout uri
     */
    public void setFrontChannelLogoutUri(String[] frontChannelLogoutUri) {
        this.frontChannelLogoutUri = frontChannelLogoutUri;
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
    public String getClientSecret() {
        return encodedClientSecret;
    }

    /**
     * Sets the client secret.
     *
     * @param clientSecret The client secret.
     */
    public void setClientSecret(String clientSecret) {
        encodedClientSecret = clientSecret;
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
     * Returns UMA2 Array of The Claims Redirect URIs to which the client wishes the authorization server to direct
     * the requesting party's user agent after completing its interaction.
     * The URI MUST be absolute, MAY contain an application/x-www-form-urlencoded-formatted query parameter component
     * that MUST be retained when adding additional parameters, and MUST NOT contain a fragment component.
     * The client SHOULD pre-register its claims_redirect_uri with the authorization server, and the authorization server
     * SHOULD require all clients to pre-register their claims redirection endpoints. Claims redirection URIs
     * are different from the redirection URIs defined in [RFC6749] in that they are intended for the exclusive use
     * of requesting parties and not resource owners. Therefore, authorization servers MUST NOT redirect requesting parties
     * to pre-registered redirection URIs defined in [RFC6749] unless such URIs are also pre-registered specifically as
     * claims redirection URIs. If the URI is pre-registered, this URI MUST exactly match one of the pre-registered claims
     * redirection URIs, with the matching performed as described in Section 6.2.1 of [RFC3986] (Simple String Comparison).
     *
     * @return claims redirect uris
     */
    public String[] getClaimRedirectUris() {
        return claimRedirectUris;
    }

    /**
     * Sets Claim redirect URIs
     *
     * @param claimRedirectUris claims redirect uris
     */
    public void setClaimRedirectUris(String[] claimRedirectUris) {
        this.claimRedirectUris = claimRedirectUris;
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
    public GrantType[] getGrantTypes() {
        return grantTypes;
    }

    /**
     * Sets a JSON array containing a list of the OAuth 2.0 grant types that the Client is declaring that it will
     * restrict itself to using.
     *
     * @param grantTypes The grant types.
     */
    public void setGrantTypes(GrantType[] grantTypes) {
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

    public String getIdTokenTokenBindingCnf() {
        return idTokenTokenBindingCnf;
    }

    public void setIdTokenTokenBindingCnf(String idTokenTokenBindingCnf) {
        this.idTokenTokenBindingCnf = idTokenTokenBindingCnf;
    }

    public boolean isTokenBindingSupported() {
        return StringUtils.isNotBlank(idTokenTokenBindingCnf);
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
     * Returns the JWE alg algorithm (JWA) the RP is declaring that it may use for encrypting Request Objects
     * sent to the OP.
     *
     * @return The JWE alg algorithm (JWA).
     */
    public String getRequestObjectEncryptionAlg() {
        return requestObjectEncryptionAlg;
    }

    /**
     * Sets the JWE alg algorithm (JWA) the RP is declaring that it may use for encrypting Request Objects
     * sent to the OP.
     *
     * @param requestObjectEncryptionAlg The JWE alg algorithm (JWA).
     */
    public void setRequestObjectEncryptionAlg(String requestObjectEncryptionAlg) {
        this.requestObjectEncryptionAlg = requestObjectEncryptionAlg;
    }

    /**
     * Returns the JWE enc algorithm (JWA) the RP is declaring that it may use for encrypting Request Objects
     * sent to the OP.
     *
     * @return The JWE enc algorithm (JWA).
     */
    public String getRequestObjectEncryptionEnc() {
        return requestObjectEncryptionEnc;
    }

    /**
     * Sets the JWE enc algorithm (JWA) the RP is declaring that it may use for encrypting Request Objects
     * sent to the OP.
     *
     * @param requestObjectEncryptionEnc The JWE enc algorithm (JWA).
     */
    public void setRequestObjectEncryptionEnc(String requestObjectEncryptionEnc) {
        this.requestObjectEncryptionEnc = requestObjectEncryptionEnc;
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
     * Returns the Requested Client Authentication method for the Token Endpoint.
     *
     * @return The Requested Client Authentication method for the Token Endpoint.
     */
    public String getTokenEndpointAuthSigningAlg() {
        return tokenEndpointAuthSigningAlg;
    }

    /**
     * Sets the Requested Client Authentication method for the Token Endpoint.
     *
     * @param tokenEndpointAuthSigningAlg The Requested Client Authentication method for the Token Endpoint.
     */
    public void setTokenEndpointAuthSigningAlg(String tokenEndpointAuthSigningAlg) {
        this.tokenEndpointAuthSigningAlg = tokenEndpointAuthSigningAlg;
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
     * Returns a boolean value specifying whether the auth_time Claim in the ID Token is required.
     * It is required when the value is true. The auth_time Claim request in the Request Object overrides this setting.
     *
     * @return The required authentication time.
     */
    public boolean getRequireAuthTime() {
        return requireAuthTime;
    }

    /**
     * Sets a boolean value specifying whether the auth_time Claim in the ID Token is required.
     * It is required when the value is true. The auth_time Claim request in the Request Object overrides this setting.
     *
     * @param requireAuthTime The required authentication time.
     */
    public void setRequireAuthTime(boolean requireAuthTime) {
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

    public String[] getClaims() {
        return claims;
    }

    public void setClaims(String[] claims) {
        this.claims = claims;
    }

    public boolean getTrustedClient() {
        return trustedClient;
    }

    public void setTrustedClient(boolean trustedClient) {
        this.trustedClient = trustedClient;
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

    public boolean getPersistClientAuthorizations() {
        return persistClientAuthorizations;
    }

    public void setPersistClientAuthorizations(boolean persistClientAuthorizations) {
        this.persistClientAuthorizations = persistClientAuthorizations;
    }

    public boolean isIncludeClaimsInIdToken() {
        return includeClaimsInIdToken;
    }

    public void setIncludeClaimsInIdToken(boolean includeClaimsInIdToken) {
        this.includeClaimsInIdToken = includeClaimsInIdToken;
    }

    public Integer getRefreshTokenLifetime() {
        return refreshTokenLifetime;
    }

    public void setRefreshTokenLifetime(Integer refreshTokenLifetime) {
        this.refreshTokenLifetime = refreshTokenLifetime;
    }

    public Integer getAccessTokenLifetime() {
        return accessTokenLifetime;
    }

    public void setAccessTokenLifetime(Integer accessTokenLifetime) {
        this.accessTokenLifetime = accessTokenLifetime;
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

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String[] getAuthorizedOrigins() {
        return authorizedOrigins;
    }

    public void setAuthorizedOrigins(String[] authorizedOrigins) {
        this.authorizedOrigins = authorizedOrigins;
    }

    public String getSoftwareId() {
        return softwareId;
    }

    public void setSoftwareId(String softwareId) {
        this.softwareId = softwareId;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public String getSoftwareStatement() {
        return softwareStatement;
    }

    public void setSoftwareStatement(String softwareStatement) {
        this.softwareStatement = softwareStatement;
    }

    public static Client instance() {
        return new Client();
    }

}