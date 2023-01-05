/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an OpenId Configuration received from the authorization server.
 *
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
public class OpenIdConfigurationResponse extends BaseResponse implements Serializable {

    private String issuer;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String revocationEndpoint;
    private String sessionRevocationEndpoint;
    private String userInfoEndpoint;
    private String clientInfoEndpoint;
    private String checkSessionIFrame;
    private String endSessionEndpoint;
    private String jwksUri;
    private String registrationEndpoint;
    private String introspectionEndpoint;
    private String parEndpoint;
    private Boolean requirePar;
    private String deviceAuthzEndpoint;
    private List<String> scopesSupported;
    private List<String> responseTypesSupported;
    private List<String> responseModesSupported;
    private List<String> grantTypesSupported;
    private List<String> acrValuesSupported;
    private List<String> subjectTypesSupported;
    private List<String> authorizationSigningAlgValuesSupported;
    private List<String> authorizationEncryptionAlgValuesSupported;
    private List<String> authorizationEncryptionEncValuesSupported;
    private List<String> userInfoSigningAlgValuesSupported;
    private List<String> userInfoEncryptionAlgValuesSupported;
    private List<String> userInfoEncryptionEncValuesSupported;
    private List<String> idTokenSigningAlgValuesSupported;
    private List<String> idTokenEncryptionAlgValuesSupported;
    private List<String> idTokenEncryptionEncValuesSupported;
    private List<String> accessTokenSigningAlgValuesSupported;
    private List<String> requestObjectSigningAlgValuesSupported;
    private List<String> requestObjectEncryptionAlgValuesSupported;
    private List<String> requestObjectEncryptionEncValuesSupported;
    private List<String> tokenEndpointAuthMethodsSupported;
    private List<String> tokenEndpointAuthSigningAlgValuesSupported;
    private List<String> dpopSigningAlgValuesSupported;
    private List<String> displayValuesSupported;
    private List<String> claimTypesSupported;
    private List<String> claimsSupported;
    private List<String> idTokenTokenBindingCnfValuesSupported;
    private String serviceDocumentation;
    private List<String> claimsLocalesSupported;
    private List<String> uiLocalesSupported;
    private Boolean claimsParameterSupported;
    private Boolean requestParameterSupported;
    private Boolean requestUriParameterSupported;
    private Boolean requireRequestUriRegistration;
    private Boolean tlsClientCertificateBoundAccessTokens;
    private Boolean frontChannelLogoutSupported;
    private Boolean frontChannelLogoutSessionSupported;
    private Boolean backchannelLogoutSupported;
    private Boolean backchannelLogoutSessionSupported;
    private String opPolicyUri;
    private String opTosUri;
    private Map<String, List<String>> scopeToClaimsMapping = new HashMap<>();
    private Map<String, Serializable> mltsAliases = new HashMap<>();

    // CIBA
    private String backchannelAuthenticationEndpoint;
    private List<String> backchannelTokenDeliveryModesSupported;
    private List<String> backchannelAuthenticationRequestSigningAlgValuesSupported;
    private Boolean backchannelUserCodeParameterSupported;

    // SSA
    private String ssaEndpoint;

    public OpenIdConfigurationResponse() {
    }

    /**
     * Constructs an OpenID Configuration Response.
     *
     * @param status The response status code.
     */
    public OpenIdConfigurationResponse(int status) {
        super(status);

        scopesSupported = new ArrayList<>();
        responseTypesSupported = new ArrayList<>();
        responseModesSupported = new ArrayList<>();
        grantTypesSupported = new ArrayList<>();
        acrValuesSupported = new ArrayList<>();
        subjectTypesSupported = new ArrayList<>();
        authorizationSigningAlgValuesSupported = new ArrayList<>();
        authorizationEncryptionAlgValuesSupported = new ArrayList<>();
        authorizationEncryptionEncValuesSupported = new ArrayList<>();
        userInfoSigningAlgValuesSupported = new ArrayList<>();
        userInfoEncryptionAlgValuesSupported = new ArrayList<>();
        userInfoEncryptionEncValuesSupported = new ArrayList<>();
        idTokenSigningAlgValuesSupported = new ArrayList<>();
        idTokenEncryptionAlgValuesSupported = new ArrayList<>();
        idTokenEncryptionEncValuesSupported = new ArrayList<>();
        accessTokenSigningAlgValuesSupported = new ArrayList<>();
        requestObjectSigningAlgValuesSupported = new ArrayList<>();
        requestObjectEncryptionAlgValuesSupported = new ArrayList<>();
        requestObjectEncryptionEncValuesSupported = new ArrayList<>();
        tokenEndpointAuthMethodsSupported = new ArrayList<>();
        tokenEndpointAuthSigningAlgValuesSupported = new ArrayList<>();
        dpopSigningAlgValuesSupported = new ArrayList<>();
        displayValuesSupported = new ArrayList<>();
        claimTypesSupported = new ArrayList<>();
        claimsSupported = new ArrayList<>();
        idTokenTokenBindingCnfValuesSupported = new ArrayList<>();
        claimsLocalesSupported = new ArrayList<>();
        uiLocalesSupported = new ArrayList<>();
        backchannelTokenDeliveryModesSupported = new ArrayList<>();
        backchannelAuthenticationRequestSigningAlgValuesSupported = new ArrayList<>();
    }

    public static Map<String, List<String>> parseScopeToClaimsMapping(String scopeToClaimsJson) throws JSONException {
        return parseScopeToClaimsMapping(new JSONArray(scopeToClaimsJson));
    }

