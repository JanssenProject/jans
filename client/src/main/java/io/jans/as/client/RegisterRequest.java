/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.jans.as.client.model.SoftwareStatement;
import io.jans.as.client.util.ClientUtil;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.json.JsonApplier;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.register.RegisterRequestParam;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.jans.as.client.util.ClientUtil.booleanOrNull;
import static io.jans.as.client.util.ClientUtil.extractListByKey;
import static io.jans.as.client.util.ClientUtil.integerOrNull;
import static io.jans.as.model.register.RegisterRequestParam.*;
import static io.jans.as.model.util.StringUtils.*;

/**
 * Represents a register request to send to the authorization server.
 *
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 * @version July 28, 2021
 */
public class RegisterRequest extends BaseRequest {

    private static final Logger log = Logger.getLogger(RegisterRequest.class);

    private String registrationAccessToken;
    private List<String> redirectUris;
    private List<String> claimsRedirectUris;

    /**
     * code: authorization_code
     * id_token: implicit
     * token id_token: implicit
     * code id_token: authorization_code, implicit
     * code token: authorization_code, implicit
     * code token id_token: authorization_code, implicit
     * <p>
     * https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata
     */
    private List<String> responseTypes;
    private List<GrantType> grantTypes;
    private ApplicationType applicationType;
    private List<String> contacts;
    private String clientName;
    private String logoUri;
    private String clientUri;
    private String policyUri;
    private String frontChannelLogoutUri;
    private Boolean frontChannelLogoutSessionRequired;
    private List<String> backchannelLogoutUris;
    private Boolean backchannelLogoutSessionRequired;
    private String tosUri;
    private String jwksUri;
    private String jwks;
    private String sectorIdentifierUri;
    private String idTokenTokenBindingCnf;
    private String tlsClientAuthSubjectDn;
    private Boolean allowSpontaneousScopes;
    private List<String> spontaneousScopes;
    private Boolean runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims;
    private Boolean keepClientAuthorizationAfterExpiration;
    private SubjectType subjectType;
    private Boolean rptAsJwt;
    private Boolean accessTokenAsJwt;
    private SignatureAlgorithm accessTokenSigningAlg;
    private SignatureAlgorithm authorizationSignedResponseAlg;
    private KeyEncryptionAlgorithm authorizationEncryptedResponseAlg;
    private BlockEncryptionAlgorithm authorizationEncryptedResponseEnc;
    private SignatureAlgorithm idTokenSignedResponseAlg;
    private KeyEncryptionAlgorithm idTokenEncryptedResponseAlg;
    private BlockEncryptionAlgorithm idTokenEncryptedResponseEnc;
    private SignatureAlgorithm userInfoSignedResponseAlg;
    private KeyEncryptionAlgorithm userInfoEncryptedResponseAlg;
    private BlockEncryptionAlgorithm userInfoEncryptedResponseEnc;
    private SignatureAlgorithm requestObjectSigningAlg;
    private KeyEncryptionAlgorithm requestObjectEncryptionAlg;
    private BlockEncryptionAlgorithm requestObjectEncryptionEnc;
    private AuthenticationMethod tokenEndpointAuthMethod;
    private SignatureAlgorithm tokenEndpointAuthSigningAlg;
    private Integer defaultMaxAge;
    private Boolean requireAuthTime;
    private List<String> defaultAcrValues;
    private String initiateLoginUri;
    private List<String> postLogoutRedirectUris;
    private List<String> requestUris;
    private List<String> authorizedOrigins;
    private Integer accessTokenLifetime;
    private Integer parLifetime;
    private Boolean requirePar;
    private String softwareId;
    private String softwareVersion;
    private String softwareStatement;
    private BackchannelTokenDeliveryMode backchannelTokenDeliveryMode;
    private String backchannelClientNotificationEndpoint;
    private AsymmetricSignatureAlgorithm backchannelAuthenticationRequestSigningAlg;
    private Boolean backchannelUserCodeParameter;
    private List<String> additionalAudience;

    /**
     * String containing a space-separated list of scope values. (correct name is 'scope' not 'scopes', see (rfc7591).)
     */
    private List<String> scope;

    /**
     * String containing a space-separated list of claims that can be requested individually.
     */
    private List<String> claims;

    private final Map<String, String> customAttributes;

    // internal state
    private JSONObject jsonObject;
    private String httpMethod;
    private String jwtRequestAsString;

    /**
     * Common constructor.
     */
    public RegisterRequest() {
        setContentType(MediaType.APPLICATION_JSON);
        setMediaType(MediaType.APPLICATION_JSON);

        this.redirectUris = new ArrayList<>();
        this.claimsRedirectUris = new ArrayList<>();
        this.responseTypes = new ArrayList<>();
        this.grantTypes = new ArrayList<>();
        this.contacts = new ArrayList<>();
        this.defaultAcrValues = new ArrayList<>();
        this.postLogoutRedirectUris = new ArrayList<>();
        this.requestUris = new ArrayList<>();
        this.authorizedOrigins = new ArrayList<>();
        this.scope = new ArrayList<>();
        this.claims = new ArrayList<>();
        this.customAttributes = new HashMap<>();
    }

    /**
     * Constructs a request for Client Registration
     *
     * @param applicationType The application type.
     * @param clientName      The Client Name
     * @param redirectUris    A list of redirection URIs.
     */
    public RegisterRequest(ApplicationType applicationType, String clientName,
                           List<String> redirectUris) {
        this();
        this.applicationType = applicationType;
        this.clientName = clientName;
        this.redirectUris = redirectUris;
    }

    /**
     * Constructs a request for Client Read
     *
     * @param registrationAccessToken The Registration Access Token.
     */
    public RegisterRequest(String registrationAccessToken) {
        this();
        this.registrationAccessToken = registrationAccessToken;
    }

    @Override
    public String getContentType() {
        if (hasJwtRequestAsString()) {
            return "application/jwt";
        }
        return super.getContentType();
    }

    public String getTlsClientAuthSubjectDn() {
        return tlsClientAuthSubjectDn;
    }

    public void setTlsClientAuthSubjectDn(String tlsClientAuthSubjectDn) {
        this.tlsClientAuthSubjectDn = tlsClientAuthSubjectDn;
    }

    public Boolean getAllowSpontaneousScopes() {
        return allowSpontaneousScopes;
    }

    public void setAllowSpontaneousScopes(Boolean allowSpontaneousScopes) {
        this.allowSpontaneousScopes = allowSpontaneousScopes;
    }

    public List<String> getSpontaneousScopes() {
        return spontaneousScopes;
    }

    public void setSpontaneousScopes(List<String> spontaneousScopes) {
        this.spontaneousScopes = spontaneousScopes;
    }

    public List<String> getAdditionalAudience() {
        return additionalAudience;
    }

    public void setAdditionalAudience(List<String> additionalAudience) {
        this.additionalAudience = additionalAudience;
    }

    public Boolean getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims() {
        return runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims;
    }

    public void setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(Boolean runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims) {
        this.runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims = runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims;
    }

    public Boolean getKeepClientAuthorizationAfterExpiration() {
        return keepClientAuthorizationAfterExpiration;
    }

    public void setKeepClientAuthorizationAfterExpiration(Boolean keepClientAuthorizationAfterExpiration) {
        this.keepClientAuthorizationAfterExpiration = keepClientAuthorizationAfterExpiration;
    }

    /**
     * Returns the Registration Access Token to authorize Client Read requests.
     *
     * @return The Registration Access Token.
     */
    public String getRegistrationAccessToken() {
        return registrationAccessToken;
    }

    /**
     * Sets the Registration Access Token to authorize Client Read requests.
     *
     * @param registrationAccessToken The Registration Access Token.
     */
    public void setAccessToken(String registrationAccessToken) {
        this.registrationAccessToken = registrationAccessToken;
    }

