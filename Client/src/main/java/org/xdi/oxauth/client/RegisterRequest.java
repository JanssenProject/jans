/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.common.SubjectType;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.register.RegisterRequestParam;

import javax.ws.rs.core.MediaType;
import java.util.*;

import static org.xdi.oxauth.model.register.RegisterRequestParam.*;
import static org.xdi.oxauth.model.util.StringUtils.toJSONArray;

/**
 * Represents a register request to send to the authorization server.
 *
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 * @version September 1, 2015
 */
public class RegisterRequest extends BaseRequest {

    private String registrationAccessToken;
    private List<String> redirectUris;
    private List<ResponseType> responseTypes;
    private List<GrantType> grantTypes;
    private ApplicationType applicationType;
    private List<String> contacts;
    private String clientName;
    private String logoUri;
    private String clientUri;
    private AuthenticationMethod tokenEndpointAuthMethod;
    private String policyUri;
    private String logoutUri;
    private Boolean logoutSessionRequired;
    private String tosUri;
    private String jwksUri;
    private String jwks;
    private String sectorIdentifierUri;
    private SubjectType subjectType;
    private SignatureAlgorithm requestObjectSigningAlg;
    private SignatureAlgorithm userInfoSignedResponseAlg;
    private KeyEncryptionAlgorithm userInfoEncryptedResponseAlg;
    private BlockEncryptionAlgorithm userInfoEncryptedResponseEnc;
    private SignatureAlgorithm idTokenSignedResponseAlg;
    private KeyEncryptionAlgorithm idTokenEncryptedResponseAlg;
    private BlockEncryptionAlgorithm idTokenEncryptedResponseEnc;
    private Integer defaultMaxAge;
    private Boolean requireAuthTime;
    private List<String> defaultAcrValues;
    private String initiateLoginUri;
    private List<String> postLogoutRedirectUris;
    private List<String> requestUris;
    private List<String> scopes;
    private String federationId;
    private String federationUrl;
    private Map<String, String> customAttributes;

    // internal state
    private JSONObject m_jsonObject;
    private String m_httpMethod;

