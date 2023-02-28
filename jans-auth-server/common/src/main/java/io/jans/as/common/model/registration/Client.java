/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.model.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.jans.as.model.common.*;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.persistence.model.ClientAttributes;
import io.jans.orm.annotation.*;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.model.base.DeletableEntity;
import io.jans.orm.model.base.LocalizedString;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author Javier Rojas Blum
 * @version October 17, 2022
 */
@DataEntry(sortBy = {"displayName"})
@ObjectClass(value = "jansClnt")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Client extends DeletableEntity implements Serializable {

    private static final long serialVersionUID = -6832496019942067971L;

    @JsonProperty("inum")
    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String clientId;

    @AttributeName(name = "jansClntSecret")
    private String clientSecret;

    @AttributeName(name = "jansLogoutURI")
    private String frontChannelLogoutUri;

    @AttributeName(name = "jansLogoutSessRequired")
    private Boolean frontChannelLogoutSessionRequired;

    @AttributeName(name = "jansRegistrationAccessTkn")
    private String registrationAccessToken;

    @AttributeName(name = "jansClntIdIssuedAt")
    private Date clientIdIssuedAt;

    @AttributeName(name = "jansClntSecretExpAt")
    private Date clientSecretExpiresAt;

    @AttributeName(name = "jansRedirectURI")
    private String[] redirectUris;

    @AttributeName(name = "jansClaimRedirectURI")
    private String[] claimRedirectUris;

    @AttributeName(name = "jansRespTyp")
    private ResponseType[] responseTypes;

    @AttributeName(name = "jansGrantTyp")
    private GrantType[] grantTypes;

    @AttributeName(name = "jansAppTyp")
    private ApplicationType applicationType = ApplicationType.WEB;

    @AttributeName(name = "jansContact")
    private String[] contacts;

    @AttributeName(name = "tknBndCnf")
    private String idTokenTokenBindingCnf;

    @AttributeName(name = "displayName")
    private String clientName;

    @AttributeName(name = "jansLogoURI")
    private String logoUri;

    @AttributeName(name = "jansClntURI")
    private String clientUri;

    @AttributeName(name = "jansPolicyURI")
    private String policyUri;

    @AttributeName(name = "jansTosURI")
    private String tosUri;

    @AttributeName(name = "displayNameLocalized")
    @JsonObject
    @LanguageTag
    private LocalizedString clientNameLocalized = new LocalizedString();

    @AttributeName(name = "jansLogoURILocalized")
    @JsonObject
    @LanguageTag
    private LocalizedString logoUriLocalized = new LocalizedString();

    @AttributeName(name = "jansClntURILocalized")
    @JsonObject
    @LanguageTag
    private LocalizedString clientUriLocalized = new LocalizedString();

    @AttributeName(name = "jansPolicyURILocalized")
    @JsonObject
    @LanguageTag
    private LocalizedString policyUriLocalized = new LocalizedString();

    @AttributeName(name = "jansTosURILocalized")
    @JsonObject
    @LanguageTag
    private LocalizedString tosUriLocalized = new LocalizedString();

    @AttributeName(name = "jansJwksURI")
    private String jwksUri;

    @AttributeName(name = "jansJwks")
    private String jwks;

    @AttributeName(name = "jansSectorIdentifierURI")
    private String sectorIdentifierUri;

    @AttributeName(name = "jansSubjectTyp")
    private SubjectType subjectType = SubjectType.PUBLIC;

    @AttributeName(name = "jansIdTknSignedRespAlg")
    private String idTokenSignedResponseAlg;

    @AttributeName(name = "jansIdTknEncRespAlg")
    private String idTokenEncryptedResponseAlg;

    @AttributeName(name = "jansIdTknEncRespEnc")
    private String idTokenEncryptedResponseEnc;

    @AttributeName(name = "jansSignedRespAlg")
    private String userInfoSignedResponseAlg;

    @AttributeName(name = "jansUsrInfEncRespAlg")
    private String userInfoEncryptedResponseAlg;

    @AttributeName(name = "jansUsrInfEncRespEnc")
    private String userInfoEncryptedResponseEnc;

    @AttributeName(name = "jansReqObjSigAlg")
    private String requestObjectSigningAlg;

    @AttributeName(name = "jansReqObjEncAlg")
    private String requestObjectEncryptionAlg;

    @AttributeName(name = "jansReqObjEncEnc")
    private String requestObjectEncryptionEnc;

    @AttributeName(name = "jansTknEndpointAuthMethod")
    private String tokenEndpointAuthMethod;

    @AttributeName(name = "jansTknEndpointAuthSigAlg")
    private String tokenEndpointAuthSigningAlg;

    @AttributeName(name = "jansDefMaxAge")
    private Integer defaultMaxAge;

    @AttributeName(name = "jansDefAcrValues")
    private String[] defaultAcrValues;

    @AttributeName(name = "jansInitiateLoginURI")
    private String initiateLoginUri;

    @AttributeName(name = "jansPostLogoutRedirectURI")
    private String[] postLogoutRedirectUris;

    @AttributeName(name = "jansReqURI")
    private String[] requestUris;

    @AttributeName(name = "jansScope")
    private String[] scopes;

    @AttributeName(name = "jansClaim")
    private String[] claims;

    @AttributeName(name = "jansTrustedClnt")
    private boolean trustedClient;

    @AttributeName(name = "jansLastAccessTime")
    private Date lastAccessTime;

    @AttributeName(name = "jansLastLogonTime")
    private Date lastLogonTime;

    @AttributeName(name = "jansPersistClntAuthzs")
    private boolean persistClientAuthorizations;

    @AttributeName(name = "jansInclClaimsInIdTkn")
    private boolean includeClaimsInIdToken;

    @AttributeName(name = "jansRefreshTknLife")
    private Integer refreshTokenLifetime;

    @AttributeName(name = "jansAccessTknLife")
    private Integer accessTokenLifetime;

    @AttributesList(name = "name", value = "values", multiValued = "multiValued", sortByName = true)
    private List<CustomObjectAttribute> customAttributes = new ArrayList<>();

    @CustomObjectClass
    private String[] customObjectClasses;

    @AttributeName(name = "jansRptAsJwt")
    private boolean rptAsJwt = false;

    @AttributeName(name = "jansAccessTknAsJwt")
    private boolean accessTokenAsJwt = false;

    @AttributeName(name = "jansAccessTknSigAlg")
    private String accessTokenSigningAlg;

    @AttributeName(name = "jansDisabled")
    private boolean disabled;

    @AttributeName(name = "jansAuthorizedOrigins")
    private String[] authorizedOrigins;

    @AttributeName(name = "jansSoftId")
    private String softwareId;

    @AttributeName(name = "jansSoftVer")
    private String softwareVersion;

    @AttributeName(name = "jansSoftStatement")
    private String softwareStatement;

    @AttributeName(name = "jansAttrs")
    @JsonObject
    private ClientAttributes attributes;

    @AttributeName(name = "jansBackchannelTknDeliveryMode")
    private BackchannelTokenDeliveryMode backchannelTokenDeliveryMode;

    @AttributeName(name = "jansBackchannelClntNotificationEndpoint")
    private String backchannelClientNotificationEndpoint;

    @AttributeName(name = "jansBackchannelAuthnReqSigAlg")
    private AsymmetricSignatureAlgorithm backchannelAuthenticationRequestSigningAlg;

    @AttributeName(name = "jansBackchannelUsrCodeParameter")
    private Boolean backchannelUserCodeParameter;

    @AttributeName(name = "description")
    private String description;

    @AttributeName(name = "o")
    private String organization;

    @AttributeName(name = "jansGrp")
    private String[] groups;

    @Expiration
    private Integer ttl;

    public String[] getGroups() {
        return groups;
    }

    public void setGroups(String[] groups) {
        this.groups = groups;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public ClientAttributes getAttributes() {
        if (attributes == null) {
            attributes = new ClientAttributes();
        }
        return attributes;
    }

    public void setAttributes(ClientAttributes attributes) {
        this.attributes = attributes;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
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

    /**
     * Gets logout session required.
     *
     * @return logout session required
     */
    public Boolean getFrontChannelLogoutSessionRequired() {
        if (frontChannelLogoutSessionRequired == null) frontChannelLogoutSessionRequired = false;
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
    public String getFrontChannelLogoutUri() {
        return frontChannelLogoutUri;
    }

    /**
     * Sets logout uri.
     *
     * @param frontChannelLogoutUri logout uri
     */
    public void setFrontChannelLogoutUri(String frontChannelLogoutUri) {
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
     * Returns the client secret.
     *
     * @return The client secret.
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Sets the client secret.
     *
     * @param clientSecret The client secret.
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
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
        if (grantTypes == null) grantTypes = new GrantType[0];
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
    public ApplicationType getApplicationType() {
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
    public void setApplicationType(ApplicationType applicationType) {
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

    public String getIdTokenTokenBindingCnf() {
        return idTokenTokenBindingCnf;
    }

    public void setIdTokenTokenBindingCnf(String idTokenTokenBindingCnf) {
        this.idTokenTokenBindingCnf = idTokenTokenBindingCnf;
    }

    @JsonIgnore
    public boolean isTokenBindingSupported() {
        return StringUtils.isNotBlank(idTokenTokenBindingCnf);
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
     * Returns a URL that references a logo for the Client application.
     *
     * @return The URL of a logo image for the Client where it can be retrieved.
     */
    public String getLogoUri() {
        return logoUri;
    }

    /**
     * Sets a URL that references a logo for the Client application.
     *
     * @param logoUri The URL of a logo image for the Client where it can be retrieved.
     */
    public void setLogoUri(String logoUri) {
        this.logoUri = logoUri;
    }

    /**
     * Returns a URL of the home page of the Client.
     *
     * @return The URL of the home page of the Client.
     */
    public String getClientUri() {
        return clientUri;
    }

    /**
     * Sets a URL of the home page of the Client.
     *
     * @param clientUri The URL of the home page of the Client.
     */
    public void setClientUri(String clientUri) {
        this.clientUri = clientUri;
    }

    /**
     * Returns a URL that the Relying Party Client provides to the End-User to read about how the profile data will
     * be used.
     *
     * @return A URL location about how the profile data will be used.
     */
    public String getPolicyUri() {
        return policyUri;
    }

    /**
     * Sets a URL that the Relying Party Client provides to the End-User to read about how the profile data will
     * be used.
     *
     * @param policyUri A URL location about how the profile data will be used.
     */
    public void setPolicyUri(String policyUri) {
        this.policyUri = policyUri;
    }

    /**
     * Returns a URL that the Relying Party Client provides to the End-User to read about the Relying Party's terms
     * of service.
     *
     * @return The terms of service URL.
     */
    public String getTosUri() {
        return tosUri;
    }

    /**
     * Sets a URL that the Relying Party Client provides to the End-User to read about the Relying Party's terms of
     * service.
     *
     * @param tosUri The terms of service URL.
     */
    public void setTosUri(String tosUri) {
        this.tosUri = tosUri;
    }

    /**
     * Returns the name of the Client to be presented to the user represented in a language and a script.
     *
     * @return The name of the Client to be presented to the user.
     */
    public LocalizedString getClientNameLocalized() {
        return clientNameLocalized;
    }

    @JsonSetter
    public void setClientNameLocalized(LocalizedString clientNameLocalized) {
        this.clientNameLocalized = clientNameLocalized;
    }

    @JsonSetter
    public void setLogoUriLocalized(LocalizedString logoUriLocalized) {
        this.logoUriLocalized = logoUriLocalized;
    }

    @JsonSetter
    public void setClientUriLocalized(LocalizedString clientUriLocalized) {
        this.clientUriLocalized = clientUriLocalized;
    }

    @JsonSetter
    public void setPolicyUriLocalized(LocalizedString policyUriLocalized) {
        this.policyUriLocalized = policyUriLocalized;
    }

    @JsonSetter
    public void setTosUriLocalized(LocalizedString tosUriLocalized) {
        this.tosUriLocalized = tosUriLocalized;
    }

    /**
     * Sets the name of the Client to be presented to the user.
     *
     * @param clientName The name of the Client to be presented to the user.
     */
    @JsonIgnore
    public void setClientNameLocalized(String clientName) {
        this.clientName = clientName;
        this.clientNameLocalized.setValue(clientName);
    }

    /**
     * Sets the name of the Client to be presented to the user represented in a language and a script.
     *
     * @param clientName The name of the Client to be presented to the user.
     * @param locale     The locale
     */
    @JsonIgnore
    public void setClientNameLocalized(String clientName, Locale locale) {
        if (StringUtils.isNotBlank(locale.toString())) {
            this.clientNameLocalized.setValue(clientName, locale);
        } else {
            setClientNameLocalized(clientName);
        }
    }

    /**
     * Returns a URL that references a logo for the Client application represented in a language and a script.
     *
     * @return The URL of a logo image for the Client where it can be retrieved.
     */
    public LocalizedString getLogoUriLocalized() {
        return logoUriLocalized;
    }

    /**
     * Sets a URL that references a logo for the Client application.
     *
     * @param logoUri The URL of a logo image for the Client where it can be retrieved.
     */
    @JsonIgnore
    public void setLogoUriLocalized(String logoUri) {
        this.logoUri = logoUri;
        this.logoUriLocalized.setValue(logoUri);
    }

    /**
     * Sets a URL that references a logo for the Client application represented in a language and script.
     *
     * @param logoUri The URL of a logo image for the Client where it can be retrieved.
     * @param locale  The locale
     */
    @JsonIgnore
    public void setLogoUriLocalized(String logoUri, Locale locale) {
        if (StringUtils.isNotBlank(locale.toString())) {
            this.logoUriLocalized.setValue(logoUri, locale);
        } else {
            setLogoUriLocalized(logoUri);
        }
    }

    /**
     * Returns a URL of the home page of the Client represented in a language and script
     *
     * @return The URL of the home page of the Client.
     */
    public LocalizedString getClientUriLocalized() {
        return clientUriLocalized;
    }

    /**
     * Sets a URL of the home page of the Client.
     *
     * @param clientUri The URL of the home page of the Client.
     */
    @JsonIgnore
    public void setClientUriLocalized(String clientUri) {
        this.clientUri = clientUri;
        this.clientUriLocalized.setValue(clientUri);
    }

    /**
     * Sets a URL of the home page of the Client represented in a language and script.
     *
     * @param clientUri The URL of the home page of the Client.
     * @param locale    The locale
     */
    @JsonIgnore
    public void setClientUriLocalized(String clientUri, Locale locale) {
        if (StringUtils.isNotBlank(locale.toString())) {
            this.clientUriLocalized.setValue(clientUri, locale);
        } else {
            setClientUriLocalized(clientUri);
        }
    }

    /**
     * Returns a URL that the Relying Party Client provides to the End-User to read about how the profile data will
     * be used represented in a language and script.
     *
     * @return A URL location about how the profile data will be used.
     */
    public LocalizedString getPolicyUriLocalized() {
        return policyUriLocalized;
    }

    /**
     * Sets a URL that the Relying Party Client provides to the End-User to read about how the profile data will
     * be used.
     *
     * @param policyUri A URL location about how the profile data will be used.
     */
    @JsonIgnore
    public void setPolicyUriLocalized(String policyUri) {
        this.policyUri = policyUri;
        this.policyUriLocalized.setValue(policyUri);
    }

    /**
     * Sets a URL that the Relying Party Client provides to the End-User to read about how the profile data will
     * be used represented in a language and script.
     *
     * @param policyUri A URL location about how the profile data will be used.
     * @param locale    The locale
     */
    @JsonIgnore
    public void setPolicyUriLocalized(String policyUri, Locale locale) {
        if (StringUtils.isNotBlank(locale.toString())) {
            this.policyUriLocalized.setValue(policyUri, locale);
        } else {
            setPolicyUriLocalized(policyUri);
        }
    }

    /**
     * Returns a URL that the Relying Party Client provides to the End-User to read about the Relying Party's terms
     * of service represented in a language and script.
     *
     * @return The terms of service URL.
     */
    public LocalizedString getTosUriLocalized() {
        return tosUriLocalized;
    }

    /**
     * Sets a URL that the Relying Party Client provides to the End-User to read about the Relying Party's terms of
     * service.
     *
     * @param tosUri The terms of service URL.
     */
    @JsonIgnore
    public void setTosUriLocalized(String tosUri) {
        this.tosUri = tosUri;
        this.tosUriLocalized.setValue(tosUri);
    }

    /**
     * Sets a URL that the Relying Party Client provides to the End-User to read about the Relying Party's terms of
     * service represented in a language and script.
     *
     * @param tosUri The terms of service URL.
     * @param locale The Locale
     */
    @JsonIgnore
    public void setTosUriLocalized(String tosUri, Locale locale) {
        if (StringUtils.isNotBlank(locale.toString())) {
            this.tosUriLocalized.setValue(tosUri, locale);
        } else {
            setTosUriLocalized(tosUri);
        }
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
    public SubjectType getSubjectType() {
        return subjectType;
    }

    /**
     * Sets the Subject type quested for the Client ID. Valid types include pairwise and public.
     *
     * @param subjectType The subject type.
     */
    public void setSubjectType(SubjectType subjectType) {
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

    public List<CustomObjectAttribute> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(List<CustomObjectAttribute> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public String[] getCustomObjectClasses() {
        return customObjectClasses;
    }

    public void setCustomObjectClasses(String[] customObjectClasses) {
        this.customObjectClasses = customObjectClasses;
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

    public void setSoftwareVersion(String jsSoftVer) {
        this.softwareVersion = jsSoftVer;
    }

    public String getSoftwareStatement() {
        return softwareStatement;
    }

    public void setSoftwareStatement(String softwareStatement) {
        this.softwareStatement = softwareStatement;
    }

    public BackchannelTokenDeliveryMode getBackchannelTokenDeliveryMode() {
        return backchannelTokenDeliveryMode;
    }

    public void setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode backchannelTokenDeliveryMode) {
        this.backchannelTokenDeliveryMode = backchannelTokenDeliveryMode;
    }

    public String getBackchannelClientNotificationEndpoint() {
        return backchannelClientNotificationEndpoint;
    }

    public void setBackchannelClientNotificationEndpoint(String backchannelClientNotificationEndpoint) {
        this.backchannelClientNotificationEndpoint = backchannelClientNotificationEndpoint;
    }

    public AsymmetricSignatureAlgorithm getBackchannelAuthenticationRequestSigningAlg() {
        return backchannelAuthenticationRequestSigningAlg;
    }

    public void setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm backchannelAuthenticationRequestSigningAlg) {
        this.backchannelAuthenticationRequestSigningAlg = backchannelAuthenticationRequestSigningAlg;
    }

    public Boolean getBackchannelUserCodeParameter() {
        return backchannelUserCodeParameter;
    }

    public void setBackchannelUserCodeParameter(Boolean backchannelUserCodeParameter) {
        this.backchannelUserCodeParameter = backchannelUserCodeParameter;
    }

    public String getDisplayName() {
        return getClientName();
    }

    public void setDisplayName(String displayName) {
        setClientName(displayName);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}