    public List<String> getBackchannelLogoutUris() {
        return backchannelLogoutUris;
    }

    public void setBackchannelLogoutUris(List<String> backchannelLogoutUris) {
        this.backchannelLogoutUris = backchannelLogoutUris;
    }

    public Boolean getBackchannelLogoutSessionRequired() {
        return backchannelLogoutSessionRequired;
    }

    public void setBackchannelLogoutSessionRequired(Boolean backchannelLogoutSessionRequired) {
        this.backchannelLogoutSessionRequired = backchannelLogoutSessionRequired;
    }

    /**
     * Gets logout uri.
     *
     * @return logout uri
     */
    public String getFrontChannelLogoutUri() {
        return frontChannelLogoutUri;
    }

    /**
     * Sets logout uri
     *
     * @param logoutUri logout uri
     */
    public void setFrontChannelLogoutUri(String logoutUri) {
        this.frontChannelLogoutUri = logoutUri;
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
     * Sets front channel logout session required.
     *
     * @param frontChannelLogoutSessionRequired front channel logout session required
     */
    public void setFrontChannelLogoutSessionRequired(Boolean frontChannelLogoutSessionRequired) {
        this.frontChannelLogoutSessionRequired = frontChannelLogoutSessionRequired;
    }

    /**
     * Returns a list of redirection URIs.
     *
     * @return The redirection URIs.
     */
    public List<String> getRedirectUris() {
        return redirectUris;
    }

    /**
     * Sets a list of redirection URIs.
     *
     * @param redirectUris The redirection URIs.
     */
    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    /**
     * Returns claims redirect URIs.
     *
     * @return claims redirect URIs
     */
    public List<String> getClaimsRedirectUris() {
        return claimsRedirectUris;
    }

    /**
     * Sets claims redirect URIs.
     *
     * @param claimsRedirectUris claims redirect URIs.
     */
    public void setClaimsRedirectUris(List<String> claimsRedirectUris) {
        this.claimsRedirectUris = claimsRedirectUris;
    }

    /**
     * Returns a list of the OAuth 2.0 response_type values that the Client is declaring that it will restrict itself
     * to using.
     *
     * @return A list of response types.
     */
    public List<ResponseType> getResponseTypes() {
        Set<ResponseType> types = Sets.newHashSet();
        responseTypes.forEach(s -> types.addAll(ResponseType.fromString(s, " ")));
        return Lists.newArrayList(types);
    }

    /**
     * Sets a list of the OAuth 2.0 response_type values that the Client is declaring that it will restrict itself to
     * using. If omitted, the default is that the Client will use only the code response type.
     *
     * @param responseTypes A list of response types.
     */
    public void setResponseTypes(List<ResponseType> responseTypes) {
        this.responseTypes = ResponseType.toStringList(responseTypes);
    }

    public List<String> getResponseTypesStrings() {
        return responseTypes;
    }

    public void setResponseTypesStrings(List<String> responseTypes) {
        this.responseTypes = responseTypes;
    }


    /**
     * Returns a list of the OAuth 2.0 grant types that the Client is declaring that it will restrict itself to using.
     *
     * @return A list of grant types.
     */
    public List<GrantType> getGrantTypes() {
        if (grantTypes == null) grantTypes = new ArrayList<>();
        return grantTypes;
    }

    /**
     * Sets a list of the OAuth 2.0 grant types that the Client is declaring that it will restrict itself to using.
     *
     * @param grantTypes A list of grant types.
     */
    public void setGrantTypes(List<GrantType> grantTypes) {
        this.grantTypes = grantTypes;
    }

    /**
     * Returns the application type.
     *
     * @return The application type.
     */
    public ApplicationType getApplicationType() {
        return applicationType;
    }

    /**
     * Sets the application type. The default if not specified is web.
     *
     * @param applicationType The application type.
     */
    public void setApplicationType(ApplicationType applicationType) {
        this.applicationType = applicationType;
    }

    public String getIdTokenTokenBindingCnf() {
        return idTokenTokenBindingCnf;
    }

    public void setIdTokenTokenBindingCnf(String idTokenTokenBindingCnf) {
        this.idTokenTokenBindingCnf = idTokenTokenBindingCnf;
    }

    /**
     * Returns a list of e-mail addresses for people allowed to administer the information
     * for this Client.
     *
     * @return A list of e-mail addresses.
     */
    public List<String> getContacts() {
        return contacts;
    }

    /**
     * Sets a list of e-mail addresses for people allowed to administer the information for
     * this Client.
     *
     * @param contacts A list of e-mail addresses.
     */
    public void setContacts(List<String> contacts) {
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
     * @return The URL that references a logo for the Client application.
     */
    public String getLogoUri() {
        return logoUri;
    }

    /**
     * Sets an URL that references a logo for the Client application.
     *
     * @param logoUri The URL that references a logo for the Client application.
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
     * Returns an URL that the Relying Party Client provides to the End-User to read about the how the profile data
     * will be used.
     *
     * @return The policy URL.
     */
    public String getPolicyUri() {
        return policyUri;
    }

    /**
     * Sets an URL that the Relying Party Client provides to the End-User to read about the how the profile data will
     * be used.
     *
     * @param policyUri The policy URL.
     */
    public void setPolicyUri(String policyUri) {
        this.policyUri = policyUri;
    }

    /**
     * Returns an URL that the Relying Party Client provides to the End-User to read about the Relying Party's terms
     * of service.
     *
     * @return The tems of service URL.
     */
    public String getTosUri() {
        return tosUri;
    }

    /**
     * Sets an URL that the Relying Party Client provides to the End-User to read about the Relying Party's terms of
     * service.
     *
     * @param tosUri The term of service URL.
     */
    public void setTosUri(String tosUri) {
        this.tosUri = tosUri;
    }

    /**
     * Returns the URL for the Client's JSON Web Key Set (JWK) document containing key(s) that are used for signing
     * requests to the OP. The JWK Set may also contain the Client's encryption keys(s) that are used by the OP to
     * encrypt the responses to the Client. When both signing and encryption keys are made available, a use (Key Use)
     * parameter value is required for all keys in the document to indicate each key's intended usage.
     *
     * @return The URL for the Client's JSON Web Key Set (JWK) document.
     */
    public String getJwksUri() {
        return jwksUri;
    }

    /**
     * Sets the URL for the Client's JSON Web Key Set (JWK) document containing key(s) that are used for signing
     * requests to the OP. The JWK Set may also contain the Client's encryption keys(s) that are used by the OP to
     * encrypt the responses to the Client. When both signing and encryption keys are made available, a use (Key Use)
     * parameter value is required for all keys in the document to indicate each key's intended usage.
     *
     * @param jwksUri The URL for the Client's JSON Web Key Set (JWK) document.
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
     * Returns the URL using the https scheme to be used in calculating Pseudonymous Identifiers by the OP.
     * The URL references a file with a single JSON array of redirect_uri values.
     *
     * @return The sector identifier URL.
     */
    public String getSectorIdentifierUri() {
        return sectorIdentifierUri;
    }

    /**
     * Sets the URL using the https scheme to be used in calculating Pseudonymous Identifiers by the OP.
     * The URL references a file with a single JSON array of redirect_uri values.
     *
     * @param sectorIdentifierUri The sector identifier URL.
     */
    public void setSectorIdentifierUri(String sectorIdentifierUri) {
        this.sectorIdentifierUri = sectorIdentifierUri;
    }

    /**
     * Returns the Subject Type. Valid types include pairwise and public.
     *
     * @return The Subject Type.
     */
    public SubjectType getSubjectType() {
        return subjectType;
    }

    /**
     * Sets the Subject Type. Valid types include pairwise and public.
     *
     * @param subjectType The Subject Type.
     */
    public void setSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public Boolean getRptAsJwt() {
        return rptAsJwt;
    }

    public void setRptAsJwt(Boolean rptAsJwt) {
        this.rptAsJwt = rptAsJwt;
    }

    public Boolean getAccessTokenAsJwt() {
        return accessTokenAsJwt;
    }

    public void setAccessTokenAsJwt(Boolean accessTokenAsJwt) {
        this.accessTokenAsJwt = accessTokenAsJwt;
    }

    public SignatureAlgorithm getAccessTokenSigningAlg() {
        return accessTokenSigningAlg;
    }

    public void setAccessTokenSigningAlg(SignatureAlgorithm accessTokenSigningAlg) {
        this.accessTokenSigningAlg = accessTokenSigningAlg;
    }

    public SignatureAlgorithm getAuthorizationSignedResponseAlg() {
        return authorizationSignedResponseAlg;
    }

    public void setAuthorizationSignedResponseAlg(SignatureAlgorithm authorizationSignedResponseAlg) {
        this.authorizationSignedResponseAlg = authorizationSignedResponseAlg;
    }

    public KeyEncryptionAlgorithm getAuthorizationEncryptedResponseAlg() {
        return authorizationEncryptedResponseAlg;
    }

    public void setAuthorizationEncryptedResponseAlg(KeyEncryptionAlgorithm authorizationEncryptedResponseAlg) {
        this.authorizationEncryptedResponseAlg = authorizationEncryptedResponseAlg;
    }

    public BlockEncryptionAlgorithm getAuthorizationEncryptedResponseEnc() {
        return authorizationEncryptedResponseEnc;
    }

    public void setAuthorizationEncryptedResponseEnc(BlockEncryptionAlgorithm authorizationEncryptedResponseEnc) {
        this.authorizationEncryptedResponseEnc = authorizationEncryptedResponseEnc;
    }

    /**
     * Returns th JWS alg algorithm (JWA) required for the ID Token issued to this client_id.
     *
     * @return The JWS algorithm (JWA).
     */
    public SignatureAlgorithm getIdTokenSignedResponseAlg() {
        return idTokenSignedResponseAlg;
    }

    /**
     * Sets the JWS alg algorithm (JWA) required for the ID Token issued to this client_id.
     *
     * @param idTokenSignedResponseAlg The JWS algorithm (JWA).
     */
    public void setIdTokenSignedResponseAlg(SignatureAlgorithm idTokenSignedResponseAlg) {
        this.idTokenSignedResponseAlg = idTokenSignedResponseAlg;
    }

    /**
     * Returns the JWE alg algorithm (JWA) required for encrypting the ID Token issued to this client_id.
     *
     * @return The JWE algorithm (JWA).
     */
    public KeyEncryptionAlgorithm getIdTokenEncryptedResponseAlg() {
        return idTokenEncryptedResponseAlg;
    }

    /**
     * Sets the JWE alg algorithm (JWA) required for encrypting the ID Token issued to this client_id.
     *
     * @param idTokenEncryptedResponseAlg The JWE algorithm (JWA).
     */
    public void setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm idTokenEncryptedResponseAlg) {
        this.idTokenEncryptedResponseAlg = idTokenEncryptedResponseAlg;
    }

    /**
     * Returns the JWE enc algorithm (JWA) required for symmetric encryption of the ID Token issued to this client_id.
     *
     * @return The JWE algorithm (JWA).
     */
    public BlockEncryptionAlgorithm getIdTokenEncryptedResponseEnc() {
        return idTokenEncryptedResponseEnc;
    }

    /**
     * Sets the JWE enc algorithm (JWA) required for symmetric encryption of the ID Token issued to this client_id.
     *
     * @param idTokenEncryptedResponseEnc The JWE algorithm (JWA).
     */
    public void setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm idTokenEncryptedResponseEnc) {
        this.idTokenEncryptedResponseEnc = idTokenEncryptedResponseEnc;
    }

