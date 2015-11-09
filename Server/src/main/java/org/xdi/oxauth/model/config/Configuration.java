/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.config;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.xdi.oxauth.model.common.Mode;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the configuration XML file.
 *
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version October 16, 2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {

    private String mode;
    private String issuer;
    private String loginPage;
    private String authorizationPage;
    private String baseEndpoint;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userInfoEndpoint;
    private String clientInfoEndpoint;
    private String checkSessionIFrame;
    private String endSessionEndpoint;
    private String endSessionPage;
    private String jwksUri;
    private String registrationEndpoint;
    private String validateTokenEndpoint;
    private String federationMetadataEndpoint;
    private String federationEndpoint;
    private String openIdDiscoveryEndpoint;
    private String openIdConfigurationEndpoint;
    private String idGenerationEndpoint;
    private String introspectionEndpoint;
    private String umaConfigurationEndpoint;
    private String openidSubAttribute;
    private List<String> responseTypesSupported;
    private List<String> grantTypesSupported;
    private List<String> subjectTypesSupported;
    private List<String> userInfoSigningAlgValuesSupported;
    private List<String> userInfoEncryptionAlgValuesSupported;
    private List<String> userInfoEncryptionEncValuesSupported;
    private List<String> idTokenSigningAlgValuesSupported;
    private List<String> idTokenEncryptionAlgValuesSupported;
    private List<String> idTokenEncryptionEncValuesSupported;
    private List<String> requestObjectSigningAlgValuesSupported;
    private List<String> requestObjectEncryptionAlgValuesSupported;
    private List<String> requestObjectEncryptionEncValuesSupported;
    private List<String> tokenEndpointAuthMethodsSupported;
    private List<String> tokenEndpointAuthSigningAlgValuesSupported;
    private List<String> dynamicRegistrationCustomAttributes;
    private List<String> displayValuesSupported;
    private List<String> claimTypesSupported;
    private String serviceDocumentation;
    private List<String> claimsLocalesSupported;
    private List<String> uiLocalesSupported;
    private Boolean claimsParameterSupported;
    private Boolean requestParameterSupported;
    private Boolean requestUriParameterSupported;
    private Boolean requireRequestUriRegistration;
    private String opPolicyUri;
    private String opTosUri;
    private int authorizationCodeLifetime;
    private int refreshTokenLifetime;
    private int idTokenLifetime;
    private int shortLivedAccessTokenLifetime;
    private int longLivedAccessTokenLifetime;
    private int umaRequesterPermissionTokenLifetime;
    private Boolean umaAddScopesAutomatically;
    private Boolean umaKeepClientDuringResourceSetRegistration;
    private int cleanServiceInterval;
    private int federationCheckInterval;
    private Boolean keyRegenerationEnabled;
    private int keyRegenerationInterval;
    private String defaultSignatureAlgorithm;
    private String oxOpenIdConnectVersion;
    private String organizationInum;
    private String oxId;
    private Boolean dynamicRegistrationEnabled;
    private int dynamicRegistrationExpirationTime;
    private Boolean dynamicRegistrationPersistClientAuthorizations;
    private Boolean trustedClientEnabled;
    private Boolean dynamicRegistrationScopesParamEnabled;
    private String dynamicRegistrationCustomObjectClass;

    private Boolean federationEnabled;
    private String federationSkipPolicy;
    private String federationScopePolicy;
    private String federationSigningAlg;
    private String federationSigningKid;

    private Boolean authenticationFiltersEnabled;
    private Boolean clientAuthenticationFiltersEnabled;
    private List<AuthenticationFilter> authenticationFilters;
    private List<ClientAuthenticationFilter> clientAuthenticationFilters;

    private String applianceInum;
    private int sessionIdUnusedLifetime;
    private int sessionIdUnauthenticatedUnusedLifetime = 120; // 120 seconds
    private Boolean sessionIdEnabled;
    private Boolean sessionIdPersistOnPromptNone;
    private int configurationUpdateInterval;

    private String cssLocation;
    private String jsLocation;
    private String imgLocation;
    private int metricReporterInterval;
    private int metricReporterKeepDataDays;

    @XmlElement(name = "uma-keep-client-during-resource-set-registration")
    public Boolean getUmaKeepClientDuringResourceSetRegistration() {
        return umaKeepClientDuringResourceSetRegistration;
    }

    public void setUmaKeepClientDuringResourceSetRegistration(Boolean p_umaKeepClientDuringResourceSetRegistration) {
        umaKeepClientDuringResourceSetRegistration = p_umaKeepClientDuringResourceSetRegistration;
    }

    @XmlElement(name = "uma-add-scopes-automatically")
    public Boolean getUmaAddScopesAutomatically() {
        return umaAddScopesAutomatically;
    }

    public void setUmaAddScopesAutomatically(Boolean p_umaAddScopesAutomatically) {
        umaAddScopesAutomatically = p_umaAddScopesAutomatically;
    }

    @XmlElement(name = "mode")
    public String getMode() {
        return mode;
    }

    public void setMode(String p_mode) {
        mode = p_mode;
    }

    @JsonIgnore
    public Mode getModeEnum() {
        return Mode.fromValueWithDefault(getMode());
    }

    /**
     * Returns the issuer identifier.
     *
     * @return The issuer identifier.
     */
    @XmlElement(name = "issuer")
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
     * Returns the URL od the login page.
     *
     * @return The URL of the login page.
     */
    @XmlElement(name = "login-page")
    public String getLoginPage() {
        return loginPage;
    }

    /**
     * Sets the URL of the login page.
     *
     * @param loginPage The URL of the login page.
     */
    public void setLoginPage(String loginPage) {
        this.loginPage = loginPage;
    }

    /**
     * Returns the URL of the authorization page.
     *
     * @return The URL of the authorization page.
     */
    @XmlElement(name = "authorization-page")
    public String getAuthorizationPage() {
        return authorizationPage;
    }

    /**
     * Sets the URL of the authorization page.
     *
     * @param authorizationPage The URL of the authorization page.
     */
    public void setAuthorizationPage(String authorizationPage) {
        this.authorizationPage = authorizationPage;
    }

    /**
     * Returns the base URI of the endpoints.
     *
     * @return The base URI of endpoints.
     */
    @XmlElement(name = "base-endpoint")
    public String getBaseEndpoint() {
        return baseEndpoint;
    }

    /**
     * Sets the base URI of the endpoints.
     *
     * @param baseEndpoint The base URI of the endpoints.
     */
    public void setBaseEndpoint(String baseEndpoint) {
        this.baseEndpoint = baseEndpoint;
    }

    /**
     * Returns the URL of the Authentication and Authorization endpoint.
     *
     * @return The URL of the Authentication and Authorization endpoint.
     */
    @XmlElement(name = "authorization-endpoint")
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
    @XmlElement(name = "token-endpoint")
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

    /**
     * Returns the URL of the User Info endpoint.
     *
     * @return The URL of the User Info endpoint.
     */
    @XmlElement(name = "userinfo-endpoint")
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
     * Returns the URL od the Client Info endpoint.
     *
     * @return The URL of the Client Info endpoint.
     */
    @XmlElement(name = "clientinfo-endpoint")
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
     * Returns the URL of an OP endpoint that provides a page to support cross-origin
     * communications for session state information with the RP client.
     *
     * @return The Check Session iFrame URL.
     */
    @XmlElement(name = "check-session-iframe")
    public String getCheckSessionIFrame() {
        return checkSessionIFrame;
    }

    /**
     * Sets the  URL of an OP endpoint that provides a page to support cross-origin
     * communications for session state information with the RP client.
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
    @XmlElement(name = "end-session-endpoint")
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
     * Returns the URL of the End Session page.
     *
     * @return The URL of the End Session page.
     */
    @XmlElement(name = "end-session-page")
    public String getEndSessionPage() {
        return endSessionPage;
    }

    /**
     * Sets the URL of the End Session page.
     *
     * @param endSessionPage The URL of the End Session page.
     */
    public void setEndSessionPage(String endSessionPage) {
        this.endSessionPage = endSessionPage;
    }

    /**
     * Returns the URL of the OP's JSON Web Key Set (JWK) document that contains the Server's signing key(s)
     * that are used for signing responses to the Client.
     * The JWK Set may also contain the Server's encryption key(s) that are used by the Client to encrypt
     * requests to the Server.
     *
     * @return The URL of the OP's JSON Web Key Set (JWK) document.
     */
    @XmlElement(name = "jwks-uri")
    public String getJwksUri() {
        return jwksUri;
    }

    /**
     * Sets the URL of the OP's JSON Web Key Set (JWK) document that contains the Server's signing key(s)
     * that are used for signing responses to the Client.
     * The JWK Set may also contain the Server's encryption key(s) that are used by the Client to encrypt
     * requests to the Server.
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
    @XmlElement(name = "registration-endpoint")
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

    @XmlElement(name = "validate-token-endpoint")
    public String getValidateTokenEndpoint() {
        return validateTokenEndpoint;
    }

    public void setValidateTokenEndpoint(String validateTokenEndpoint) {
        this.validateTokenEndpoint = validateTokenEndpoint;
    }

    @XmlElement(name = "federation-metadata-endpoint")
    public String getFederationMetadataEndpoint() {
        return federationMetadataEndpoint;
    }

    public void setFederationMetadataEndpoint(String federationMetadataEndpoint) {
        this.federationMetadataEndpoint = federationMetadataEndpoint;
    }

    @XmlElement(name = "federation-endpoint")
    public String getFederationEndpoint() {
        return federationEndpoint;
    }

    public void setFederationEndpoint(String federationEndpoint) {
        this.federationEndpoint = federationEndpoint;
    }

    @XmlElement(name = "openid-discovery-endpoint")
    public String getOpenIdDiscoveryEndpoint() {
        return openIdDiscoveryEndpoint;
    }

    public void setOpenIdDiscoveryEndpoint(String openIdDiscoveryEndpoint) {
        this.openIdDiscoveryEndpoint = openIdDiscoveryEndpoint;
    }

    @XmlElement(name = "uma-configuration-endpoint")
    public String getUmaConfigurationEndpoint() {
        return umaConfigurationEndpoint;
    }

    public void setUmaConfigurationEndpoint(String p_umaConfigurationEndpoint) {
        umaConfigurationEndpoint = p_umaConfigurationEndpoint;
    }

    @XmlElement(name = "openid-sub-attribute")
    public String getOpenidSubAttribute() {
        return openidSubAttribute;
    }

    public void setOpenidSubAttribute(String openidSubAttribute) {
        this.openidSubAttribute = openidSubAttribute;
    }

    @XmlElement(name = "id-generation-endpoint")
    public String getIdGenerationEndpoint() {
        return idGenerationEndpoint;
    }

    public void setIdGenerationEndpoint(String p_idGenerationEndpoint) {
        idGenerationEndpoint = p_idGenerationEndpoint;
    }

    @XmlElement(name = "introspection-endpoint")
    public String getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    public void setIntrospectionEndpoint(String p_introspectionEndpoint) {
        introspectionEndpoint = p_introspectionEndpoint;
    }

    @XmlElement(name = "openid-configuration-endpoint")
    public String getOpenIdConfigurationEndpoint() {
        return openIdConfigurationEndpoint;
    }

    public void setOpenIdConfigurationEndpoint(String openIdConfigurationEndpoint) {
        this.openIdConfigurationEndpoint = openIdConfigurationEndpoint;
    }

    @XmlElementWrapper(name = "response-types-supported")
    @XmlElement(name = "response-type")
    public List<String> getResponseTypesSupported() {
        return responseTypesSupported;
    }

    public void setResponseTypesSupported(List<String> responseTypesSupported) {
        this.responseTypesSupported = responseTypesSupported;
    }

    @XmlElementWrapper(name = "grant-types-supported")
    @XmlElement(name = "grant-type")
    public List<String> getGrantTypesSupported() {
        return grantTypesSupported;
    }

    public void setGrantTypesSupported(List<String> grantTypesSupported) {
        this.grantTypesSupported = grantTypesSupported;
    }

    @XmlElementWrapper(name = "subject-types-supported")
    @XmlElement(name = "subject-type")
    public List<String> getSubjectTypesSupported() {
        return subjectTypesSupported;
    }

    public void setSubjectTypesSupported(List<String> subjectTypesSupported) {
        this.subjectTypesSupported = subjectTypesSupported;
    }

    @XmlElementWrapper(name = "userinfo-signing-alg-values-supported")
    @XmlElement(name = "userinfo-signing-alg")
    public List<String> getUserInfoSigningAlgValuesSupported() {
        return userInfoSigningAlgValuesSupported;
    }

    public void setUserInfoSigningAlgValuesSupported(List<String> userInfoSigningAlgValuesSupported) {
        this.userInfoSigningAlgValuesSupported = userInfoSigningAlgValuesSupported;
    }

    @XmlElementWrapper(name = "userinfo-encryption-alg-values-supported")
    @XmlElement(name = "userinfo-encryption-alg")
    public List<String> getUserInfoEncryptionAlgValuesSupported() {
        return userInfoEncryptionAlgValuesSupported;
    }

    public void setUserInfoEncryptionAlgValuesSupported(List<String> userInfoEncryptionAlgValuesSupported) {
        this.userInfoEncryptionAlgValuesSupported = userInfoEncryptionAlgValuesSupported;
    }

    @XmlElementWrapper(name = "userinfo-encryption-enc-values-supported")
    @XmlElement(name = "userinfo-encryption-enc")
    public List<String> getUserInfoEncryptionEncValuesSupported() {
        return userInfoEncryptionEncValuesSupported;
    }

    public void setUserInfoEncryptionEncValuesSupported(List<String> userInfoEncryptionEncValuesSupported) {
        this.userInfoEncryptionEncValuesSupported = userInfoEncryptionEncValuesSupported;
    }

    @XmlElementWrapper(name = "id-token-signing-alg-values-supported")
    @XmlElement(name = "id-token-signing-alg")
    public List<String> getIdTokenSigningAlgValuesSupported() {
        return idTokenSigningAlgValuesSupported;
    }

    public void setIdTokenSigningAlgValuesSupported(List<String> idTokenSigningAlgValuesSupported) {
        this.idTokenSigningAlgValuesSupported = idTokenSigningAlgValuesSupported;
    }

    @XmlElementWrapper(name = "id-token-encryption-alg-values-supported")
    @XmlElement(name = "id-token-encryption-alg")
    public List<String> getIdTokenEncryptionAlgValuesSupported() {
        return idTokenEncryptionAlgValuesSupported;
    }

    public void setIdTokenEncryptionAlgValuesSupported(List<String> idTokenEncryptionAlgValuesSupported) {
        this.idTokenEncryptionAlgValuesSupported = idTokenEncryptionAlgValuesSupported;
    }

    @XmlElementWrapper(name = "id-token-encryption-enc-values-supported")
    @XmlElement(name = "id-token-encryption-enc")
    public List<String> getIdTokenEncryptionEncValuesSupported() {
        return idTokenEncryptionEncValuesSupported;
    }

    public void setIdTokenEncryptionEncValuesSupported(List<String> idTokenEncryptionEncValuesSupported) {
        this.idTokenEncryptionEncValuesSupported = idTokenEncryptionEncValuesSupported;
    }

    @XmlElementWrapper(name = "request-object-signing-alg-values-supported")
    @XmlElement(name = "request-object-signing-alg")
    public List<String> getRequestObjectSigningAlgValuesSupported() {
        return requestObjectSigningAlgValuesSupported;
    }

    public void setRequestObjectSigningAlgValuesSupported(List<String> requestObjectSigningAlgValuesSupported) {
        this.requestObjectSigningAlgValuesSupported = requestObjectSigningAlgValuesSupported;
    }

    @XmlElementWrapper(name = "request-object-encryption-alg-values-supported")
    @XmlElement(name = "request-object-encryption-alg")
    public List<String> getRequestObjectEncryptionAlgValuesSupported() {
        return requestObjectEncryptionAlgValuesSupported;
    }

    public void setRequestObjectEncryptionAlgValuesSupported(List<String> requestObjectEncryptionAlgValuesSupported) {
        this.requestObjectEncryptionAlgValuesSupported = requestObjectEncryptionAlgValuesSupported;
    }

    @XmlElementWrapper(name = "request-object-encryption-enc-values-supported")
    @XmlElement(name = "request-object-encryption-enc")
    public List<String> getRequestObjectEncryptionEncValuesSupported() {
        return requestObjectEncryptionEncValuesSupported;
    }

    public void setRequestObjectEncryptionEncValuesSupported(List<String> requestObjectEncryptionEncValuesSupported) {
        this.requestObjectEncryptionEncValuesSupported = requestObjectEncryptionEncValuesSupported;
    }

    @XmlElementWrapper(name = "token-endpoint-auth-methods-supported")
    @XmlElement(name = "token-endpoint-auth-method")
    public List<String> getTokenEndpointAuthMethodsSupported() {
        return tokenEndpointAuthMethodsSupported;
    }

    public void setTokenEndpointAuthMethodsSupported(List<String> tokenEndpointAuthMethodsSupported) {
        this.tokenEndpointAuthMethodsSupported = tokenEndpointAuthMethodsSupported;
    }

    @XmlElementWrapper(name = "token-endpoint-auth-signing-alg-values-supported")
    @XmlElement(name = "token-endpoint-auth-signing-alg")
    public List<String> getTokenEndpointAuthSigningAlgValuesSupported() {
        return tokenEndpointAuthSigningAlgValuesSupported;
    }

    public void setTokenEndpointAuthSigningAlgValuesSupported(List<String> tokenEndpointAuthSigningAlgValuesSupported) {
        this.tokenEndpointAuthSigningAlgValuesSupported = tokenEndpointAuthSigningAlgValuesSupported;
    }

    @XmlElementWrapper(name = "dynamic-registration-custom-attribute-supported")
    @XmlElement(name = "dynamic-registration-custom-attribute")
    public List<String> getDynamicRegistrationCustomAttributes() {
        return dynamicRegistrationCustomAttributes;
    }

    public void setDynamicRegistrationCustomAttributes(List<String> p_dynamicRegistrationCustomAttributes) {
        dynamicRegistrationCustomAttributes = p_dynamicRegistrationCustomAttributes;
    }

    @XmlElementWrapper(name = "display-values-supported")
    @XmlElement(name = "display-value")
    public List<String> getDisplayValuesSupported() {
        return displayValuesSupported;
    }

    public void setDisplayValuesSupported(List<String> displayValuesSupported) {
        this.displayValuesSupported = displayValuesSupported;
    }

    @XmlElementWrapper(name = "claim-types-supported")
    @XmlElement(name = "claim-type")
    public List<String> getClaimTypesSupported() {
        return claimTypesSupported;
    }

    public void setClaimTypesSupported(List<String> claimTypesSupported) {
        this.claimTypesSupported = claimTypesSupported;
    }

    @XmlElement(name = "service-documentation")
    public String getServiceDocumentation() {
        return serviceDocumentation;
    }

    public void setServiceDocumentation(String serviceDocumentation) {
        this.serviceDocumentation = serviceDocumentation;
    }

    @XmlElementWrapper(name = "claims-locales-supported")
    @XmlElement(name = "claim-locale")
    public List<String> getClaimsLocalesSupported() {
        return claimsLocalesSupported;
    }

    public void setClaimsLocalesSupported(List<String> claimsLocalesSupported) {
        this.claimsLocalesSupported = claimsLocalesSupported;
    }

    @XmlElementWrapper(name = "ui-locales-supported")
    @XmlElement(name = "ui-locale")
    public List<String> getUiLocalesSupported() {
        return uiLocalesSupported;
    }

    public void setUiLocalesSupported(List<String> uiLocalesSupported) {
        this.uiLocalesSupported = uiLocalesSupported;
    }

    @XmlElement(name = "claims-parameter-supported")
    public Boolean getClaimsParameterSupported() {
        return claimsParameterSupported;
    }

    public void setClaimsParameterSupported(Boolean claimsParameterSupported) {
        this.claimsParameterSupported = claimsParameterSupported;
    }

    @XmlElement(name = "request-parameter-supported")
    public Boolean getRequestParameterSupported() {
        return requestParameterSupported;
    }

    public void setRequestParameterSupported(Boolean requestParameterSupported) {
        this.requestParameterSupported = requestParameterSupported;
    }

    @XmlElement(name = "request-uri-parameter-supported")
    public Boolean getRequestUriParameterSupported() {
        return requestUriParameterSupported;
    }

    public void setRequestUriParameterSupported(Boolean requestUriParameterSupported) {
        this.requestUriParameterSupported = requestUriParameterSupported;
    }

    @XmlElement(name = "require-request-uri-registration")
    public Boolean getRequireRequestUriRegistration() {
        return requireRequestUriRegistration;
    }

    public void setRequireRequestUriRegistration(Boolean requireRequestUriRegistration) {
        this.requireRequestUriRegistration = requireRequestUriRegistration;
    }

    @XmlElement(name = "op-policy-uri")
    public String getOpPolicyUri() {
        return opPolicyUri;
    }

    public void setOpPolicyUri(String opPolicyUri) {
        this.opPolicyUri = opPolicyUri;
    }

    @XmlElement(name = "op-tos-uri")
    public String getOpTosUri() {
        return opTosUri;
    }

    public void setOpTosUri(String opTosUri) {
        this.opTosUri = opTosUri;
    }

    @XmlElement(name = "authorization-code-lifetime")
    public int getAuthorizationCodeLifetime() {
        return authorizationCodeLifetime;
    }

    public void setAuthorizationCodeLifetime(int authorizationCodeLifetime) {
        this.authorizationCodeLifetime = authorizationCodeLifetime;
    }

    @XmlElement(name = "refresh-token-lifetime")
    public int getRefreshTokenLifetime() {
        return refreshTokenLifetime;
    }

    public void setRefreshTokenLifetime(int refreshTokenLifetime) {
        this.refreshTokenLifetime = refreshTokenLifetime;
    }

    @XmlElement(name = "id-token-lifetime")
    public int getIdTokenLifetime() {
        return idTokenLifetime;
    }

    public void setIdTokenLifetime(int idTokenLifetime) {
        this.idTokenLifetime = idTokenLifetime;
    }

    @XmlElement(name = "short-lived-access-token-lifetime")
    public int getShortLivedAccessTokenLifetime() {
        return shortLivedAccessTokenLifetime;
    }

    public void setShortLivedAccessTokenLifetime(int shortLivedAccessTokenLifetime) {
        this.shortLivedAccessTokenLifetime = shortLivedAccessTokenLifetime;
    }

    @XmlElement(name = "long-lived-access-token-lifetime")
    public int getLongLivedAccessTokenLifetime() {
        return longLivedAccessTokenLifetime;
    }

    public void setLongLivedAccessTokenLifetime(int longLivedAccessTokenLifetime) {
        this.longLivedAccessTokenLifetime = longLivedAccessTokenLifetime;
    }

    @XmlElement(name = "uma-requester-permission-token-lifetime")
    public int getUmaRequesterPermissionTokenLifetime() {
        return umaRequesterPermissionTokenLifetime;
    }

    public void setUmaRequesterPermissionTokenLifetime(int umaRequesterPermissionTokenLifetime) {
        this.umaRequesterPermissionTokenLifetime = umaRequesterPermissionTokenLifetime;
    }

    @XmlElement(name = "clean-service-interval")
    public int getCleanServiceInterval() {
        return cleanServiceInterval;
    }

    public void setCleanServiceInterval(int p_cleanServiceInterval) {
        cleanServiceInterval = p_cleanServiceInterval;
    }

    @XmlElement(name = "federation-check-interval")
    public int getFederationCheckInterval() {
        return federationCheckInterval;
    }

    public void setFederationCheckInterval(int p_federationCheckInterval) {
        federationCheckInterval = p_federationCheckInterval;
    }

    @XmlElement(name = "key-regeneration-enabled")
    public Boolean getKeyRegenerationEnabled() {
        return keyRegenerationEnabled;
    }

    public void setKeyRegenerationEnabled(Boolean keyRegenerationEnabled) {
        this.keyRegenerationEnabled = keyRegenerationEnabled;
    }

    @XmlElement(name = "key-regeneration-interval")
    public int getKeyRegenerationInterval() {
        return keyRegenerationInterval;
    }

    public void setKeyRegenerationInterval(int keyRegenerationInterval) {
        this.keyRegenerationInterval = keyRegenerationInterval;
    }

    @XmlElement(name = "default-signature-algorithm")
    public String getDefaultSignatureAlgorithm() {
        return defaultSignatureAlgorithm;
    }

    public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {
        this.defaultSignatureAlgorithm = defaultSignatureAlgorithm;
    }

    @XmlElement(name = "oxOpenIDConnectVersion")
    public String getOxOpenIdConnectVersion() {
        return oxOpenIdConnectVersion;
    }

    public void setOxOpenIdConnectVersion(String oxOpenIdConnectVersion) {
        this.oxOpenIdConnectVersion = oxOpenIdConnectVersion;
    }

    @XmlElement(name = "organization-inum")
    public String getOrganizationInum() {
        return organizationInum;
    }

    public void setOrganizationInum(String organizationInum) {
        this.organizationInum = organizationInum;
    }

    @XmlElement(name = "oxID")
    public String getOxId() {
        return oxId;
    }

    public void setOxId(String oxId) {
        this.oxId = oxId;
    }

    @XmlElement(name = "dynamic-registration-enabled")
    public Boolean getDynamicRegistrationEnabled() {
        return dynamicRegistrationEnabled;
    }

    public void setDynamicRegistrationEnabled(Boolean dynamicRegistrationEnabled) {
        this.dynamicRegistrationEnabled = dynamicRegistrationEnabled;
    }

    @XmlElement(name = "dynamic-registration-expiration-time")
    public int getDynamicRegistrationExpirationTime() {
        return dynamicRegistrationExpirationTime;
    }

    public void setDynamicRegistrationExpirationTime(int dynamicRegistrationExpirationTime) {
        this.dynamicRegistrationExpirationTime = dynamicRegistrationExpirationTime;
    }

    @XmlElement(name = "dynamic-registration-persist-client-authorizations")
    public Boolean getDynamicRegistrationPersistClientAuthorizations() {
        return dynamicRegistrationPersistClientAuthorizations;
    }

    public void setDynamicRegistrationPersistClientAuthorizations(Boolean dynamicRegistrationPersistClientAuthorizations) {
        this.dynamicRegistrationPersistClientAuthorizations = dynamicRegistrationPersistClientAuthorizations;
    }

    @XmlElement(name = "trusted-client-enabled")
    public Boolean getTrustedClientEnabled() {
        return trustedClientEnabled;
    }

    public void setTrustedClientEnabled(Boolean trustedClientEnabled) {
        this.trustedClientEnabled = trustedClientEnabled;
    }

    @XmlElement(name = "dynamic-registration-scopes-param-enabled")
    public Boolean getDynamicRegistrationScopesParamEnabled() {
        return dynamicRegistrationScopesParamEnabled;
    }

    public void setDynamicRegistrationScopesParamEnabled(Boolean dynamicRegistrationScopesParamEnabled) {
        this.dynamicRegistrationScopesParamEnabled = dynamicRegistrationScopesParamEnabled;
    }

    @XmlElement(name = "dynamic-registration-custom-object-class")
    public String getDynamicRegistrationCustomObjectClass() {
        return dynamicRegistrationCustomObjectClass;
    }

    public void setDynamicRegistrationCustomObjectClass(String p_dynamicRegistrationCustomObjectClass) {
        dynamicRegistrationCustomObjectClass = p_dynamicRegistrationCustomObjectClass;
    }

    @XmlElement(name = "federation-enabled")
    public Boolean getFederationEnabled() {
        return federationEnabled;
    }

    public void setFederationEnabled(Boolean p_federationEnabled) {
        federationEnabled = p_federationEnabled;
    }

    @XmlElement(name = "federation-skip-policy")
    public String getFederationSkipPolicy() {
        return federationSkipPolicy;
    }

    public void setFederationSkipPolicy(String p_federationSkipPolicy) {
        federationSkipPolicy = p_federationSkipPolicy;
    }

    @XmlElement(name = "federation-signing-kid")
    public String getFederationSigningKid() {
        return federationSigningKid;
    }

    public void setFederationSigningKid(String p_federationSigningKid) {
        federationSigningKid = p_federationSigningKid;
    }

    @XmlElement(name = "federation-signing-alg")
    public String getFederationSigningAlg() {
        return federationSigningAlg;
    }

    public void setFederationSigningAlg(String p_federationSigningAlg) {
        federationSigningAlg = p_federationSigningAlg;
    }

    @XmlElement(name = "federation-scope-policy")
    public String getFederationScopePolicy() {
        return federationScopePolicy;
    }

    public void setFederationScopePolicy(String p_federationScopePolicy) {
        federationScopePolicy = p_federationScopePolicy;
    }

    @XmlElement(name = "auth-filters-enabled")
    public Boolean getAuthenticationFiltersEnabled() {
        return authenticationFiltersEnabled;
    }

    public void setAuthenticationFiltersEnabled(Boolean authenticationFiltersEnabled) {
        this.authenticationFiltersEnabled = authenticationFiltersEnabled;
    }

    @XmlElement(name = "client-auth-filters-enabled")
    public Boolean getClientAuthenticationFiltersEnabled() {
        return clientAuthenticationFiltersEnabled;
    }

    public void setClientAuthenticationFiltersEnabled(Boolean p_clientAuthenticationFiltersEnabled) {
        clientAuthenticationFiltersEnabled = p_clientAuthenticationFiltersEnabled;
    }

    @XmlElementWrapper(name = "auth-filters")
    @XmlElement(name = "auth-filter")
    public List<AuthenticationFilter> getAuthenticationFilters() {
        if (authenticationFilters == null) {
            authenticationFilters = new ArrayList<AuthenticationFilter>();
        }

        return authenticationFilters;
    }

    @XmlElementWrapper(name = "client-auth-filters")
    @XmlElement(name = "client-auth-filter")
    public List<ClientAuthenticationFilter> getClientAuthenticationFilters() {
        if (clientAuthenticationFilters == null) {
            clientAuthenticationFilters = new ArrayList<ClientAuthenticationFilter>();
        }

        return clientAuthenticationFilters;
    }

    @XmlElement(name = "appliance-inum")
    public String getApplianceInum() {
        return applianceInum;
    }

    public void setApplianceInum(String applianceInum) {
        this.applianceInum = applianceInum;
    }

    @XmlElement(name = "session-id-unused-lifetime")
    public int getSessionIdUnusedLifetime() {
        return sessionIdUnusedLifetime;
    }

    public void setSessionIdUnusedLifetime(int p_sessionIdUnusedLifetime) {
        sessionIdUnusedLifetime = p_sessionIdUnusedLifetime;
    }

    @XmlElement(name = "session-id-unauthenticated-unused-lifetime")
    public int getSessionIdUnauthenticatedUnusedLifetime() {
        return sessionIdUnauthenticatedUnusedLifetime;
    }

    public void setSessionIdUnauthenticatedUnusedLifetime(int sessionIdUnauthenticatedUnusedLifetime) {
        this.sessionIdUnauthenticatedUnusedLifetime = sessionIdUnauthenticatedUnusedLifetime;
    }

    @XmlElement(name = "session-id-persist-on-prompt-none")
    public Boolean getSessionIdPersistOnPromptNone() {
        return sessionIdPersistOnPromptNone;
    }

    public void setSessionIdPersistOnPromptNone(Boolean sessionIdPersistOnPromptNone) {
        this.sessionIdPersistOnPromptNone = sessionIdPersistOnPromptNone;
    }

    @XmlElement(name = "session-id-enabled")
    public Boolean getSessionIdEnabled() {
        return sessionIdEnabled;
    }

    public void setSessionIdEnabled(Boolean p_sessionIdEnabled) {
        sessionIdEnabled = p_sessionIdEnabled;
    }

    @XmlElement(name = "configuration-update-interval")
    public int getConfigurationUpdateInterval() {
        return configurationUpdateInterval;
    }

    public void setConfigurationUpdateInterval(int p_configurationUpdateInterval) {
        configurationUpdateInterval = p_configurationUpdateInterval;
    }

    @XmlElement(name = "jsLocation")
    public String getJsLocation() {
        return jsLocation;
    }

    public void setJsLocation(String jsLocation) {
        this.jsLocation = jsLocation;
    }

    @XmlElement(name = "cssLocation")
    public String getCssLocation() {
        return cssLocation;
    }

    public void setCssLocation(String cssLocation) {
        this.cssLocation = cssLocation;
    }

    @XmlElement(name = "imgLocation")
    public String getImgLocation() {
        return imgLocation;
    }

    public void setImgLocation(String imgLocation) {
        this.imgLocation = imgLocation;
    }

    @XmlElement(name = "metric-reporter-interval")
    public int getMetricReporterInterval() {
        return metricReporterInterval;
    }

    public void setMetricReporterInterval(int metricReporterInterval) {
        this.metricReporterInterval = metricReporterInterval;
    }

    @XmlElement(name = "metric-reporter-keep-data-days")
    public int getMetricReporterKeepDataDays() {
        return metricReporterKeepDataDays;
    }

    public void setMetricReporterKeepDataDays(int metricReporterKeepDataDays) {
        this.metricReporterKeepDataDays = metricReporterKeepDataDays;
    }

}