    /**
     * Private common constructor.
     */
    private RegisterRequest() {
        setContentType(MediaType.APPLICATION_JSON);
        setMediaType(MediaType.APPLICATION_JSON);

        this.redirectUris = new ArrayList<String>();
        this.responseTypes = new ArrayList<ResponseType>();
        this.grantTypes = new ArrayList<GrantType>();
        this.contacts = new ArrayList<String>();
        this.defaultAcrValues = new ArrayList<String>();
        this.postLogoutRedirectUris = new ArrayList<String>();
        this.requestUris = new ArrayList<String>();
        this.scopes = new ArrayList<String>();
        this.customAttributes = new HashMap<String, String>();
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

    /**
     * Gets logout uri.
     * @return logout uri
     */
    public String getLogoutUri() {
        return logoutUri;
    }

    /**
     * Sets logout uri
     *
     * @param logoutUri logout uri
     */
    public void setLogoutUri(String logoutUri) {
        this.logoutUri = logoutUri;
    }

    /**
     * Gets logout session required.
     *
     * @return logout session required
     */
    public Boolean getLogoutSessionRequired() {
        return logoutSessionRequired;
    }

    /**
     * Sets logout session required.
     *
     * @param logoutSessionRequired logout session required
     */
    public void setLogoutSessionRequired(Boolean logoutSessionRequired) {
        this.logoutSessionRequired = logoutSessionRequired;
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
     * Returns a list of the OAuth 2.0 response_type values that the Client is declaring that it will restrict itself
     * to using.
     *
     * @return A list of response types.
     */
    public List<ResponseType> getResponseTypes() {
        return responseTypes;
    }

    /**
     * Sets a list of the OAuth 2.0 response_type values that the Client is declaring that it will restrict itself to
     * using. If omitted, the default is that the Client will use only the code response type.
     *
     * @param responseTypes A list of response types.
     */
    public void setResponseTypes(List<ResponseType> responseTypes) {
        this.responseTypes = responseTypes;
    }

    /**
     * Returns a list of the OAuth 2.0 grant types that the Client is declaring that it will restrict itself to using.
     *
     * @return A list of grant types.
     */
    public List<GrantType> getGrantTypes() {
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

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getFederationId() {
        return federationId;
    }

    public void setFederationId(String p_federationId) {
        federationId = p_federationId;
    }

    public String getFederationUrl() {
        return federationUrl;
    }

    public void setFederationUrl(String p_federationUrl) {
        federationUrl = p_federationUrl;
    }

    public String getHttpMethod() {
        return m_httpMethod;
    }

    public void setHttpMethod(String p_httpMethod) {
        m_httpMethod = p_httpMethod;
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

    public void addCustomAttribute(String p_name, String p_value) {
        if (RegisterRequestParam.isCustomParameterValid(p_name)) {
            this.customAttributes.put(p_name, p_value);
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
        Map<String, String> parameters = new HashMap<String, String>();

        if (redirectUris != null && !redirectUris.isEmpty()) {
            parameters.put(REDIRECT_URIS.toString(), toJSONArray(redirectUris).toString());
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
        if (tokenEndpointAuthMethod != null) {
            parameters.put(TOKEN_ENDPOINT_AUTH_METHOD.toString(), tokenEndpointAuthMethod.toString());
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
        if (requestObjectSigningAlg != null) {
            parameters.put(REQUEST_OBJECT_SIGNING_ALG.toString(), requestObjectSigningAlg.getName());
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
        if (idTokenSignedResponseAlg != null) {
            parameters.put(ID_TOKEN_SIGNED_RESPONSE_ALG.toString(), idTokenSignedResponseAlg.getName());
        }
        if (idTokenEncryptedResponseAlg != null) {
            parameters.put(ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString(), idTokenEncryptedResponseAlg.getName());
        }
        if (idTokenEncryptedResponseEnc != null) {
            parameters.put(ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString(), idTokenEncryptedResponseEnc.getName());
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
        if (!Strings.isNullOrEmpty(logoutUri)) {
            parameters.put(LOGOUT_URI.toString(), logoutUri);
        }
        if (logoutSessionRequired != null) {
            parameters.put(LOGOUT_SESSION_REQUIRED.toString(), logoutSessionRequired.toString());
        }
        if (requestUris != null && !requestUris.isEmpty()) {
            parameters.put(REQUEST_URIS.toString(), toJSONArray(requestUris).toString());
        }
        if (scopes != null && !scopes.isEmpty()) {
            parameters.put(SCOPES.toString(), toJSONArray(scopes).toString());
        }
        // Federation params
        if (!StringUtils.isBlank(federationUrl)) {
            parameters.put(FEDERATION_METADATA_URL.toString(), federationUrl);
        }
        if (!StringUtils.isBlank(federationId)) {
            parameters.put(FEDERATION_METADATA_ID.toString(), federationId);
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

    public static RegisterRequest fromJson(String p_json) throws JSONException {
        final JSONObject requestObject = new JSONObject(p_json);

        final List<String> redirectUris = new ArrayList<String>();
        if (requestObject.has(REDIRECT_URIS.toString())) {
            JSONArray redirectUrisJsonArray = requestObject.getJSONArray(REDIRECT_URIS.toString());
            for (int i = 0; i < redirectUrisJsonArray.length(); i++) {
                String redirectionUri = redirectUrisJsonArray.getString(i);
                redirectUris.add(redirectionUri);
            }
        }

        final Set<ResponseType> responseTypes = new HashSet<ResponseType>();
        final Set<GrantType> grantTypes = new HashSet<GrantType>();
        if (requestObject.has(RESPONSE_TYPES.toString())) {
            JSONArray responseTypesJsonArray = requestObject.getJSONArray(RESPONSE_TYPES.toString());
            for (int i = 0; i < responseTypesJsonArray.length(); i++) {
                ResponseType rt = ResponseType.fromString(responseTypesJsonArray.getString(i));
                if (rt != null) {
                    responseTypes.add(rt);
                }
            }
        } else { // Default
            responseTypes.add(ResponseType.CODE);
        }
        if (responseTypes.contains(ResponseType.CODE)) {
            grantTypes.add(GrantType.AUTHORIZATION_CODE);
        }
        if (responseTypes.contains(ResponseType.ID_TOKEN)
                || responseTypes.containsAll(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN))) {
            grantTypes.add(GrantType.IMPLICIT);
        }
        if (responseTypes.containsAll(Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN))
                || responseTypes.containsAll(Arrays.asList(ResponseType.CODE, ResponseType.TOKEN))
                || responseTypes.containsAll(Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN))) {
            grantTypes.add(GrantType.AUTHORIZATION_CODE);
            grantTypes.add(GrantType.IMPLICIT);
        }
        if (requestObject.has(GRANT_TYPES.toString())) {
            JSONArray grantTypesJsonArray = requestObject.getJSONArray(GRANT_TYPES.toString());
            for (int i = 0; i < grantTypesJsonArray.length(); i++) {
                GrantType gt = GrantType.fromString(grantTypesJsonArray.getString(i));
                if (gt != null) {
                    grantTypes.add(gt);
                    switch (gt) {
                        case AUTHORIZATION_CODE:
                            responseTypes.add(ResponseType.CODE);
                            break;
                        case IMPLICIT:
                            responseTypes.add(ResponseType.TOKEN);
                            responseTypes.add(ResponseType.ID_TOKEN);
                            break;
                        case REFRESH_TOKEN:
                            break;
                        default:
                            break;
                    }
                }
            }
        } else { // Default
            grantTypes.add(GrantType.AUTHORIZATION_CODE);
        }

        final List<String> contacts = new ArrayList<String>();
        if (requestObject.has(CONTACTS.toString())) {
            JSONArray contactsJsonArray = requestObject.getJSONArray(CONTACTS.toString());
            for (int i = 0; i < contactsJsonArray.length(); i++) {
                contacts.add(contactsJsonArray.getString(i));
            }
        }

        final List<String> defaultAcrValues = new ArrayList<String>();
        if (requestObject.has(DEFAULT_ACR_VALUES.toString())) {
            JSONArray defaultAcrValuesJsonArray = requestObject.getJSONArray(DEFAULT_ACR_VALUES.toString());
            for (int i = 0; i < defaultAcrValuesJsonArray.length(); i++) {
                defaultAcrValues.add(defaultAcrValuesJsonArray.getString(i));
            }
        }

        final List<String> postLogoutRedirectUris = new ArrayList<String>();
        if (requestObject.has(POST_LOGOUT_REDIRECT_URIS.toString())) {
            JSONArray postLogoutRedirectUrisJsonArray = requestObject.getJSONArray(POST_LOGOUT_REDIRECT_URIS.toString());
            for (int i = 0; i < postLogoutRedirectUrisJsonArray.length(); i++) {
                postLogoutRedirectUris.add(postLogoutRedirectUrisJsonArray.getString(i));
            }
        }

        final List<String> requestUris = new ArrayList<String>();
        if (requestObject.has(REQUEST_URIS.toString())) {
            JSONArray requestUrisJsonArray = requestObject.getJSONArray(REQUEST_URIS.toString());
            for (int i = 0; i < requestUrisJsonArray.length(); i++) {
                requestUris.add(requestUrisJsonArray.getString(i));
            }
        }

        final List<String> scopes = new ArrayList<String>();
        if (requestObject.has(SCOPES.toString())) {
            JSONArray scopesJsonArray = requestObject.getJSONArray(SCOPES.toString());
            for (int i = 0; i < scopesJsonArray.length(); i++) {
                scopes.add(scopesJsonArray.getString(i));
            }
        }

        final RegisterRequest result = new RegisterRequest();
        result.setJsonObject(requestObject);
        result.setFederationUrl(requestObject.optString(FEDERATION_METADATA_URL.toString()));
        result.setFederationId(requestObject.optString(FEDERATION_METADATA_ID.toString()));
        result.setRequestUris(requestUris);
        result.setInitiateLoginUri(requestObject.optString(INITIATE_LOGIN_URI.toString()));
        result.setPostLogoutRedirectUris(postLogoutRedirectUris);
        result.setDefaultAcrValues(defaultAcrValues);
        result.setRequireAuthTime(requestObject.has(REQUIRE_AUTH_TIME.toString()) && requestObject.getBoolean(REQUIRE_AUTH_TIME.toString()));
        result.setLogoutUri(requestObject.optString(LOGOUT_URI.toString()));
        result.setLogoutSessionRequired(requestObject.optBoolean(LOGOUT_SESSION_REQUIRED.toString()));
        result.setDefaultMaxAge(requestObject.has(DEFAULT_MAX_AGE.toString()) ?
                requestObject.getInt(DEFAULT_MAX_AGE.toString()) : null);
        result.setIdTokenEncryptedResponseEnc(requestObject.has(ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString()) ?
                BlockEncryptionAlgorithm.fromName(requestObject.getString(ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString())) : null);
        result.setIdTokenEncryptedResponseAlg(requestObject.has(ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString()) ?
                KeyEncryptionAlgorithm.fromName(requestObject.getString(ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString())) : null);
        result.setIdTokenSignedResponseAlg(requestObject.has(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()) ?
                SignatureAlgorithm.fromName(requestObject.getString(ID_TOKEN_SIGNED_RESPONSE_ALG.toString())) : null);
        result.setUserInfoEncryptedResponseEnc(requestObject.has(USERINFO_ENCRYPTED_RESPONSE_ENC.toString()) ?
                BlockEncryptionAlgorithm.fromName(requestObject.getString(USERINFO_ENCRYPTED_RESPONSE_ENC.toString())) : null);
        result.setUserInfoEncryptedResponseAlg(requestObject.has(USERINFO_ENCRYPTED_RESPONSE_ALG.toString()) ?
                KeyEncryptionAlgorithm.fromName(requestObject.getString(USERINFO_ENCRYPTED_RESPONSE_ALG.toString())) : null);
        result.setUserInfoSignedResponseAlg(requestObject.has(USERINFO_SIGNED_RESPONSE_ALG.toString()) ?
                SignatureAlgorithm.fromName(requestObject.getString(USERINFO_SIGNED_RESPONSE_ALG.toString())) : null);
        result.setRedirectUris(redirectUris);
        result.setScopes(scopes);
        result.setResponseTypes(new ArrayList<ResponseType>(responseTypes));
        result.setGrantTypes(new ArrayList<GrantType>(grantTypes));
        result.setApplicationType(requestObject.has(APPLICATION_TYPE.toString()) ?
                ApplicationType.fromString(requestObject.getString(APPLICATION_TYPE.toString())) : ApplicationType.WEB);
        result.setContacts(contacts);
        result.setClientName(requestObject.optString(CLIENT_NAME.toString()));
        result.setLogoUri(requestObject.optString(LOGO_URI.toString()));
        result.setClientUri(requestObject.optString(CLIENT_URI.toString()));
        result.setTokenEndpointAuthMethod(requestObject.has(TOKEN_ENDPOINT_AUTH_METHOD.toString()) ?
                AuthenticationMethod.fromString(requestObject.getString(TOKEN_ENDPOINT_AUTH_METHOD.toString())) : AuthenticationMethod.CLIENT_SECRET_BASIC);
        result.setPolicyUri(requestObject.optString(POLICY_URI.toString()));
        result.setTosUri(requestObject.optString(TOS_URI.toString()));
        result.setJwksUri(requestObject.optString(JWKS_URI.toString()));
        result.setJwks(requestObject.optString(JWKS.toString()));
        result.setSectorIdentifierUri(requestObject.optString(SECTOR_IDENTIFIER_URI.toString()));
        result.setSubjectType(requestObject.has(SUBJECT_TYPE.toString()) ?
                SubjectType.fromString(requestObject.getString(SUBJECT_TYPE.toString())) : null);
        result.setRequestObjectSigningAlg(requestObject.has(REQUEST_OBJECT_SIGNING_ALG.toString()) ?
                SignatureAlgorithm.fromName(requestObject.getString(REQUEST_OBJECT_SIGNING_ALG.toString())) : null);
        return result;
    }

    @Override
    public JSONObject getJSONParameters() throws JSONException {
        JSONObject parameters = new JSONObject();

        if (redirectUris != null && !redirectUris.isEmpty()) {
            parameters.put(REDIRECT_URIS.toString(), toJSONArray(redirectUris));
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
        if (StringUtils.isNotBlank(logoUri)) {
            parameters.put(LOGO_URI.toString(), logoUri);
        }
        if (StringUtils.isNotBlank(clientUri)) {
            parameters.put(CLIENT_URI.toString(), clientUri);
        }
        if (tokenEndpointAuthMethod != null) {
            parameters.put(TOKEN_ENDPOINT_AUTH_METHOD.toString(), tokenEndpointAuthMethod.toString());
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
            parameters.put(JWKS_URI.toString(), jwks);
        }
        if (StringUtils.isNotBlank(sectorIdentifierUri)) {
            parameters.put(SECTOR_IDENTIFIER_URI.toString(), sectorIdentifierUri);
        }
        if (subjectType != null) {
            parameters.put(SUBJECT_TYPE.toString(), subjectType.toString());
        }
        if (requestObjectSigningAlg != null) {
            parameters.put(REQUEST_OBJECT_SIGNING_ALG.toString(), requestObjectSigningAlg.getName());
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
        if (idTokenSignedResponseAlg != null) {
            parameters.put(ID_TOKEN_SIGNED_RESPONSE_ALG.toString(), idTokenSignedResponseAlg.getName());
        }
        if (idTokenEncryptedResponseAlg != null) {
            parameters.put(ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString(), idTokenEncryptedResponseAlg.getName());
        }
        if (idTokenEncryptedResponseEnc != null) {
            parameters.put(ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString(), idTokenEncryptedResponseEnc.getName());
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
        if (!Strings.isNullOrEmpty(logoutUri)) {
            parameters.put(LOGOUT_URI.toString(), logoutUri);
        }
        if (logoutSessionRequired != null) {
            parameters.put(LOGOUT_SESSION_REQUIRED.toString(), logoutSessionRequired.toString());
        }
        if (requestUris != null && !requestUris.isEmpty()) {
            parameters.put(REQUEST_URIS.toString(), toJSONArray(requestUris));
        }
        if (scopes != null && !scopes.isEmpty()) {
            parameters.put(SCOPES.toString(), toJSONArray(scopes));
        }
        // Federation params
        if (!StringUtils.isBlank(federationUrl)) {
            parameters.put(FEDERATION_METADATA_URL.toString(), federationUrl);
        }
        if (!StringUtils.isBlank(federationId)) {
            parameters.put(FEDERATION_METADATA_ID.toString(), federationId);
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
        return m_jsonObject;
    }

    public void setJsonObject(JSONObject p_jsonObject) {
        m_jsonObject = p_jsonObject;
    }

    @Override
    public String getQueryString() {
        String jsonQueryString = null;

        try {
            jsonQueryString = getJSONParameters().toString(4).replace("\\/", "/");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonQueryString;
    }
}