    /**
     * Returns the JWS alg algorithm (JWA) required for UserInfo responses.
     *
     * @return The JWS algorithm (JWA).
     */
    public SignatureAlgorithm getUserInfoSignedResponseAlg() {
        return userInfoSignedResponseAlg;
    }

    /**
     * Sets the JWS alg algorithm (JWA) required for UserInfo responses.
     *
     * @param userInfoSignedResponseAlg The JWS algorithm (JWA).
     */
    public void setUserInfoSignedResponseAlg(SignatureAlgorithm userInfoSignedResponseAlg) {
        this.userInfoSignedResponseAlg = userInfoSignedResponseAlg;
    }

    /**
     * Returns the JWE alg algorithm (JWA) required for encrypting UserInfo responses.
     *
     * @return The JWE algorithm (JWA).
     */
    public KeyEncryptionAlgorithm getUserInfoEncryptedResponseAlg() {
        return userInfoEncryptedResponseAlg;
    }

    /**
     * Sets the JWE alg algorithm (JWA) required for encrypting UserInfo responses.
     *
     * @param userInfoEncryptedResponseAlg The JWE algorithm (JWA).
     */
    public void setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm userInfoEncryptedResponseAlg) {
        this.userInfoEncryptedResponseAlg = userInfoEncryptedResponseAlg;
    }

    /**
     * Returns the JWE enc algorithm (JWA) required for symmetric encryption of UserInfo responses.
     *
     * @return The JWE algorithm (JWA).
     */
    public BlockEncryptionAlgorithm getUserInfoEncryptedResponseEnc() {
        return userInfoEncryptedResponseEnc;
    }

    /**
     * Sets the JWE enc algorithm (JWA) required for symmetric encryption of UserInfo responses.
     *
     * @param userInfoEncryptedResponseEnc The JWE algorithm (JWA).
     */
    public void setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm userInfoEncryptedResponseEnc) {
        this.userInfoEncryptedResponseEnc = userInfoEncryptedResponseEnc;
    }

    /**
     * Returns the JWS alg algorithm (JWA) that must be required by the Authorization Server.
     *
     * @return The JWS algorithm (JWA).
     */
    public SignatureAlgorithm getRequestObjectSigningAlg() {
        return requestObjectSigningAlg;
    }

    /**
     * Sets the JWS alg algorithm (JWA) that must be required by the Authorization Server.
     *
     * @param requestObjectSigningAlg The JWS algorithm (JWA).
     */
    public void setRequestObjectSigningAlg(SignatureAlgorithm requestObjectSigningAlg) {
        this.requestObjectSigningAlg = requestObjectSigningAlg;
    }

    /**
     * Returns the JWE alg algorithm (JWA) the RP is declaring that it may use for encrypting Request Objects
     * sent to the OP.
     *
     * @return The JWE alg algorithm (JWA).
     */
    public KeyEncryptionAlgorithm getRequestObjectEncryptionAlg() {
        return requestObjectEncryptionAlg;
    }

    /**
     * Sets the JWE alg algorithm (JWA) the RP is declaring that it may use for encrypting Request Objects
     * sent to the OP.
     *
     * @param requestObjectEncryptionAlg The JWE alg algorithm (JWA).
     */
    public void setRequestObjectEncryptionAlg(KeyEncryptionAlgorithm requestObjectEncryptionAlg) {
        this.requestObjectEncryptionAlg = requestObjectEncryptionAlg;
    }

    /**
     * Returns the JWE enc algorithm (JWA) the RP is declaring that it may use for encrypting Request Objects
     * sent to the OP.
     *
     * @return The JWE enc algorithm (JWA).
     */
    public BlockEncryptionAlgorithm getRequestObjectEncryptionEnc() {
        return requestObjectEncryptionEnc;
    }

    /**
     * Sets the JWE enc algorithm (JWA) the RP is declaring that it may use for encrypting Request Objects
     * sent to the OP.
     *
     * @param requestObjectEncryptionEnc The JWE enc algorithm (JWA).
     */
    public void setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm requestObjectEncryptionEnc) {
        this.requestObjectEncryptionEnc = requestObjectEncryptionEnc;
    }

    /**
     * Returns the requested authentication method for the Token Endpoint.
     *
     * @return The requested authentication method for the Token Endpoint.
     */
    public AuthenticationMethod getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    /**
     * Sets the requested authentication method for the Token Endpoint.
     *
     * @param tokenEndpointAuthMethod The requested authentication method for the Token Endpoint.
     */
    public void setTokenEndpointAuthMethod(AuthenticationMethod tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    /**
     * Returns the Requested Client Authentication method for the Token Endpoint.
     *
     * @return The Requested Client Authentication method for the Token Endpoint.
     */
    public SignatureAlgorithm getTokenEndpointAuthSigningAlg() {
        return tokenEndpointAuthSigningAlg;
    }

    /**
     * Sets the Requested Client Authentication method for the Token Endpoint.
     *
     * @param tokenEndpointAuthSigningAlg The Requested Client Authentication method for the Token Endpoint.
     */
    public void setTokenEndpointAuthSigningAlg(SignatureAlgorithm tokenEndpointAuthSigningAlg) {
        this.tokenEndpointAuthSigningAlg = tokenEndpointAuthSigningAlg;
    }

    /**
     * Returns the Default Maximum Authentication Age.
     *
     * @return The Default Maximum Authentication Age.
     */
    public Integer getDefaultMaxAge() {
        return defaultMaxAge;
    }

    /**
     * Sets the Default Maximum Authentication Age.
     *
     * @param defaultMaxAge The Default Maximum Authentication Age.
     */
    public void setDefaultMaxAge(Integer defaultMaxAge) {
        this.defaultMaxAge = defaultMaxAge;
    }

    /**
     * Returns the Boolean value specifying whether the auth_time claim in the id_token is required.
     * It is required when the value is true. The auth_time claim request in the request object overrides this setting.
     *
     * @return The Boolean value specifying whether the auth_time claim in the id_token is required.
     */
    public Boolean getRequireAuthTime() {
        return requireAuthTime;
    }

    /**
     * Sets the Boolean value specifying whether the auth_time claim in the id_token is required.
     * Ir is required when the value is true. The auth_time claim request in the request object overrides this setting.
     *
     * @param requireAuthTime The Boolean value specifying whether the auth_time claim in the id_token is required.
     */
    public void setRequireAuthTime(Boolean requireAuthTime) {
        this.requireAuthTime = requireAuthTime;
    }

    /**
     * Returns the Default requested Authentication Context Class Reference values.
     *
     * @return The Default requested Authentication Context Class Reference values.
     */
    public List<String> getDefaultAcrValues() {
        return defaultAcrValues;
    }

    /**
     * Sets the Default requested Authentication Context Class Reference values.
     *
     * @param defaultAcrValues The Default requested Authentication Context Class Reference values.
     */
    public void setDefaultAcrValues(List<String> defaultAcrValues) {
        this.defaultAcrValues = defaultAcrValues;
    }

    /**
     * Returns the URI using the https: scheme that the authorization server can call to initiate a login at the client.
     *
     * @return The URI using the https: scheme that the authorization server can call to initiate a login at the client.
     */
    public String getInitiateLoginUri() {
        return initiateLoginUri;
    }

    /**
     * Sets the URI using the https: scheme that the authorization server can call to initiate a login at the client.
     *
     * @param initiateLoginUri The URI using the https: scheme that the authorization server can call to initiate a
     *                         login at the client.
     */
    public void setInitiateLoginUri(String initiateLoginUri) {
        this.initiateLoginUri = initiateLoginUri;
    }

    /**
     * Returns the URLs supplied by the RP to request that the user be redirected to this location after a logout has
     * been performed.
     *
     * @return The URLs supplied by the RP to request that the user be redirected to this location after a logout has
     * been performed.
     */
    public List<String> getPostLogoutRedirectUris() {
        return postLogoutRedirectUris;
    }

    /**
     * Sets the URLs supplied by the RP to request that the user be redirected to this location after a logout has
     * been performed.
     *
     * @param postLogoutRedirectUris The URLs supplied by the RP to request that the user be redirected to this location
     *                               after a logout has been performed.
     */
    public void setPostLogoutRedirectUris(List<String> postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
    }

    /**
     * Returns a list of request_uri values that are pre-registered by the Client for use at the Authorization Server.
     *
     * @return A list of request URIs.
     */
    public List<String> getRequestUris() {
        return requestUris;
    }

    /**
     * Sets a list of request_uri values that are pre-registered by the Client for use at the Authorization Server.
     *
     * @param requestUris A list of request URIs.
     */
    public void setRequestUris(List<String> requestUris) {
        this.requestUris = requestUris;
    }

    /**
     * Returns authorized JavaScript origins.
     *
     * @return Authorized JavaScript origins.
     */
    public List<String> getAuthorizedOrigins() {
        return authorizedOrigins;
    }

    /**
     * Sets authorized JavaScript origins.
     *
     * @param authorizedOrigins Authorized JavaScript origins.
     */
    public void setAuthorizedOrigins(List<String> authorizedOrigins) {
        this.authorizedOrigins = authorizedOrigins;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public List<String> getClaims() {
        return claims;
    }

    public void setClaims(List<String> claims) {
        this.claims = claims;
    }

    /**
     * Returns the Client-specific access token expiration.
     *
     * @return The Client-specific access token expiration.
     */
    public Integer getAccessTokenLifetime() {
        return accessTokenLifetime;
    }

    /**
     * Sets the Client-specific access token expiration (in seconds). Set it to Null or Zero to use the system default value.
     *
     * @param accessTokenLifetime The Client-specific access token expiration.
     */
    public void setAccessTokenLifetime(Integer accessTokenLifetime) {
        this.accessTokenLifetime = accessTokenLifetime;
    }

    public Integer getParLifetime() {
        return parLifetime;
    }

    public void setParLifetime(Integer parLifetime) {
        this.parLifetime = parLifetime;
    }

    public Boolean getRequirePar() {
        return requirePar;
    }

    public void setRequirePar(Boolean requirePar) {
        this.requirePar = requirePar;
    }

    /**
     * Returns a unique identifier string (UUID) assigned by the client developer or software publisher used by
     * registration endpoints to identify the client software to be dynamically registered.
     *
     * @return The software identifier.
     */
    public String getSoftwareId() {
        return softwareId;
    }

    /**
     * Sets a unique identifier string (UUID) assigned by the client developer or software publisher used by
     * registration endpoints to identify the client software to be dynamically registered.
     *
     * @param softwareId The software identifier.
     */
    public void setSoftwareId(String softwareId) {
        this.softwareId = softwareId;
    }

    /**
     * Returns a version identifier string for the client software identified by "software_id".
     * The value of the "software_version" should change on any update to the client software identified by the same
     * "software_id".
     *
     * @return The version identifier.
     */
    public String getSoftwareVersion() {
        return softwareVersion;
    }

    /**
     * Sets a version identifier string for the client software identified by "software_id".
     * The value of the "software_version" should change on any update to the client software identified by the same
     * "software_id".
     *
     * @param softwareVersion The version identifier.
     */
    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    /**
     * Returns a software statement containing client metadata values about the client software as claims.
     * This is a string value containing the entire signed JWT.
     *
     * @return The software statement.
     */
    public String getSoftwareStatement() {
        return softwareStatement;
    }

    /**
     * Sets  a software statement containing client metadata values about the client software as claims.
     * This is a string value containing the entire signed JWT.
     *
     * @param softwareStatement The software statement.
     */
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

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    /**
     * Gets custom attribute map copy.
     *
     * @return custom attribute map copy
     */
    public Map<String, String> getCustomAttributes() {
        // return unmodifiable map to force add custom attribute via addCustomAttribute() that has validation
        return Collections.unmodifiableMap(this.customAttributes);
    }

    public void addCustomAttribute(String name, String value) {
        if (RegisterRequestParam.isCustomParameterValid(name)) {
            this.customAttributes.put(name, value);
        }
    }

    /**
     * Returns a collection of parameters of the register request.
     * Any <code>null</code> or empty parameter will be omitted.
     *
     * @return A collection of parameters.
     */
    @Override
    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<>();

        JsonApplier.getInstance().apply(this, parameters);

        if (redirectUris != null && !redirectUris.isEmpty()) {
            parameters.put(REDIRECT_URIS.toString(), toJSONArray(redirectUris).toString());
        }
        if (claimsRedirectUris != null && !claimsRedirectUris.isEmpty()) {
            parameters.put(CLAIMS_REDIRECT_URIS.toString(), toJSONArray(claimsRedirectUris).toString());
        }
        if (responseTypes != null && !responseTypes.isEmpty()) {
            parameters.put(RESPONSE_TYPES.toString(), toJSONArray(responseTypes).toString());
        }
        if (grantTypes != null && !grantTypes.isEmpty()) {
            parameters.put(GRANT_TYPES.toString(), toJSONArray(grantTypes).toString());
        }
        if (applicationType != null) {
            parameters.put(APPLICATION_TYPE.toString(), applicationType.toString());
        }
        if (contacts != null && !contacts.isEmpty()) {
            parameters.put(CONTACTS.toString(), toJSONArray(contacts).toString());
        }
        if (StringUtils.isNotBlank(clientName)) {
            parameters.put(CLIENT_NAME.toString(), clientName);
        }
        if (StringUtils.isNotBlank(logoUri)) {
            parameters.put(LOGO_URI.toString(), logoUri);
        }
        if (StringUtils.isNotBlank(clientUri)) {
            parameters.put(CLIENT_URI.toString(), clientUri);
        }
        if (StringUtils.isNotBlank(policyUri)) {
            parameters.put(POLICY_URI.toString(), policyUri);
        }
        if (StringUtils.isNotBlank(tosUri)) {
            parameters.put(TOS_URI.toString(), tosUri);
        }
        if (StringUtils.isNotBlank(jwksUri)) {
            parameters.put(JWKS_URI.toString(), jwksUri);
        }
        if (StringUtils.isNotBlank(jwks)) {
            parameters.put(JWKS.toString(), jwks);
        }
        if (StringUtils.isNotBlank(sectorIdentifierUri)) {
            parameters.put(SECTOR_IDENTIFIER_URI.toString(), sectorIdentifierUri);
        }
        if (subjectType != null) {
            parameters.put(SUBJECT_TYPE.toString(), subjectType.toString());
        }
        if (rptAsJwt != null) {
            parameters.put(RPT_AS_JWT.toString(), rptAsJwt.toString());
        }
        if (accessTokenAsJwt != null) {
            parameters.put(ACCESS_TOKEN_AS_JWT.toString(), accessTokenAsJwt.toString());
        }
        if (accessTokenSigningAlg != null) {
            parameters.put(ACCESS_TOKEN_SIGNING_ALG.toString(), accessTokenSigningAlg.toString());
        }
        if (authorizationSignedResponseAlg != null) {
            parameters.put(AUTHORIZATION_SIGNED_RESPONSE_ALG.toString(), authorizationSignedResponseAlg.getName());
        }
        if (authorizationEncryptedResponseAlg != null) {
            parameters.put(AUTHORIZATION_ENCRYPTED_RESPONSE_ALG.toString(), authorizationEncryptedResponseAlg.getName());
        }
        if (authorizationEncryptedResponseEnc != null) {
            parameters.put(AUTHORIZATION_ENCRYPTED_RESPONSE_ENC.toString(), authorizationEncryptedResponseEnc.getName());
        }
        if (idTokenSignedResponseAlg != null) {
            parameters.put(ID_TOKEN_SIGNED_RESPONSE_ALG.toString(), idTokenSignedResponseAlg.getName());
        }
        if (idTokenEncryptedResponseAlg != null) {
            parameters.put(ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString(), idTokenEncryptedResponseAlg.getName());
        }
        if (idTokenEncryptedResponseEnc != null) {
            parameters.put(ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString(), idTokenEncryptedResponseEnc.getName());
        }
        if (userInfoSignedResponseAlg != null) {
            parameters.put(USERINFO_SIGNED_RESPONSE_ALG.toString(), userInfoSignedResponseAlg.getName());
        }
        if (userInfoEncryptedResponseAlg != null) {
            parameters.put(USERINFO_ENCRYPTED_RESPONSE_ALG.toString(), userInfoEncryptedResponseAlg.getName());
        }
        if (userInfoEncryptedResponseEnc != null) {
            parameters.put(USERINFO_ENCRYPTED_RESPONSE_ENC.toString(), userInfoEncryptedResponseEnc.getName());
        }
        if (requestObjectSigningAlg != null) {
            parameters.put(REQUEST_OBJECT_SIGNING_ALG.toString(), requestObjectSigningAlg.getName());
        }
        if (requestObjectEncryptionAlg != null) {
            parameters.put(REQUEST_OBJECT_ENCRYPTION_ALG.toString(), requestObjectEncryptionAlg.getName());
        }
        if (requestObjectEncryptionEnc != null) {
            parameters.put(REQUEST_OBJECT_ENCRYPTION_ENC.toString(), requestObjectEncryptionEnc.getName());
        }
        if (tokenEndpointAuthMethod != null) {
            parameters.put(TOKEN_ENDPOINT_AUTH_METHOD.toString(), tokenEndpointAuthMethod.toString());
        }
        if (tokenEndpointAuthSigningAlg != null) {
            parameters.put(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString(), tokenEndpointAuthSigningAlg.toString());
        }
        if (defaultMaxAge != null) {
            parameters.put(DEFAULT_MAX_AGE.toString(), defaultMaxAge.toString());
        }
        if (requireAuthTime != null) {
            parameters.put(REQUIRE_AUTH_TIME.toString(), requireAuthTime.toString());
        }
        if (defaultAcrValues != null && !defaultAcrValues.isEmpty()) {
            parameters.put(DEFAULT_ACR_VALUES.toString(), toJSONArray(defaultAcrValues).toString());
        }
        if (StringUtils.isNotBlank(initiateLoginUri)) {
            parameters.put(INITIATE_LOGIN_URI.toString(), initiateLoginUri);
        }
        if (postLogoutRedirectUris != null && !postLogoutRedirectUris.isEmpty()) {
            parameters.put(POST_LOGOUT_REDIRECT_URIS.toString(), toJSONArray(postLogoutRedirectUris).toString());
        }
        if (StringUtils.isNotBlank(frontChannelLogoutUri)) {
            parameters.put(FRONT_CHANNEL_LOGOUT_URI.toString(), frontChannelLogoutUri);
        }
        if (frontChannelLogoutSessionRequired != null) {
            parameters.put(FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED.toString(), frontChannelLogoutSessionRequired.toString());
        }
        if (backchannelLogoutUris != null && !backchannelLogoutUris.isEmpty()) {
            parameters.put(BACKCHANNEL_LOGOUT_URI.toString(), toJSONArray(backchannelLogoutUris).toString());
        }
        if (backchannelLogoutSessionRequired != null) {
            parameters.put(BACKCHANNEL_LOGOUT_SESSION_REQUIRED.toString(), backchannelLogoutSessionRequired.toString());
        }
        if (requestUris != null && !requestUris.isEmpty()) {
            parameters.put(REQUEST_URIS.toString(), toJSONArray(requestUris).toString());
        }
        if (authorizedOrigins != null && !authorizedOrigins.isEmpty()) {
            parameters.put(AUTHORIZED_ORIGINS.toString(), toJSONArray(authorizedOrigins).toString());
        }
        if (scope != null && !scope.isEmpty()) {
            parameters.put(SCOPE.toString(), implode(scope, " "));
        }
        if (StringUtils.isNotBlank(idTokenTokenBindingCnf)) {
            parameters.put(ID_TOKEN_TOKEN_BINDING_CNF.toString(), idTokenTokenBindingCnf);
        }
        if (StringUtils.isNotBlank(tlsClientAuthSubjectDn)) {
            parameters.put(TLS_CLIENT_AUTH_SUBJECT_DN.toString(), tlsClientAuthSubjectDn);
        }
        if (allowSpontaneousScopes != null) {
            parameters.put(ALLOW_SPONTANEOUS_SCOPES.toString(), allowSpontaneousScopes.toString());
        }
        if (spontaneousScopes != null && !spontaneousScopes.isEmpty()) {
            parameters.put(SPONTANEOUS_SCOPES.toString(), implode(spontaneousScopes, " "));
        }
        if (runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims != null) {
            parameters.put(RUN_INTROSPECTION_SCRIPT_BEFORE_ACCESS_TOKEN_CREATION_AS_JWT_AND_INCLUDE_CLAIMS.toString(), runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims.toString());
        }
        if (keepClientAuthorizationAfterExpiration != null) {
            parameters.put(KEEP_CLIENT_AUTHORIZATION_AFTER_EXPIRATION.toString(), keepClientAuthorizationAfterExpiration.toString());
        }
        if (claims != null && !claims.isEmpty()) {
            parameters.put(CLAIMS.toString(), implode(claims, " "));
        }
        if (accessTokenLifetime != null) {
            parameters.put(ACCESS_TOKEN_LIFETIME.toString(), accessTokenLifetime.toString());
        }
        if (parLifetime != null) {
            parameters.put(PAR_LIFETIME.toString(), parLifetime.toString());
        }
        if (requirePar != null) {
            parameters.put(REQUIRE_PAR.toString(), requirePar.toString());
        }
        if (StringUtils.isNotBlank(softwareId)) {
            parameters.put(SOFTWARE_ID.toString(), softwareId);
        }
        if (StringUtils.isNotBlank(softwareVersion)) {
            parameters.put(SOFTWARE_VERSION.toString(), softwareVersion);
        }
        if (StringUtils.isNotBlank(softwareStatement)) {
            parameters.put(SOFTWARE_STATEMENT.toString(), softwareStatement);
        }
        if (backchannelTokenDeliveryMode != null) {
            parameters.put(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString(), backchannelTokenDeliveryMode.toString());
        }
        if (StringUtils.isNotBlank(backchannelClientNotificationEndpoint)) {
            parameters.put(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString(), backchannelClientNotificationEndpoint);
        }
        if (backchannelAuthenticationRequestSigningAlg != null) {
            parameters.put(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString(), backchannelAuthenticationRequestSigningAlg.toString());
        }
        if (backchannelUserCodeParameter != null && backchannelUserCodeParameter) {
            parameters.put(BACKCHANNEL_USER_CODE_PARAMETER.toString(), backchannelUserCodeParameter.toString());
        }

        // Custom params
        if (customAttributes != null && !customAttributes.isEmpty()) {
            for (Map.Entry<String, String> entry : customAttributes.entrySet()) {
                final String name = entry.getKey();
                final String value = entry.getValue();
                if (RegisterRequestParam.isCustomParameterValid(name) && StringUtils.isNotBlank(value)) {
                    parameters.put(name, value);
                }
            }
        }
        return parameters;
    }

    public static RegisterRequest fromJson(String json) throws JSONException {
        return fromJson(new JSONObject(json));
    }

    public static RegisterRequest fromJson(JSONObject requestObject) throws JSONException {
        final RegisterRequest result = new RegisterRequest();

        JsonApplier.getInstance().apply(requestObject, result);

        result.setJsonObject(requestObject);
        result.setRequestUris(extractListByKey(requestObject, REQUEST_URIS.toString()));
        result.setAuthorizedOrigins(extractListByKey(requestObject, AUTHORIZED_ORIGINS.toString()));
        result.setClaimsRedirectUris(extractListByKey(requestObject, CLAIMS_REDIRECT_URIS.toString()));
        result.setInitiateLoginUri(requestObject.optString(INITIATE_LOGIN_URI.toString()));
        result.setPostLogoutRedirectUris(extractListByKey(requestObject, POST_LOGOUT_REDIRECT_URIS.toString()));
        result.setDefaultAcrValues(extractListByKey(requestObject, DEFAULT_ACR_VALUES.toString()));
        result.setRequireAuthTime(requestObject.optBoolean(REQUIRE_AUTH_TIME.toString()));
        result.setFrontChannelLogoutUri(requestObject.optString(FRONT_CHANNEL_LOGOUT_URI.toString()));
        result.setFrontChannelLogoutSessionRequired(requestObject.optBoolean(FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED.toString()));
        result.setBackchannelLogoutUris(extractListByKey(requestObject, BACKCHANNEL_LOGOUT_URI.toString()));
        result.setBackchannelLogoutSessionRequired(requestObject.optBoolean(BACKCHANNEL_LOGOUT_SESSION_REQUIRED.toString()));
        result.setAccessTokenLifetime(integerOrNull(requestObject, ACCESS_TOKEN_LIFETIME.toString()));
        result.setParLifetime(integerOrNull(requestObject, PAR_LIFETIME.toString()));
        result.setRequirePar(booleanOrNull(requestObject, REQUIRE_PAR.toString()));
        result.setDefaultMaxAge(integerOrNull(requestObject, DEFAULT_MAX_AGE.toString()));
        result.setTlsClientAuthSubjectDn(requestObject.optString(TLS_CLIENT_AUTH_SUBJECT_DN.toString()));
        result.setAllowSpontaneousScopes(requestObject.optBoolean(ALLOW_SPONTANEOUS_SCOPES.toString()));
        result.setSpontaneousScopes(extractListByKey(requestObject, SPONTANEOUS_SCOPES.toString()));
        result.setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(requestObject.optBoolean(RUN_INTROSPECTION_SCRIPT_BEFORE_ACCESS_TOKEN_CREATION_AS_JWT_AND_INCLUDE_CLAIMS.toString()));
        result.setKeepClientAuthorizationAfterExpiration(requestObject.optBoolean(KEEP_CLIENT_AUTHORIZATION_AFTER_EXPIRATION.toString()));
        result.setRptAsJwt(requestObject.optBoolean(RPT_AS_JWT.toString()));
        result.setAccessTokenAsJwt(requestObject.optBoolean(ACCESS_TOKEN_AS_JWT.toString()));
        result.setAccessTokenSigningAlg(SignatureAlgorithm.fromString(requestObject.optString(ACCESS_TOKEN_SIGNING_ALG.toString())));
        result.setAuthorizationSignedResponseAlg(SignatureAlgorithm.fromString(requestObject.optString(AUTHORIZATION_SIGNED_RESPONSE_ALG.toString())));
        result.setAuthorizationEncryptedResponseAlg(KeyEncryptionAlgorithm.fromName(requestObject.optString(AUTHORIZATION_ENCRYPTED_RESPONSE_ALG.toString())));
        result.setAuthorizationEncryptedResponseEnc(BlockEncryptionAlgorithm.fromName(requestObject.optString(AUTHORIZATION_ENCRYPTED_RESPONSE_ENC.toString())));
        result.setIdTokenSignedResponseAlg(SignatureAlgorithm.fromString(requestObject.optString(ID_TOKEN_SIGNED_RESPONSE_ALG.toString())));
        result.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.fromName(requestObject.optString(ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString())));
        result.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.fromName(requestObject.optString(ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString())));
        result.setUserInfoSignedResponseAlg(SignatureAlgorithm.fromString(requestObject.optString(USERINFO_SIGNED_RESPONSE_ALG.toString())));
        result.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.fromName(requestObject.optString(USERINFO_ENCRYPTED_RESPONSE_ALG.toString())));
        result.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.fromName(requestObject.optString(USERINFO_ENCRYPTED_RESPONSE_ENC.toString())));
        result.setRequestObjectSigningAlg(SignatureAlgorithm.fromString(requestObject.optString(REQUEST_OBJECT_SIGNING_ALG.toString())));
        result.setRequestObjectEncryptionAlg(KeyEncryptionAlgorithm.fromName(requestObject.optString(REQUEST_OBJECT_ENCRYPTION_ALG.toString())));
        result.setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm.fromName(requestObject.optString(REQUEST_OBJECT_ENCRYPTION_ENC.toString())));
        result.setTokenEndpointAuthMethod(AuthenticationMethod.fromString(requestObject.optString(TOKEN_ENDPOINT_AUTH_METHOD.toString())));
        result.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.fromString(requestObject.optString(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString())));
        result.setRedirectUris(extractListByKey(requestObject, REDIRECT_URIS.toString()));
        result.setScope(extractListByKey(requestObject, SCOPE.toString()));
        result.setClaims(extractListByKey(requestObject, CLAIMS.toString()));
        result.setResponseTypesStrings(extractListByKey(requestObject, RESPONSE_TYPES.toString()));
        result.setGrantTypes(extractGrantTypes(requestObject));
        result.setApplicationType(ApplicationType.fromString(requestObject.optString(APPLICATION_TYPE.toString())));
        result.setContacts(extractListByKey(requestObject, CONTACTS.toString()));
        result.setClientName(requestObject.optString(CLIENT_NAME.toString()));
        result.setIdTokenTokenBindingCnf(requestObject.optString(ID_TOKEN_TOKEN_BINDING_CNF.toString(), ""));
        result.setLogoUri(requestObject.optString(LOGO_URI.toString()));
        result.setClientUri(requestObject.optString(CLIENT_URI.toString()));
        result.setPolicyUri(requestObject.optString(POLICY_URI.toString()));
        result.setTosUri(requestObject.optString(TOS_URI.toString()));
        result.setJwksUri(requestObject.optString(JWKS_URI.toString()));
        result.setJwks(requestObject.optString(JWKS.toString()));
        result.setSectorIdentifierUri(requestObject.optString(SECTOR_IDENTIFIER_URI.toString()));
        result.setSubjectType(SubjectType.fromString(requestObject.optString(SUBJECT_TYPE.toString())));
        result.setSoftwareId(requestObject.optString(SOFTWARE_ID.toString()));
        result.setSoftwareVersion(requestObject.optString(SOFTWARE_VERSION.toString()));
        result.setSoftwareStatement(requestObject.optString(SOFTWARE_STATEMENT.toString()));
        result.setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode.fromString(requestObject.optString(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString())));
        result.setBackchannelClientNotificationEndpoint(requestObject.optString(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString()));
        result.setBackchannelAuthenticationRequestSigningAlg(AsymmetricSignatureAlgorithm.fromString(requestObject.optString(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString())));
        result.setBackchannelUserCodeParameter(booleanOrNull(requestObject, BACKCHANNEL_USER_CODE_PARAMETER.toString()));

        return result;
    }

    public static List<GrantType> extractGrantTypes(JSONObject requestObject) {
        final Set<GrantType> grantTypes = new HashSet<>();
        if (requestObject.has(GRANT_TYPES.toString())) {
            JSONArray grantTypesJsonArray = requestObject.getJSONArray(GRANT_TYPES.toString());
            for (int i = 0; i < grantTypesJsonArray.length(); i++) {
                GrantType gt = GrantType.fromString(grantTypesJsonArray.getString(i));
                if (gt != null) {
                    grantTypes.add(gt);
                }
            }
        }
        return new ArrayList<>(grantTypes);
    }


    @Override
    public JSONObject getJSONParameters() throws JSONException {
        JSONObject parameters = new JSONObject();

        JsonApplier.getInstance().apply(this, parameters);

        if (redirectUris != null && !redirectUris.isEmpty()) {
            parameters.put(REDIRECT_URIS.toString(), toJSONArray(redirectUris));
        }
        if (claimsRedirectUris != null && !claimsRedirectUris.isEmpty()) {
            parameters.put(CLAIMS_REDIRECT_URIS.toString(), toJSONArray(claimsRedirectUris));
        }
        if (responseTypes != null && !responseTypes.isEmpty()) {
            parameters.put(RESPONSE_TYPES.toString(), toJSONArray(responseTypes));
        }
        if (grantTypes != null && !grantTypes.isEmpty()) {
            parameters.put(GRANT_TYPES.toString(), toJSONArray(grantTypes));
        }
        if (applicationType != null) {
            parameters.put(APPLICATION_TYPE.toString(), applicationType.toString());
        }
        if (contacts != null && !contacts.isEmpty()) {
            parameters.put(CONTACTS.toString(), toJSONArray(contacts));
        }
        if (StringUtils.isNotBlank(clientName)) {
            parameters.put(CLIENT_NAME.toString(), clientName);
        }
        if (StringUtils.isNotBlank(idTokenTokenBindingCnf)) {
            parameters.put(ID_TOKEN_TOKEN_BINDING_CNF.toString(), idTokenTokenBindingCnf);
        }
        if (StringUtils.isNotBlank(tlsClientAuthSubjectDn)) {
            parameters.put(TLS_CLIENT_AUTH_SUBJECT_DN.toString(), tlsClientAuthSubjectDn);
        }
        if (allowSpontaneousScopes != null) {
            parameters.put(ALLOW_SPONTANEOUS_SCOPES.toString(), allowSpontaneousScopes);
        }
        if (spontaneousScopes != null && !spontaneousScopes.isEmpty()) {
            parameters.put(SPONTANEOUS_SCOPES.toString(), toJSONArray(spontaneousScopes));
        }
        if (runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims != null) {
            parameters.put(RUN_INTROSPECTION_SCRIPT_BEFORE_ACCESS_TOKEN_CREATION_AS_JWT_AND_INCLUDE_CLAIMS.toString(), runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims);
        }
        if (keepClientAuthorizationAfterExpiration != null) {
            parameters.put(KEEP_CLIENT_AUTHORIZATION_AFTER_EXPIRATION.toString(), keepClientAuthorizationAfterExpiration);
        }
        if (StringUtils.isNotBlank(logoUri)) {
            parameters.put(LOGO_URI.toString(), logoUri);
        }
        if (StringUtils.isNotBlank(clientUri)) {
            parameters.put(CLIENT_URI.toString(), clientUri);
        }
        if (StringUtils.isNotBlank(policyUri)) {
            parameters.put(POLICY_URI.toString(), policyUri);
        }
        if (StringUtils.isNotBlank(tosUri)) {
            parameters.put(TOS_URI.toString(), tosUri);
        }
        if (StringUtils.isNotBlank(jwksUri)) {
            parameters.put(JWKS_URI.toString(), jwksUri);
        }
        if (StringUtils.isNotBlank(jwks)) {
            parameters.put(JWKS.toString(), jwks);
        }
        if (StringUtils.isNotBlank(sectorIdentifierUri)) {
            parameters.put(SECTOR_IDENTIFIER_URI.toString(), sectorIdentifierUri);
        }
        if (subjectType != null) {
            parameters.put(SUBJECT_TYPE.toString(), subjectType.toString());
        }
        if (rptAsJwt != null) {
            parameters.put(RPT_AS_JWT.toString(), rptAsJwt.toString());
        }
        if (accessTokenAsJwt != null) {
            parameters.put(ACCESS_TOKEN_AS_JWT.toString(), accessTokenAsJwt.toString());
        }
        if (accessTokenSigningAlg != null) {
            parameters.put(ACCESS_TOKEN_SIGNING_ALG.toString(), accessTokenSigningAlg.toString());
        }
        if (authorizationSignedResponseAlg != null) {
            parameters.put(AUTHORIZATION_SIGNED_RESPONSE_ALG.toString(), authorizationSignedResponseAlg.toString());
        }
        if (authorizationEncryptedResponseAlg != null) {
            parameters.put(AUTHORIZATION_ENCRYPTED_RESPONSE_ALG.toString(), authorizationEncryptedResponseAlg.toString());
        }
        if (authorizationEncryptedResponseEnc != null) {
            parameters.put(AUTHORIZATION_ENCRYPTED_RESPONSE_ENC.toString(), authorizationEncryptedResponseEnc.toString());
        }
        if (idTokenSignedResponseAlg != null) {
            parameters.put(ID_TOKEN_SIGNED_RESPONSE_ALG.toString(), idTokenSignedResponseAlg.getName());
        }
        if (idTokenEncryptedResponseAlg != null) {
            parameters.put(ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString(), idTokenEncryptedResponseAlg.getName());
        }
        if (idTokenEncryptedResponseEnc != null) {
            parameters.put(ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString(), idTokenEncryptedResponseEnc.getName());
        }
        if (userInfoSignedResponseAlg != null) {
            parameters.put(USERINFO_SIGNED_RESPONSE_ALG.toString(), userInfoSignedResponseAlg.getName());
        }
        if (userInfoEncryptedResponseAlg != null) {
            parameters.put(USERINFO_ENCRYPTED_RESPONSE_ALG.toString(), userInfoEncryptedResponseAlg.getName());
        }
        if (userInfoEncryptedResponseEnc != null) {
            parameters.put(USERINFO_ENCRYPTED_RESPONSE_ENC.toString(), userInfoEncryptedResponseEnc.getName());
        }
        if (requestObjectSigningAlg != null) {
            parameters.put(REQUEST_OBJECT_SIGNING_ALG.toString(), requestObjectSigningAlg.getName());
        }
        if (requestObjectEncryptionAlg != null) {
            parameters.put(REQUEST_OBJECT_ENCRYPTION_ALG.toString(), requestObjectEncryptionAlg.getName());
        }
        if (requestObjectEncryptionEnc != null) {
            parameters.put(REQUEST_OBJECT_ENCRYPTION_ENC.toString(), requestObjectEncryptionEnc.getName());
        }
        if (tokenEndpointAuthMethod != null) {
            parameters.put(TOKEN_ENDPOINT_AUTH_METHOD.toString(), tokenEndpointAuthMethod.toString());
        }
        if (tokenEndpointAuthSigningAlg != null) {
            parameters.put(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString(), tokenEndpointAuthSigningAlg.toString());
        }
        if (defaultMaxAge != null) {
            parameters.put(DEFAULT_MAX_AGE.toString(), defaultMaxAge.toString());
        }
        if (requireAuthTime != null) {
            parameters.put(REQUIRE_AUTH_TIME.toString(), requireAuthTime.toString());
        }
        if (defaultAcrValues != null && !defaultAcrValues.isEmpty()) {
            parameters.put(DEFAULT_ACR_VALUES.toString(), toJSONArray(defaultAcrValues));
        }
        if (StringUtils.isNotBlank(initiateLoginUri)) {
            parameters.put(INITIATE_LOGIN_URI.toString(), initiateLoginUri);
        }
        if (postLogoutRedirectUris != null && !postLogoutRedirectUris.isEmpty()) {
            parameters.put(POST_LOGOUT_REDIRECT_URIS.toString(), toJSONArray(postLogoutRedirectUris));
        }
        if (StringUtils.isNotBlank(frontChannelLogoutUri)) {
            parameters.put(FRONT_CHANNEL_LOGOUT_URI.toString(), frontChannelLogoutUri);
        }
        if (frontChannelLogoutSessionRequired != null) {
            parameters.put(FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED.toString(), frontChannelLogoutSessionRequired.toString());
        }
        if (backchannelLogoutUris != null && !backchannelLogoutUris.isEmpty()) {
            parameters.put(BACKCHANNEL_LOGOUT_URI.toString(), toJSONArray(backchannelLogoutUris));
        }
        if (backchannelLogoutSessionRequired != null) {
            parameters.put(BACKCHANNEL_LOGOUT_SESSION_REQUIRED.toString(), backchannelLogoutSessionRequired.toString());
        }
        if (requestUris != null && !requestUris.isEmpty()) {
            parameters.put(REQUEST_URIS.toString(), toJSONArray(requestUris));
        }
        if (authorizedOrigins != null && !authorizedOrigins.isEmpty()) {
            parameters.put(AUTHORIZED_ORIGINS.toString(), toJSONArray(authorizedOrigins));
        }
        if (scope != null && !scope.isEmpty()) {
            parameters.put(SCOPE.toString(), implode(scope, " "));
        }
        if (claims != null && !claims.isEmpty()) {
            parameters.put(CLAIMS.toString(), implode(claims, " "));
        }
        if (accessTokenLifetime != null) {
            parameters.put(ACCESS_TOKEN_LIFETIME.toString(), accessTokenLifetime);
        }
        if (parLifetime != null) {
            parameters.put(PAR_LIFETIME.toString(), parLifetime);
        }
        if (requirePar != null) {
            parameters.put(REQUIRE_PAR.toString(), requirePar);
        }
        if (StringUtils.isNotBlank(softwareId)) {
            parameters.put(SOFTWARE_ID.toString(), softwareId);
        }
        if (StringUtils.isNotBlank(softwareVersion)) {
            parameters.put(SOFTWARE_VERSION.toString(), softwareVersion);
        }
        if (StringUtils.isNotBlank(softwareStatement)) {
            parameters.put(SOFTWARE_STATEMENT.toString(), softwareStatement);
        }
        if (backchannelTokenDeliveryMode != null) {
            parameters.put(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString(), backchannelTokenDeliveryMode);
        }
        if (StringUtils.isNotBlank(backchannelClientNotificationEndpoint)) {
            parameters.put(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString(), backchannelClientNotificationEndpoint);
        }
        if (backchannelAuthenticationRequestSigningAlg != null) {
            parameters.put(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString(), backchannelAuthenticationRequestSigningAlg.toString());
        }
        if (backchannelUserCodeParameter != null) {
            parameters.put(BACKCHANNEL_USER_CODE_PARAMETER.toString(), backchannelUserCodeParameter);
        }
        // Custom params
        if (customAttributes != null && !customAttributes.isEmpty()) {
            for (Map.Entry<String, String> entry : customAttributes.entrySet()) {
                final String name = entry.getKey();
                final String value = entry.getValue();
                if (RegisterRequestParam.isCustomParameterValid(name) && StringUtils.isNotBlank(value)) {
                    parameters.put(name, value);
                }
            }
        }
        return parameters;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    @Override
    public String getQueryString() {
        try {
            return ClientUtil.toPrettyJson(getJSONParameters()).replace("\\/", "/");
        } catch (JSONException | JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public RegisterRequest sign(SignatureAlgorithm signatureAlgorithm, String kid, AuthCryptoProvider cryptoProvider) throws Exception {
        final SoftwareStatement ssa = new SoftwareStatement(signatureAlgorithm, cryptoProvider);
        ssa.setKeyId(kid);
        return sign(ssa);
    }

    public RegisterRequest signWithSharedKey(SignatureAlgorithm signatureAlgorithm, String sharedKey, AuthCryptoProvider cryptoProvider) throws Exception {
        return sign(new SoftwareStatement(signatureAlgorithm, sharedKey, cryptoProvider));
    }

    private RegisterRequest sign(SoftwareStatement softwareStatement) throws Exception {
        softwareStatement.setClaims(getJSONParameters());
        jwtRequestAsString = softwareStatement.getEncodedJwt();
        return this;
    }

    public String getJwtRequestAsString() {
        return jwtRequestAsString;
    }

    public void setJwtRequestAsString(String jwtRequestAsString) {
        this.jwtRequestAsString = jwtRequestAsString;
    }

    public boolean hasJwtRequestAsString() {
        return StringUtils.isNotBlank(jwtRequestAsString);
    }
}