    public static Map<String, List<String>> parseScopeToClaimsMapping(JSONArray jsonArray) throws JSONException {
        final Map<String, List<String>> map = new HashMap<>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                final JSONObject obj = jsonArray.getJSONObject(i);
                final String scope = obj.names().getString(0);
                final JSONArray claimsArray = obj.getJSONArray(scope);
                final List<String> claimsList = new ArrayList<>();
                for (int j = 0; j < claimsArray.length(); j++) {
                    final String claim = claimsArray.getString(j);
                    if (StringUtils.isNotBlank(claim)) {
                        claimsList.add(claim);
                    }
                }
                map.put(scope, claimsList);
            }

        }
        return map;
    }

    /**
     * Gets scopes to claims map.
     *
     * @return scopes to claims map
     */
    public Map<String, List<String>> getScopeToClaimsMapping() {
        return scopeToClaimsMapping;
    }

    /**
     * Sets scope to claim map.
     *
     * @param scopeToClaimsMapping scope to claim map
     */
    public void setScopeToClaimsMapping(Map<String, List<String>> scopeToClaimsMapping) {
        this.scopeToClaimsMapping = scopeToClaimsMapping;
    }

    /**
     * Returns the issuer identifier.
     *
     * @return The issuer identifier.
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Sets the issuer identifier.
     *
     * @param issuer The issuer identifier.
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * Returns the URL of the Authentication and Authorization endpoint.
     *
     * @return The URL of the Authentication and Authorization endpoint.
     */
    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    /**
     * Sets the URL of the Authentication and Authorization endpoint.
     *
     * @param authorizationEndpoint The URL of the Authentication and Authorization endpoint.
     */
    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    /**
     * Returns the URL of the Token endpoint.
     *
     * @return The URL of the Token endpoint.
     */
    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    /**
     * Sets the URL of the Token endpoint.
     *
     * @param tokenEndpoint The URL of the Token endpoint.
     */
    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getSessionRevocationEndpoint() {
        return sessionRevocationEndpoint;
    }

    public void setSessionRevocationEndpoint(String sessionRevocationEndpoint) {
        this.sessionRevocationEndpoint = sessionRevocationEndpoint;
    }

    /**
     * Returns the URL of the Token Revocation endpoint.
     *
     * @return The URL of the Token Revocation endpoint.
     */
    public String getRevocationEndpoint() {
        return revocationEndpoint;
    }

    /**
     * Sets the URL of the Token Revocation endpoint.
     *
     * @param revocationEndpoint The URL of the Token Revocation endpoint.
     */
    public void setRevocationEndpoint(String revocationEndpoint) {
        this.revocationEndpoint = revocationEndpoint;
    }

    /**
     * Returns the URL of the User Info endpoint.
     *
     * @return The URL of the User Info endpoint.
     */
    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    /**
     * Sets the URL for the User Info endpoint.
     *
     * @param userInfoEndpoint The URL for the User Info endpoint.
     */
    public void setUserInfoEndpoint(String userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
    }

    /**
     * Returns the URL of the Client Info endpoint.
     *
     * @return The URL of the Client Info endpoint.
     */
    public String getClientInfoEndpoint() {
        return clientInfoEndpoint;
    }

    /**
     * Sets the URL for the Client Info endpoint.
     *
     * @param clientInfoEndpoint The URL for the Client Info endpoint.
     */
    public void setClientInfoEndpoint(String clientInfoEndpoint) {
        this.clientInfoEndpoint = clientInfoEndpoint;
    }

    /**
     * Returns the URL of an OP endpoint that provides a page to support
     * cross-origin communications for session state information with the RP
     * client.
     *
     * @return The Check Session iFrame URL.
     */
    public String getCheckSessionIFrame() {
        return checkSessionIFrame;
    }

    /**
     * Sets the URL of an OP endpoint that provides a page to support
     * cross-origin communications for session state information with the RP
     * client.
     *
     * @param checkSessionIFrame The Check Session iFrame URL.
     */
    public void setCheckSessionIFrame(String checkSessionIFrame) {
        this.checkSessionIFrame = checkSessionIFrame;
    }

    /**
     * Returns the URL of the End Session endpoint.
     *
     * @return The URL of the End Session endpoint.
     */
    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    /**
     * Sets the URL of the End Session endpoint.
     *
     * @param endSessionEndpoint The URL of the End Session endpoint.
     */
    public void setEndSessionEndpoint(String endSessionEndpoint) {
        this.endSessionEndpoint = endSessionEndpoint;
    }

    /**
     * Returns the URL of the OP's JSON Web Key Set (JWK) document that contains
     * the Server's signing key(s) that are used for signing responses to the
     * Client. The JWK Set may also contain the Server's encryption key(s) that
     * are used by the Client to encrypt requests to the Server.
     *
     * @return The URL of the OP's JSON Web Key Set (JWK) document.
     */
    public String getJwksUri() {
        return jwksUri;
    }

    /**
     * Sets the URL of the OP's JSON Web Key Set (JWK) document that contains
     * the Server's signing key(s) that are used for signing responses to the
     * Client. The JWK Set may also contain the Server's encryption key(s) that
     * are used by the Client to encrypt requests to the Server.
     *
     * @param jwksUri The URL of the OP's JSON Web Key Set (JWK) document.
     */
    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    /**
     * Returns the URL of the Dynamic Client Registration endpoint.
     *
     * @return The URL of the Dynamic Client Registration endpoint.
     */
    public String getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    /**
     * Sets the URL of the Dynamic Client Registration endpoint.
     *
     * @param registrationEndpoint The URL of the Dynamic Client Registration endpoint.
     */
    public void setRegistrationEndpoint(String registrationEndpoint) {
        this.registrationEndpoint = registrationEndpoint;
    }

    public String getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    public void setIntrospectionEndpoint(String introspectionEndpoint) {
        this.introspectionEndpoint = introspectionEndpoint;
    }

    public String getParEndpoint() {
        return parEndpoint;
    }

    public void setParEndpoint(String parEndpoint) {
        this.parEndpoint = parEndpoint;
    }

    public Boolean getRequirePar() {
        return requirePar;
    }

    public void setRequirePar(Boolean requirePar) {
        this.requirePar = requirePar;
    }

    /**
     * Returns a list of the OAuth 2.0 scopes that the server supports.
     *
     * @return A list of the OAuth 2.0 scopes that the server supports.
     */
    public List<String> getScopesSupported() {
        return scopesSupported;
    }

    /**
     * Sets a list of the OAuth 2.0 scopes that the server supports.
     *
     * @param scopesSupported A list of the OAuth 2.0 scopes that the server supports.
     */
    public void setScopesSupported(List<String> scopesSupported) {
        this.scopesSupported = scopesSupported;
    }

    /**
     * Returns a list of the response types that the server supports.
     *
     * @return A list of the response types that the server supports.
     */
    public List<String> getResponseTypesSupported() {
        return responseTypesSupported;
    }

    /**
     * Sets a list of the response types that the server supports.
     *
     * @param responseTypesSupported A list of the response types that the server supports.
     */
    public void setResponseTypesSupported(List<String> responseTypesSupported) {
        this.responseTypesSupported = responseTypesSupported;
    }

    public List<String> getResponseModesSupported() {
        return responseModesSupported;
    }

    public void setResponseModesSupported(List<String> responseModesSupported) {
        this.responseModesSupported = responseModesSupported;
    }

    /**
     * Returns a list of the OAuth 2.0 grant type values that this server
     * supports.
     *
     * @return A list of the OAuth 2.0 grant type values that this server
     * supports.
     */
    public List<String> getGrantTypesSupported() {
        return grantTypesSupported;
    }

    /**
     * Sets a list of the OAuth 2.0 grant type values that this server supports.
     *
     * @param grantTypesSupported A list of the OAuth 2.0 grant type values that this server
     *                            supports.
     */
    public void setGrantTypesSupported(List<String> grantTypesSupported) {
        this.grantTypesSupported = grantTypesSupported;
    }

    /**
     * Returns a list of the Authentication Context Class References that this
     * server supports.
     *
     * @return A list of the Authentication Context Class References
     */
    public List<String> getAcrValuesSupported() {
        return acrValuesSupported;
    }

    /**
     * Sets a list of the Authentication Context Class References that this
     * server supports.
     *
     * @param acrValuesSupported A list of the Authentication Context Class References
     */
    public void setAcrValuesSupported(List<String> acrValuesSupported) {
        this.acrValuesSupported = acrValuesSupported;
    }

    /**
     * Returns a list of the subject identifier types that this server supports.
     * Valid types include pairwise and public.
     *
     * @return A list of the subject identifier types that this server supports.
     */
    public List<String> getSubjectTypesSupported() {
        return subjectTypesSupported;
    }

    /**
     * Sets a list of the subject identifier types that this server supports.
     * Valid types include pairwise and public.
     *
     * @param subjectTypesSupported A list of the subject identifier types that this server
     *                              supports.
     */
    public void setSubjectTypesSupported(List<String> subjectTypesSupported) {
        this.subjectTypesSupported = subjectTypesSupported;
    }

    /**
     * Returns a list of the JWS signing algorithms (alg values) JWA supported by the authorization endpoint to sign the
     * responses.
     *
     * @return A list of the JWS singing algorithms.
     */
    public List<String> getAuthorizationSigningAlgValuesSupported() {
        return authorizationSigningAlgValuesSupported;
    }

    /**
     * Sets a list of the JWS signing algorithms (alg values) JWA supported by the authorization endpoint to sign the
     * responses.
     *
     * @param authorizationSigningAlgValuesSupported A list of the JWS singing algorithms.
     */
    public void setAuthorizationSigningAlgValuesSupported(List<String> authorizationSigningAlgValuesSupported) {
        this.authorizationSigningAlgValuesSupported = authorizationSigningAlgValuesSupported;
    }

    /**
     * Returns a list of the JWE encryption algorithms (alg values) JWA supported by the authorization endpoint to
     * encrypt the response.
     *
     * @return A list of the JWE encryption algorithms.
     */
    public List<String> getAuthorizationEncryptionAlgValuesSupported() {
        return authorizationEncryptionAlgValuesSupported;
    }

    /**
     * Sets a list of the JWE encryption algorithms (alg values) JWA supported by the authorization endpoint to encrypt
     * the response.
     *
     * @param authorizationEncryptionAlgValuesSupported A list of the JWE encryption algorithms.
     */
    public void setAuthorizationEncryptionAlgValuesSupported(List<String> authorizationEncryptionAlgValuesSupported) {
        this.authorizationEncryptionAlgValuesSupported = authorizationEncryptionAlgValuesSupported;
    }

    /**
     * Returns a list of the JWE encryption algorithms (enc values) JWA supported by the authorization endpoint to
     * encrypt the response.
     *
     * @return A list of the JWE encryption algorithms.
     */
    public List<String> getAuthorizationEncryptionEncValuesSupported() {
        return authorizationEncryptionEncValuesSupported;
    }

    /**
     * Sets a list of the JWE encryption algorithms (enc values) JWA supported by the authorization endpoint to encrypt
     * the response.
     *
     * @param authorizationEncryptionEncValuesSupported A list of the JWE encryption algorithms.
     */
    public void setAuthorizationEncryptionEncValuesSupported(List<String> authorizationEncryptionEncValuesSupported) {
        this.authorizationEncryptionEncValuesSupported = authorizationEncryptionEncValuesSupported;
    }

    /**
     * Returns a list of the JWS signing algorithms (alg values JWA) supported
     * by the UserInfo Endpoint to encode the claims in a JWT
     *
     * @return A list of the JWS signing algorithms.
     */
    public List<String> getUserInfoSigningAlgValuesSupported() {
        return userInfoSigningAlgValuesSupported;
    }

    /**
     * Sets a list of the JWS signing algorithms (alg values JWA) supported by
     * the UserInfo Endpoint to encode the claims in a JWT
     *
     * @param userInfoSigningAlgValuesSupported A list of the JWS signing algorithms.
     */
    public void setUserInfoSigningAlgValuesSupported(List<String> userInfoSigningAlgValuesSupported) {
        this.userInfoSigningAlgValuesSupported = userInfoSigningAlgValuesSupported;
    }

    /**
     * Returns a list of the JWE encryption algorithms (alg values JWA)
     * supported by the UserInfo Endpoint to encode the claims in a JWT.
     *
     * @return A list of the JWE encryption algorithms.
     */
    public List<String> getUserInfoEncryptionAlgValuesSupported() {
        return userInfoEncryptionAlgValuesSupported;
    }

    /**
     * Sets a list of the JWE encryption algorithms (alg values JWA) supported
     * by the UserInfo Endpoint to encode the claims in a JWT.
     *
     * @param userInfoEncryptionAlgValuesSupported A list of the JWE encryption algorithms.
     */
    public void setUserInfoEncryptionAlgValuesSupported(List<String> userInfoEncryptionAlgValuesSupported) {
        this.userInfoEncryptionAlgValuesSupported = userInfoEncryptionAlgValuesSupported;
    }

    /**
     * Returns a list of the JWE encryption algorithms (enc values JWA)
     * supported by the UserInfo Endpoint to encode the claims in a JWT.
     *
     * @return A list of the JWE encryption algorithms.
     */
    public List<String> getUserInfoEncryptionEncValuesSupported() {
        return userInfoEncryptionEncValuesSupported;
    }

    /**
     * Sets a list of the JWE encryption algorithms (enc values JWA) supported
     * by the UserInfo Endpoint to encode the claims in a JWT.
     *
     * @param userInfoEncryptionEncValuesSupported A list of the JWE encryption algorithms.
     */
    public void setUserInfoEncryptionEncValuesSupported(List<String> userInfoEncryptionEncValuesSupported) {
        this.userInfoEncryptionEncValuesSupported = userInfoEncryptionEncValuesSupported;
    }

    public List<String> getAccessTokenSigningAlgValuesSupported() {
        if (accessTokenSigningAlgValuesSupported == null) accessTokenSigningAlgValuesSupported = new ArrayList<>();
        return accessTokenSigningAlgValuesSupported;
    }

    public void setAccessTokenSigningAlgValuesSupported(List<String> accessTokenSigningAlgValuesSupported) {
        this.accessTokenSigningAlgValuesSupported = accessTokenSigningAlgValuesSupported;
    }

    /**
     * Returns a list of the JWS signing algorithms (alg values) supported by
     * the Authorization Server for the ID Token to encode the claims in a JWT.
     *
     * @return A list of the JWS signing algorithms.
     */
    public List<String> getIdTokenSigningAlgValuesSupported() {
        return idTokenSigningAlgValuesSupported;
    }

    /**
     * Sets a list of the JWS signing algorithms (alg values) supported by the
     * Authorization Server for the ID Token to encode the claims in a JWT.
     *
     * @param idTokenSigningAlgValuesSupported A list of the JWS signing algorithms.
     */
    public void setIdTokenSigningAlgValuesSupported(List<String> idTokenSigningAlgValuesSupported) {
        this.idTokenSigningAlgValuesSupported = idTokenSigningAlgValuesSupported;
    }

    /**
     * Returns a list of the JWE encryption algorithms (alg values) supported by
     * the Authorization Server for the ID Token to encode the claims in a JWT.
     *
     * @return A list of the JWE encryption algorithms.
     */
    public List<String> getIdTokenEncryptionAlgValuesSupported() {
        return idTokenEncryptionAlgValuesSupported;
    }

    /**
     * Sets a list of the JWE encryption algorithms (alg values) supported by
     * the Authorization Server for the ID Token to encode the claims in a JWT.
     *
     * @param idTokenEncryptionAlgValuesSupported A list of the JWE encryption algorithms.
     */
    public void setIdTokenEncryptionAlgValuesSupported(List<String> idTokenEncryptionAlgValuesSupported) {
        this.idTokenEncryptionAlgValuesSupported = idTokenEncryptionAlgValuesSupported;
    }

    /**
     * Returns a list of the JWE encryption algorithms (enc values) supported by
     * the Authorization Server for the ID Token to encode the claims in a JWT.
     *
     * @return A list of the JWE encryption algorithms.
     */
    public List<String> getIdTokenEncryptionEncValuesSupported() {
        return idTokenEncryptionEncValuesSupported;
    }

    /**
     * Sets a list of the JWE encryption algorithms (enc values) supported by
     * the Authorization Server for the ID Token to encode the claims in a JWT.
     *
     * @param idTokenEncryptionEncValuesSupported A list of the JWE encryption algorithms.
     */
    public void setIdTokenEncryptionEncValuesSupported(List<String> idTokenEncryptionEncValuesSupported) {
        this.idTokenEncryptionEncValuesSupported = idTokenEncryptionEncValuesSupported;
    }

    /**
     * Returns a list of the JWS signing algorithms (alg values) supported by
     * the Authorization Server for the OpenID Request Object.
     *
     * @return A list of the JWS signing algorithms.
     */
    public List<String> getRequestObjectSigningAlgValuesSupported() {
        return requestObjectSigningAlgValuesSupported;
    }

    /**
     * Sets a list of the JWS signing algorithms (alg values) supported by the
     * Authorization Server for the OpenID Request Object.
     *
     * @param requestObjectSigningAlgValuesSupported A list of the JWS signing algorithms.
     */
    public void setRequestObjectSigningAlgValuesSupported(List<String> requestObjectSigningAlgValuesSupported) {
        this.requestObjectSigningAlgValuesSupported = requestObjectSigningAlgValuesSupported;
    }

    /**
     * Returns a list of the JWE encryption algorithms (alg values) supported by
     * the Authorization Server for the OpenID Request Object.
     *
     * @return A list of the JWE encryption algorithms.
     */
    public List<String> getRequestObjectEncryptionAlgValuesSupported() {
        return requestObjectEncryptionAlgValuesSupported;
    }

    /**
     * Sets a list of the JWE encryption algorithms (alg values) supported by
     * the Authorization Server for the OpenID Request Object.
     *
     * @param requestObjectEncryptionAlgValuesSupported A list of the JWE encryption algorithms.
     */
    public void setRequestObjectEncryptionAlgValuesSupported(List<String> requestObjectEncryptionAlgValuesSupported) {
        this.requestObjectEncryptionAlgValuesSupported = requestObjectEncryptionAlgValuesSupported;
    }

    /**
     * Returns a list of the JWE encryption algorithms (enc values) supported by
     * the Authorization Server for the OpenID Request Object.
     *
     * @return A list of the JWE encryption algorithms.
     */
    public List<String> getRequestObjectEncryptionEncValuesSupported() {
        return requestObjectEncryptionEncValuesSupported;
    }

    /**
     * Sets a list of the JWE encryption algorithms (enc values) supported by
     * the Authorization Server for the OpenID Request Object.
     *
     * @param requestObjectEncryptionEncValuesSupported A list of the JWE encryption algorithms.
     */
    public void setRequestObjectEncryptionEncValuesSupported(List<String> requestObjectEncryptionEncValuesSupported) {
        this.requestObjectEncryptionEncValuesSupported = requestObjectEncryptionEncValuesSupported;
    }

    /**
     * Returns a list of authentication types supported by this Token Endpoint.
     * The options are client_secret_post, client_secret_basic,
     * client_secret_jwt, and private_key_jwt. Other authentication types may be
     * defined by extension. If unspecified or omitted, the default is
     * client_secret_basic, the HTTP Basic Authentication Scheme.
     *
     * @return A list of authentication types.
     */
    public List<String> getTokenEndpointAuthMethodsSupported() {
        return tokenEndpointAuthMethodsSupported;
    }

    /**
     * Sets a list of authentication types supported by this Token Endpoint. The
     * options are client_secret_post, client_secret_basic, client_secret_jwt,
     * and private_key_jwt. Other authentication types may be defined by
     * extension. If unspecified or omitted, the default is client_secret_basic,
     * the HTTP Basic Authentication Scheme.
     *
     * @param tokenEndpointAuthMethodsSupported A list of authentication types.
     */
    public void setTokenEndpointAuthMethodsSupported(List<String> tokenEndpointAuthMethodsSupported) {
        this.tokenEndpointAuthMethodsSupported = tokenEndpointAuthMethodsSupported;
    }

    /**
     * Returns a list of the JWS signing algorithms (alg values) supported by
     * the Token Endpoint for the private_key_jwt and client_secret_jwt methods
     * to encode the JWT. Servers SHOULD support RS256.
     *
     * @return A list of the JWS signing algorithms.
     */
    public List<String> getTokenEndpointAuthSigningAlgValuesSupported() {
        return tokenEndpointAuthSigningAlgValuesSupported;
    }

    /**
     * Sets a list of the JWS signing algorithms (alg values) supported by the
     * Token Endpoint for the private_key_jwt and client_secret_jwt methods to
     * encode the JWT. Servers SHOULD support RS256.
     *
     * @param tokenEndpointAuthSigningAlgValuesSupported A list of the JWS signing algorithms.
     */
    public void setTokenEndpointAuthSigningAlgValuesSupported(List<String> tokenEndpointAuthSigningAlgValuesSupported) {
        this.tokenEndpointAuthSigningAlgValuesSupported = tokenEndpointAuthSigningAlgValuesSupported;
    }

    /**
     * Returns a list of JWS alg values supported by the authorization server for DPoP proof JWTs.
     *
     * @return A list of JWS alg values supported by the authorization server for DPoP proof JWTs.
     */
    public List<String> getDpopSigningAlgValuesSupported() {
        return dpopSigningAlgValuesSupported;
    }

    /**
     * Sets a list of JWS alg values supported by the authorization server for DPoP proof JWTs.
     *
     * @param dpopSigningAlgValuesSupported A list of JWS alg values supported by the authorization server for DPoP proof JWTs.
     */
    public void setDpopSigningAlgValuesSupported(List<String> dpopSigningAlgValuesSupported) {
        this.dpopSigningAlgValuesSupported = dpopSigningAlgValuesSupported;
    }

    /**
     * Returns a list of the display parameter values that the OpenID Provider
     * supports.
     *
     * @return A list of the display parameter values.
     */
    public List<String> getDisplayValuesSupported() {
        return displayValuesSupported;
    }

    /**
     * Sets a list of the display parameter values that the OpenID Provider
     * supports.
     *
     * @param displayValuesSupported A list of the display parameter values.
     */
    public void setDisplayValuesSupported(List<String> displayValuesSupported) {
        this.displayValuesSupported = displayValuesSupported;
    }

    /**
     * Returns a list of the claim types that the OpenID Provider supports. If
     * not specified, the implementation supports only normal claims.
     *
     * @return A list of the claim types.
     */
    public List<String> getClaimTypesSupported() {
        return claimTypesSupported;
    }

    /**
     * Sets a list of the claim types that the OpenID Provider supports. If not
     * specified, the implementation supports only normal claims.
     *
     * @param claimTypesSupported A list of the claim types.
     */
    public void setClaimTypesSupported(List<String> claimTypesSupported) {
        this.claimTypesSupported = claimTypesSupported;
    }

    /**
     * Returns a list of the Claim Names of the Claims that the OpenID Provider
     * may be able to supply values for. Note that for privacy or other reasons,
     * this may not be an exhaustive list.
     *
     * @return A list of Claim Names.
     */
    public List<String> getClaimsSupported() {
        return claimsSupported;
    }

    /**
     * Sets a list of the Claim Names of the Claims that the OpenID Provider may
     * be able to supply values for. Note that for privacy or other reasons,
     * this may not be an exhaustive list.
     *
     * @param claimsSupported A list of Claim Names.
     */
    public void setClaimsSupported(List<String> claimsSupported) {
        this.claimsSupported = claimsSupported;
    }


    public List<String> getIdTokenTokenBindingCnfValuesSupported() {
        return idTokenTokenBindingCnfValuesSupported;
    }

    public void setIdTokenTokenBindingCnfValuesSupported(List<String> idTokenTokenBindingCnfValuesSupported) {
        this.idTokenTokenBindingCnfValuesSupported = idTokenTokenBindingCnfValuesSupported;
    }

    /**
     * Returns an URL of a page containing human-readable information that
     * developers might want or need to know when using the OpenID Provider. In
     * particular, if the OpenID Provider does not support dynamic client
     * registration, then information on how to register clients should be
     * provided in this documentation.
     *
     * @return An URL with information for developers.
     */
    public String getServiceDocumentation() {
        return serviceDocumentation;
    }

    /**
     * Sets an URL of a page containing human-readable information that
     * developers might want or need to know when using the OpenID Provider. In
     * particular, if the OpenID Provider does not support dynamic client
     * registration, then information on how to register clients should be
     * provided in this documentation.
     *
     * @param serviceDocumentation An URL with information for developers.
     */
    public void setServiceDocumentation(String serviceDocumentation) {
        this.serviceDocumentation = serviceDocumentation;
    }

    /**
     * Returns a list of languages and scripts supported for values in Claims
     * being returned.
     *
     * @return A list of languages and scripts supported for values in Claims
     * being returned.
     */
    public List<String> getClaimsLocalesSupported() {
        return claimsLocalesSupported;
    }

    /**
     * Sets a list of languages and scripts supported for values in Claims being
     * returned.
     *
     * @param claimsLocalesSupported A list of languages and scripts supported for values in Claims
     *                               being returned.
     */
    public void setClaimsLocalesSupported(List<String> claimsLocalesSupported) {
        this.claimsLocalesSupported = claimsLocalesSupported;
    }

    /**
     * Returns a list of languages and scripts supported for the user interface.
     *
     * @return A list of languages and scripts supported for the user interface.
     */
    public List<String> getUiLocalesSupported() {
        return uiLocalesSupported;
    }

    /**
     * Sets a list of languages and scripts supported for the user interface.
     *
     * @param uiLocalesSupported A list of languages and scripts supported for the user
     *                           interface.
     */
    public void setUiLocalesSupported(List<String> uiLocalesSupported) {
        this.uiLocalesSupported = uiLocalesSupported;
    }

    /**
     * Returns a Boolean value specifying whether the OP supports use of the
     * claims parameter, with <code>true</code> indicating support. If omitted,
     * the default value is <code>false</code>.
     *
     * @return A Boolean value specifying whether the OP supports use of the
     * claims parameter.
     */
    public Boolean getClaimsParameterSupported() {
        return claimsParameterSupported;
    }

    /**
     * Sets a Boolean value specifying whether the OP supports use of the claims
     * parameter, with <code>true</code> indicating support. If omitted, the
     * default value is <code>false</code>.
     *
     * @param claimsParameterSupported A Boolean value specifying whether the OP supports use of the
     *                                 claims parameter.
     */
    public void setClaimsParameterSupported(Boolean claimsParameterSupported) {
        this.claimsParameterSupported = claimsParameterSupported;
    }

    /**
     * Returns a Boolean value specifying whether the OP supports use of the
     * request parameter, with <code>true</code> indicating support. If omitted,
     * the default value is <code>false</code>.
     *
     * @return A Boolean value specifying whether the OP supports use of the
     * request parameter.
     */
    public Boolean getRequestParameterSupported() {
        return requestParameterSupported;
    }

    /**
     * Sets a Boolean value specifying whether the OP supports use of the
     * request parameter, with <code>true</code> indicating support. If omitted,
     * the default value is <code>false</code>.
     *
     * @param requestParameterSupported A Boolean value specifying whether the OP supports use of the
     *                                  request parameter.
     */
    public void setRequestParameterSupported(Boolean requestParameterSupported) {
        this.requestParameterSupported = requestParameterSupported;
    }

    /**
     * Returns a Boolean value specifying whether the OP supports use of the
     * request_uri parameter, with <code>true</code> indicating support. If
     * omitted, the default value is <code>true</code>.
     *
     * @return A Boolean value specifying whether the OP supports use of the
     * request_uri parameter.
     */
    public Boolean getRequestUriParameterSupported() {
        return requestUriParameterSupported;
    }

    /**
     * Sets a Boolean value specifying whether the OP supports use of the
     * request_uri parameter, with <code>true</code> indicating support. If
     * omitted, the default value is <code>true</code>.
     *
     * @param requestUriParameterSupported A Boolean value specifying whether the OP supports use of the
     *                                     request_uri parameter.
     */
    public void setRequestUriParameterSupported(Boolean requestUriParameterSupported) {
        this.requestUriParameterSupported = requestUriParameterSupported;
    }

    /**
     * Returns a Boolean value specifying whether the OP requires any
     * request_uri values used to be pre-registered using the request_uris
     * registration parameter. Pre-registration is required when the value is
     * <code>true</code>.
     *
     * @return A Boolean value specifying whether the OP requires any
     * request_uri values used to be pre-registered using the
     * request_uris registration parameter.
     */
    public Boolean getRequireRequestUriRegistration() {
        return requireRequestUriRegistration;
    }

    /**
     * Sets a Boolean value specifying whether the OP requires any request_uri
     * values used to be pre-registered using the request_uris registration
     * parameter. Pre-registration is required when the value is
     * <code>true</code>.
     *
     * @param requireRequestUriRegistration A Boolean value specifying whether the OP requires any
     *                                      request_uri values used to be pre-registered using the
     *                                      request_uris registration parameter.
     */
    public void setRequireRequestUriRegistration(Boolean requireRequestUriRegistration) {
        this.requireRequestUriRegistration = requireRequestUriRegistration;
    }

    /**
     * Returns a URL that the OpenID Provider provides to the person registering
     * the Client to read about the OP's requirements on how the Relying Party
     * may use the data provided by the OP.
     *
     * @return The OP's policy URI.
     */
    public String getOpPolicyUri() {
        return opPolicyUri;
    }

    /**
     * Sets a URL that the OpenID Provider provides to the person registering
     * the Client to read about the OP's requirements on how the Relying Party
     * may use the data provided by the OP.
     *
     * @param opPolicyUri The OP's policy URI.
     */
    public void setOpPolicyUri(String opPolicyUri) {
        this.opPolicyUri = opPolicyUri;
    }

    /**
     * Returns a URL that the OpenID Provider provides to the person registering
     * the Client to read about OpenID Provider's terms of service.
     *
     * @return The OP's policy URI.
     */
    public String getOpTosUri() {
        return opTosUri;
    }

    /**
     * Sets a URL that the OpenID Provider provides to the person registering
     * the Client to read about OpenID Provider's terms of service.
     *
     * @param opTosUri The OP's policy URI.
     */
    public void setOpTosUri(String opTosUri) {
        this.opTosUri = opTosUri;
    }

    public Boolean getFrontChannelLogoutSupported() {
        return frontChannelLogoutSupported;
    }

    public void setFrontChannelLogoutSupported(Boolean frontChannelLogoutSupported) {
        this.frontChannelLogoutSupported = frontChannelLogoutSupported;
    }

    public Boolean getBackchannelLogoutSupported() {
        return backchannelLogoutSupported;
    }

    public void setBackchannelLogoutSupported(Boolean backchannelLogoutSupported) {
        this.backchannelLogoutSupported = backchannelLogoutSupported;
    }

    public Boolean getBackchannelLogoutSessionSupported() {
        return backchannelLogoutSessionSupported;
    }

    public void setBackchannelLogoutSessionSupported(Boolean backchannelLogoutSessionSupported) {
        this.backchannelLogoutSessionSupported = backchannelLogoutSessionSupported;
    }

    public Boolean getTlsClientCertificateBoundAccessTokens() {
        return tlsClientCertificateBoundAccessTokens;
    }

    public void setTlsClientCertificateBoundAccessTokens(Boolean tlsClientCertificateBoundAccessTokens) {
        this.tlsClientCertificateBoundAccessTokens = tlsClientCertificateBoundAccessTokens;
    }

    public Boolean getFrontChannelLogoutSessionSupported() {
        return frontChannelLogoutSessionSupported;
    }

    public void setFrontChannelLogoutSessionSupported(Boolean frontChannelLogoutSessionSupported) {
        this.frontChannelLogoutSessionSupported = frontChannelLogoutSessionSupported;
    }

    public String getBackchannelAuthenticationEndpoint() {
        return backchannelAuthenticationEndpoint;
    }

    public void setBackchannelAuthenticationEndpoint(String backchannelAuthenticationEndpoint) {
        this.backchannelAuthenticationEndpoint = backchannelAuthenticationEndpoint;
    }

    public List<String> getBackchannelTokenDeliveryModesSupported() {
        return backchannelTokenDeliveryModesSupported;
    }

    public void setBackchannelTokenDeliveryModesSupported(List<String> backchannelTokenDeliveryModesSupported) {
        this.backchannelTokenDeliveryModesSupported = backchannelTokenDeliveryModesSupported;
    }

    public List<String> getBackchannelAuthenticationRequestSigningAlgValuesSupported() {
        return backchannelAuthenticationRequestSigningAlgValuesSupported;
    }

    public void setBackchannelAuthenticationRequestSigningAlgValuesSupported(List<String> backchannelAuthenticationRequestSigningAlgValuesSupported) {
        this.backchannelAuthenticationRequestSigningAlgValuesSupported = backchannelAuthenticationRequestSigningAlgValuesSupported;
    }

    public Boolean getBackchannelUserCodeParameterSupported() {
        return backchannelUserCodeParameterSupported;
    }

    public void setBackchannelUserCodeParameterSupported(Boolean backchannelUserCodeParameterSupported) {
        this.backchannelUserCodeParameterSupported = backchannelUserCodeParameterSupported;
    }

    public String getDeviceAuthzEndpoint() {
        return deviceAuthzEndpoint;
    }

    public void setDeviceAuthzEndpoint(String deviceAuthzEndpoint) {
        this.deviceAuthzEndpoint = deviceAuthzEndpoint;
    }

    public Map<String, Serializable> getMltsAliases() {
        return mltsAliases;
    }

    public void setMltsAliases(Map<String, Serializable> mltsAliases) {
        this.mltsAliases = mltsAliases;
    }

    public String getSsaEndpoint() {
        return ssaEndpoint;
    }

    public void setSsaEndpoint(String ssaEndpoint) {
        this.ssaEndpoint = ssaEndpoint;
    }

    @Override
    public String toString() {
        return "OpenIdConfigurationResponse{" +
                "issuer='" + issuer + '\'' +
                ", authorizationEndpoint='" + authorizationEndpoint + '\'' +
                ", tokenEndpoint='" + tokenEndpoint + '\'' +
                ", revocationEndpoint='" + revocationEndpoint + '\'' +
                ", userInfoEndpoint='" + userInfoEndpoint + '\'' +
                ", clientInfoEndpoint='" + clientInfoEndpoint + '\'' +
                ", checkSessionIFrame='" + checkSessionIFrame + '\'' +
                ", endSessionEndpoint='" + endSessionEndpoint + '\'' +
                ", jwksUri='" + jwksUri + '\'' +
                ", registrationEndpoint='" + registrationEndpoint + '\'' +
                ", introspectionEndpoint='" + introspectionEndpoint + '\'' +
                ", deviceAuthzEndpoint='" + deviceAuthzEndpoint + '\'' +
                ", scopesSupported=" + scopesSupported +
                ", responseTypesSupported=" + responseTypesSupported +
                ", responseModesSupported=" + responseModesSupported +
                ", grantTypesSupported=" + grantTypesSupported +
                ", acrValuesSupported=" + acrValuesSupported +
                ", subjectTypesSupported=" + subjectTypesSupported +
                ", authorizationSigningAlgValuesSupported=" + authorizationSigningAlgValuesSupported +
                ", authorizationEncryptionAlgValuesSupported=" + authorizationEncryptionAlgValuesSupported +
                ", authorizationEncryptionEncValuesSupported=" + authorizationEncryptionEncValuesSupported +
                ", userInfoSigningAlgValuesSupported=" + userInfoSigningAlgValuesSupported +
                ", userInfoEncryptionAlgValuesSupported=" + userInfoEncryptionAlgValuesSupported +
                ", userInfoEncryptionEncValuesSupported=" + userInfoEncryptionEncValuesSupported +
                ", idTokenSigningAlgValuesSupported=" + idTokenSigningAlgValuesSupported +
                ", idTokenEncryptionAlgValuesSupported=" + idTokenEncryptionAlgValuesSupported +
                ", idTokenEncryptionEncValuesSupported=" + idTokenEncryptionEncValuesSupported +
                ", accessTokenSigningAlgValuesSupported=" + accessTokenSigningAlgValuesSupported +
                ", requestObjectSigningAlgValuesSupported=" + requestObjectSigningAlgValuesSupported +
                ", requestObjectEncryptionAlgValuesSupported=" + requestObjectEncryptionAlgValuesSupported +
                ", requestObjectEncryptionEncValuesSupported=" + requestObjectEncryptionEncValuesSupported +
                ", tokenEndpointAuthMethodsSupported=" + tokenEndpointAuthMethodsSupported +
                ", tokenEndpointAuthSigningAlgValuesSupported=" + tokenEndpointAuthSigningAlgValuesSupported +
                ", displayValuesSupported=" + displayValuesSupported +
                ", claimTypesSupported=" + claimTypesSupported +
                ", claimsSupported=" + claimsSupported +
                ", idTokenTokenBindingCnfValuesSupported=" + idTokenTokenBindingCnfValuesSupported +
                ", serviceDocumentation='" + serviceDocumentation + '\'' +
                ", claimsLocalesSupported=" + claimsLocalesSupported +
                ", uiLocalesSupported=" + uiLocalesSupported +
                ", claimsParameterSupported=" + claimsParameterSupported +
                ", requestParameterSupported=" + requestParameterSupported +
                ", requestUriParameterSupported=" + requestUriParameterSupported +
                ", tlsClientCertificateBoundAccessTokens=" + tlsClientCertificateBoundAccessTokens +
                ", frontChannelLogoutSupported=" + frontChannelLogoutSupported +
                ", frontChannelLogoutSessionSupported=" + frontChannelLogoutSessionSupported +
                ", backchannelLogoutSupported=" + backchannelLogoutSupported +
                ", backchannelLogoutSessionSupported=" + backchannelLogoutSessionSupported +
                ", requireRequestUriRegistration=" + requireRequestUriRegistration +
                ", opPolicyUri='" + opPolicyUri + '\'' +
                ", opTosUri='" + opTosUri + '\'' +
                ", scopeToClaimsMapping=" + scopeToClaimsMapping + '\'' +
                ", backchannelAuthenticationEndpoint=" + backchannelAuthenticationEndpoint + '\'' +
                ", backchannelTokenDeliveryModesSupported=" + backchannelTokenDeliveryModesSupported + '\'' +
                ", backchannelAuthenticationRequestSigningAlgValuesSupported=" + backchannelAuthenticationRequestSigningAlgValuesSupported + '\'' +
                ", backchannelUserCodeParameterSupported=" + backchannelUserCodeParameterSupported + '\'' +
                ", mltsAliases=" + mltsAliases + '\'' +
                ", ssaEndpoint=" + ssaEndpoint + '\'' +
                '}';
    }
}
