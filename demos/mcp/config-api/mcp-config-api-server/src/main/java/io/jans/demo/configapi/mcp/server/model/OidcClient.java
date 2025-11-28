package io.jans.demo.configapi.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OidcClient {

    // Core Identity Fields
    private String dn;
    private String baseDn;
    private String inum;

    @JsonProperty("clientName")
    private String clientName;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("clientSecret")
    private String clientSecret;

    private String description;

    // URI Fields
    @JsonProperty("redirectUris")
    private List<String> redirectUris;

    @JsonProperty("postLogoutRedirectUris")
    private List<String> postLogoutRedirectUris;

    @JsonProperty("logoUri")
    private String logoUri;

    @JsonProperty("logoUriLocalized")
    private Map<String, String> logoUriLocalized;

    @JsonProperty("clientUri")
    private String clientUri;

    @JsonProperty("clientUriLocalized")
    private Map<String, String> clientUriLocalized;

    @JsonProperty("policyUri")
    private String policyUri;

    @JsonProperty("policyUriLocalized")
    private Map<String, String> policyUriLocalized;

    @JsonProperty("tosUri")
    private String tosUri;

    @JsonProperty("tosUriLocalized")
    private Map<String, String> tosUriLocalized;

    @JsonProperty("frontChannelLogoutUri")
    private String frontChannelLogoutUri;

    @JsonProperty("jwksUri")
    private String jwksUri;

    @JsonProperty("sectorIdentifierUri")
    private String sectorIdentifierUri;

    @JsonProperty("initiateLoginUri")
    private String initiateLoginUri;

    // OAuth/OIDC Configuration
    @JsonProperty("grantTypes")
    private List<String> grantTypes;

    @JsonProperty("responseTypes")
    private List<String> responseTypes;

    @JsonProperty("scopes")
    private List<String> scopes;

    @JsonProperty("applicationType")
    private String applicationType;

    @JsonProperty("subjectType")
    private String subjectType;

    @JsonProperty("requestUris")
    private List<String> requestUris;

    @JsonProperty("defaultAcrValues")
    private List<String> defaultAcrValues;

    @JsonProperty("contacts")
    private List<String> contacts;

    // Token & Signing Configuration
    @JsonProperty("tokenEndpointAuthMethod")
    private String tokenEndpointAuthMethod;

    @JsonProperty("allAuthenticationMethods")
    private List<String> allAuthenticationMethods;

    @JsonProperty("authenticationMethod")
    private String authenticationMethod;

    @JsonProperty("idTokenSignedResponseAlg")
    private String idTokenSignedResponseAlg;

    @JsonProperty("idTokenEncryptedResponseAlg")
    private String idTokenEncryptedResponseAlg;

    @JsonProperty("idTokenEncryptedResponseEnc")
    private String idTokenEncryptedResponseEnc;

    @JsonProperty("userInfoSignedResponseAlg")
    private String userInfoSignedResponseAlg;

    @JsonProperty("userInfoEncryptedResponseAlg")
    private String userInfoEncryptedResponseAlg;

    @JsonProperty("userInfoEncryptedResponseEnc")
    private String userInfoEncryptedResponseEnc;

    @JsonProperty("accessTokenSigningAlg")
    private String accessTokenSigningAlg;

    @JsonProperty("accessTokenAsJwt")
    private Boolean accessTokenAsJwt;

    @JsonProperty("rptAsJwt")
    private Boolean rptAsJwt;

    @JsonProperty("requestObjectSigningAlg")
    private String requestObjectSigningAlg;

    @JsonProperty("requestObjectEncryptionAlg")
    private String requestObjectEncryptionAlg;

    @JsonProperty("requestObjectEncryptionEnc")
    private String requestObjectEncryptionEnc;

    // Client Behavior
    @JsonProperty("trustedClient")
    private Boolean trustedClient;

    @JsonProperty("persistClientAuthorizations")
    private Boolean persistClientAuthorizations;

    @JsonProperty("includeClaimsInIdToken")
    private Boolean includeClaimsInIdToken;

    @JsonProperty("disabled")
    private Boolean disabled;

    @JsonProperty("deletable")
    private Boolean deletable;

    @JsonProperty("frontChannelLogoutSessionRequired")
    private Boolean frontChannelLogoutSessionRequired;

    @JsonProperty("backchannelLogoutSessionRequired")
    private Boolean backchannelLogoutSessionRequired;

    @JsonProperty("tokenBindingSupported")
    private Boolean tokenBindingSupported;

    // Lifetime & Expiration
    @JsonProperty("accessTokenLifetime")
    private Integer accessTokenLifetime;

    @JsonProperty("defaultMaxAge")
    private Integer defaultMaxAge;

    @JsonProperty("requireAuthTime")
    private Boolean requireAuthTime;

    // JWKS
    @JsonProperty("jwks")
    private String jwks;

    // Backchannel
    @JsonProperty("backchannelTokenDeliveryMode")
    private String backchannelTokenDeliveryMode;

    @JsonProperty("backchannelClientNotificationEndpoint")
    private String backchannelClientNotificationEndpoint;

    @JsonProperty("backchannelAuthenticationRequestSigningAlg")
    private String backchannelAuthenticationRequestSigningAlg;

    @JsonProperty("backchannelUserCodeParameter")
    private Boolean backchannelUserCodeParameter;

    @JsonProperty("backchannelLogoutUri")
    private List<String> backchannelLogoutUri;

    // Authorization Details
    @JsonProperty("authorizationDetailsTypes")
    private List<String> authorizationDetailsTypes;

    // Custom & Advanced Fields
    @JsonProperty("customAttributes")
    private List<Map<String, Object>> customAttributes;

    @JsonProperty("customObjectClasses")
    private List<String> customObjectClasses;

    @JsonProperty("attributes")
    private Map<String, Object> attributes;

    // Software Statement
    @JsonProperty("softwareId")
    private String softwareId;

    @JsonProperty("softwareVersion")
    private String softwareVersion;

    @JsonProperty("softwareStatement")
    private String softwareStatement;

    // Organization
    @JsonProperty("organization")
    private String organization;

    // Expiration
    @JsonProperty("expirationDate")
    private String expirationDate;

    @JsonProperty("clientIdIssuedAt")
    private Long clientIdIssuedAt;

    @JsonProperty("clientSecretExpiresAt")
    private Long clientSecretExpiresAt;

    // Registration
    @JsonProperty("registrationAccessToken")
    private String registrationAccessToken;

    @JsonProperty("clientRegistrationUri")
    private String clientRegistrationUri;

    // MTLS
    @JsonProperty("tlsClientAuthSubjectDn")
    private String tlsClientAuthSubjectDn;

    @JsonProperty("tlsClientCertificateBoundAccessTokens")
    private Boolean tlsClientCertificateBoundAccessTokens;

    // PAR (Pushed Authorization Request)
    @JsonProperty("requirePushedAuthorizationRequests")
    private Boolean requirePushedAuthorizationRequests;

    // Additional fields
    @JsonProperty("selected")
    private Boolean selected;

    @JsonProperty("clientNameLocalized")
    private Map<String, String> clientNameLocalized;

    // Getters and Setters
    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public List<String> getPostLogoutRedirectUris() {
        return postLogoutRedirectUris;
    }

    public void setPostLogoutRedirectUris(List<String> postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
    }

    public String getLogoUri() {
        return logoUri;
    }

    public void setLogoUri(String logoUri) {
        this.logoUri = logoUri;
    }

    public Map<String, String> getLogoUriLocalized() {
        return logoUriLocalized;
    }

    public void setLogoUriLocalized(Map<String, String> logoUriLocalized) {
        this.logoUriLocalized = logoUriLocalized;
    }

    public String getClientUri() {
        return clientUri;
    }

    public void setClientUri(String clientUri) {
        this.clientUri = clientUri;
    }

    public Map<String, String> getClientUriLocalized() {
        return clientUriLocalized;
    }

    public void setClientUriLocalized(Map<String, String> clientUriLocalized) {
        this.clientUriLocalized = clientUriLocalized;
    }

    public String getPolicyUri() {
        return policyUri;
    }

    public void setPolicyUri(String policyUri) {
        this.policyUri = policyUri;
    }

    public Map<String, String> getPolicyUriLocalized() {
        return policyUriLocalized;
    }

    public void setPolicyUriLocalized(Map<String, String> policyUriLocalized) {
        this.policyUriLocalized = policyUriLocalized;
    }

    public String getTosUri() {
        return tosUri;
    }

    public void setTosUri(String tosUri) {
        this.tosUri = tosUri;
    }

    public Map<String, String> getTosUriLocalized() {
        return tosUriLocalized;
    }

    public void setTosUriLocalized(Map<String, String> tosUriLocalized) {
        this.tosUriLocalized = tosUriLocalized;
    }

    public String getFrontChannelLogoutUri() {
        return frontChannelLogoutUri;
    }

    public void setFrontChannelLogoutUri(String frontChannelLogoutUri) {
        this.frontChannelLogoutUri = frontChannelLogoutUri;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getSectorIdentifierUri() {
        return sectorIdentifierUri;
    }

    public void setSectorIdentifierUri(String sectorIdentifierUri) {
        this.sectorIdentifierUri = sectorIdentifierUri;
    }

    public String getInitiateLoginUri() {
        return initiateLoginUri;
    }

    public void setInitiateLoginUri(String initiateLoginUri) {
        this.initiateLoginUri = initiateLoginUri;
    }

    public List<String> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(List<String> grantTypes) {
        this.grantTypes = grantTypes;
    }

    public List<String> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(List<String> responseTypes) {
        this.responseTypes = responseTypes;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public List<String> getRequestUris() {
        return requestUris;
    }

    public void setRequestUris(List<String> requestUris) {
        this.requestUris = requestUris;
    }

    public List<String> getDefaultAcrValues() {
        return defaultAcrValues;
    }

    public void setDefaultAcrValues(List<String> defaultAcrValues) {
        this.defaultAcrValues = defaultAcrValues;
    }

    public List<String> getContacts() {
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    public List<String> getAllAuthenticationMethods() {
        return allAuthenticationMethods;
    }

    public void setAllAuthenticationMethods(List<String> allAuthenticationMethods) {
        this.allAuthenticationMethods = allAuthenticationMethods;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    public String getIdTokenSignedResponseAlg() {
        return idTokenSignedResponseAlg;
    }

    public void setIdTokenSignedResponseAlg(String idTokenSignedResponseAlg) {
        this.idTokenSignedResponseAlg = idTokenSignedResponseAlg;
    }

    public String getIdTokenEncryptedResponseAlg() {
        return idTokenEncryptedResponseAlg;
    }

    public void setIdTokenEncryptedResponseAlg(String idTokenEncryptedResponseAlg) {
        this.idTokenEncryptedResponseAlg = idTokenEncryptedResponseAlg;
    }

    public String getIdTokenEncryptedResponseEnc() {
        return idTokenEncryptedResponseEnc;
    }

    public void setIdTokenEncryptedResponseEnc(String idTokenEncryptedResponseEnc) {
        this.idTokenEncryptedResponseEnc = idTokenEncryptedResponseEnc;
    }

    public String getUserInfoSignedResponseAlg() {
        return userInfoSignedResponseAlg;
    }

    public void setUserInfoSignedResponseAlg(String userInfoSignedResponseAlg) {
        this.userInfoSignedResponseAlg = userInfoSignedResponseAlg;
    }

    public String getUserInfoEncryptedResponseAlg() {
        return userInfoEncryptedResponseAlg;
    }

    public void setUserInfoEncryptedResponseAlg(String userInfoEncryptedResponseAlg) {
        this.userInfoEncryptedResponseAlg = userInfoEncryptedResponseAlg;
    }

    public String getUserInfoEncryptedResponseEnc() {
        return userInfoEncryptedResponseEnc;
    }

    public void setUserInfoEncryptedResponseEnc(String userInfoEncryptedResponseEnc) {
        this.userInfoEncryptedResponseEnc = userInfoEncryptedResponseEnc;
    }

    public String getAccessTokenSigningAlg() {
        return accessTokenSigningAlg;
    }

    public void setAccessTokenSigningAlg(String accessTokenSigningAlg) {
        this.accessTokenSigningAlg = accessTokenSigningAlg;
    }

    public Boolean getAccessTokenAsJwt() {
        return accessTokenAsJwt;
    }

    public void setAccessTokenAsJwt(Boolean accessTokenAsJwt) {
        this.accessTokenAsJwt = accessTokenAsJwt;
    }

    public Boolean getRptAsJwt() {
        return rptAsJwt;
    }

    public void setRptAsJwt(Boolean rptAsJwt) {
        this.rptAsJwt = rptAsJwt;
    }

    public String getRequestObjectSigningAlg() {
        return requestObjectSigningAlg;
    }

    public void setRequestObjectSigningAlg(String requestObjectSigningAlg) {
        this.requestObjectSigningAlg = requestObjectSigningAlg;
    }

    public String getRequestObjectEncryptionAlg() {
        return requestObjectEncryptionAlg;
    }

    public void setRequestObjectEncryptionAlg(String requestObjectEncryptionAlg) {
        this.requestObjectEncryptionAlg = requestObjectEncryptionAlg;
    }

    public String getRequestObjectEncryptionEnc() {
        return requestObjectEncryptionEnc;
    }

    public void setRequestObjectEncryptionEnc(String requestObjectEncryptionEnc) {
        this.requestObjectEncryptionEnc = requestObjectEncryptionEnc;
    }

    public Boolean getTrustedClient() {
        return trustedClient;
    }

    public void setTrustedClient(Boolean trustedClient) {
        this.trustedClient = trustedClient;
    }

    public Boolean getPersistClientAuthorizations() {
        return persistClientAuthorizations;
    }

    public void setPersistClientAuthorizations(Boolean persistClientAuthorizations) {
        this.persistClientAuthorizations = persistClientAuthorizations;
    }

    public Boolean getIncludeClaimsInIdToken() {
        return includeClaimsInIdToken;
    }

    public void setIncludeClaimsInIdToken(Boolean includeClaimsInIdToken) {
        this.includeClaimsInIdToken = includeClaimsInIdToken;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getDeletable() {
        return deletable;
    }

    public void setDeletable(Boolean deletable) {
        this.deletable = deletable;
    }

    public Boolean getFrontChannelLogoutSessionRequired() {
        return frontChannelLogoutSessionRequired;
    }

    public void setFrontChannelLogoutSessionRequired(Boolean frontChannelLogoutSessionRequired) {
        this.frontChannelLogoutSessionRequired = frontChannelLogoutSessionRequired;
    }

    public Boolean getBackchannelLogoutSessionRequired() {
        return backchannelLogoutSessionRequired;
    }

    public void setBackchannelLogoutSessionRequired(Boolean backchannelLogoutSessionRequired) {
        this.backchannelLogoutSessionRequired = backchannelLogoutSessionRequired;
    }

    public Boolean getTokenBindingSupported() {
        return tokenBindingSupported;
    }

    public void setTokenBindingSupported(Boolean tokenBindingSupported) {
        this.tokenBindingSupported = tokenBindingSupported;
    }

    public Integer getAccessTokenLifetime() {
        return accessTokenLifetime;
    }

    public void setAccessTokenLifetime(Integer accessTokenLifetime) {
        this.accessTokenLifetime = accessTokenLifetime;
    }

    public Integer getDefaultMaxAge() {
        return defaultMaxAge;
    }

    public void setDefaultMaxAge(Integer defaultMaxAge) {
        this.defaultMaxAge = defaultMaxAge;
    }

    public Boolean getRequireAuthTime() {
        return requireAuthTime;
    }

    public void setRequireAuthTime(Boolean requireAuthTime) {
        this.requireAuthTime = requireAuthTime;
    }

    public String getJwks() {
        return jwks;
    }

    public void setJwks(String jwks) {
        this.jwks = jwks;
    }

    public String getBackchannelTokenDeliveryMode() {
        return backchannelTokenDeliveryMode;
    }

    public void setBackchannelTokenDeliveryMode(String backchannelTokenDeliveryMode) {
        this.backchannelTokenDeliveryMode = backchannelTokenDeliveryMode;
    }

    public String getBackchannelClientNotificationEndpoint() {
        return backchannelClientNotificationEndpoint;
    }

    public void setBackchannelClientNotificationEndpoint(String backchannelClientNotificationEndpoint) {
        this.backchannelClientNotificationEndpoint = backchannelClientNotificationEndpoint;
    }

    public String getBackchannelAuthenticationRequestSigningAlg() {
        return backchannelAuthenticationRequestSigningAlg;
    }

    public void setBackchannelAuthenticationRequestSigningAlg(String backchannelAuthenticationRequestSigningAlg) {
        this.backchannelAuthenticationRequestSigningAlg = backchannelAuthenticationRequestSigningAlg;
    }

    public Boolean getBackchannelUserCodeParameter() {
        return backchannelUserCodeParameter;
    }

    public void setBackchannelUserCodeParameter(Boolean backchannelUserCodeParameter) {
        this.backchannelUserCodeParameter = backchannelUserCodeParameter;
    }

    public List<String> getBackchannelLogoutUri() {
        return backchannelLogoutUri;
    }

    public void setBackchannelLogoutUri(List<String> backchannelLogoutUri) {
        this.backchannelLogoutUri = backchannelLogoutUri;
    }

    public List<String> getAuthorizationDetailsTypes() {
        return authorizationDetailsTypes;
    }

    public void setAuthorizationDetailsTypes(List<String> authorizationDetailsTypes) {
        this.authorizationDetailsTypes = authorizationDetailsTypes;
    }

    public List<Map<String, Object>> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(List<Map<String, Object>> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public List<String> getCustomObjectClasses() {
        return customObjectClasses;
    }

    public void setCustomObjectClasses(List<String> customObjectClasses) {
        this.customObjectClasses = customObjectClasses;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
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

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Long getClientIdIssuedAt() {
        return clientIdIssuedAt;
    }

    public void setClientIdIssuedAt(Long clientIdIssuedAt) {
        this.clientIdIssuedAt = clientIdIssuedAt;
    }

    public Long getClientSecretExpiresAt() {
        return clientSecretExpiresAt;
    }

    public void setClientSecretExpiresAt(Long clientSecretExpiresAt) {
        this.clientSecretExpiresAt = clientSecretExpiresAt;
    }

    public String getRegistrationAccessToken() {
        return registrationAccessToken;
    }

    public void setRegistrationAccessToken(String registrationAccessToken) {
        this.registrationAccessToken = registrationAccessToken;
    }

    public String getClientRegistrationUri() {
        return clientRegistrationUri;
    }

    public void setClientRegistrationUri(String clientRegistrationUri) {
        this.clientRegistrationUri = clientRegistrationUri;
    }

    public String getTlsClientAuthSubjectDn() {
        return tlsClientAuthSubjectDn;
    }

    public void setTlsClientAuthSubjectDn(String tlsClientAuthSubjectDn) {
        this.tlsClientAuthSubjectDn = tlsClientAuthSubjectDn;
    }

    public Boolean getTlsClientCertificateBoundAccessTokens() {
        return tlsClientCertificateBoundAccessTokens;
    }

    public void setTlsClientCertificateBoundAccessTokens(Boolean tlsClientCertificateBoundAccessTokens) {
        this.tlsClientCertificateBoundAccessTokens = tlsClientCertificateBoundAccessTokens;
    }

    public Boolean getRequirePushedAuthorizationRequests() {
        return requirePushedAuthorizationRequests;
    }

    public void setRequirePushedAuthorizationRequests(Boolean requirePushedAuthorizationRequests) {
        this.requirePushedAuthorizationRequests = requirePushedAuthorizationRequests;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public Map<String, String> getClientNameLocalized() {
        return clientNameLocalized;
    }

    public void setClientNameLocalized(Map<String, String> clientNameLocalized) {
        this.clientNameLocalized = clientNameLocalized;
    }
}
