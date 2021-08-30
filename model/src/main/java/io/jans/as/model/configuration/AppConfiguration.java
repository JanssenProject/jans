/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;
import io.jans.as.model.common.ComponentType;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SoftwareStatementValidationType;
import io.jans.as.model.common.WebKeyStorage;
import io.jans.as.model.error.ErrorHandlingMethod;
import io.jans.as.model.jwk.KeySelectionStrategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the configuration JSON file.
 *
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version July 28, 2021
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfiguration implements Configuration {

    public static final int DEFAULT_SESSION_ID_LIFETIME = 86400;
    public static final KeySelectionStrategy DEFAULT_KEY_SELECTION_STRATEGY = KeySelectionStrategy.OLDER;
    public static final String DEFAULT_STAT_SCOPE = "jans_stat";

    private String issuer;
    private String baseEndpoint;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String tokenRevocationEndpoint;
    private String userInfoEndpoint;
    private String clientInfoEndpoint;
    private String checkSessionIFrame;
    private String endSessionEndpoint;
    private String jwksUri;
    private String registrationEndpoint;
    private String openIdDiscoveryEndpoint;
    private String openIdConfigurationEndpoint;
    private String idGenerationEndpoint;
    private String introspectionEndpoint;
    private String parEndpoint;
    private Boolean requirePar = false;
    private String deviceAuthzEndpoint;

    private String mtlsAuthorizationEndpoint;
    private String mtlsTokenEndpoint;
    private String mtlsTokenRevocationEndpoint;
    private String mtlsUserInfoEndpoint;
    private String mtlsClientInfoEndpoint;
    private String mtlsCheckSessionIFrame;
    private String mtlsEndSessionEndpoint;
    private String mtlsJwksUri;
    private String mtlsRegistrationEndpoint;
    private String mtlsIdGenerationEndpoint;
    private String mtlsIntrospectionEndpoint;
    private String mtlsParEndpoint;
    private String mtlsDeviceAuthzEndpoint;

    private Boolean sessionAsJwt = false;
    private Boolean requireRequestObjectEncryption = false;

    private int sectorIdentifierCacheLifetimeInMinutes = 1440;

    private String umaConfigurationEndpoint;
    private Boolean umaRptAsJwt = false;
    private int umaRptLifetime;
    private int umaTicketLifetime;
    private int umaPctLifetime;
    private int umaResourceLifetime;
    private Boolean umaAddScopesAutomatically;
    private Boolean umaValidateClaimToken = false;
    private Boolean umaGrantAccessIfNoPolicies = false;
    private Boolean umaRestrictResourceToAssociatedClient = false;

    private int statTimerIntervalInSeconds;
    private int statWebServiceIntervalLimitInSeconds;
    private String statAuthorizationScope;

    private int spontaneousScopeLifetime;
    private String openidSubAttribute;
    private Set<Set<ResponseType>> responseTypesSupported;
    private Set<ResponseMode> responseModesSupported;
    private Set<GrantType> grantTypesSupported;
    private List<String> subjectTypesSupported;
    private String defaultSubjectType;
    private List<String> authorizationSigningAlgValuesSupported;
    private List<String> authorizationEncryptionAlgValuesSupported;
    private List<String> authorizationEncryptionEncValuesSupported;
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
    private List<String> jwksAlgorithmsSupported;
    private String serviceDocumentation;
    private List<String> claimsLocalesSupported;
    private List<String> idTokenTokenBindingCnfValuesSupported;
    private List<String> uiLocalesSupported;
    private Boolean claimsParameterSupported;
    private Boolean requestParameterSupported;
    private Boolean requestUriParameterSupported;
    private Boolean requestUriHashVerificationEnabled;
    private Boolean requireRequestUriRegistration;
    private String opPolicyUri;
    private String opTosUri;
    private int authorizationCodeLifetime;
    private int refreshTokenLifetime;
    private int idTokenLifetime;
    private Boolean idTokenFilterClaimsBasedOnAccessToken;
    private int accessTokenLifetime;

    private int cleanServiceInterval;
    private int cleanServiceBatchChunkSize = 100;

    private Boolean keyRegenerationEnabled;
    private int keyRegenerationInterval;
    private String defaultSignatureAlgorithm;
    private String oxOpenIdConnectVersion;
    private String oxId;
    private int dynamicRegistrationExpirationTime = -1;
    private Boolean dynamicRegistrationPersistClientAuthorizations;
    private Boolean trustedClientEnabled;
    private Boolean skipAuthorizationForOpenIdScopeAndPairwiseId = false;
    private Boolean dynamicRegistrationScopesParamEnabled;
    private Boolean dynamicRegistrationPasswordGrantTypeEnabled = false;
    private List<String> dynamicRegistrationAllowedPasswordGrantScopes;
    private String dynamicRegistrationCustomObjectClass;
    private List<String> personCustomObjectClassList;

    private Boolean persistIdTokenInLdap = false;
    private Boolean persistRefreshTokenInLdap = true;
    private Boolean allowPostLogoutRedirectWithoutValidation = false;
    private Boolean invalidateSessionCookiesAfterAuthorizationFlow = false;
    private Boolean returnClientSecretOnRead = false;
    private Boolean rejectJwtWithNoneAlg = true;
    private Boolean expirationNotificatorEnabled = false;
    private Boolean useNestedJwtDuringEncryption = true;
    private int expirationNotificatorMapSizeLimit = 100000;
    private int expirationNotificatorIntervalInSeconds = 600;

    private Boolean authenticationFiltersEnabled;
    private Boolean clientAuthenticationFiltersEnabled;
    private Boolean clientRegDefaultToCodeFlowWithRefresh;
    private List<AuthenticationFilter> authenticationFilters;
    private List<ClientAuthenticationFilter> clientAuthenticationFilters;
    private List<CorsConfigurationFilter> corsConfigurationFilters;

    private int sessionIdUnusedLifetime;
    private int sessionIdUnauthenticatedUnusedLifetime = 120; // 120 seconds
    private Boolean sessionIdEnabled;
    private Boolean sessionIdPersistOnPromptNone;
    private Boolean sessionIdRequestParameterEnabled = false; // #1195
    private Boolean changeSessionIdOnAuthentication = true;
    private Boolean sessionIdPersistInCache = false;
    private Boolean includeSidInResponse = false;
    /**
     * SessionId will be expired after sessionIdLifetime seconds
     */
    private Integer sessionIdLifetime = DEFAULT_SESSION_ID_LIFETIME;
    private Integer serverSessionIdLifetime = sessionIdLifetime; // by default same as sessionIdLifetime
    private int configurationUpdateInterval;

    private Boolean enableClientGrantTypeUpdate;
    private Set<GrantType> dynamicGrantTypeDefault;

    private String cssLocation;
    private String jsLocation;
    private String imgLocation;
    private int metricReporterInterval;
    private int metricReporterKeepDataDays;
    private String pairwiseIdType; // persistent, algorithmic
    private String pairwiseCalculationKey;
    private String pairwiseCalculationSalt;
    private Boolean shareSubjectIdBetweenClientsWithSameSectorId = false;

    private WebKeyStorage webKeysStorage;
    private String dnName;
    // Jans Auth KeyStore
    private String keyStoreFile;
    private String keyStoreSecret;
    private KeySelectionStrategy keySelectionStrategy = DEFAULT_KEY_SELECTION_STRATEGY;
    private List<String> keyAlgsAllowedForGeneration = new ArrayList<>();
    private Boolean keySignWithSameKeyButDiffAlg; // https://github.com/JanssenProject/jans-auth-server/issues/95
    private String staticKid;
    private String staticDecryptionKid;

    //oxEleven
    private String oxElevenTestModeToken;
    private String oxElevenGenerateKeyEndpoint;
    private String oxElevenSignEndpoint;
    private String oxElevenVerifySignatureEndpoint;
    private String oxElevenDeleteKeyEndpoint;

    private Boolean introspectionAccessTokenMustHaveUmaProtectionScope = false;
    private Boolean introspectionSkipAuthorization;

    private Boolean endSessionWithAccessToken;
    private String cookieDomain;
    private Boolean enabledOAuthAuditLogging;
    private Set<String> jmsBrokerURISet;
    private String jmsUserName;
    private String jmsPassword;
    private List<String> clientWhiteList;
    private List<String> clientBlackList;
    private Boolean legacyIdTokenClaims;
    private Boolean customHeadersWithAuthorizationResponse;
    private Boolean frontChannelLogoutSessionSupported;
    private String loggingLevel;
    private String loggingLayout;
    private Boolean updateUserLastLogonTime;
    private Boolean updateClientAccessTime;
    private Boolean logClientIdOnClientAuthentication;
    private Boolean logClientNameOnClientAuthentication;
    private Boolean disableJdkLogger = true;
    private Set<String> authorizationRequestCustomAllowedParameters;
    private Boolean openidScopeBackwardCompatibility = false;
    private Boolean disableU2fEndpoint = false;

    private Boolean dcrSignatureValidationEnabled = false;
    private String dcrSignatureValidationSharedSecret;
    private String dcrSignatureValidationSoftwareStatementJwksURIClaim;
    private String dcrSignatureValidationSoftwareStatementJwksClaim;
    private String dcrSignatureValidationJwks;
    private String dcrSignatureValidationJwksUri;
    private Boolean dcrAuthorizationWithClientCredentials = false;
    private Boolean dcrSkipSignatureValidation = false;

    private Boolean useLocalCache = false;
    private Boolean fapiCompatibility = false;
    private Boolean forceIdTokenHintPrecense = false;
    private Boolean forceOfflineAccessScopeToEnableRefreshToken = true;
    private Boolean errorReasonEnabled = false;
    private Boolean removeRefreshTokensForClientOnLogout = true;
    private Boolean skipRefreshTokenDuringRefreshing = false;
    private Boolean refreshTokenExtendLifetimeOnRotation = false;
    private Boolean consentGatheringScriptBackwardCompatibility = false; // means ignore client configuration (as defined in 4.2) and determine it globally (as in 4.1 and earlier)
    private Boolean introspectionScriptBackwardCompatibility = false; // means ignore client configuration (as defined in 4.2) and determine it globally (as in 4.1 and earlier)
    private Boolean introspectionResponseScopesBackwardCompatibility = false;

    private String softwareStatementValidationType = SoftwareStatementValidationType.DEFAULT.getValue();
    private String softwareStatementValidationClaimName;

    private AuthenticationProtectionConfiguration authenticationProtectionConfiguration;

    private ErrorHandlingMethod errorHandlingMethod = ErrorHandlingMethod.INTERNAL;

    private Boolean keepAuthenticatorAttributesOnAcrChange = false;
    private int deviceAuthzRequestExpiresIn;
    private int deviceAuthzTokenPollInterval;
    private String deviceAuthzResponseTypeToProcessAuthz;

    // CIBA
    private String backchannelClientId;
    private String backchannelRedirectUri;
    private String backchannelAuthenticationEndpoint;
    private String backchannelDeviceRegistrationEndpoint;
    private List<String> backchannelTokenDeliveryModesSupported;
    private List<String> backchannelAuthenticationRequestSigningAlgValuesSupported;
    private Boolean backchannelUserCodeParameterSupported;
    private String backchannelBindingMessagePattern;
    private int backchannelAuthenticationResponseExpiresIn;
    private int backchannelAuthenticationResponseInterval;
    private List<String> backchannelLoginHintClaims;
    private CIBAEndUserNotificationConfig cibaEndUserNotificationConfig;
    private int backchannelRequestsProcessorJobIntervalSec;
    private int backchannelRequestsProcessorJobChunkSize;
    private int cibaGrantLifeExtraTimeSec;
    private int cibaMaxExpirationTimeAllowedSec;

    private Boolean allowIdTokenWithoutImplicitGrantType;

    private int discoveryCacheLifetimeInMinutes = 60;
    private List<String> discoveryAllowedKeys;

    private List<String> enabledComponents;

    private Boolean httpLoggingEnabled; // Used in ServletLoggingFilter to enable http request/response logging.
    private Set<String> httpLoggingExludePaths; // Used in ServletLoggingFilter to exclude some paths from logger. Paths example: ["/jans-auth/img", "/jans-auth/stylesheet"]
    private String externalLoggerConfiguration; // Path to external log4j2 configuration file. This property might be configured from oxTrust: /identity/logviewer/configure

    public Boolean getRequireRequestObjectEncryption() {
        if (requireRequestObjectEncryption == null) requireRequestObjectEncryption = false;
        return requireRequestObjectEncryption;
    }

    public void setRequireRequestObjectEncryption(Boolean requireRequestObjectEncryption) {
        this.requireRequestObjectEncryption = requireRequestObjectEncryption;
    }

    public Boolean getAllowIdTokenWithoutImplicitGrantType() {
        if (allowIdTokenWithoutImplicitGrantType == null) allowIdTokenWithoutImplicitGrantType = false;
        return allowIdTokenWithoutImplicitGrantType;
    }

    public void setAllowIdTokenWithoutImplicitGrantType(Boolean allowIdTokenWithoutImplicitGrantType) {
        this.allowIdTokenWithoutImplicitGrantType = allowIdTokenWithoutImplicitGrantType;
    }

    public List<String> getDiscoveryAllowedKeys() {
        if (discoveryAllowedKeys == null) discoveryAllowedKeys = new ArrayList<>();
        return discoveryAllowedKeys;
    }

    public void setDiscoveryAllowedKeys(List<String> discoveryAllowedKeys) {
        this.discoveryAllowedKeys = discoveryAllowedKeys;
    }

    public Set<ComponentType> getEnabledComponentTypes() {
        return ComponentType.fromValues(getEnabledComponents());
    }

    public boolean isEnabledComponent(ComponentType componentType) {
        final Set<ComponentType> enabledComponentTypes = getEnabledComponentTypes();
        if (enabledComponentTypes.isEmpty())
            return true;

        return enabledComponentTypes.contains(componentType);
    }

    public List<String> getEnabledComponents() {
        if (enabledComponents == null) enabledComponents = new ArrayList<>();
        return enabledComponents;
    }

    public void setEnabledComponents(List<String> enabledComponents) {
        this.enabledComponents = enabledComponents;
    }

    public Boolean getUseNestedJwtDuringEncryption() {
        if (useNestedJwtDuringEncryption == null) useNestedJwtDuringEncryption = true;
        return useNestedJwtDuringEncryption;
    }

    public void setUseNestedJwtDuringEncryption(Boolean useNestedJwtDuringEncryption) {
        this.useNestedJwtDuringEncryption = useNestedJwtDuringEncryption;
    }

    public KeySelectionStrategy getKeySelectionStrategy() {
        if (keySelectionStrategy == null) keySelectionStrategy = DEFAULT_KEY_SELECTION_STRATEGY;
        return keySelectionStrategy;
    }

    public void setKeySelectionStrategy(KeySelectionStrategy keySelectionStrategy) {
        this.keySelectionStrategy = keySelectionStrategy;
    }

    public Boolean getKeySignWithSameKeyButDiffAlg() {
        if (keySignWithSameKeyButDiffAlg == null) keySignWithSameKeyButDiffAlg = false;
        return keySignWithSameKeyButDiffAlg;
    }

    public void setKeySignWithSameKeyButDiffAlg(Boolean keySignWithSameKeyButDiffAlg) {
        this.keySignWithSameKeyButDiffAlg = keySignWithSameKeyButDiffAlg;
    }

    public String getStaticKid() {
        return staticKid;
    }

    public void setStaticKid(String staticKid) {
        this.staticKid = staticKid;
    }

    public String getStaticDecryptionKid() {
        return staticDecryptionKid;
    }

    public void setStaticDecryptionKid(String staticDecryptionKid) {
        this.staticDecryptionKid = staticDecryptionKid;
    }

    public List<String> getKeyAlgsAllowedForGeneration() {
        if (keyAlgsAllowedForGeneration == null) keyAlgsAllowedForGeneration = new ArrayList<>();
        return keyAlgsAllowedForGeneration;
    }

    public void setKeyAlgsAllowedForGeneration(List<String> keyAlgsAllowedForGeneration) {
        this.keyAlgsAllowedForGeneration = keyAlgsAllowedForGeneration;
    }

    public int getDiscoveryCacheLifetimeInMinutes() {
        return discoveryCacheLifetimeInMinutes;
    }

    public void setDiscoveryCacheLifetimeInMinutes(int discoveryCacheLifetimeInMinutes) {
        this.discoveryCacheLifetimeInMinutes = discoveryCacheLifetimeInMinutes;
    }

    public String getSoftwareStatementValidationType() {
        if (softwareStatementValidationType == null)
            return softwareStatementValidationType = SoftwareStatementValidationType.DEFAULT.getValue();
        return softwareStatementValidationType;
    }

    public String getSoftwareStatementValidationClaimName() {
        return softwareStatementValidationClaimName;
    }

    public void setSoftwareStatementValidationType(String softwareStatementValidationType) {
        this.softwareStatementValidationType = softwareStatementValidationType;
    }

    public void setSoftwareStatementValidationClaimName(String softwareStatementValidationClaimName) {
        this.softwareStatementValidationClaimName = softwareStatementValidationClaimName;
    }

    public Boolean getSkipRefreshTokenDuringRefreshing() {
        if (skipRefreshTokenDuringRefreshing == null) skipRefreshTokenDuringRefreshing = false;
        return skipRefreshTokenDuringRefreshing;
    }

    public void setSkipRefreshTokenDuringRefreshing(Boolean skipRefreshTokenDuringRefreshing) {
        this.skipRefreshTokenDuringRefreshing = skipRefreshTokenDuringRefreshing;
    }

    public Boolean getRefreshTokenExtendLifetimeOnRotation() {
        if (refreshTokenExtendLifetimeOnRotation == null) refreshTokenExtendLifetimeOnRotation = false;
        return refreshTokenExtendLifetimeOnRotation;
    }

    public void setRefreshTokenExtendLifetimeOnRotation(Boolean refreshTokenExtendLifetimeOnRotation) {
        this.refreshTokenExtendLifetimeOnRotation = refreshTokenExtendLifetimeOnRotation;
    }

    public int getSectorIdentifierCacheLifetimeInMinutes() {
        return sectorIdentifierCacheLifetimeInMinutes;
    }

    public void setSectorIdentifierCacheLifetimeInMinutes(int sectorIdentifierCacheLifetimeInMinutes) {
        this.sectorIdentifierCacheLifetimeInMinutes = sectorIdentifierCacheLifetimeInMinutes;
    }

    public Boolean getExpirationNotificatorEnabled() {
        if (expirationNotificatorEnabled == null) expirationNotificatorEnabled = false;
        return expirationNotificatorEnabled;
    }

    public void setExpirationNotificatorEnabled(Boolean expirationNotificatorEnabled) {
        this.expirationNotificatorEnabled = expirationNotificatorEnabled;
    }

    public int getExpirationNotificatorMapSizeLimit() {
        if (expirationNotificatorMapSizeLimit == 0) expirationNotificatorMapSizeLimit = 100000;
        return expirationNotificatorMapSizeLimit;
    }

    public void setExpirationNotificatorMapSizeLimit(int expirationNotificatorMapSizeLimit) {
        this.expirationNotificatorMapSizeLimit = expirationNotificatorMapSizeLimit;
    }

    public int getExpirationNotificatorIntervalInSeconds() {
        return expirationNotificatorIntervalInSeconds;
    }

    public void setExpirationNotificatorIntervalInSeconds(int expirationNotificatorIntervalInSeconds) {
        this.expirationNotificatorIntervalInSeconds = expirationNotificatorIntervalInSeconds;
    }

    public Boolean getRejectJwtWithNoneAlg() {
        if (rejectJwtWithNoneAlg == null) rejectJwtWithNoneAlg = true;
        return rejectJwtWithNoneAlg;
    }

    public void setRejectJwtWithNoneAlg(Boolean rejectJwtWithNoneAlg) {
        this.rejectJwtWithNoneAlg = rejectJwtWithNoneAlg;
    }

    public Boolean getIntrospectionScriptBackwardCompatibility() {
        if (introspectionScriptBackwardCompatibility == null) introspectionScriptBackwardCompatibility = false;
        return introspectionScriptBackwardCompatibility;
    }

    public void setIntrospectionScriptBackwardCompatibility(Boolean introspectionScriptBackwardCompatibility) {
        this.introspectionScriptBackwardCompatibility = introspectionScriptBackwardCompatibility;
    }

    public Boolean getIntrospectionResponseScopesBackwardCompatibility() {
        if (introspectionResponseScopesBackwardCompatibility == null)
            introspectionResponseScopesBackwardCompatibility = false;
        return introspectionScriptBackwardCompatibility;
    }

    public void setIntrospectionResponseScopesBackwardCompatibility(Boolean introspectionResponseScopesBackwardCompatibility) {
        this.introspectionResponseScopesBackwardCompatibility = introspectionResponseScopesBackwardCompatibility;
    }

    public Boolean getConsentGatheringScriptBackwardCompatibility() {
        if (consentGatheringScriptBackwardCompatibility == null) consentGatheringScriptBackwardCompatibility = false;
        return consentGatheringScriptBackwardCompatibility;
    }

    public void setConsentGatheringScriptBackwardCompatibility(Boolean consentGatheringScriptBackwardCompatibility) {
        this.consentGatheringScriptBackwardCompatibility = consentGatheringScriptBackwardCompatibility;
    }

    public Boolean getErrorReasonEnabled() {
        if (errorReasonEnabled == null) errorReasonEnabled = false;
        return errorReasonEnabled;
    }

    public void setErrorReasonEnabled(Boolean errorReasonEnabled) {
        this.errorReasonEnabled = errorReasonEnabled;
    }

    public Boolean getForceOfflineAccessScopeToEnableRefreshToken() {
        if (forceOfflineAccessScopeToEnableRefreshToken == null) forceOfflineAccessScopeToEnableRefreshToken = true;
        return forceOfflineAccessScopeToEnableRefreshToken;
    }

    public void setForceOfflineAccessScopeToEnableRefreshToken(Boolean forceOfflineAccessScopeToEnableRefreshToken) {
        this.forceOfflineAccessScopeToEnableRefreshToken = forceOfflineAccessScopeToEnableRefreshToken;
    }

    public Boolean getIncludeSidInResponse() {
        if (includeSidInResponse == null) includeSidInResponse = false;
        return includeSidInResponse;
    }

    public void setIncludeSidInResponse(Boolean includeSidInResponse) {
        this.includeSidInResponse = includeSidInResponse;
    }

    public Boolean getSessionIdPersistInCache() {
        if (sessionIdPersistInCache == null) sessionIdPersistInCache = false;
        return sessionIdPersistInCache;
    }

    public void setSessionIdPersistInCache(Boolean sessionIdPersistInCache) {
        this.sessionIdPersistInCache = sessionIdPersistInCache;
    }

    public Boolean getChangeSessionIdOnAuthentication() {
        if (changeSessionIdOnAuthentication == null) changeSessionIdOnAuthentication = true;
        return changeSessionIdOnAuthentication;
    }

    public void setChangeSessionIdOnAuthentication(Boolean changeSessionIdOnAuthentication) {
        this.changeSessionIdOnAuthentication = changeSessionIdOnAuthentication;
    }

    public Boolean getReturnClientSecretOnRead() {
        if (returnClientSecretOnRead == null) returnClientSecretOnRead = false;
        return returnClientSecretOnRead;
    }

    public void setReturnClientSecretOnRead(Boolean returnClientSecretOnRead) {
        this.returnClientSecretOnRead = returnClientSecretOnRead;
    }

    public Boolean getFapiCompatibility() {
        if (fapiCompatibility == null) fapiCompatibility = false;
        return fapiCompatibility;
    }

    public void setFapiCompatibility(Boolean fapiCompatibility) {
        this.fapiCompatibility = fapiCompatibility;
    }

    public Boolean getDcrSkipSignatureValidation() {
        if (dcrSkipSignatureValidation == null) dcrSkipSignatureValidation = false;
        return dcrSkipSignatureValidation;
    }

    public void setDcrSkipSignatureValidation(Boolean dcrSkipSignatureValidation) {
        this.dcrSkipSignatureValidation = dcrSkipSignatureValidation;
    }

    public Boolean getDcrAuthorizationWithClientCredentials() {
        if (dcrAuthorizationWithClientCredentials == null) dcrAuthorizationWithClientCredentials = false;
        return dcrAuthorizationWithClientCredentials;
    }

    public void setDcrAuthorizationWithClientCredentials(Boolean dcrAuthorizationWithClientCredentials) {
        this.dcrAuthorizationWithClientCredentials = dcrAuthorizationWithClientCredentials;
    }

    public String getDcrSignatureValidationSharedSecret() {
        return dcrSignatureValidationSharedSecret;
    }

    public void setDcrSignatureValidationSharedSecret(String dcrSignatureValidationSharedSecret) {
        this.dcrSignatureValidationSharedSecret = dcrSignatureValidationSharedSecret;
    }

    public Boolean getDcrSignatureValidationEnabled() {
        if (dcrSignatureValidationEnabled == null) dcrSignatureValidationEnabled = false;
        return dcrSignatureValidationEnabled;
    }

    public void setDcrSignatureValidationEnabled(Boolean dcrSignatureValidationEnabled) {
        this.dcrSignatureValidationEnabled = dcrSignatureValidationEnabled;
    }

    public String getDcrSignatureValidationSoftwareStatementJwksURIClaim() {
        return dcrSignatureValidationSoftwareStatementJwksURIClaim;
    }

    public void setDcrSignatureValidationSoftwareStatementJwksURIClaim(String dcrSignatureValidationSoftwareStatementJwksURIClaim) {
        this.dcrSignatureValidationSoftwareStatementJwksURIClaim = dcrSignatureValidationSoftwareStatementJwksURIClaim;
    }

    public String getDcrSignatureValidationSoftwareStatementJwksClaim() {
        return dcrSignatureValidationSoftwareStatementJwksClaim;
    }

    public void setDcrSignatureValidationSoftwareStatementJwksClaim(String dcrSignatureValidationSoftwareStatementJwksClaim) {
        this.dcrSignatureValidationSoftwareStatementJwksClaim = dcrSignatureValidationSoftwareStatementJwksClaim;
    }

    public String getDcrSignatureValidationJwks() {
        return dcrSignatureValidationJwks;
    }

    public void setDcrSignatureValidationJwks(String dcrSignatureValidationJwks) {
        this.dcrSignatureValidationJwks = dcrSignatureValidationJwks;
    }

    public String getDcrSignatureValidationJwksUri() {
        return dcrSignatureValidationJwksUri;
    }

    public void setDcrSignatureValidationJwksUri(String dcrSignatureValidationJwksUri) {
        this.dcrSignatureValidationJwksUri = dcrSignatureValidationJwksUri;
    }

    public Boolean getForceIdTokenHintPrecense() {
        if (forceIdTokenHintPrecense == null) forceIdTokenHintPrecense = false;
        return forceIdTokenHintPrecense;
    }

    public void setForceIdTokenHintPrecense(Boolean forceIdTokenHintPrecense) {
        this.forceIdTokenHintPrecense = forceIdTokenHintPrecense;
    }

    public Boolean getRemoveRefreshTokensForClientOnLogout() {
        if (removeRefreshTokensForClientOnLogout == null) removeRefreshTokensForClientOnLogout = true;
        return removeRefreshTokensForClientOnLogout;
    }

    public void setRemoveRefreshTokensForClientOnLogout(Boolean removeRefreshTokensForClientOnLogout) {
        this.removeRefreshTokensForClientOnLogout = removeRefreshTokensForClientOnLogout;
    }

    public Boolean getDisableJdkLogger() {
        return disableJdkLogger;
    }

    public void setDisableJdkLogger(Boolean disableJdkLogger) {
        this.disableJdkLogger = disableJdkLogger;
    }

    public Boolean getFrontChannelLogoutSessionSupported() {
        return frontChannelLogoutSessionSupported;
    }

    public void setFrontChannelLogoutSessionSupported(
            Boolean frontChannelLogoutSessionSupported) {
        this.frontChannelLogoutSessionSupported = frontChannelLogoutSessionSupported;
    }

    public Boolean getIntrospectionAccessTokenMustHaveUmaProtectionScope() {
        return introspectionAccessTokenMustHaveUmaProtectionScope;
    }

    public void setIntrospectionAccessTokenMustHaveUmaProtectionScope(Boolean introspectionAccessTokenMustHaveUmaProtectionScope) {
        this.introspectionAccessTokenMustHaveUmaProtectionScope = introspectionAccessTokenMustHaveUmaProtectionScope;
    }

    public Boolean getIntrospectionSkipAuthorization() {
        if (introspectionSkipAuthorization == null) introspectionSkipAuthorization = false;
        return introspectionSkipAuthorization;
    }

    public void setIntrospectionSkipAuthorization(Boolean introspectionSkipAuthorization) {
        this.introspectionSkipAuthorization = introspectionSkipAuthorization;
    }

    public Boolean getUmaRptAsJwt() {
        return umaRptAsJwt;
    }

    public void setUmaRptAsJwt(Boolean umaRptAsJwt) {
        this.umaRptAsJwt = umaRptAsJwt;
    }

    public Boolean getSessionAsJwt() {
        return sessionAsJwt;
    }

    public void setSessionAsJwt(Boolean sessionAsJwt) {
        this.sessionAsJwt = sessionAsJwt;
    }

    public Boolean getUmaAddScopesAutomatically() {
        return umaAddScopesAutomatically;
    }

    public void setUmaAddScopesAutomatically(Boolean p_umaAddScopesAutomatically) {
        umaAddScopesAutomatically = p_umaAddScopesAutomatically;
    }

    public Boolean getUmaValidateClaimToken() {
        return umaValidateClaimToken;
    }

    public void setUmaValidateClaimToken(Boolean umaValidateClaimToken) {
        this.umaValidateClaimToken = umaValidateClaimToken;
    }

    public Boolean getUmaGrantAccessIfNoPolicies() {
        return umaGrantAccessIfNoPolicies;
    }

    public void setUmaGrantAccessIfNoPolicies(Boolean umaGrantAccessIfNoPolicies) {
        this.umaGrantAccessIfNoPolicies = umaGrantAccessIfNoPolicies;
    }

    public Boolean getUmaRestrictResourceToAssociatedClient() {
        return umaRestrictResourceToAssociatedClient;
    }

    public void setUmaRestrictResourceToAssociatedClient(Boolean umaRestrictResourceToAssociatedClient) {
        this.umaRestrictResourceToAssociatedClient = umaRestrictResourceToAssociatedClient;
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
     * Returns the base URI of the endpoints.
     *
     * @return The base URI of endpoints.
     */
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

    /**
     * Returns the URL of the Token Revocation endpoint.
     *
     * @return The URL of the Token Revocation endpoint.
     */
    public String getTokenRevocationEndpoint() {
        return tokenRevocationEndpoint;
    }

    /**
     * Sets the URL of the Token Revocation endpoint.
     *
     * @param tokenRevocationEndpoint The URL of the Token Revocation endpoint.
     */
    public void setTokenRevocationEndpoint(String tokenRevocationEndpoint) {
        this.tokenRevocationEndpoint = tokenRevocationEndpoint;
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
     * Returns the URL od the Client Info endpoint.
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
     * Returns the URL of an OP endpoint that provides a page to support cross-origin
     * communications for session state information with the RP client.
     *
     * @return The Check Session iFrame URL.
     */
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
     * Returns the URL of the OP's JSON Web Key Set (JWK) document that contains the Server's signing key(s)
     * that are used for signing responses to the Client.
     * The JWK Set may also contain the Server's encryption key(s) that are used by the Client to encrypt
     * requests to the Server.
     *
     * @return The URL of the OP's JSON Web Key Set (JWK) document.
     */
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

    public String getOpenIdDiscoveryEndpoint() {
        return openIdDiscoveryEndpoint;
    }

    public void setOpenIdDiscoveryEndpoint(String openIdDiscoveryEndpoint) {
        this.openIdDiscoveryEndpoint = openIdDiscoveryEndpoint;
    }

    public String getUmaConfigurationEndpoint() {
        return umaConfigurationEndpoint;
    }

    public void setUmaConfigurationEndpoint(String p_umaConfigurationEndpoint) {
        umaConfigurationEndpoint = p_umaConfigurationEndpoint;
    }

    public String getOpenidSubAttribute() {
        return openidSubAttribute;
    }

    public void setOpenidSubAttribute(String openidSubAttribute) {
        this.openidSubAttribute = openidSubAttribute;
    }

    public String getIdGenerationEndpoint() {
        return idGenerationEndpoint;
    }

    public void setIdGenerationEndpoint(String p_idGenerationEndpoint) {
        idGenerationEndpoint = p_idGenerationEndpoint;
    }

    public String getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    public void setIntrospectionEndpoint(String p_introspectionEndpoint) {
        introspectionEndpoint = p_introspectionEndpoint;
    }

    public String getParEndpoint() {
        return parEndpoint;
    }

    public void setParEndpoint(String parEndpoint) {
        this.parEndpoint = parEndpoint;
    }

    public Boolean getRequirePar() {
        if (requirePar == null) requirePar = false;
        return requirePar;
    }

    public void setRequirePar(Boolean requirePar) {
        this.requirePar = requirePar;
    }

    public String getOpenIdConfigurationEndpoint() {
        return openIdConfigurationEndpoint;
    }

    public void setOpenIdConfigurationEndpoint(String openIdConfigurationEndpoint) {
        this.openIdConfigurationEndpoint = openIdConfigurationEndpoint;
    }

    public Set<Set<ResponseType>> getResponseTypesSupported() {
        return responseTypesSupported;
    }

    public Set<ResponseType> getAllResponseTypesSupported() {
        Set<ResponseType> types = new HashSet<>();
        if (responseTypesSupported != null) {
            for (Set<ResponseType> set : responseTypesSupported) {
                types.addAll(set);
            }
        }
        return types;
    }

    public void setResponseTypesSupported(Set<Set<ResponseType>> responseTypesSupported) {
        this.responseTypesSupported = responseTypesSupported;
    }

    public Set<ResponseMode> getResponseModesSupported() {
        return responseModesSupported;
    }

    public void setResponseModesSupported(Set<ResponseMode> responseModesSupported) {
        this.responseModesSupported = responseModesSupported;
    }

    public Set<GrantType> getGrantTypesSupported() {
        return grantTypesSupported;
    }

    public void setGrantTypesSupported(Set<GrantType> grantTypesSupported) {
        this.grantTypesSupported = grantTypesSupported;
    }

    public List<String> getSubjectTypesSupported() {
        return subjectTypesSupported;
    }

    public void setSubjectTypesSupported(List<String> subjectTypesSupported) {
        this.subjectTypesSupported = subjectTypesSupported;
    }

    public String getDefaultSubjectType() {
        return defaultSubjectType;
    }

    public void setDefaultSubjectType(String defaultSubjectType) {
        this.defaultSubjectType = defaultSubjectType;
    }

    public List<String> getAuthorizationSigningAlgValuesSupported() {
        return authorizationSigningAlgValuesSupported;
    }

    public void setAuthorizationSigningAlgValuesSupported(List<String> authorizationSigningAlgValuesSupported) {
        this.authorizationSigningAlgValuesSupported = authorizationSigningAlgValuesSupported;
    }

    public List<String> getAuthorizationEncryptionAlgValuesSupported() {
        return authorizationEncryptionAlgValuesSupported;
    }

    public void setAuthorizationEncryptionAlgValuesSupported(List<String> authorizationEncryptionAlgValuesSupported) {
        this.authorizationEncryptionAlgValuesSupported = authorizationEncryptionAlgValuesSupported;
    }

    public List<String> getAuthorizationEncryptionEncValuesSupported() {
        return authorizationEncryptionEncValuesSupported;
    }

    public void setAuthorizationEncryptionEncValuesSupported(List<String> authorizationEncryptionEncValuesSupported) {
        this.authorizationEncryptionEncValuesSupported = authorizationEncryptionEncValuesSupported;
    }

    public List<String> getUserInfoSigningAlgValuesSupported() {
        return userInfoSigningAlgValuesSupported;
    }

    public void setUserInfoSigningAlgValuesSupported(List<String> userInfoSigningAlgValuesSupported) {
        this.userInfoSigningAlgValuesSupported = userInfoSigningAlgValuesSupported;
    }

    public List<String> getUserInfoEncryptionAlgValuesSupported() {
        return userInfoEncryptionAlgValuesSupported;
    }

    public void setUserInfoEncryptionAlgValuesSupported(List<String> userInfoEncryptionAlgValuesSupported) {
        this.userInfoEncryptionAlgValuesSupported = userInfoEncryptionAlgValuesSupported;
    }

    public int getStatWebServiceIntervalLimitInSeconds() {
        return statWebServiceIntervalLimitInSeconds;
    }

    public void setStatWebServiceIntervalLimitInSeconds(int statWebServiceIntervalLimitInSeconds) {
        this.statWebServiceIntervalLimitInSeconds = statWebServiceIntervalLimitInSeconds;
    }

    public int getStatTimerIntervalInSeconds() {
        return statTimerIntervalInSeconds;
    }

    public void setStatTimerIntervalInSeconds(int statTimerIntervalInSeconds) {
        this.statTimerIntervalInSeconds = statTimerIntervalInSeconds;
    }

    public String getStatAuthorizationScope() {
        if (statAuthorizationScope == null) statAuthorizationScope = DEFAULT_STAT_SCOPE;
        return statAuthorizationScope;
    }

    public void setStatAuthorizationScope(String statAuthorizationScope) {
        this.statAuthorizationScope = statAuthorizationScope;
    }

    public List<String> getUserInfoEncryptionEncValuesSupported() {
        return userInfoEncryptionEncValuesSupported;
    }

    public void setUserInfoEncryptionEncValuesSupported(List<String> userInfoEncryptionEncValuesSupported) {
        this.userInfoEncryptionEncValuesSupported = userInfoEncryptionEncValuesSupported;
    }

    public List<String> getIdTokenSigningAlgValuesSupported() {
        return idTokenSigningAlgValuesSupported;
    }

    public void setIdTokenSigningAlgValuesSupported(List<String> idTokenSigningAlgValuesSupported) {
        this.idTokenSigningAlgValuesSupported = idTokenSigningAlgValuesSupported;
    }

    public List<String> getIdTokenEncryptionAlgValuesSupported() {
        return idTokenEncryptionAlgValuesSupported;
    }

    public void setIdTokenEncryptionAlgValuesSupported(List<String> idTokenEncryptionAlgValuesSupported) {
        this.idTokenEncryptionAlgValuesSupported = idTokenEncryptionAlgValuesSupported;
    }

    public List<String> getIdTokenEncryptionEncValuesSupported() {
        return idTokenEncryptionEncValuesSupported;
    }

    public void setIdTokenEncryptionEncValuesSupported(List<String> idTokenEncryptionEncValuesSupported) {
        this.idTokenEncryptionEncValuesSupported = idTokenEncryptionEncValuesSupported;
    }

    public List<String> getRequestObjectSigningAlgValuesSupported() {
        return requestObjectSigningAlgValuesSupported;
    }

    public void setRequestObjectSigningAlgValuesSupported(List<String> requestObjectSigningAlgValuesSupported) {
        this.requestObjectSigningAlgValuesSupported = requestObjectSigningAlgValuesSupported;
    }

    public List<String> getRequestObjectEncryptionAlgValuesSupported() {
        return requestObjectEncryptionAlgValuesSupported;
    }

    public void setRequestObjectEncryptionAlgValuesSupported(List<String> requestObjectEncryptionAlgValuesSupported) {
        this.requestObjectEncryptionAlgValuesSupported = requestObjectEncryptionAlgValuesSupported;
    }

    public List<String> getRequestObjectEncryptionEncValuesSupported() {
        return requestObjectEncryptionEncValuesSupported;
    }

    public void setRequestObjectEncryptionEncValuesSupported(List<String> requestObjectEncryptionEncValuesSupported) {
        this.requestObjectEncryptionEncValuesSupported = requestObjectEncryptionEncValuesSupported;
    }

    public List<String> getTokenEndpointAuthMethodsSupported() {
        return tokenEndpointAuthMethodsSupported;
    }

    public void setTokenEndpointAuthMethodsSupported(List<String> tokenEndpointAuthMethodsSupported) {
        this.tokenEndpointAuthMethodsSupported = tokenEndpointAuthMethodsSupported;
    }

    public List<String> getTokenEndpointAuthSigningAlgValuesSupported() {
        return tokenEndpointAuthSigningAlgValuesSupported;
    }

    public void setTokenEndpointAuthSigningAlgValuesSupported(List<String> tokenEndpointAuthSigningAlgValuesSupported) {
        this.tokenEndpointAuthSigningAlgValuesSupported = tokenEndpointAuthSigningAlgValuesSupported;
    }

    public List<String> getDynamicRegistrationCustomAttributes() {
        return dynamicRegistrationCustomAttributes;
    }

    public void setDynamicRegistrationCustomAttributes(List<String> p_dynamicRegistrationCustomAttributes) {
        dynamicRegistrationCustomAttributes = p_dynamicRegistrationCustomAttributes;
    }

    public List<String> getDisplayValuesSupported() {
        return displayValuesSupported;
    }

    public void setDisplayValuesSupported(List<String> displayValuesSupported) {
        this.displayValuesSupported = displayValuesSupported;
    }

    public List<String> getClaimTypesSupported() {
        return claimTypesSupported;
    }

    public void setClaimTypesSupported(List<String> claimTypesSupported) {
        this.claimTypesSupported = claimTypesSupported;
    }

    public List<String> getJwksAlgorithmsSupported() {
        return jwksAlgorithmsSupported;
    }

    public void setJwksAlgorithmsSupported(List<String> jwksAlgorithmsSupported) {
        this.jwksAlgorithmsSupported = jwksAlgorithmsSupported;
    }

    public String getServiceDocumentation() {
        return serviceDocumentation;
    }

    public void setServiceDocumentation(String serviceDocumentation) {
        this.serviceDocumentation = serviceDocumentation;
    }

    public List<String> getClaimsLocalesSupported() {
        return claimsLocalesSupported;
    }

    public void setClaimsLocalesSupported(List<String> claimsLocalesSupported) {
        this.claimsLocalesSupported = claimsLocalesSupported;
    }

    public List<String> getIdTokenTokenBindingCnfValuesSupported() {
        if (idTokenTokenBindingCnfValuesSupported == null) {
            idTokenTokenBindingCnfValuesSupported = new ArrayList<String>();
        }
        return idTokenTokenBindingCnfValuesSupported;
    }

    public void setIdTokenTokenBindingCnfValuesSupported(List<String> idTokenTokenBindingCnfValuesSupported) {
        this.idTokenTokenBindingCnfValuesSupported = idTokenTokenBindingCnfValuesSupported;
    }

    public List<String> getUiLocalesSupported() {
        return uiLocalesSupported;
    }

    public void setUiLocalesSupported(List<String> uiLocalesSupported) {
        this.uiLocalesSupported = uiLocalesSupported;
    }

    public Boolean getClaimsParameterSupported() {
        return claimsParameterSupported;
    }

    public void setClaimsParameterSupported(Boolean claimsParameterSupported) {
        this.claimsParameterSupported = claimsParameterSupported;
    }

    public Boolean getRequestParameterSupported() {
        return requestParameterSupported;
    }

    public void setRequestParameterSupported(Boolean requestParameterSupported) {
        this.requestParameterSupported = requestParameterSupported;
    }

    public Boolean getRequestUriParameterSupported() {
        return requestUriParameterSupported;
    }

    public void setRequestUriParameterSupported(Boolean requestUriParameterSupported) {
        this.requestUriParameterSupported = requestUriParameterSupported;
    }

    public Boolean getRequireRequestUriRegistration() {
        return requireRequestUriRegistration;
    }

    public void setRequireRequestUriRegistration(Boolean requireRequestUriRegistration) {
        this.requireRequestUriRegistration = requireRequestUriRegistration;
    }

    public String getOpPolicyUri() {
        return opPolicyUri;
    }

    public void setOpPolicyUri(String opPolicyUri) {
        this.opPolicyUri = opPolicyUri;
    }

    public String getOpTosUri() {
        return opTosUri;
    }

    public void setOpTosUri(String opTosUri) {
        this.opTosUri = opTosUri;
    }

    public int getAuthorizationCodeLifetime() {
        return authorizationCodeLifetime;
    }

    public void setAuthorizationCodeLifetime(int authorizationCodeLifetime) {
        this.authorizationCodeLifetime = authorizationCodeLifetime;
    }

    public int getRefreshTokenLifetime() {
        return refreshTokenLifetime;
    }

    public void setRefreshTokenLifetime(int refreshTokenLifetime) {
        this.refreshTokenLifetime = refreshTokenLifetime;
    }

    public int getIdTokenLifetime() {
        return idTokenLifetime;
    }

    public void setIdTokenLifetime(int idTokenLifetime) {
        this.idTokenLifetime = idTokenLifetime;
    }

    public int getAccessTokenLifetime() {
        return accessTokenLifetime;
    }

    public void setAccessTokenLifetime(int accessTokenLifetime) {
        this.accessTokenLifetime = accessTokenLifetime;
    }

    public int getUmaRptLifetime() {
        return umaRptLifetime;
    }

    public void setUmaRptLifetime(int umaRptLifetime) {
        this.umaRptLifetime = umaRptLifetime;
    }

    public int getUmaTicketLifetime() {
        return umaTicketLifetime;
    }

    public void setUmaTicketLifetime(int umaTicketLifetime) {
        this.umaTicketLifetime = umaTicketLifetime;
    }

    public int getUmaResourceLifetime() {
        return umaResourceLifetime;
    }

    public void setUmaResourceLifetime(int umaResourceLifetime) {
        this.umaResourceLifetime = umaResourceLifetime;
    }

    public int getUmaPctLifetime() {
        return umaPctLifetime;
    }

    public void setUmaPctLifetime(int umaPctLifetime) {
        this.umaPctLifetime = umaPctLifetime;
    }

    public int getSpontaneousScopeLifetime() {
        return spontaneousScopeLifetime;
    }

    public void setSpontaneousScopeLifetime(int spontaneousScopeLifetime) {
        this.spontaneousScopeLifetime = spontaneousScopeLifetime;
    }

    public int getCleanServiceInterval() {
        return cleanServiceInterval;
    }

    public void setCleanServiceInterval(int p_cleanServiceInterval) {
        cleanServiceInterval = p_cleanServiceInterval;
    }

    public int getCleanServiceBatchChunkSize() {
        return cleanServiceBatchChunkSize;
    }

    public void setCleanServiceBatchChunkSize(int cleanServiceBatchChunkSize) {
        this.cleanServiceBatchChunkSize = cleanServiceBatchChunkSize;
    }

    public Boolean getKeyRegenerationEnabled() {
        return keyRegenerationEnabled;
    }

    public void setKeyRegenerationEnabled(Boolean keyRegenerationEnabled) {
        this.keyRegenerationEnabled = keyRegenerationEnabled;
    }

    public int getKeyRegenerationInterval() {
        return keyRegenerationInterval;
    }

    public void setKeyRegenerationInterval(int keyRegenerationInterval) {
        this.keyRegenerationInterval = keyRegenerationInterval;
    }

    public String getDefaultSignatureAlgorithm() {
        return defaultSignatureAlgorithm;
    }

    public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {
        this.defaultSignatureAlgorithm = defaultSignatureAlgorithm;
    }

    public String getOxOpenIdConnectVersion() {
        return oxOpenIdConnectVersion;
    }

    public void setOxOpenIdConnectVersion(String oxOpenIdConnectVersion) {
        this.oxOpenIdConnectVersion = oxOpenIdConnectVersion;
    }

    public String getOxId() {
        return oxId;
    }

    public void setOxId(String oxId) {
        this.oxId = oxId;
    }

    public int getDynamicRegistrationExpirationTime() {
        return dynamicRegistrationExpirationTime;
    }

    public void setDynamicRegistrationExpirationTime(int dynamicRegistrationExpirationTime) {
        this.dynamicRegistrationExpirationTime = dynamicRegistrationExpirationTime;
    }

    public Boolean getDynamicRegistrationPersistClientAuthorizations() {
        return dynamicRegistrationPersistClientAuthorizations;
    }

    public void setDynamicRegistrationPersistClientAuthorizations(Boolean dynamicRegistrationPersistClientAuthorizations) {
        this.dynamicRegistrationPersistClientAuthorizations = dynamicRegistrationPersistClientAuthorizations;
    }

    public Boolean getTrustedClientEnabled() {
        return trustedClientEnabled;
    }

    public void setTrustedClientEnabled(Boolean trustedClientEnabled) {
        this.trustedClientEnabled = trustedClientEnabled;
    }

    public Boolean getSkipAuthorizationForOpenIdScopeAndPairwiseId() {
        return skipAuthorizationForOpenIdScopeAndPairwiseId;
    }

    public void setSkipAuthorizationForOpenIdScopeAndPairwiseId(Boolean skipAuthorizationForOpenIdScopeAndPairwiseId) {
        this.skipAuthorizationForOpenIdScopeAndPairwiseId = skipAuthorizationForOpenIdScopeAndPairwiseId;
    }

    public Boolean getDynamicRegistrationScopesParamEnabled() {
        return dynamicRegistrationScopesParamEnabled;
    }

    public void setDynamicRegistrationScopesParamEnabled(Boolean dynamicRegistrationScopesParamEnabled) {
        this.dynamicRegistrationScopesParamEnabled = dynamicRegistrationScopesParamEnabled;
    }

    public Boolean getPersistIdTokenInLdap() {
        return persistIdTokenInLdap;
    }

    public void setPersistIdTokenInLdap(Boolean persistIdTokenInLdap) {
        this.persistIdTokenInLdap = persistIdTokenInLdap;
    }

    public Boolean getPersistRefreshTokenInLdap() {
        return persistRefreshTokenInLdap;
    }

    public void setPersistRefreshTokenInLdap(Boolean persistRefreshTokenInLdap) {
        this.persistRefreshTokenInLdap = persistRefreshTokenInLdap;
    }

    public Boolean getAllowPostLogoutRedirectWithoutValidation() {
        if (allowPostLogoutRedirectWithoutValidation == null) allowPostLogoutRedirectWithoutValidation = false;
        return allowPostLogoutRedirectWithoutValidation;
    }

    public void setAllowPostLogoutRedirectWithoutValidation(Boolean allowPostLogoutRedirectWithoutValidation) {
        this.allowPostLogoutRedirectWithoutValidation = allowPostLogoutRedirectWithoutValidation;
    }

    public Boolean getInvalidateSessionCookiesAfterAuthorizationFlow() {
        if (invalidateSessionCookiesAfterAuthorizationFlow == null) {
            invalidateSessionCookiesAfterAuthorizationFlow = false;
        }
        return invalidateSessionCookiesAfterAuthorizationFlow;
    }

    public void setInvalidateSessionCookiesAfterAuthorizationFlow(Boolean invalidateSessionCookiesAfterAuthorizationFlow) {
        this.invalidateSessionCookiesAfterAuthorizationFlow = invalidateSessionCookiesAfterAuthorizationFlow;
    }

    public String getDynamicRegistrationCustomObjectClass() {
        return dynamicRegistrationCustomObjectClass;
    }

    public void setDynamicRegistrationCustomObjectClass(String p_dynamicRegistrationCustomObjectClass) {
        dynamicRegistrationCustomObjectClass = p_dynamicRegistrationCustomObjectClass;
    }

    public List<String> getPersonCustomObjectClassList() {
        return personCustomObjectClassList;
    }

    public void setPersonCustomObjectClassList(List<String> personCustomObjectClassList) {
        this.personCustomObjectClassList = personCustomObjectClassList;
    }

    public Boolean getAuthenticationFiltersEnabled() {
        return authenticationFiltersEnabled;
    }

    public void setAuthenticationFiltersEnabled(Boolean authenticationFiltersEnabled) {
        this.authenticationFiltersEnabled = authenticationFiltersEnabled;
    }

    public Boolean getClientAuthenticationFiltersEnabled() {
        return clientAuthenticationFiltersEnabled;
    }

    public void setClientAuthenticationFiltersEnabled(Boolean p_clientAuthenticationFiltersEnabled) {
        clientAuthenticationFiltersEnabled = p_clientAuthenticationFiltersEnabled;
    }

    public List<AuthenticationFilter> getAuthenticationFilters() {
        if (authenticationFilters == null) {
            authenticationFilters = new ArrayList<AuthenticationFilter>();
        }

        return authenticationFilters;
    }

    public List<ClientAuthenticationFilter> getClientAuthenticationFilters() {
        if (clientAuthenticationFilters == null) {
            clientAuthenticationFilters = new ArrayList<ClientAuthenticationFilter>();
        }

        return clientAuthenticationFilters;
    }


    public List<CorsConfigurationFilter> getCorsConfigurationFilters() {
        if (corsConfigurationFilters == null) {
            corsConfigurationFilters = new ArrayList<CorsConfigurationFilter>();
        }

        return corsConfigurationFilters;
    }

    public int getSessionIdUnusedLifetime() {
        return sessionIdUnusedLifetime;
    }

    public void setSessionIdUnusedLifetime(int p_sessionIdUnusedLifetime) {
        sessionIdUnusedLifetime = p_sessionIdUnusedLifetime;
    }

    public int getSessionIdUnauthenticatedUnusedLifetime() {
        return sessionIdUnauthenticatedUnusedLifetime;
    }

    public void setSessionIdUnauthenticatedUnusedLifetime(int sessionIdUnauthenticatedUnusedLifetime) {
        this.sessionIdUnauthenticatedUnusedLifetime = sessionIdUnauthenticatedUnusedLifetime;
    }

    public Boolean getSessionIdPersistOnPromptNone() {
        return sessionIdPersistOnPromptNone;
    }

    public void setSessionIdPersistOnPromptNone(Boolean sessionIdPersistOnPromptNone) {
        this.sessionIdPersistOnPromptNone = sessionIdPersistOnPromptNone;
    }

    public Boolean getSessionIdRequestParameterEnabled() {
        if (sessionIdRequestParameterEnabled == null) {
            sessionIdRequestParameterEnabled = false;
        }
        return sessionIdRequestParameterEnabled;
    }

    public void setSessionIdRequestParameterEnabled(Boolean sessionIdRequestParameterEnabled) {
        this.sessionIdRequestParameterEnabled = sessionIdRequestParameterEnabled;
    }

    public Boolean getSessionIdEnabled() {
        return sessionIdEnabled;
    }

    public void setSessionIdEnabled(Boolean p_sessionIdEnabled) {
        sessionIdEnabled = p_sessionIdEnabled;
    }

    public int getConfigurationUpdateInterval() {
        return configurationUpdateInterval;
    }

    public void setConfigurationUpdateInterval(int p_configurationUpdateInterval) {
        configurationUpdateInterval = p_configurationUpdateInterval;
    }

    public String getJsLocation() {
        return jsLocation;
    }

    public void setJsLocation(String jsLocation) {
        this.jsLocation = jsLocation;
    }

    public String getCssLocation() {
        return cssLocation;
    }

    public void setCssLocation(String cssLocation) {
        this.cssLocation = cssLocation;
    }

    public String getImgLocation() {
        return imgLocation;
    }

    public void setImgLocation(String imgLocation) {
        this.imgLocation = imgLocation;
    }

    public int getMetricReporterInterval() {
        return metricReporterInterval;
    }

    public void setMetricReporterInterval(int metricReporterInterval) {
        this.metricReporterInterval = metricReporterInterval;
    }

    public int getMetricReporterKeepDataDays() {
        return metricReporterKeepDataDays;
    }

    public void setMetricReporterKeepDataDays(int metricReporterKeepDataDays) {
        this.metricReporterKeepDataDays = metricReporterKeepDataDays;
    }

    public String getPairwiseIdType() {
        return pairwiseIdType;
    }

    public void setPairwiseIdType(String pairwiseIdType) {
        this.pairwiseIdType = pairwiseIdType;
    }

    public String getPairwiseCalculationKey() {
        return pairwiseCalculationKey;
    }

    public void setPairwiseCalculationKey(String pairwiseCalculationKey) {
        this.pairwiseCalculationKey = pairwiseCalculationKey;
    }

    public String getPairwiseCalculationSalt() {
        return pairwiseCalculationSalt;
    }

    public void setPairwiseCalculationSalt(String pairwiseCalculationSalt) {
        this.pairwiseCalculationSalt = pairwiseCalculationSalt;
    }

    public Boolean isShareSubjectIdBetweenClientsWithSameSectorId() {
        return shareSubjectIdBetweenClientsWithSameSectorId;
    }

    public void setShareSubjectIdBetweenClientsWithSameSectorId(Boolean shareSubjectIdBetweenClientsWithSameSectorId) {
        this.shareSubjectIdBetweenClientsWithSameSectorId = shareSubjectIdBetweenClientsWithSameSectorId;
    }

    public WebKeyStorage getWebKeysStorage() {
        return webKeysStorage;
    }

    public void setWebKeysStorage(WebKeyStorage webKeysStorage) {
        this.webKeysStorage = webKeysStorage;
    }

    public String getDnName() {
        return dnName;
    }

    public void setDnName(String dnName) {
        this.dnName = dnName;
    }

    public String getKeyStoreFile() {
        return keyStoreFile;
    }

    public void setKeyStoreFile(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }

    public String getKeyStoreSecret() {
        return keyStoreSecret;
    }

    public void setKeyStoreSecret(String keyStoreSecret) {
        this.keyStoreSecret = keyStoreSecret;
    }

    public String getOxElevenTestModeToken() {
        return oxElevenTestModeToken;
    }

    public void setOxElevenTestModeToken(String oxElevenTestModeToken) {
        this.oxElevenTestModeToken = oxElevenTestModeToken;
    }

    public String getOxElevenGenerateKeyEndpoint() {
        return oxElevenGenerateKeyEndpoint;
    }

    public void setOxElevenGenerateKeyEndpoint(String oxElevenGenerateKeyEndpoint) {
        this.oxElevenGenerateKeyEndpoint = oxElevenGenerateKeyEndpoint;
    }

    public String getOxElevenSignEndpoint() {
        return oxElevenSignEndpoint;
    }

    public void setOxElevenSignEndpoint(String oxElevenSignEndpoint) {
        this.oxElevenSignEndpoint = oxElevenSignEndpoint;
    }

    public String getOxElevenVerifySignatureEndpoint() {
        return oxElevenVerifySignatureEndpoint;
    }

    public void setOxElevenVerifySignatureEndpoint(String oxElevenVerifySignatureEndpoint) {
        this.oxElevenVerifySignatureEndpoint = oxElevenVerifySignatureEndpoint;
    }

    public String getOxElevenDeleteKeyEndpoint() {
        return oxElevenDeleteKeyEndpoint;
    }

    public void setOxElevenDeleteKeyEndpoint(String oxElevenDeleteKeyEndpoint) {
        this.oxElevenDeleteKeyEndpoint = oxElevenDeleteKeyEndpoint;
    }

    public Boolean getEndSessionWithAccessToken() {
        return endSessionWithAccessToken;
    }

    public void setEndSessionWithAccessToken(Boolean endSessionWithAccessToken) {
        this.endSessionWithAccessToken = endSessionWithAccessToken;
    }

    public String getCookieDomain() {
        return cookieDomain;
    }

    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }

    public Boolean getEnabledOAuthAuditLogging() {
        return enabledOAuthAuditLogging;
    }

    public void setEnabledOAuthAuditLogging(Boolean enabledOAuthAuditLogging) {
        this.enabledOAuthAuditLogging = enabledOAuthAuditLogging;
    }

    public Set<String> getJmsBrokerURISet() {
        return jmsBrokerURISet;
    }

    public void setJmsBrokerURISet(Set<String> jmsBrokerURISet) {
        this.jmsBrokerURISet = jmsBrokerURISet;
    }

    public String getJmsUserName() {
        return jmsUserName;
    }

    public void setJmsUserName(String jmsUserName) {
        this.jmsUserName = jmsUserName;
    }

    public String getJmsPassword() {
        return jmsPassword;
    }

    public void setJmsPassword(String jmsPassword) {
        this.jmsPassword = jmsPassword;
    }

    public List<String> getClientWhiteList() {
        return clientWhiteList;
    }

    public void setClientWhiteList(List<String> clientWhiteList) {
        this.clientWhiteList = clientWhiteList;
    }

    public List<String> getClientBlackList() {
        return clientBlackList;
    }

    public void setClientBlackList(List<String> clientBlackList) {
        this.clientBlackList = clientBlackList;
    }

    public Boolean getLegacyIdTokenClaims() {
        return legacyIdTokenClaims;
    }

    public void setLegacyIdTokenClaims(Boolean legacyIdTokenClaims) {
        this.legacyIdTokenClaims = legacyIdTokenClaims;
    }

    public Boolean getCustomHeadersWithAuthorizationResponse() {
        if (customHeadersWithAuthorizationResponse == null) {
            return false;
        }

        return customHeadersWithAuthorizationResponse;
    }

    public void setCustomHeadersWithAuthorizationResponse(Boolean customHeadersWithAuthorizationResponse) {
        this.customHeadersWithAuthorizationResponse = customHeadersWithAuthorizationResponse;
    }

    public Boolean getUpdateUserLastLogonTime() {
        return updateUserLastLogonTime != null ? updateUserLastLogonTime : false;
    }

    public void setUpdateUserLastLogonTime(Boolean updateUserLastLogonTime) {
        this.updateUserLastLogonTime = updateUserLastLogonTime;
    }

    public Boolean getUpdateClientAccessTime() {
        return updateClientAccessTime != null ? updateClientAccessTime : false;
    }

    public void setUpdateClientAccessTime(Boolean updateClientAccessTime) {
        this.updateClientAccessTime = updateClientAccessTime;
    }

    public Boolean getHttpLoggingEnabled() {
        return httpLoggingEnabled;
    }

    public void setHttpLoggingEnabled(Boolean httpLoggingEnabled) {
        this.httpLoggingEnabled = httpLoggingEnabled;
    }

    public Set<String> getHttpLoggingExludePaths() {
        return httpLoggingExludePaths;
    }

    public void setHttpLoggingExludePaths(Set<String> httpLoggingExludePaths) {
        this.httpLoggingExludePaths = httpLoggingExludePaths;
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public String getLoggingLayout() {
        return loggingLayout;
    }

    public void setLoggingLayout(String loggingLayout) {
        this.loggingLayout = loggingLayout;
    }

    public Boolean getEnableClientGrantTypeUpdate() {
        return enableClientGrantTypeUpdate;
    }

    public void setEnableClientGrantTypeUpdate(Boolean enableClientGrantTypeUpdate) {
        this.enableClientGrantTypeUpdate = enableClientGrantTypeUpdate;
    }

    public Set<GrantType> getDynamicGrantTypeDefault() {
        return dynamicGrantTypeDefault;
    }

    public void setDynamicGrantTypeDefault(Set<GrantType> dynamicGrantTypeDefault) {
        this.dynamicGrantTypeDefault = dynamicGrantTypeDefault;
    }

    /**
     * @return session_id lifetime. If null or value is zero or less then session_id lifetime is not set and will expire when browser session ends.
     */
    public Integer getSessionIdLifetime() {
        return sessionIdLifetime;
    }

    public void setSessionIdLifetime(Integer sessionIdLifetime) {
        this.sessionIdLifetime = sessionIdLifetime;
    }

    public Integer getServerSessionIdLifetime() {
        return serverSessionIdLifetime;
    }

    public void setServerSessionIdLifetime(Integer serverSessionIdLifetime) {
        this.serverSessionIdLifetime = serverSessionIdLifetime;
    }

    public Boolean getLogClientIdOnClientAuthentication() {
        return logClientIdOnClientAuthentication;
    }

    public void setLogClientIdOnClientAuthentication(Boolean logClientIdOnClientAuthentication) {
        this.logClientIdOnClientAuthentication = logClientIdOnClientAuthentication;
    }

    public Boolean getLogClientNameOnClientAuthentication() {
        return logClientNameOnClientAuthentication;
    }

    public void setLogClientNameOnClientAuthentication(Boolean logClientNameOnClientAuthentication) {
        this.logClientNameOnClientAuthentication = logClientNameOnClientAuthentication;
    }

    public String getExternalLoggerConfiguration() {
        return externalLoggerConfiguration;
    }

    public void setExternalLoggerConfiguration(String externalLoggerConfiguration) {
        this.externalLoggerConfiguration = externalLoggerConfiguration;
    }

    public Set<String> getAuthorizationRequestCustomAllowedParameters() {
        return authorizationRequestCustomAllowedParameters;
    }

    public void setAuthorizationRequestCustomAllowedParameters(Set<String> authorizationRequestCustomAllowedParameters) {
        this.authorizationRequestCustomAllowedParameters = authorizationRequestCustomAllowedParameters;
    }

    public Boolean getOpenidScopeBackwardCompatibility() {
        return openidScopeBackwardCompatibility;
    }

    public void setOpenidScopeBackwardCompatibility(Boolean openidScopeBackwardCompatibility) {
        this.openidScopeBackwardCompatibility = openidScopeBackwardCompatibility;
    }

    public Boolean getDisableU2fEndpoint() {
        return disableU2fEndpoint;
    }

    public void setDisableU2fEndpoint(Boolean disableU2fEndpoint) {
        this.disableU2fEndpoint = disableU2fEndpoint;
    }

    public AuthenticationProtectionConfiguration getAuthenticationProtectionConfiguration() {
        return authenticationProtectionConfiguration;
    }

    public void setAuthenticationProtectionConfiguration(AuthenticationProtectionConfiguration authenticationProtectionConfiguration) {
        this.authenticationProtectionConfiguration = authenticationProtectionConfiguration;
    }

    public ErrorHandlingMethod getErrorHandlingMethod() {
        return errorHandlingMethod;
    }

    public void setErrorHandlingMethod(ErrorHandlingMethod errorHandlingMethod) {
        this.errorHandlingMethod = errorHandlingMethod;
    }

    public Boolean getUseLocalCache() {
        return useLocalCache;
    }

    public void setUseLocalCache(Boolean useLocalCache) {
        this.useLocalCache = useLocalCache;
    }

    public Boolean getKeepAuthenticatorAttributesOnAcrChange() {
        return keepAuthenticatorAttributesOnAcrChange;
    }

    public void setKeepAuthenticatorAttributesOnAcrChange(Boolean keepAuthenticatorAttributesOnAcrChange) {
        this.keepAuthenticatorAttributesOnAcrChange = keepAuthenticatorAttributesOnAcrChange;
    }

    public String getBackchannelClientId() {
        return backchannelClientId;
    }

    public void setBackchannelClientId(String backchannelClientId) {
        this.backchannelClientId = backchannelClientId;
    }

    public String getBackchannelRedirectUri() {
        return backchannelRedirectUri;
    }

    public void setBackchannelRedirectUri(String backchannelRedirectUri) {
        this.backchannelRedirectUri = backchannelRedirectUri;
    }

    public String getBackchannelAuthenticationEndpoint() {
        return backchannelAuthenticationEndpoint;
    }

    public void setBackchannelAuthenticationEndpoint(String backchannelAuthenticationEndpoint) {
        this.backchannelAuthenticationEndpoint = backchannelAuthenticationEndpoint;
    }

    public String getBackchannelDeviceRegistrationEndpoint() {
        return backchannelDeviceRegistrationEndpoint;
    }

    public void setBackchannelDeviceRegistrationEndpoint(String backchannelDeviceRegistrationEndpoint) {
        this.backchannelDeviceRegistrationEndpoint = backchannelDeviceRegistrationEndpoint;
    }

    public List<String> getBackchannelTokenDeliveryModesSupported() {
        if (backchannelTokenDeliveryModesSupported == null)
            backchannelTokenDeliveryModesSupported = Lists.newArrayList();
        return backchannelTokenDeliveryModesSupported;
    }

    public void setBackchannelTokenDeliveryModesSupported(List<String> backchannelTokenDeliveryModesSupported) {
        this.backchannelTokenDeliveryModesSupported = backchannelTokenDeliveryModesSupported;
    }

    public List<String> getBackchannelAuthenticationRequestSigningAlgValuesSupported() {
        if (backchannelAuthenticationRequestSigningAlgValuesSupported == null)
            backchannelAuthenticationRequestSigningAlgValuesSupported = Lists.newArrayList();
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

    public String getBackchannelBindingMessagePattern() {
        return backchannelBindingMessagePattern;
    }

    public void setBackchannelBindingMessagePattern(String backchannelBindingMessagePattern) {
        this.backchannelBindingMessagePattern = backchannelBindingMessagePattern;
    }

    /**
     * Returns a number with a positive integer value indicating the expiration time
     * of the "auth_req_id" in seconds since the authentication request was received.
     *
     * @return Default expires_in value.
     */
    public int getBackchannelAuthenticationResponseExpiresIn() {
        return backchannelAuthenticationResponseExpiresIn;
    }

    public void setBackchannelAuthenticationResponseExpiresIn(int backchannelAuthenticationResponseExpiresIn) {
        this.backchannelAuthenticationResponseExpiresIn = backchannelAuthenticationResponseExpiresIn;
    }

    /**
     * Returns a number with a positive integer value indicating the minimum amount
     * of time in seconds that the Client must wait between polling requests to the
     * token endpoint.
     * This parameter will only be present if the Client is registered to use the
     * Poll or Ping modes.
     *
     * @return Interval value.
     */
    public int getBackchannelAuthenticationResponseInterval() {
        return backchannelAuthenticationResponseInterval;
    }

    public void setBackchannelAuthenticationResponseInterval(int backchannelAuthenticationResponseInterval) {
        this.backchannelAuthenticationResponseInterval = backchannelAuthenticationResponseInterval;
    }

    public List<String> getBackchannelLoginHintClaims() {
        return backchannelLoginHintClaims;
    }

    public void setBackchannelLoginHintClaims(List<String> backchannelLoginHintClaims) {
        this.backchannelLoginHintClaims = backchannelLoginHintClaims;
    }

    public CIBAEndUserNotificationConfig getCibaEndUserNotificationConfig() {
        return cibaEndUserNotificationConfig;
    }

    public void setCibaEndUserNotificationConfig(CIBAEndUserNotificationConfig cibaEndUserNotificationConfig) {
        this.cibaEndUserNotificationConfig = cibaEndUserNotificationConfig;
    }

    public List<String> getDynamicRegistrationAllowedPasswordGrantScopes() {
        if (dynamicRegistrationAllowedPasswordGrantScopes == null)
            dynamicRegistrationAllowedPasswordGrantScopes = Lists.newArrayList();
        return dynamicRegistrationAllowedPasswordGrantScopes;
    }

    public void setDynamicRegistrationAllowedPasswordGrantScopes(List<String> dynamicRegistrationAllowedPasswordGrantScopes) {
        this.dynamicRegistrationAllowedPasswordGrantScopes = dynamicRegistrationAllowedPasswordGrantScopes;
    }

    /**
     * Returns a flag to determinate if Jans Auth supports password grant type for
     * dynamic client registration.
     *
     * @return Boolean, true if it supports, false if it doesn't support.
     */
    public Boolean getDynamicRegistrationPasswordGrantTypeEnabled() {
        return dynamicRegistrationPasswordGrantTypeEnabled;
    }

    /**
     * This method sets the flag that define if Jans Auth supports or not password
     * grant type for dynamic client registration.
     *
     * @param dynamicRegistrationPasswordGrantTypeEnabled Boolean value for
     *                                                    the flag.
     */
    public void setDynamicRegistrationPasswordGrantTypeEnabled(Boolean dynamicRegistrationPasswordGrantTypeEnabled) {
        this.dynamicRegistrationPasswordGrantTypeEnabled = dynamicRegistrationPasswordGrantTypeEnabled;
    }

    public int getBackchannelRequestsProcessorJobIntervalSec() {
        return backchannelRequestsProcessorJobIntervalSec;
    }

    public void setBackchannelRequestsProcessorJobIntervalSec(int backchannelRequestsProcessorJobIntervalSec) {
        this.backchannelRequestsProcessorJobIntervalSec = backchannelRequestsProcessorJobIntervalSec;
    }

    public int getCibaGrantLifeExtraTimeSec() {
        return cibaGrantLifeExtraTimeSec;
    }

    public void setCibaGrantLifeExtraTimeSec(int cibaGrantLifeExtraTimeSec) {
        this.cibaGrantLifeExtraTimeSec = cibaGrantLifeExtraTimeSec;
    }

    public int getCibaMaxExpirationTimeAllowedSec() {
        return cibaMaxExpirationTimeAllowedSec;
    }

    public void setCibaMaxExpirationTimeAllowedSec(int cibaMaxExpirationTimeAllowedSec) {
        this.cibaMaxExpirationTimeAllowedSec = cibaMaxExpirationTimeAllowedSec;
    }

    public int getBackchannelRequestsProcessorJobChunkSize() {
        return backchannelRequestsProcessorJobChunkSize;
    }

    public void setBackchannelRequestsProcessorJobChunkSize(int backchannelRequestsProcessorJobChunkSize) {
        this.backchannelRequestsProcessorJobChunkSize = backchannelRequestsProcessorJobChunkSize;
    }

    public Boolean getClientRegDefaultToCodeFlowWithRefresh() {
        if (clientRegDefaultToCodeFlowWithRefresh == null) clientRegDefaultToCodeFlowWithRefresh = false;
        return clientRegDefaultToCodeFlowWithRefresh;
    }

    public void setClientRegDefaultToCodeFlowWithRefresh(Boolean clientRegDefaultToCodeFlowWithRefresh) {
        this.clientRegDefaultToCodeFlowWithRefresh = clientRegDefaultToCodeFlowWithRefresh;
    }

    public String getDeviceAuthzEndpoint() {
        return deviceAuthzEndpoint;
    }

    public void setDeviceAuthzEndpoint(String deviceAuthzEndpoint) {
        this.deviceAuthzEndpoint = deviceAuthzEndpoint;
    }

    public int getDeviceAuthzRequestExpiresIn() {
        return deviceAuthzRequestExpiresIn;
    }

    public void setDeviceAuthzRequestExpiresIn(int deviceAuthzRequestExpiresIn) {
        this.deviceAuthzRequestExpiresIn = deviceAuthzRequestExpiresIn;
    }

    public int getDeviceAuthzTokenPollInterval() {
        return deviceAuthzTokenPollInterval;
    }

    public void setDeviceAuthzTokenPollInterval(int deviceAuthzTokenPollInterval) {
        this.deviceAuthzTokenPollInterval = deviceAuthzTokenPollInterval;
    }

    public String getDeviceAuthzResponseTypeToProcessAuthz() {
        return deviceAuthzResponseTypeToProcessAuthz;
    }

    public void setDeviceAuthzResponseTypeToProcessAuthz(String deviceAuthzResponseTypeToProcessAuthz) {
        this.deviceAuthzResponseTypeToProcessAuthz = deviceAuthzResponseTypeToProcessAuthz;
    }

    public Boolean getRequestUriHashVerificationEnabled() {
        return requestUriHashVerificationEnabled != null ? requestUriHashVerificationEnabled : false;
    }

    public void setRequestUriHashVerificationEnabled(Boolean requestUriHashVerificationEnabled) {
        this.requestUriHashVerificationEnabled = requestUriHashVerificationEnabled;
    }

    public Boolean getIdTokenFilterClaimsBasedOnAccessToken() {
        return idTokenFilterClaimsBasedOnAccessToken != null ? idTokenFilterClaimsBasedOnAccessToken : false;
    }

    public void setIdTokenFilterClaimsBasedOnAccessToken(Boolean idTokenFilterClaimsBasedOnAccessToken) {
        this.idTokenFilterClaimsBasedOnAccessToken = idTokenFilterClaimsBasedOnAccessToken;
    }

    public String getMtlsAuthorizationEndpoint() {
        return mtlsAuthorizationEndpoint;
    }

    public void setMtlsAuthorizationEndpoint(String mtlsAuthorizationEndpoint) {
        this.mtlsAuthorizationEndpoint = mtlsAuthorizationEndpoint;
    }

    public String getMtlsTokenEndpoint() {
        return mtlsTokenEndpoint;
    }

    public void setMtlsTokenEndpoint(String mtlsTokenEndpoint) {
        this.mtlsTokenEndpoint = mtlsTokenEndpoint;
    }

    public String getMtlsTokenRevocationEndpoint() {
        return mtlsTokenRevocationEndpoint;
    }

    public void setMtlsTokenRevocationEndpoint(String mtlsTokenRevocationEndpoint) {
        this.mtlsTokenRevocationEndpoint = mtlsTokenRevocationEndpoint;
    }

    public String getMtlsUserInfoEndpoint() {
        return mtlsUserInfoEndpoint;
    }

    public void setMtlsUserInfoEndpoint(String mtlsUserInfoEndpoint) {
        this.mtlsUserInfoEndpoint = mtlsUserInfoEndpoint;
    }

    public String getMtlsClientInfoEndpoint() {
        return mtlsClientInfoEndpoint;
    }

    public void setMtlsClientInfoEndpoint(String mtlsClientInfoEndpoint) {
        this.mtlsClientInfoEndpoint = mtlsClientInfoEndpoint;
    }

    public String getMtlsCheckSessionIFrame() {
        return mtlsCheckSessionIFrame;
    }

    public void setMtlsCheckSessionIFrame(String mtlsCheckSessionIFrame) {
        this.mtlsCheckSessionIFrame = mtlsCheckSessionIFrame;
    }

    public String getMtlsEndSessionEndpoint() {
        return mtlsEndSessionEndpoint;
    }

    public void setMtlsEndSessionEndpoint(String mtlsEndSessionEndpoint) {
        this.mtlsEndSessionEndpoint = mtlsEndSessionEndpoint;
    }

    public String getMtlsJwksUri() {
        return mtlsJwksUri;
    }

    public void setMtlsJwksUri(String mtlsJwksUri) {
        this.mtlsJwksUri = mtlsJwksUri;
    }

    public String getMtlsRegistrationEndpoint() {
        return mtlsRegistrationEndpoint;
    }

    public void setMtlsRegistrationEndpoint(String mtlsRegistrationEndpoint) {
        this.mtlsRegistrationEndpoint = mtlsRegistrationEndpoint;
    }

    public String getMtlsIdGenerationEndpoint() {
        return mtlsIdGenerationEndpoint;
    }

    public void setMtlsIdGenerationEndpoint(String mtlsIdGenerationEndpoint) {
        this.mtlsIdGenerationEndpoint = mtlsIdGenerationEndpoint;
    }

    public String getMtlsIntrospectionEndpoint() {
        return mtlsIntrospectionEndpoint;
    }

    public void setMtlsIntrospectionEndpoint(String mtlsIntrospectionEndpoint) {
        this.mtlsIntrospectionEndpoint = mtlsIntrospectionEndpoint;
    }

    public String getMtlsParEndpoint() {
        return mtlsParEndpoint;
    }

    public void setMtlsParEndpoint(String mtlsParEndpoint) {
        this.mtlsParEndpoint = mtlsParEndpoint;
    }

    public String getMtlsDeviceAuthzEndpoint() {
        return mtlsDeviceAuthzEndpoint;
    }

    public void setMtlsDeviceAuthzEndpoint(String mtlsDeviceAuthzEndpoint) {
        this.mtlsDeviceAuthzEndpoint = mtlsDeviceAuthzEndpoint;
    }
}